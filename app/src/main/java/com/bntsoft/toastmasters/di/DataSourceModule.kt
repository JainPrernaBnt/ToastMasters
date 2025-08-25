package com.bntsoft.toastmasters.di

import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSourceImpl
import com.bntsoft.toastmasters.data.remote.FirebaseMemberResponseDataSource
import com.bntsoft.toastmasters.data.remote.FirebaseMemberResponseDataSourceImpl
import com.bntsoft.toastmasters.utils.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    fun provideFirebaseMeetingDataSource(
        meetingMapper: MeetingDomainMapper
    ): FirebaseMeetingDataSource = FirebaseMeetingDataSourceImpl(meetingMapper)

    @Provides
    @Singleton
    fun provideFirebaseMemberResponseDataSource(): FirebaseMemberResponseDataSource =
        FirebaseMemberResponseDataSourceImpl()

}