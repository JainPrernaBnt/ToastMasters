package com.bntsoft.toastmasters.di

import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.data.remote.NotificationService
import com.bntsoft.toastmasters.data.remote.UserService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideFirestoreService(
        firestore: FirebaseFirestore
    ): FirestoreService = FirestoreService(firestore)

    @Provides
    @Singleton
    fun provideNotificationService(
        firestore: FirebaseFirestore
    ): NotificationService = NotificationService(firestore)

    @Provides
    @Singleton
    fun provideUserService(
        firestore: FirebaseFirestore
    ): UserService = UserService(firestore)
}