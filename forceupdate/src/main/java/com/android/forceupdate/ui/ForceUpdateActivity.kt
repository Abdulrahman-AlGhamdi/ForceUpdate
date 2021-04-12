package com.android.forceupdate.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.forceupdate.R
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus.*
import com.android.forceupdate.databinding.ActivityForceUpdateBinding
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.DownloadStatus.*
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ForceUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForceUpdateBinding
    private val viewModel: ForceUpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForceUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        requestUpdate()
    }

    private fun requestUpdate() {
        binding.apply {
            update.visibility = View.VISIBLE
            message.text = getString(R.string.forceupdate_update)
            progressBar.visibility = View.GONE
            downloaded.visibility = View.GONE

            title.text = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.forceupdate_new_update)
            message.text = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.forceupdate)
            update.text = getString(R.string.forceupdate_update)

            if (intent.getIntExtra(EXTRA_LOGO_IMAGE, 0) != 0) {
                logo.visibility = View.VISIBLE
                logo.setImageResource(intent.getIntExtra(EXTRA_LOGO_IMAGE, 0))
            } else binding.logo.visibility = View.GONE

            applicationName.text = intent.getStringExtra(EXTRA_APPLICATION_NAME) ?: getString(R.string.forceupdate)
            update.setOnClickListener {
                downloadApk()
            }
        }
    }

    private fun downloadApk() {
        binding.update.visibility = View.GONE
        binding.message.text = getString(R.string.forceupdate_downloading)
        binding.progressBar.visibility = View.VISIBLE
        binding.downloaded.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.Main) {
            intent.getStringExtra(EXTRA_APK_LINK)?.let { apkLink ->
                viewModel.downloadApk(apkLink).collect {
                    when (it) {
                        DownloadCanceled -> {
                            binding.message.text = getString(R.string.forceupdate_canceled)
                            requestUpdate()
                        }
                        is DownloadCompleted -> {
                            binding.message.text = getString(R.string.forceupdate_completed)
                            installApk(it.localFile)
                        }
                        is DownloadProgress -> {
                            binding.progressBar.progress = it.progress
                            binding.progressBar.max = 100
                            binding.downloaded.text = getString(R.string.forceupdate_percentage, it.progress)
                        }
                    }
                }
            }
        }
    }

    private fun installApk(localFile: File) {
        lifecycleScope.launchWhenStarted {
            viewModel.installApk(localFile).collect {
                when (it) {
                    InstallCanceled -> {
                        requestUpdate()
                    }
                    is InstallError -> {
                        Snackbar.make(binding.root, it.message, Snackbar.LENGTH_SHORT).show()
                        requestUpdate()
                    }
                    InstallSucceeded -> {
                        finish()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {}

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_APK_LINK = "link"
        const val EXTRA_LOGO_IMAGE = "logo"
        const val EXTRA_APPLICATION_NAME = "application name"
    }
}