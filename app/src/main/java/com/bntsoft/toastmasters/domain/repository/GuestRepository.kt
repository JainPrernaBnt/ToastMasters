package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.Guest
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.flow.Flow

interface GuestRepository {
    suspend fun getAllGuests(): List<Guest>
    suspend fun getGuestById(id: String): Guest?
    suspend fun createGuest(guest: Guest): Resource<Guest>
    suspend fun updateGuest(guest: Guest): Resource<Unit>
    suspend fun deleteGuest(id: String): Resource<Unit>
    suspend fun addGuestToMeeting(meetingId: String, guestId: String): Resource<Unit>
    suspend fun removeGuestFromMeeting(meetingId: String, guestId: String): Resource<Unit>
    suspend fun getGuestsForMeeting(meetingId: String): List<Guest>
}
