package com.tianhu.app

import android.app.Application
import com.tianhu.app.network.ApiClient

class FreshIDApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        NetworkMonitor.init(this)
    }
}
