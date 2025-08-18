package com.bntsoft.toastmasters.di

import com.bntsoft.toastmasters.data.local.dao.MeetingDao
import com.bntsoft.toastmasters.data.local.dao.MemberResponseDao
import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.mapper.MemberResponseMapper
import com.bntsoft.toastmasters.data.remote.FirebaseAuthService
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSourceImpl
import com.bntsoft.toastmasters.data.remote.FirebaseMemberResponseDataSource
import com.bntsoft.toastmasters.data.remote.FirebaseMemberResponseDataSourceImpl
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.data.remote.NotificationService
import com.bntsoft.toastmasters.data.remote.UserService
import com.bntsoft.toastmasters.data.repository.AuthRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MeetingRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MemberRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MemberResponseRepositoryImpl
import com.bntsoft.toastmasters.data.repository.NotificationRepositoryImpl
import com.bntsoft.toastmasters.data.repository.UserRepositoryImpl
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
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
    fun provideFirebaseAuthService(
        firebaseAuth: FirebaseAuth
    ): FirebaseAuthService = FirebaseAuthService(firebaseAuth)

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
        firestoreService: FirestoreService,
        notificationRepository: NotificationRepository
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, firebaseAuthService, firestoreService, notificationRepository)

    @Provides
    @Singleton
    fun provideFirebaseMeetingDataSource(
        meetingMapper: MeetingDomainMapper
    ): FirebaseMeetingDataSource = FirebaseMeetingDataSourceImpl(meetingMapper)

    @Provides
    @Singleton
    fun provideFirebaseMemberResponseDataSource(): FirebaseMemberResponseDataSource = FirebaseMemberResponseDataSourceImpl()

    @Provides
    @Singleton
    fun provideMeetingDomainMapper(): MeetingDomainMapper = MeetingDomainMapper()

    @Provides
    @Singleton
    fun provideMemberResponseMapper(): MemberResponseMapper = MemberResponseMapper()

    @Provides
    @Singleton
    fun provideMeetingRepository(
        firebaseDataSource: FirebaseMeetingDataSource
    ): MeetingRepository = MeetingRepositoryImpl(firebaseDataSource)

    @Provides
    @Singleton
    fun provideMemberRepository(
        firestoreService: FirestoreService,
        notificationRepository: NotificationRepository
    ): MemberRepository = MemberRepositoryImpl(firestoreService, notificationRepository)

    @Provides
    @Singleton
    fun provideMemberResponseRepository(
        remoteDataSource: FirebaseMemberResponseDataSource,
        mapper: MemberResponseMapper
    ): MemberResponseRepository = MemberResponseRepositoryImpl(remoteDataSource, mapper)

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
