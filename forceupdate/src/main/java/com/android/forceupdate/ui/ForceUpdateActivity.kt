package com.android.forceupdate.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.forceupdate.R
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus.*
import com.android.forceupdate.databinding.ActivityForceUpdateBinding
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.DownloadStatus.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

internal class ForceUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForceUpdateBinding
    private lateinit var viewModel: ForceUpdateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForceUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        init()
        requestUpdate()
    }

    private fun init() {
        val factory = ForceUpdateProviderFactory(ForceUpdateRepositoryImpl(this))
        viewModel = ViewModelProvider(this, factory)[ForceUpdateViewModel::class.java]
    }

    private fun requestUpdate() {
        binding.apply {
            button.visibility = View.VISIBLE
            downloaded.visibility = View.GONE
            progressBar.visibility = View.GONE
            message.text = getString(R.string.forceupdate_update)
            button.text = getString(R.string.forceupdate_update)
            title.text = getString(R.string.forceupdate_new_update)

            if (intent.getStringExtra(EXTRA_VERSION_NAME) != null)
                versionName.text = getString(R.string.forceupdate_version_name, intent.getStringExtra(EXTRA_VERSION_NAME))
            else versionName.visibility = View.GONE

            if (intent.getIntExtra(EXTRA_VERSION_CODE, 0) != 0)
                versionCode.text = getString(R.string.forceupdate_version_code, intent.getIntExtra(EXTRA_VERSION_CODE, 0))
            else versionCode.visibility = View.GONE

            if (intent.getIntExtra(EXTRA_LOGO_IMAGE, 0) != 0)
                logo.setImageResource(intent.getIntExtra(EXTRA_LOGO_IMAGE, 0))
            else logo.visibility = View.GONE

            if (intent.getStringExtra(EXTRA_APPLICATION_NAME) != null) {
                val name = intent.getStringExtra(EXTRA_APPLICATION_NAME)
                message.text = getString(R.string.forceupdate_update_message_name, name)
                applicationName.text = intent.getStringExtra(EXTRA_APPLICATION_NAME)
            } else {
                message.text = getString(R.string.forceupdate_update_message)
                applicationName.text = getString(R.string.forceupdate_new_update)
            }

            button.setOnClickListener {
                downloadApk()
            }
        }
    }

    private fun downloadApk() {
        lifecycleScope.launch(Dispatchers.Main) {
            intent.getStringExtra(EXTRA_APK_LINK)?.let { apkLink ->
                viewModel.downloadApk(apkLink).collect { downloadStatus ->
                    when (downloadStatus) {
                        DownloadCanceled -> {
                            binding.message.text = getString(R.string.forceupdate_canceled)
                            requestUpdate()
                        }
                        is DownloadCompleted -> {
                            binding.button.visibility = View.VISIBLE
                            binding.downloaded.visibility = View.GONE
                            binding.progressBar.visibility = View.GONE
                            binding.button.text = getString(R.string.forceupdate_install)
                            binding.message.text = getString(R.string.forceupdate_download_completed)
                            binding.button.setOnClickListener { installApk(viewModel.getLocalFile()) }
                        }
                        is DownloadProgress -> {
                            binding.button.visibility = View.GONE
                            binding.progressBar.visibility = View.VISIBLE
                            binding.downloaded.visibility = View.VISIBLE
                            binding.message.text = getString(R.string.forceupdate_downloading)
                            binding.progressBar.progress = downloadStatus.progress
                            binding.progressBar.max = 100
                            binding.downloaded.text = getString(R.string.forceupdate_download_percentage, downloadStatus.progress)
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
                        binding.button.setOnClickListener { installApk(viewModel.getLocalFile()) }
                    }
                    is InstallError -> {
                        Snackbar.make(binding.root, it.message, Snackbar.LENGTH_SHORT).show()
                        binding.button.setOnClickListener { installApk(viewModel.getLocalFile()) }
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
        const val EXTRA_APK_LINK = "link"
        const val EXTRA_LOGO_IMAGE = "logo"
        const val EXTRA_VERSION_NAME = "version name"
        const val EXTRA_VERSION_CODE = "version code"
        const val EXTRA_APPLICATION_NAME = "application name"
    }
}