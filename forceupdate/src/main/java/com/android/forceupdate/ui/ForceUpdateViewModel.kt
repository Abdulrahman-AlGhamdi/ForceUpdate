package com.android.forceupdate.ui

import androidx.lifecycle.ViewModel
import com.android.forceupdate.repository.ForceUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class ForceUpdateViewModel @Inject constructor(
    private val forceUpdateRepository: ForceUpdateRepository
) : ViewModel() {

    suspend fun downloadApk(apkUrl: String) = forceUpdateRepository.downloadApk(apkUrl)

    fun installApk(localFile: File) = forceUpdateRepository.installApk(localFile)
}