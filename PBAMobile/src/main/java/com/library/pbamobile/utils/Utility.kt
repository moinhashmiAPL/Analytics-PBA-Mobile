package com.library.pbamobile.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


object Utility {

    fun hasPermission(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun getScreenResolution(context: Context):String{
        val activity=context as Activity
        val height:Int=getScreenHeight(activity)
        val width:Int=getScreenWidth(activity)

        return "$height x $width"
    }

    fun getScreenWidth(activity: Activity): Int {
        val displayMetrics = activity.resources.displayMetrics
        return displayMetrics.widthPixels
    }

    fun getScreenHeight(activity: Activity): Int {
        val displayMetrics = activity.resources.displayMetrics
        return displayMetrics.heightPixels
    }

    fun getScreenDensity(context: Context):Int{
        val metrics: DisplayMetrics = context.resources.displayMetrics
        return (metrics.density * 160f).toInt()
    }

}