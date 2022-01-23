package com.android.forceupdate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.forceupdate.repository.install.InstallRepositoryImpl
import com.android.forceupdate.repository.download.DownloadRepositoryImpl

internal class ForceUpdateProviderFactory(
    private val downloadRepositoryImpl: DownloadRepositoryImpl,
    private val forceUpdateRepositoryImpl: InstallRepositoryImpl
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ForceUpdateViewModel(downloadRepositoryImpl, forceUpdateRepositoryImpl) as T
    }
}