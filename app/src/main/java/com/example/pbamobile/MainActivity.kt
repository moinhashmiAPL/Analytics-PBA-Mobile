package com.example.pbamobile

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.library.pbamobile.PBAManager

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

        buttonDeviceInfo.setOnClickListener {view->
            PBAManager.fetchDeviceInfo(view.context,{
                Log.i("DeviceINfo","DataReceived: $it")
            },{
                Log.e("DeviceINfo","exception: $it")
            })
        }

        buttonLocation.setOnClickListener{view->
            PBAManager.fetchLocation(view.context,{
                Log.i("DeviceLocation","DataReceived: $it")
            },{
                Log.e("DeviceLocation","exception: $it")
            })
        }

        buttonApplicationList.setOnClickListener{view->
            PBAManager.fetchApplicationList(view.context, {
                Log.i("INSTALLED_APPS", "Received List Size: ${it.size}")
                it.forEach {
                    Log.i("INSTALLED_APPS", "Installed App: ${it}")
                }
            },{
                Log.e("INSTALLED_APPS", "exception: $it")
            })
        }

        buttonUsageStats.setOnClickListener{view->
            PBAManager.fetchAppTimeUsage(view.context,
                System.currentTimeMillis() - (1000*3600*24),
                System.currentTimeMillis(),{
                    Log.i("USAGE_STATS", "Received Stats List Size: ${it.size}")
                    it.forEach {
                        Log.i("USAGE_STATS", "Installed App Usage Stats: ${it.appName}")
                    }

            },{
                    Log.e("USAGE_STATS", "exception: $it")
            })
        }

        buttonContact.setOnClickListener{view->

            PBAManager.fetchContactList(view.context,{
                Log.i("CONTACT_LIST", "Contact List Size: ${it.size}")
                it.forEach {
                    Log.i("CONTACT_LIST", "Contact: ${it}")
                }

            },{
                Log.e("CONTACT_LIST", "exception: $it")
            })
        }

    }
}