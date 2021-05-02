package com.android.forceupdate.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorFilter
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.widget.ImageViewCompat
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
        if (viewModel.getLocalFile().exists()) customView(START_INSTALL) else customView(START_UPDATE)
    }

    private fun customView(state: ViewState) {
        when (state) {
            START_UPDATE -> {
                showPackageInfo()
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
                showPackageInfo()
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

    private fun showPackageInfo() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)

        if (intent.getBooleanExtra(EXTRA_OPTIONAL_DOWNLOAD, false))
             binding.optional.visibility = View.VISIBLE else binding.optional.visibility = View.GONE

        val versionName = packageInfo.versionName
        val versionCode = if (SDK_INT >= P) packageInfo.longVersionCode else packageInfo.versionCode
        binding.currentVersion.text = getString(R.string.forceupdate_current_version, versionCode.toString(), versionName)

        val applicationLogo = packageManager.getApplicationIcon(packageInfo.packageName)
        binding.logo.setImageDrawable(applicationLogo)

        val applicationName = packageManager.getApplicationLabel(packageInfo.applicationInfo)
        binding.message.text = getString(R.string.forceupdate_update_message, applicationName)
        binding.applicationName.text = applicationName

        obtainStyledAttributes(TypedValue().data, intArrayOf(R.attr.colorPrimary)).use {
            val color = it.getColor(0, 0)
            binding.button.setTextColor(Color.WHITE)
            binding.button.background.setTint(color)
            ImageViewCompat.setImageTintList(binding.optional, ColorStateList.valueOf(color))
        }

        binding.optional.setOnClickListener {
            finish()
        }
    }

    private fun downloadApk() {
        lifecycleScope.launch(Dispatchers.Main) {
            val header = intent.getSerializableExtra(EXTRA_HEADER) as? Pair<String, String>
            intent.getStringExtra(EXTRA_APK_LINK)?.let { apkLink ->
                viewModel.downloadApk(apkLink, header).collect { downloadStatus ->
                    when (downloadStatus) {
                        is DownloadCanceled -> {
                            Snackbar.make(binding.root, downloadStatus.message, Snackbar.LENGTH_SHORT).show()
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
                        customView(START_INSTALL)
                    }
                    is InstallFailure -> {
                        Snackbar.make(binding.root, it.message, Snackbar.LENGTH_SHORT).show()
                        customView(START_UPDATE)
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
        const val EXTRA_HEADER = "header"
        const val EXTRA_OPTIONAL_DOWNLOAD = "optional"
    }
}