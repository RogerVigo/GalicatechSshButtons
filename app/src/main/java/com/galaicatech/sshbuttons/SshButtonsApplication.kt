package com.galaicatech.sshbuttons

import android.app.Application
import com.google.android.gms.ads.MobileAds

class SshButtonsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
