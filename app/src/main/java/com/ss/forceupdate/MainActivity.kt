package com.ss.forceupdate

import android.app.Activity
import android.app.DownloadManager
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ss.forceupdate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var forceUpdate: ForceUpdate
    private lateinit var intentFilter: IntentFilter
    val PACKAGE_INSTALLED_ACTION = "com.ss.forceupdate.SESSION_API_PACKAGE_INSTALLED"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        forceUpdate = ForceUpdate(this, this)
        intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        forceUpdate.isApplicationUpdated("1.1")
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(forceUpdate.downloadBroadCastReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(forceUpdate.downloadBroadCastReceiver)
    }
}