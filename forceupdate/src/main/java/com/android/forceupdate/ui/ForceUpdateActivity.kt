package com.android.forceupdate.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
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
import com.android.forceupdate.util.showSnackBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class ForceUpdateActivity : AppCompatActivity() {

    private lateinit var binding   : ActivityForceUpdateBinding
    private lateinit var viewModel : ForceUpdateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForceUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        init()
        getDownloadStatus()
        getInstallStatus()
    }

    private fun init() {
        showPackageInfo()

        /** init view model **/
        val factory = ForceUpdateProviderFactory(DownloadRepositoryImpl(this), InstallRepositoryImpl(this))
        viewModel = ViewModelProvider(this, factory)[ForceUpdateViewModel::class.java]

        /** check if apk file is exist **/
        if (viewModel.getLocalFile().exists()) getForceUpdateState(InstallReady())
        else getForceUpdateState(DownloadReady())
    }

    private fun showPackageInfo() {
        val packageInfo     = packageManager.getPackageInfo(packageName, 0)
        val versionName     = packageInfo.versionName
        val versionCode     = if (SDK_INT >= P) packageInfo.longVersionCode else packageInfo.versionCode
        val applicationLogo = packageManager.getApplicationIcon(packageInfo.packageName)
        val applicationName = packageManager.getApplicationLabel(packageInfo.applicationInfo)
        val animation       = intent.getStringExtra(EXTRA_ANIMATION)

        binding.currentVersion.text  = getString(R.string.forceupdate_current_version, versionCode.toString(), versionName)
        binding.applicationName.text = applicationName
        binding.logo.setImageDrawable(applicationLogo)

        if (intent.getBooleanExtra(EXTRA_OPTIONAL_DOWNLOAD, false))
            binding.optional.visibility = View.VISIBLE else binding.optional.visibility = View.GONE

        if (!animation.isNullOrEmpty() && animation.endsWith(".json"))
            binding.animation.setAnimation(animation)
        else binding.animation.setAnimation("force_update_animation.json")

        obtainStyledAttributes(TypedValue().data, intArrayOf(R.attr.colorPrimary)).use {
            val color = it.getColor(0, 0)
            binding.button.setTextColor(Color.WHITE)
            binding.button.background.setTint(color)
            ImageViewCompat.setImageTintList(binding.optional, ColorStateList.valueOf(color))
        }

        binding.optional.setOnClickListener { finish() }
    }

    private fun getDownloadStatus() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.downloadStatus.collect {
            when (it) {
                is DownloadCompleted -> getForceUpdateState(InstallReady(it.uri))
                is DownloadCanceled  -> getForceUpdateState(DownloadReady(it.reason))
                is DownloadProgress  -> getForceUpdateState(Downloading(it.progress))
                else -> Unit
            }
        }
    }

    private fun getInstallStatus() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.installStatus.collect {
            when (it) {
                InstallSucceeded  -> finish()
                InstallProgress   -> getForceUpdateState(Installing)
                InstallCanceled   -> getForceUpdateState(InstallReady())
                is InstallFailure -> getForceUpdateState(DownloadReady(it.message))
                else -> Unit
            }
        }
    }

    private fun getForceUpdateState(state: ForceUpdateState): Unit = when (state) {
        is DownloadReady    -> {
            val packageInfo     = packageManager.getPackageInfo(packageName, 0)
            val applicationName = packageManager.getApplicationLabel(packageInfo.applicationInfo)

            binding.button.visibility      = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.message.text           = getString(R.string.forceupdate_update_message, applicationName)
            binding.button.text            = getString(R.string.forceupdate_update)

            val header  = intent.getSerializableExtra(EXTRA_HEADER) as? Pair<*, *>
            val apkLink = intent.getStringExtra(EXTRA_APK_LINK)

            state.message?.let { binding.root.showSnackBar(it) }
            binding.button.setOnClickListener { viewModel.downloadApk(apkLink!!, header) }
        }
        is Downloading -> {
            binding.button.visibility      = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.message.text           = getString(R.string.forceupdate_downloading, state.progress)
            binding.progressBar.setProgress(state.progress, true)
        }
        is InstallReady  -> {
            binding.button.visibility       = View.VISIBLE
            binding.progressBar.visibility  = View.GONE
            binding.button.text             = getString(R.string.forceupdate_install)
            binding.message.text            = getString(R.string.forceupdate_download_completed)

            state.uri?.let { viewModel.writeFileToInternalStorage(it) }
            binding.button.setOnClickListener { viewModel.installApk(viewModel.getLocalFile()) }
        }
        is Installing -> {
            binding.button.visibility      = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.message.text           = getString(R.string.forceupdate_installing)
            binding.progressBar.isIndeterminate = true
        }
    }

    override fun onBackPressed() {}

    sealed class ForceUpdateState {
        data class DownloadReady(val message: String? = null) : ForceUpdateState()
        data class Downloading(val progress: Int)             : ForceUpdateState()
        data class InstallReady(val uri: String? = null)      : ForceUpdateState()
        object Installing                                     : ForceUpdateState()
    }

    companion object {
        const val EXTRA_APK_LINK          = "link"
        const val EXTRA_HEADER            = "header"
        const val EXTRA_ANIMATION         = "animation"
        const val EXTRA_OPTIONAL_DOWNLOAD = "optional"
    }
}