package com.example.pbamobile

import android.app.usage.UsageStats
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.library.pbamobile.PBAManager
import com.library.pbamobile.callback.PBACallbackListener
import com.library.pbamobile.model.InstalledApplication
import com.library.pbamobile.model.PhoneContact
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    lateinit var buttonDeviceInfo:Button
    lateinit var buttonLocation:Button
    lateinit var buttonApplicationList:Button
    lateinit var buttonUsageStats:Button
    lateinit var buttonContact:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonDeviceInfo=findViewById(R.id.buttonDeviceInfo)
        buttonLocation=findViewById(R.id.buttonLocation)
        buttonApplicationList=findViewById(R.id.buttonApplicationList)
        buttonUsageStats=findViewById(R.id.buttonUsageStats)
        buttonContact=findViewById(R.id.buttonContact)

        buttonDeviceInfo.setOnClickListener {
            PBAManager.fetchDeviceInfo(it.context,object : PBACallbackListener {
                override fun onSuccess(data: Any) {
                    Log.i("DeviceINfo","DataReceived: $data")
                }

                override fun onFailure(e: Exception) {
                    Log.e("DeviceINfo","exception: $e")
                }

            })
        }

        buttonLocation.setOnClickListener{
            PBAManager.fetchLocation(it.context,object: PBACallbackListener{
                override fun onSuccess(data: Any) {
                    Log.i("DeviceLocation","DataReceived: $data")
                }

                override fun onFailure(e: Exception) {
                    Log.e("DeviceLocation","exception: $e")
                }

            })
        }

        buttonApplicationList.setOnClickListener{
            PBAManager.fetchApplicationList(it.context, object : PBACallbackListener {
                override fun onSuccess(data: Any) {
                    val list = data as ArrayList<InstalledApplication>
                    Log.i("INSTALLED_APPS", "Received List Size: ${list.size}")

                    list.forEach {
                        Log.i("INSTALLED_APPS", "Installed App: ${it.appName}")
                    }
                }

                override fun onFailure(e: Exception) {
                    Log.e("INSTALLED_APPS", "exception: $e")
                }

            })
        }

        buttonUsageStats.setOnClickListener{
            PBAManager.fetchAppTimeUsage(it.context,System.currentTimeMillis() - (1000*3600*24), object : PBACallbackListener {
                override fun onSuccess(data: Any) {
                    val list = data as ArrayList<UsageStats>
                    Log.i("USAGE_STATS", "Received Stats List Size: ${list.size}")

                    list.forEach {
                        Log.i("USAGE_STATS", "Installed App Usage Stats: ${it.packageName}")
                    }
                }

                override fun onFailure(e: Exception) {
                    Log.e("USAGE_STATS", "exception: $e")
                }

            })
        }

        buttonContact.setOnClickListener{



            PBAManager.fetchContactList(it.context,object: PBACallbackListener{
                override fun onSuccess(data: Any) {
                    val list = data as ArrayList<PhoneContact>
                    Log.i("CONTACT_LIST", "Contact List Size: ${list.size}")

                    list.forEach {
                        Log.i("CONTACT_LIST", "Contact: ${it.name}")
                    }
                }

                override fun onFailure(e: Exception) {
                    Log.e("CONTACT_LIST", "exception: $e")
                }

            })
        }

    }
}