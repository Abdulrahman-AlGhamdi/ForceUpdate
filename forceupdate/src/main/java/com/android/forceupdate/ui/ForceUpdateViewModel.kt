package com.android.forceupdate.ui

import androidx.lifecycle.ViewModel
import com.android.forceupdate.repository.ForceUpdateRepository
import java.io.File

internal class ForceUpdateViewModel(
    private val forceUpdateRepository: ForceUpdateRepository
) : ViewModel() {

    suspend fun downloadApk(apkUrl: String, header: Pair<String, String>?) = forceUpdateRepository.downloadApk(apkUrl, header)

    fun getLocalFile() = forceUpdateRepository.getLocalFile()

    fun installApk(localFile: File) = forceUpdateRepository.installApk(localFile)
}