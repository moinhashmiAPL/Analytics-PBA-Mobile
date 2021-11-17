package com.library.pbamobile.utils

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ExternalPermissionManager(registry: ActivityResultRegistry?) {
    private val enabled = MutableLiveData(false)

    private val getPermission = registry?.register(REGISTRY_KEY,
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        enabled.value = granted
    }

    fun requestPermission(permission:String): LiveData<Boolean> {
        getPermission?.launch(permission)
        return enabled
    }

    companion object {
        private const val REGISTRY_KEY = "Get Permission"
    }
}