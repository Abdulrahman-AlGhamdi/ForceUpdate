package com.android.forceupdate.ui

import android.content.pm.PackageInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.use
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.forceupdate.R
import com.android.forceupdate.databinding.ActivityForceUpdateBinding
import com.android.forceupdate.repository.download.DownloadRepositoryImpl
import com.android.forceupdate.repository.download.DownloadRepositoryImpl.DownloadStatus.*
import com.android.forceupdate.repository.install.InstallRepositoryImpl
import com.android.forceupdate.repository.install.InstallRepositoryImpl.InstallStatus.*
import com.android.forceupdate.ui.ForceUpdateActivity.ForceUpdateState.*
import com.android.forceupdate.util.getAppVersion
import com.android.forceupdate.util.showSnackBar
import com.android.forceupdate.util.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class ForceUpdateActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityForceUpdateBinding::inflate)
    private lateinit var viewModel: ForceUpdateViewModel

    private lateinit var packageInfo: PackageInfo
    private lateinit var applicationName: CharSequence

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()

        init()
        getDownloadStatus()
        getInstallStatus()
    }

    private fun init() {
        packageInfo = packageManager.getPackageInfo(packageName, 0)
        applicationName = packageManager.getApplicationLabel(packageInfo.applicationInfo)
        val applicationLogo = packageManager.getApplicationIcon(packageInfo.packageName)

        val installRepository = InstallRepositoryImpl(this)
        val downloadRepository = DownloadRepositoryImpl(this)
        val factory = ForceUpdateProviderFactory(downloadRepository, installRepository)
        viewModel = ViewModelProvider(this, factory)[ForceUpdateViewModel::class.java]

        binding.logo.setImageDrawable(applicationLogo)
        binding.applicationName.text = applicationName
        binding.currentVersion.text = packageInfo.getAppVersion(this)
        binding.optional.visibility = viewModel.getIsOptional(intent)
        binding.optional.setOnClickListener { finish() }
        binding.animation.setAnimation(viewModel.getForceUpdateAnimation(intent))

        obtainStyledAttributes(TypedValue().data, intArrayOf(R.attr.colorPrimary)).use {
            val color = it.getColor(0, 0)
            binding.button.setTextColor(Color.WHITE)
            binding.button.background.setTint(color)
            ImageViewCompat.setImageTintList(binding.optional, ColorStateList.valueOf(color))
        }

        if (viewModel.getLocalFile().exists()) getForceUpdateState(InstallReady())
        else getForceUpdateState(DownloadReady())
    }

    private fun getDownloadStatus() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.downloadStatus.collect {
            when (it) {
                is DownloadCompleted -> getForceUpdateState(InstallReady())
                is DownloadProgress -> getForceUpdateState(Downloading(it.progress))
                is DownloadCanceled -> getForceUpdateState(DownloadReady(it.reason))
                else -> Unit
            }
        }
    }

    private fun getInstallStatus() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.installStatus.collect {
            when (it) {
                InstallSucceeded -> finish()
                InstallProgress -> getForceUpdateState(Installing)
                InstallCanceled -> getForceUpdateState(InstallReady())
                is InstallFailure -> getForceUpdateState(DownloadReady(it.message))
                else -> Unit
            }
        }
    }

    private fun getForceUpdateState(state: ForceUpdateState): Unit = when (state) {
        is DownloadReady -> {
            binding.button.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.message.text = getString(R.string.forceupdate_update_message, applicationName)
            binding.button.text = getString(R.string.forceupdate_update)

            val header = intent.getSerializableExtra(EXTRA_HEADER) as? Pair<*, *>
            val apkLink = intent.getStringExtra(EXTRA_APK_LINK)

            state.message?.let { binding.root.showSnackBar(it) }
            binding.button.setOnClickListener { viewModel.downloadApk(apkLink!!, header) }
        }
        is Downloading -> {
            binding.progressBar.isIndeterminate = state.progress == 0
            binding.message.text = getString(R.string.forceupdate_downloading, state.progress)
            binding.button.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.setProgress(state.progress, true)
        }
        is InstallReady -> {
            binding.button.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.button.text = getString(R.string.forceupdate_install)
            binding.message.text = getString(R.string.forceupdate_download_completed)

            binding.button.setOnClickListener { viewModel.installApk(viewModel.getLocalFile()) }
        }
        is Installing -> {
            binding.button.visibility = View.GONE
            binding.progressBar.isIndeterminate = true
            binding.progressBar.visibility = View.VISIBLE
            binding.message.text = getString(R.string.forceupdate_installing)
        }
    }

    override fun onBackPressed() = Unit

    sealed class ForceUpdateState {
        object Installing : ForceUpdateState()
        data class Downloading(val progress: Int) : ForceUpdateState()
        data class InstallReady(val uri: String? = null) : ForceUpdateState()
        data class DownloadReady(val message: String? = null) : ForceUpdateState()
    }

    companion object {
        const val EXTRA_HEADER = "header"
        const val EXTRA_APK_LINK = "link"
        const val EXTRA_ANIMATION = "animation"
        const val EXTRA_OPTIONAL_DOWNLOAD = "optional"
    }
}