package com.library.pbamobile.callback

import java.lang.Exception

interface PBACallbackListener {
    fun onSuccess(data: Any)
    fun onFailure(e:Exception)
}