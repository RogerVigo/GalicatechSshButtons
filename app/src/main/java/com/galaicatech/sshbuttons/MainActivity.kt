package com.galaicatech.sshbuttons

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.galaicatech.sshbuttons.ads.AdsConfig
import com.galaicatech.sshbuttons.ui.MainScreen
import com.galaicatech.sshbuttons.ui.theme.SshButtonsTheme

class MainActivity : ComponentActivity() {

    private var adsAllowed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAdsConsent()

        setContent {
            SshButtonsTheme {
                MainScreen(adsAllowed = adsAllowed)
            }
        }
    }

    private fun requestAdsConsent() {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val usingTestAds = getString(R.string.ad_banner_unit_id) == AdsConfig.TEST_BANNER_UNIT_ID
        if (debuggable && usingTestAds) {
            adsAllowed = true
            return
        }

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        adsAllowed = consentInformation.canRequestAds()

        val parameters = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            this,
            parameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { _ ->
                    adsAllowed = consentInformation.canRequestAds()
                }
            },
            {
                adsAllowed = consentInformation.canRequestAds()
            }
        )
    }
}
