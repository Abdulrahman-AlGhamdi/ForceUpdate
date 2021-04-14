package com.android.forceupdate.hilt

import android.content.Context
import com.android.forceupdate.repository.ForceUpdateRepository
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

    @Singleton
    @Provides
    fun provideForceUpdateImplementation(@ApplicationContext context: Context): ForceUpdateRepository {
        return ForceUpdateRepositoryImpl(context)
    }
}