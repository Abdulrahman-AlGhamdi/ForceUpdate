package com.android.forceupdate.ui

import android.content.Intent
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.forceupdate.repository.download.DownloadRepository
import com.android.forceupdate.repository.install.InstallRepository
import com.android.forceupdate.util.ConstantsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class ForceUpdateViewModel(
    private val downloadRepository: DownloadRepository,
    private val installRepository: InstallRepository
) : ViewModel() {

    val downloadStatus = downloadRepository.downloadStatus
    val installStatus = installRepository.installStatus

    fun downloadApk(apkUrl: String, header: Pair<*, *>?) = viewModelScope.launch(Dispatchers.IO) {
        downloadRepository.downloadApk(apkUrl, header)
    }

    fun getApkFile() = downloadRepository.getApkFile()

    fun installApk() = viewModelScope.launch(Dispatchers.IO) {
        installRepository.installApk(getApkFile())
    }

    fun getForceUpdateAnimation(intent: Intent): String {
        val animation = intent.getStringExtra(ConstantsUtils.EXTRA_ANIMATION)

        return if (!animation.isNullOrEmpty() && animation.endsWith(".json")) animation
        else "force_update_animation.json"
    }

    fun getIsOptional(intent: Intent): Int {
        val isOptional = intent.getBooleanExtra(ConstantsUtils.EXTRA_OPTIONAL_DOWNLOAD, false)
        return if (isOptional) View.VISIBLE else View.GONE
    }
}