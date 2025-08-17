package com.bntsoft.toastmasters.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Add user-related methods here
    // Example:
    // fun getUser(userId: String): Flow<User> { ... }
    // fun updateUser(user: User): Flow<Result<Unit>> { ... }
}
