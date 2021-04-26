package com.android.forceupdate.ui

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
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
        showPackageInfo()
        if (viewModel.getLocalFile().exists()) customView(START_INSTALL) else customView(START_UPDATE)
    }

    private fun showPackageInfo() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)

        val versionName = packageInfo.versionName
        val versionCode = if (SDK_INT >= P) packageInfo.longVersionCode else packageInfo.versionCode
        binding.currentVersion.text = getString(R.string.forceupdate_current_version, versionCode.toString(), versionName)

        val applicationLogo = packageManager.getApplicationIcon(packageInfo.packageName)
        binding.logo.setImageDrawable(applicationLogo)

        val applicationName = packageManager.getApplicationLabel(packageInfo.applicationInfo)
        binding.message.text = getString(R.string.forceupdate_update_message, applicationName)
        binding.applicationName.text = applicationName
    }

    private fun customView(state: ViewState) {
        when (state) {
            START_UPDATE -> {
                binding.button.visibility = View.VISIBLE
                binding.downloaded.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.button.text = getString(R.string.forceupdate_update)
                binding.title.text = getString(R.string.forceupdate_new_update)
                binding.button.setOnClickListener { downloadApk() }
            }
            START_DOWNLOAD -> {
                binding.button.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                binding.downloaded.visibility = View.VISIBLE
                binding.message.text = getString(R.string.forceupdate_downloading)
                binding.progressBar.max = 100
            }
            START_INSTALL -> {
                binding.button.visibility = View.VISIBLE
                binding.downloaded.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.button.text = getString(R.string.forceupdate_install)
                binding.title.text = getString(R.string.forceupdate_new_update)
                binding.message.text = getString(R.string.forceupdate_download_completed)
                binding.button.setOnClickListener { installApk(viewModel.getLocalFile()) }
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
                            customView(START_UPDATE)
                        }
                        is DownloadCompleted -> {
                            customView(START_INSTALL)
                        }
                        is DownloadProgress -> {
                            customView(START_DOWNLOAD)
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

    override fun onBackPressed() {}

    enum class ViewState {
        START_UPDATE,
        START_DOWNLOAD,
        START_INSTALL
    }

    companion object {
        const val EXTRA_APK_LINK = "link"
    }
}