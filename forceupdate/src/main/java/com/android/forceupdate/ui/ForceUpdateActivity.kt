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
import com.android.forceupdate.ui.ForceUpdateActivity.ViewState.*
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
    }

    private fun init() {
        val factory = ForceUpdateProviderFactory(ForceUpdateRepositoryImpl(this))
        viewModel = ViewModelProvider(this, factory)[ForceUpdateViewModel::class.java]
        if (viewModel.getLocalFile().exists()) customView(DOWNLOAD_COMPLETED) else customView(UPDATE_STATE)
    }

    private fun requestUpdate() {
        if (intent.getStringExtra(EXTRA_VERSION_NAME) != null)
            binding.versionName.text = getString(R.string.forceupdate_version_name, intent.getStringExtra(EXTRA_VERSION_NAME))
        else binding.versionName.visibility = View.GONE

        if (intent.getIntExtra(EXTRA_VERSION_CODE, 0) != 0)
            binding.versionCode.text = getString(R.string.forceupdate_version_code, intent.getIntExtra(EXTRA_VERSION_CODE, 0))
        else binding.versionCode.visibility = View.GONE

        if (intent.getIntExtra(EXTRA_LOGO_IMAGE, 0) != 0)
            binding.logo.setImageResource(intent.getIntExtra(EXTRA_LOGO_IMAGE, 0))
        else binding.logo.visibility = View.GONE

        if (intent.getStringExtra(EXTRA_APPLICATION_NAME) != null) {
            val name = intent.getStringExtra(EXTRA_APPLICATION_NAME)
            binding.message.text = getString(R.string.forceupdate_update_message_name, name)
            binding.applicationName.text = intent.getStringExtra(EXTRA_APPLICATION_NAME)
        } else {
            binding.message.text = getString(R.string.forceupdate_update_message)
            binding.applicationName.text = getString(R.string.forceupdate_new_update)
        }

        binding.button.setOnClickListener {
                downloadApk()
        }
    }

    private fun downloadApk() {
        lifecycleScope.launch(Dispatchers.Main) {
            intent.getStringExtra(EXTRA_APK_LINK)?.let { apkLink ->
                viewModel.downloadApk(apkLink).collect { downloadStatus ->
                    when (downloadStatus) {
                        DownloadCanceled -> {
                            binding.message.text = getString(R.string.forceupdate_canceled)
                            customView(UPDATE_STATE)
                        }
                        is DownloadCompleted -> {
                            customView(DOWNLOAD_COMPLETED)
                        }
                        is DownloadProgress -> {
                            customView(DOWNLOAD_PROGRESS)
                            binding.progressBar.progress = downloadStatus.progress
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

    private fun customView(state: ViewState) {
        when (state) {
            UPDATE_STATE -> {
                binding.button.visibility = View.VISIBLE
                binding.downloaded.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.button.text = getString(R.string.forceupdate_update)
                binding.message.text = getString(R.string.forceupdate_update)
                binding.title.text = getString(R.string.forceupdate_new_update)
                requestUpdate()
            }
            DOWNLOAD_PROGRESS -> {
                binding.button.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                binding.downloaded.visibility = View.VISIBLE
                binding.message.text = getString(R.string.forceupdate_downloading)
                binding.progressBar.max = 100
            }
            DOWNLOAD_COMPLETED -> {
                binding.button.visibility = View.VISIBLE
                binding.downloaded.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.button.text = getString(R.string.forceupdate_install)
                binding.message.text = getString(R.string.forceupdate_download_completed)
                binding.button.setOnClickListener { installApk(viewModel.getLocalFile()) }
            }
        }
    }

    override fun onBackPressed() {}

    enum class ViewState {
        UPDATE_STATE,
        DOWNLOAD_PROGRESS,
        DOWNLOAD_COMPLETED
    }

    companion object {
        const val EXTRA_APK_LINK = "link"
        const val EXTRA_LOGO_IMAGE = "logo"
        const val EXTRA_VERSION_NAME = "version name"
        const val EXTRA_VERSION_CODE = "version code"
        const val EXTRA_APPLICATION_NAME = "application name"
    }
}