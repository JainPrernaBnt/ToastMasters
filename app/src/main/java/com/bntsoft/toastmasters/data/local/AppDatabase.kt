package com.bntsoft.toastmasters.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bntsoft.toastmasters.data.local.converters.MeetingStatusConverter
import com.bntsoft.toastmasters.data.local.converters.PreferredRoleConverter
import com.bntsoft.toastmasters.data.local.converters.ReportTypeConverter
import com.bntsoft.toastmasters.data.local.dao.MeetingAvailabilityDao
import com.bntsoft.toastmasters.data.local.dao.MeetingDao
import com.bntsoft.toastmasters.data.local.dao.MemberResponseDao
import com.bntsoft.toastmasters.data.local.dao.UserDao
import com.bntsoft.toastmasters.data.local.entity.AgendaItems
import com.bntsoft.toastmasters.data.local.entity.AgendaMaster
import com.bntsoft.toastmasters.data.local.entity.ClubOfficers
import com.bntsoft.toastmasters.data.local.entity.Guest
import com.bntsoft.toastmasters.data.local.entity.MeetingAvailability
import com.bntsoft.toastmasters.data.local.entity.MeetingEntity
import com.bntsoft.toastmasters.data.local.entity.MeetingRoles
import com.bntsoft.toastmasters.data.local.entity.MeetingRolesBackout
import com.bntsoft.toastmasters.data.local.entity.MemberResponseEntity
import com.bntsoft.toastmasters.data.local.entity.ReportRequests
import com.bntsoft.toastmasters.data.local.entity.UserEntity
import com.bntsoft.toastmasters.data.local.entity.Winners

@Database(
    entities = [
        UserEntity::class,
        MeetingEntity::class,
        MeetingAvailability::class,
        MeetingRoles::class,
        MeetingRolesBackout::class,
        AgendaMaster::class,
        AgendaItems::class,
        ClubOfficers::class,
        Guest::class,
        Winners::class,
        ReportRequests::class,
        MemberResponseEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    PreferredRoleConverter::class,
    ReportTypeConverter::class,
    MeetingStatusConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun memberResponseDao(): MemberResponseDao
    abstract fun userDao(): UserDao
    abstract fun meetingAvailabilityDao(): MeetingAvailabilityDao
}