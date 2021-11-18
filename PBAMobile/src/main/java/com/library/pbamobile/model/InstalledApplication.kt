package com.library.pbamobile.model

data class InstalledApplication(val appName:String,val appIcon:Int, val packageName:String,
                                val versionName:String,val versionCode:Long,val installationTime:Long,
                                val lastUpdate:Long)
