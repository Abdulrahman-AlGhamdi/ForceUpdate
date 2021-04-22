package com.android.forceupdate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.forceupdate.repository.ForceUpdateRepository
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl

internal class ForceUpdateProviderFactory(
    private val forceUpdateRepositoryImpl: ForceUpdateRepositoryImpl
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ForceUpdateViewModel(forceUpdateRepositoryImpl) as T
    }
}