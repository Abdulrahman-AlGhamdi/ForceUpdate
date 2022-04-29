package com.android.forceupdate.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AlertDialog
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
import com.android.forceupdate.util.ConstantsUtils
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
    private lateinit var applicationLogo: Drawable

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
        applicationLogo = packageManager.getApplicationIcon(packageInfo.packageName)

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

        if (viewModel.getApkFile().exists()) setForceUpdateView(InstallReady())
        else setForceUpdateView(DownloadReady())
    }

    private fun checkPackageInstallPermission(action: () -> (Any)) {
        if (!packageManager.canRequestPackageInstalls()) AlertDialog.Builder(this).apply {
            this.setTitle(applicationName)
            this.setIcon(applicationLogo)
            this.setMessage(getString(R.string.forceupdate_permission_message, applicationName))
            this.setPositiveButton(R.string.forceupdate_permission_ok) { _, _ ->
                val settings = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                startActivity(Intent(settings, Uri.parse("package:$packageName")))
            }
            this.show()
        } else action()
    }

    private fun getDownloadStatus() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.downloadStatus.collect {
            when (it) {
                is DownloadCompleted -> setForceUpdateView(InstallReady())
                is DownloadProgress -> setForceUpdateView(Downloading(it.progress))
                is DownloadCanceled -> setForceUpdateView(DownloadReady(it.reason))
                else -> Unit
            }
        }
    }

    private fun getInstallStatus() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.installStatus.collect {
            when (it) {
                InstallSucceeded -> finish()
                InstallProgress -> setForceUpdateView(Installing)
                InstallCanceled -> setForceUpdateView(InstallReady())
                is InstallFailure -> setForceUpdateView(DownloadReady(it.message))
                else -> Unit
            }
        }
    }

    private fun setForceUpdateView(state: ForceUpdateState): Unit = when (state) {
        is DownloadReady -> {
            binding.button.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.message.text = getString(R.string.forceupdate_update_message, applicationName)
            binding.button.text = getString(R.string.forceupdate_update)
            state.message?.let { binding.root.showSnackBar(it) }

            val header = intent.getSerializableExtra(ConstantsUtils.EXTRA_HEADER) as? Pair<*, *>
            val apkLink = intent.getStringExtra(ConstantsUtils.EXTRA_APK_LINK) ?: ""

            binding.button.setOnClickListener {
                checkPackageInstallPermission { viewModel.downloadApk(apkLink, header) }
            }
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

            binding.button.setOnClickListener {
                checkPackageInstallPermission { viewModel.installApk() }
            }
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
}