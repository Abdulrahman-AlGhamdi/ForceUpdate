package com.android.forceupdate.hilt

import android.app.Application
import com.android.forceupdate.repository.ForceUpdateRepository
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideForceUpdateImplementation(application: Application): ForceUpdateRepository {
        return ForceUpdateRepositoryImpl(application)
    }
}