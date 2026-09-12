package com.rfragancias.app

import android.app.Application
import com.rfragancias.app.network.ApiClient

class RFraganciasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
