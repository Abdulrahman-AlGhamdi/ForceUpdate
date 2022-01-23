package com.android.forceupdate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.forceupdate.repository.install.InstallRepository
import com.android.forceupdate.repository.download.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

internal class ForceUpdateViewModel(
    private val downloadRepository: DownloadRepository,
    private val installRepository: InstallRepository
) : ViewModel() {

    val downloadStatus = downloadRepository.downloadStatus

    fun downloadApk(
        apkUrl: String,
        header: Pair<*, *>?
    ) = viewModelScope.launch(Dispatchers.IO) { downloadRepository.downloadApk(apkUrl, header) }

    fun writeFileToInternalStorage(uri: String) = viewModelScope.launch(Dispatchers.IO) {
        downloadRepository.writeFileToInternalStorage(uri)
    }

    fun getLocalFile() = downloadRepository.getLocalFile()

    fun installApk(localFile: File) = installRepository.installApk(localFile)
}