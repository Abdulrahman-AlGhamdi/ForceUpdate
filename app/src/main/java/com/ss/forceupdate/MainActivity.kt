package com.ss.forceupdate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ss.forceupdate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ForceUpdateManager(
            activity = this,
            context = applicationContext,
            newVersion = "1.1",
            apkLink = "https://storage.evozi.com/apk/dl/16/09/04/com.soundcloud.android_65060.apk",
            title = "New Update",
            message = "New Update Available",
            logo = R.drawable.google_play_logo,
            applicationName = getString(R.string.app_name)
            ).isApplicationUpdated()
    }
}