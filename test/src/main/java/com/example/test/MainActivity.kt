package com.example.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.forceupdate.manager.ForceUpdateManager
import com.example.test.BuildConfig.VERSION_CODE
import com.example.test.BuildConfig.VERSION_NAME

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val forceUpdateManager = ForceUpdateManager(activity = this)

        BuildConfig.APPLICATION_ID

        if (forceUpdateManager.checkAppVersion(2)) {
            forceUpdateManager.updateApplication(
                apkLink = "https://storage.evozi.com/apk/dl/16/09/04/com.soundcloud.android_65060.apk",
                logo = R.drawable.ic_launcher_background,
                versionCode = VERSION_CODE,
                versionName = VERSION_NAME
            )
        }

//        forceUpdateManager.destroyApplication()
    }
}