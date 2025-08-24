package com.bntsoft.toastmasters.di

import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.mapper.MemberResponseMapper
import com.bntsoft.toastmasters.data.mapper.RoleMapper
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSourceImpl
import com.bntsoft.toastmasters.data.remote.FirebaseMemberResponseDataSource
import com.bntsoft.toastmasters.data.remote.FirebaseMemberResponseDataSourceImpl
import com.bntsoft.toastmasters.data.remote.FirebaseRoleRemoteDataSource
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.data.remote.NotificationService
import com.bntsoft.toastmasters.data.remote.UserService
import com.bntsoft.toastmasters.data.repository.AuthRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MeetingRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MemberRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MemberResponseRepositoryImpl
import com.bntsoft.toastmasters.data.repository.NotificationRepositoryImpl
import com.bntsoft.toastmasters.data.repository.RoleRepositoryImpl
import com.bntsoft.toastmasters.data.repository.UserRepositoryImpl
import com.bntsoft.toastmasters.data.source.remote.RoleRemoteDataSource
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.domain.repository.RoleRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.util.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMeetingRepository(
        meetingRepositoryImpl: MeetingRepositoryImpl
    ): MeetingRepository

    @Binds
    @Singleton
    abstract fun bindMemberRepository(
        memberRepositoryImpl: MemberRepositoryImpl
    ): MemberRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindRoleRepository(
        roleRepositoryImpl: RoleRepositoryImpl
    ): RoleRepository

    @Binds
    @Singleton
    abstract fun bindRoleRemoteDataSource(
        firebaseRoleRemoteDataSource: FirebaseRoleRemoteDataSource
    ): RoleRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindMemberResponseRepository(
        memberResponseRepositoryImpl: MemberResponseRepositoryImpl
    ): MemberResponseRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ProvidesModule {
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseMeetingDataSource(
        meetingMapper: MeetingDomainMapper
    ): FirebaseMeetingDataSource = FirebaseMeetingDataSourceImpl(meetingMapper)

    @Provides
    @Singleton
    fun provideFirebaseMemberResponseDataSource(): FirebaseMemberResponseDataSource =
        FirebaseMemberResponseDataSourceImpl()

    @Provides
    @Singleton
    fun provideFirebaseRoleRemoteDataSource(
        firestore: FirebaseFirestore,
        networkMonitor: NetworkMonitor
    ): FirebaseRoleRemoteDataSource {
        return FirebaseRoleRemoteDataSource(firestore, networkMonitor)
    }

    @Provides
    @Singleton
    fun provideMeetingDomainMapper(): MeetingDomainMapper = MeetingDomainMapper()

    @Provides
    @Singleton
    fun provideMemberResponseMapper(): MemberResponseMapper = MemberResponseMapper()

    @Provides
    @Singleton
    fun provideRoleMapper(): RoleMapper = RoleMapper()

    @Provides
    @Singleton
    fun provideMeetingRepository(
        firebaseDataSource: FirebaseMeetingDataSource,
        memberResponseRepository: MemberResponseRepository
    ): MeetingRepository = MeetingRepositoryImpl(firebaseDataSource, memberResponseRepository)

    @Provides
    @Singleton
    fun provideMemberRepository(
        firestoreService: FirestoreService,
        notificationRepository: NotificationRepository,
        roleRepository: RoleRepository
    ): MemberRepository = MemberRepositoryImpl(firestoreService, notificationRepository, roleRepository)

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

    @Provides
    @Singleton
    fun provideRoleRepository(
        remoteDataSource: RoleRemoteDataSource,
        roleMapper: RoleMapper,
        networkMonitor: NetworkMonitor
    ): RoleRepository = RoleRepositoryImpl(remoteDataSource, roleMapper, networkMonitor)
}
