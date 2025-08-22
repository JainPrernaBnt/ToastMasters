package com.bntsoft.toastmasters.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

//    @Provides
//    @Singleton
//    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
//        return Room.databaseBuilder(
//            context,
//            AppDatabase::class.java,
//            "toastmasters_database"
//        ).build()
//    }

//    @Provides
//    fun provideMeetingDao(database: AppDatabase) = database.meetingDao()

//    @Provides
//    fun provideMemberResponseDao(database: AppDatabase) = database.memberResponseDao()

//    @Provides
//    fun provideUserDao(database: AppDatabase) = database.userDao()

//    @Provides
//    fun provideMeetingAvailabilityDao(database: AppDatabase) = database.meetingAvailabilityDao()
}