package com.android.application

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.forceupdate.manager.ForceUpdateManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val forceUpdateManager = ForceUpdateManager(activity = this)

        forceUpdateManager.updateApplication(
            apkLink   = "https://storage.evozi.com/apk/dl/16/09/04/com.spotify.music_82316170.apk",
            header    = null,
            optional  = false,
            animation = null
        )
    }
}