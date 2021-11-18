package com.library.pbamobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ContentResolver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArrayMap
import androidx.core.app.AppOpsManagerCompat.MODE_ALLOWED
import androidx.lifecycle.coroutineScope
import com.library.pbamobile.model.*
import com.library.pbamobile.utils.Constant
import com.library.pbamobile.utils.Constant.Companion.BUTTON_CANCEL
import com.library.pbamobile.utils.Constant.Companion.BUTTON_NO
import com.library.pbamobile.utils.Constant.Companion.BUTTON_OK
import com.library.pbamobile.utils.Constant.Companion.BUTTON_YES
import com.library.pbamobile.utils.Constant.Companion.MESSAGE_ALLOW_TIME_USAGE
import com.library.pbamobile.utils.Constant.Companion.MESSAGE_TURN_ON_GPS
import com.library.pbamobile.utils.Constant.Companion.TITLE_ALLOW_FROM_SETTINGS
import com.library.pbamobile.utils.Constant.Companion.TITLE_GPS
import com.library.pbamobile.utils.ExternalPermissionManager
import com.library.pbamobile.utils.Utility
import com.library.pbamobile.utils.Utility.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


object PBAManager {

    private const val TAG="PBAManager"

    enum class DataType{
        DEVICE_INFO,CONTACTS,LOCATION,APPLICATION_LIST,APP_TIME_USAGE,NONE
    }

    var dataType=DataType.NONE

    fun fetchDeviceInfo(context: Context, onSuccess: OnSuccess<DeviceInfo>, onFailure: OnFailure<Exception>?){
        dataType=DataType.DEVICE_INFO
        val activity=context as Activity
        val externalPermissionManager= ExternalPermissionManager((activity as? AppCompatActivity)?.activityResultRegistry!!)

        val lifeCycle=activity.lifecycle
        val scope=lifeCycle.coroutineScope

        if(!hasPermission(context, Manifest.permission.READ_PHONE_STATE)){
            externalPermissionManager.
            requestPermission(Manifest.permission.READ_PHONE_STATE).
            observe(activity){ granted->
                if(granted){
                    scope.launch(Dispatchers.IO){
                        val deviceInfoAsync= async { getDeviceInfo(context) }
                        val deviceInfo=deviceInfoAsync.await()
                        onSuccess(deviceInfo)
                        dataType=DataType.NONE
                    }
                }else{
                    onFailure?.let { it(Exception()) }
                }
            }
        }else{
            scope.launch(Dispatchers.IO){
                val deviceInfoAsync= async { getDeviceInfo(context) }
                val deviceInfo=deviceInfoAsync.await()
                onSuccess(deviceInfo)
            }
        }
    }

