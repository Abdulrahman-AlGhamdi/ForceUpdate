package com.android.forceupdate.ui

import androidx.lifecycle.ViewModel
import com.android.forceupdate.repository.install.InstallRepository
import com.android.forceupdate.repository.download.DownloadRepository
import java.io.File

internal class ForceUpdateViewModel(
    private val downloadRepository: DownloadRepository,
    private val installRepository: InstallRepository
) : ViewModel() {

    fun downloadApk(apkUrl: String, header: Pair<String, String>?) = downloadRepository.downloadApk(apkUrl, header)

    fun writeFileToInternalStorage(uri: String) = downloadRepository.writeFileToInternalStorage(uri)

    fun getLocalFile() = downloadRepository.getLocalFile()

    fun installApk(localFile: File) = installRepository.installApk(localFile)
}