package com.bntsoft.toastmasters.di

import android.content.Context
import com.bntsoft.toastmasters.utils.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrefsModule {

    @Provides
    @Singleton
    fun providePrefsManager(@ApplicationContext context: Context): PreferenceManager {
        return PreferenceManager(context)
    }
}
