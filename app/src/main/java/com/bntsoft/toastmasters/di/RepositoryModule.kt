package com.bntsoft.toastmasters.di

import com.bntsoft.toastmasters.data.remote.FirebaseAuthService
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.data.remote.NotificationService
import com.bntsoft.toastmasters.data.remote.UserService
import com.bntsoft.toastmasters.data.repository.AuthRepository
import com.bntsoft.toastmasters.data.repository.AuthRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MeetingRepository
import com.bntsoft.toastmasters.data.repository.MeetingRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MemberRepository
import com.bntsoft.toastmasters.data.repository.MemberRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MemberResponseRepository
import com.bntsoft.toastmasters.data.repository.MemberResponseRepositoryImpl
import com.bntsoft.toastmasters.data.repository.NotificationRepository
import com.bntsoft.toastmasters.data.repository.NotificationRepositoryImpl
import com.bntsoft.toastmasters.data.repository.UserRepository
import com.bntsoft.toastmasters.data.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuthService(
        firebaseAuth: FirebaseAuth,
        firestoreService: FirestoreService
    ): FirebaseAuthService = FirebaseAuthService(firebaseAuth, firestoreService)

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

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firebaseAuthService: FirebaseAuthService,
        firestoreService: FirestoreService
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, firebaseAuthService, firestoreService)

    @Provides
    @Singleton
    fun provideMeetingRepository(
        firestoreService: FirestoreService,
        notificationService: NotificationService
    ): MeetingRepository = MeetingRepositoryImpl(firestoreService, notificationService)

    @Provides
    @Singleton
    fun provideMemberRepository(
        firestoreService: FirestoreService
    ): MemberRepository = MemberRepositoryImpl(firestoreService)

    @Provides
    @Singleton
    fun provideMemberResponseRepository(
        firestoreService: FirestoreService,
        notificationService: NotificationService
    ): MemberResponseRepository = MemberResponseRepositoryImpl(firestoreService, notificationService)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationService: NotificationService
    ): NotificationRepository = NotificationRepositoryImpl(notificationService)

    @Provides
    @Singleton
    fun provideUserRepository(
        userService: UserService
    ): UserRepository = UserRepositoryImpl(userService)
}
