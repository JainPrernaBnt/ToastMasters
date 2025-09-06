package com.bntsoft.toastmasters.di

import com.bntsoft.toastmasters.data.repository.AuthRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MeetingRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MemberRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MemberResponseRepositoryImpl
import com.bntsoft.toastmasters.data.repository.NotificationRepositoryImpl
import com.bntsoft.toastmasters.data.repository.MeetingAgendaRepositoryImpl
import com.bntsoft.toastmasters.data.repository.UserRepositoryImpl
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import com.bntsoft.toastmasters.domain.repository.MeetingAgendaRepository
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

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
    abstract fun bindMemberResponseRepository(
        memberResponseRepositoryImpl: MemberResponseRepositoryImpl
    ): MemberResponseRepository

    @Binds
    @Singleton
    abstract fun bindMeetingAgendaRepository(
        meetingAgendaRepositoryImpl: MeetingAgendaRepositoryImpl
    ): MeetingAgendaRepository
}