package com.bntsoft.toastmasters.di

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.utils.NotificationHelper
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.bntsoft.toastmasters.utils.DeviceIdManager
import com.bntsoft.toastmasters.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }

    @Provides
    @Singleton
    fun provideNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(
        context: Context,
        firebaseMessaging: FirebaseMessaging,
        prefsManager: PreferenceManager
    ): NotificationHelper {
        return NotificationHelper(context, firebaseMessaging, prefsManager)
    }

    @Provides
    @Singleton
    fun provideUserManager(
        userRepository: UserRepository,
    ) = UserManager(userRepository)

    @Provides
    @Singleton
    fun provideCurrentUserId(
        firebaseAuth: FirebaseAuth
    ): String? {
        return firebaseAuth.currentUser?.uid
    }

    @Provides
    @Singleton
    fun provideDeviceIdManager(
        context: Context,
        preferenceManager: PreferenceManager
    ): DeviceIdManager {
        return DeviceIdManager(context, preferenceManager)
    }
}
