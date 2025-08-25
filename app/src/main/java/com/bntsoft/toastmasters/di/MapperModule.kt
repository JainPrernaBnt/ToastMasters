package com.bntsoft.toastmasters.di

import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.mapper.MemberResponseMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapperModule {
    @Provides
    @Singleton
    fun provideMeetingDomainMapper(): MeetingDomainMapper = MeetingDomainMapper()

    @Provides
    @Singleton
    fun provideMemberResponseMapper(): MemberResponseMapper = MemberResponseMapper()

}