    fun fetchLocation(context: Context, onSuccess: OnSuccess<LocationDetail>, onFailure: OnFailure<Exception>?){
        dataType=DataType.LOCATION
        val activity=context as Activity
        val externalPermissionManager=ExternalPermissionManager((activity as? AppCompatActivity)?.activityResultRegistry!!)

        val PERMISSIONS= arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if(hasPermission(context, *PERMISSIONS)){
            getLocation(context, onSuccess, onFailure)
        }else{

            externalPermissionManager
                .requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .observe(activity){ granted->
                    if(granted){
                        if(hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)){
                            getLocation(context, onSuccess, onFailure)
                        }else{
                            externalPermissionManager
                                .requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                                .observe(activity){ _granted->
                                    if(_granted){
                                        getLocation(context, onSuccess, onFailure)
                                    }else{
                                        onFailure?.let { it(Exception(Constant.MESSAGE_LOCATION_PERMISSION_NOT_GRANTED)) }
                                    }
                                }
                        }
                    }else{
                        onFailure?.let { it(Exception(Constant.MESSAGE_LOCATION_PERMISSION_NOT_GRANTED)) }
                    }
                }

        }

    }

    fun fetchApplicationList(context: Context, onSuccess: OnSuccess<ArrayList<InstalledApplication>>,
                             onFailure: OnFailure<Exception>?){
        dataType=DataType.APPLICATION_LIST
        var installedAppList=ArrayList<InstalledApplication>()

        val intent=Intent(ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pm: PackageManager = context.packageManager

        val installedPackageList=context.packageManager.queryIntentActivities(intent, 0)

        if(installedPackageList.isNotEmpty()){

            installedAppList= installedPackageList.map {
                val packageInfo: PackageInfo = pm.getPackageInfo(it.activityInfo.packageName, 0)
                val appName = pm.getApplicationLabel(pm.getApplicationInfo(packageInfo.packageName,
                        PackageManager.GET_META_DATA)) as String

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    InstalledApplication(
                            appName,
                            packageInfo.applicationInfo.icon,
                            packageInfo.packageName,
                            packageInfo.versionName,
                            packageInfo.longVersionCode,
                            packageInfo.firstInstallTime,
                            packageInfo.lastUpdateTime,
                    )
                } else {
                    InstalledApplication(
                            appName,
                            packageInfo.applicationInfo.icon,
                            packageInfo.packageName,
                            packageInfo.versionName,
                            packageInfo.versionCode.toLong(),
                            packageInfo.firstInstallTime,
                            packageInfo.lastUpdateTime,
                    )
                }

            } as ArrayList<InstalledApplication>

            onSuccess(installedAppList)
            dataType=DataType.NONE
        }else{
            onFailure?.let { it(Exception(Constant.MESSAGE_NO_DATA_FOUND)) }
        }

    }

    fun fetchContactList(context: Context, onSuccess: OnSuccess<ArrayList<PhoneContact>>,
                         onFailure: OnFailure<Exception>?){
        dataType=DataType.CONTACTS
        val activity=context as Activity
        val externalPermissionManager=ExternalPermissionManager((activity as? AppCompatActivity)?.activityResultRegistry!!)

        val lifeCycle=activity.lifecycle
        val scope=lifeCycle.coroutineScope

        if(hasPermission(context, Manifest.permission.READ_CONTACTS)){
            scope.launch(Dispatchers.IO) {
                val contactListAsync= async { getContactList(context) }
                val contactList=contactListAsync.await()
                onSuccess(contactList)
                dataType=DataType.NONE
            }
        }else{
            externalPermissionManager.
            requestPermission(Manifest.permission.READ_CONTACTS).
            observe(activity){ granted->
                if(granted){
                    scope.launch {
                        val contactListAsync= async { getContactList(context) }
                        val contactList=contactListAsync.await()
                        onSuccess(contactList)
                        dataType=DataType.NONE
                    }
                }else{
                    onFailure?.let { it(Exception()) }
                }
            }
        }
    }

    fun fetchAppTimeUsage(context: Context, timefrom: Long, timeTo: Long,
                          onSuccess: OnSuccess<ArrayList<AppUsageStats>>,
                          onFailure: OnFailure<Exception>?){
        dataType=DataType.APP_TIME_USAGE
        val activity=context as Activity
        val lifeCycle=(activity as? AppCompatActivity)?.lifecycle
        val scope=lifeCycle?.coroutineScope

        if(getGrantStatus(context)){
            scope?.launch(Dispatchers.IO) {
                val usageStatsAsync= async { getUsageStats(context, timefrom, timeTo) }
                val usageStats=usageStatsAsync.await()
                onSuccess(usageStats)
                dataType=DataType.NONE
            }

        }else{
            showSettingsAlert(
                    context,
                    TITLE_ALLOW_FROM_SETTINGS,
                    MESSAGE_ALLOW_TIME_USAGE,
                    BUTTON_OK,
                    BUTTON_CANCEL,
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
            )
        }

    }


    @SuppressLint("MissingPermission")
    private fun getDeviceInfo(context: Context): DeviceInfo {

        lateinit var deviceInfo: DeviceInfo

        try {
            val actManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            // Declaring MemoryInfo object
            val memInfo = ActivityManager.MemoryInfo()
            // Fetching the data from the ActivityManager
            actManager.getMemoryInfo(memInfo)

            val manufacturer = Build.MANUFACTURER
            val brand=Build.BRAND
            val model = Build.MODEL
            val hardware=Build.HARDWARE
            val serialNo=Build.SERIAL
            val androidId= Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
            )
            val resolution= Utility.getScreenResolution(context)
            val density=Utility.getScreenDensity(context)
            val user=Build.USER
            val host=Build.HOST
            val apiLevel=Build.VERSION.SDK_INT
            val buildID=Build.ID
            val buildTime=Build.TIME
            val securityPatch=Build.VERSION.SECURITY_PATCH
            val bootLoader=Build.BOOTLOADER
            val ram=(memInfo.totalMem.toDouble()/(1024*1024*1024)).roundToInt()

            val subManager: SubscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as (SubscriptionManager)
            var simCount=0
            lateinit var lstOperatorInfo:List<SubscriptionInfo>
            val lstSimOperator=ArrayList<Operator>()

            if(hasPermission(context, Manifest.permission.READ_PHONE_STATE)){
                simCount=subManager.activeSubscriptionInfoCount;
                lstOperatorInfo= subManager.activeSubscriptionInfoList

                lstOperatorInfo.forEach {
                    lstSimOperator.add(Operator(it.carrierName.toString()))
                }

            }



            deviceInfo=DeviceInfo(
                    manufacturer,
                    brand,
                    model,
                    hardware,
                    serialNo,
                    androidId,
                    resolution,
                    density,
                    user,
                    host,
                    apiLevel,
                    buildID,
                    buildTime,
                    securityPatch,
                    bootLoader,
                    "${ram}GB",
                    lstSimOperator,
                    simCount
            )
            return deviceInfo
        }catch (e: Exception){
            Log.e(TAG, e.message.toString())
        }

        return deviceInfo
    }


    @SuppressLint("MissingPermission")
    private fun getLocation(context: Context, onSuccess: OnSuccess<LocationDetail>, onFailure: OnFailure<Exception>?){

        val locationManager:LocationManager  = context.getSystemService(LOCATION_SERVICE) as (LocationManager)

        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)


        if(isGPSEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f
            ) { location ->
                fetchLocationDetail(context, location, onSuccess, onFailure)
                dataType=DataType.NONE
            }

        }else{
            showSettingsAlert(context,TITLE_GPS,MESSAGE_TURN_ON_GPS,BUTTON_YES,BUTTON_NO,
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        }
    }

    private fun fetchLocationDetail(context: Context, location: Location,
                                    onSuccess: OnSuccess<LocationDetail>,
                                    onFailure: OnFailure<Exception>?){
        val activity=context as Activity
        val lifeCycle=(activity as? AppCompatActivity)?.lifecycle
        val scope=lifeCycle?.coroutineScope

        scope?.launch(Dispatchers.IO) {
            val locationDetailAsync= async { getLocationDetail(context, location) }
            val locationDetail=locationDetailAsync.await()
            onSuccess(locationDetail)
            dataType=DataType.NONE
        }

    }

    private fun getLocationDetail(context: Context, location: Location):LocationDetail{

        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)

        var countryName=""
        var locality=""
        var subLocality=""
        var postalCode=""
        var adminArea=""


        addresses?.get(0)?.let {
            countryName=it.countryName?:""
            locality=it.locality?:""
            subLocality=it.subLocality?:""
            postalCode=it.postalCode?:""
            adminArea=it.adminArea?:""
        }

        return LocationDetail(
                location.latitude,
                location.longitude,
                countryName,
                locality,
                subLocality,
                postalCode,
                adminArea
        )

    }

    private fun getUsageStats(
            context: Context,
            timeFrom: Long,
            timeTo: Long
    ): ArrayList<AppUsageStats> {
        val usageStatsManager=  context.getSystemService(Context.USAGE_STATS_SERVICE) as (UsageStatsManager)
        val packageManager=context.packageManager
        val listUsageStats=ArrayList<AppUsageStats>()

        val cal: Calendar = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 0)

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                timeFrom, timeTo
        )

        val map: ArrayMap<String, UsageStats> = ArrayMap()
        val mAppLabelMap:ArrayMap<String, String> = ArrayMap()
        val pm: PackageManager = context.packageManager
        val statCount = stats.size
        for (i in 0 until statCount) {
            val pkgStats = stats[i]

            // load application labels for each application
            try {
                val appInfo: ApplicationInfo = packageManager.getApplicationInfo(
                        pkgStats.packageName,
                        0
                )
                val packageInfo: PackageInfo = pm.getPackageInfo(pkgStats.packageName, 0)
                val appName = pm.getApplicationLabel(pm.getApplicationInfo(packageInfo.packageName,
                    PackageManager.GET_META_DATA)) as String
                val label = appInfo.loadLabel(packageManager).toString()
                mAppLabelMap.put(pkgStats.packageName, label)
                val existingStats: UsageStats? = map.get(pkgStats.packageName)
                if (existingStats == null) {
                    map.put(pkgStats.packageName, pkgStats)

                    listUsageStats.add(AppUsageStats(
                        appName,
                        pkgStats.lastTimeUsed,
                        pkgStats.totalTimeInForeground
                    ))

                } else {
                    existingStats.add(pkgStats)
                    listUsageStats.add(AppUsageStats(
                        appName,
                        pkgStats.lastTimeUsed,
                        pkgStats.totalTimeInForeground
                    ))
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // This package may be gone.
//                callbackListener.onFailure(e)
                Log.e(TAG, e.message.toString())
            }
        }
//        appStats.addAll(map.values)

        listUsageStats.sortBy { it.lastUsed }
        listUsageStats.reverse()
        return listUsageStats

    }



    @SuppressLint("Range")
    private fun getContactList(context: Context):ArrayList<PhoneContact> {
        val contactList=ArrayList<PhoneContact>()
        val cr: ContentResolver = context.contentResolver
        val cur: Cursor? = cr.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
        )

        if ((cur?.count ?: 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                val id: String = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID)
                )
                val name: String = cur.getString(
                        cur.getColumnIndex(
                                ContactsContract.Contacts.DISPLAY_NAME
                        )
                )
                val numberArr=ArrayList<String>()
                val emailArr=ArrayList<String>()
                if (cur.getInt(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER)) > 0) {
                    val pCur: Cursor? = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                    )

                    val emailCur = cr.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", arrayOf(id), null)


                    while (emailCur!!.moveToNext()) {
                        val email = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA))
                        emailArr.add(email) // Here you will get list of email
                    }
                    emailCur.close()


                    while (pCur?.moveToNext() == true) {
                        val phoneNo: String = pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                        numberArr.add(phoneNo)
                    }

                    pCur?.close()
                }
                contactList.add(PhoneContact(name, numberArr, emailArr))
            }
        }
        cur?.close()
        contactList.sortBy { it.name }
        return contactList
    }



    private fun showSettingsAlert(context: Context,title:String,message:String,
                                  positiveButtonTitle:String,negativeButtonTitle:String,
                                  settingIntentName:String) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(context)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton(positiveButtonTitle) { dialog, which ->
            dialog.dismiss()
            context.startActivity(Intent(settingIntentName))
        }
        alertDialog.setNegativeButton(negativeButtonTitle) { dialog, which -> dialog.cancel() }
        alertDialog.show()
    }

    fun getGrantStatus(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
                OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.getPackageName()
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(
                    Manifest.permission.PACKAGE_USAGE_STATS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == MODE_ALLOWED
        }
    }


}