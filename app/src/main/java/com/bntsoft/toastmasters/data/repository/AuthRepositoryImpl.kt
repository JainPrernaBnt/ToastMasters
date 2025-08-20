package com.bntsoft.toastmasters.data.repository

import android.util.Log
import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.data.model.UserDeserializer
import com.bntsoft.toastmasters.data.model.UserRole
import com.bntsoft.toastmasters.data.remote.FirebaseAuthService
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.domain.model.AuthResult
import com.bntsoft.toastmasters.domain.model.SignupResult
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseAuthService: FirebaseAuthService,
    private val firestoreService: FirestoreService,
    private val notificationRepository: NotificationRepository
) : AuthRepository {

    override suspend fun login(identifier: String, password: String): AuthResult<User> {
        return try {
            // Determine if identifier is email or phone
            val emailToUse = if (identifier.contains("@")) {
                identifier
            } else {
                // Lookup by phone to get email
                val phoneQuery = firestoreService.getUserByPhone(identifier)
                val doc = phoneQuery.documents.firstOrNull()
                doc?.getString("email") ?: identifier // fallback to original
            }

            val result = firebaseAuth.signInWithEmailAndPassword(emailToUse, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // Get user data from Firestore using our custom deserializer
                val userDoc = firestoreService.getUserDocument(firebaseUser.uid).get().await()
                if (userDoc.exists()) {
                    val user = UserDeserializer.fromDocument(userDoc)
                    if (user != null) {
                        // Log user details for debugging
                        Timber.tag("AuthRepositoryImpl")
                            .d("User role: ${user.role}, isApproved: ${user.isApproved}, isVpEducation: ${user.isVpEducation}")
                        
                        // Check if user is VP Education (always allow login) or an approved member
                        if (user.isVpEducation) {
                            Timber.tag("AuthRepositoryImpl")
                                .d("VP Education user logged in successfully")
                            AuthResult.Success(user)
                        } else if (user.role == UserRole.MEMBER) {
                            if (user.isApproved) {
                                Timber.tag("AuthRepositoryImpl")
                                    .d("Approved member logged in successfully")
                                AuthResult.Success(user)
                            } else {
                                // Regular member not approved yet
                                Timber.tag("AuthRepositoryImpl")
                                    .d("Member not approved, signing out")
                                firebaseAuth.signOut()
                                AuthResult.Error("Your account is pending approval")
                            }
                        } else {
                            // Unknown role
                            Timber.tag("AuthRepositoryImpl").d("Unknown role, signing out")
                            firebaseAuth.signOut()
                            AuthResult.Error("Invalid user role")
                        }
                    } else {
                        AuthResult.Error("Failed to parse user data")
                    }
                } else {
                    // User document doesn't exist, which shouldn't happen
                    AuthResult.Error("User data not found")
                }
            } else {
                AuthResult.Error("Authentication failed")
            }
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    AuthResult.Error("Invalid email or password")
                }

                else -> {
                    AuthResult.Error(e.message ?: "Authentication failed")
                }
            }
        }
    }

    override suspend fun signUp(user: User, password: String): AuthResult<SignupResult> {
        return try {
            // Create Firebase Auth user
            val authResult =
                firebaseAuth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Create user document in Firestore with all required fields
                val userWithId = user.copy(
                    id = firebaseUser.uid,
                    isApproved = false, // New users are not approved by default
                    role = UserRole.MEMBER // Default role for new users
                )
                firestoreService.setUserDocument(firebaseUser.uid, userWithId)

                // Send email verification
                firebaseUser.sendEmailVerification().await()

                // Notify VP Education about new signup (pending approval)
                val vpNotification = NotificationData(
                    title = "New member signup",
                    message = "${userWithId.name} has signed up and is pending approval.",
                    type = NotificationHelper.TYPE_MEMBER_APPROVAL,
                    data = mapOf(
                        NotificationHelper.EXTRA_USER_ID to userWithId.id
                    )
                )
                try {
                    notificationRepository.sendNotificationToRole("VP_EDUCATION", vpNotification)
                } catch (_: Exception) {
                    // Don't fail signup on notification errors
                }

                val requiresApproval = !(userWithId.isVpEducation || userWithId.isApproved)
                AuthResult.Success(SignupResult(userWithId, requiresApproval))
            } else {
                AuthResult.Error("Failed to create user")
            }
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthWeakPasswordException -> {
                    AuthResult.Error("Password is too weak")
                }

                is FirebaseAuthUserCollisionException -> {
                    AuthResult.Error("An account already exists with this email")
                }

                else -> {
                    Timber.tag("AuthRepository").e(e, "Sign up error")
                    AuthResult.Error(e.message ?: "Failed to create account")
                }
            }
        }
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser
        return if (firebaseUser != null) {
            try {
                val userDoc = firestoreService.getUserDocument(firebaseUser.uid).get().await()
                userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun userExists(email: String, phone: String): Boolean {
        return try {
            // Check by email
            val emailResult = firebaseAuth.fetchSignInMethodsForEmail(email).await()
            if (emailResult.signInMethods?.isNotEmpty() == true) {
                return true
            }

            // Check by phone (you'll need to implement this based on your Firestore structure)
            // This is a simplified check - you might need to adjust based on your actual data structure
            val phoneQuery = firestoreService.getUserByPhone(phone)
            !phoneQuery.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateFcmToken(userId: String, token: String) {
        try {
            firestoreService.updateUserField(userId, "fcmToken", token)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update FCM token", e)
        }
    }

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                launch {
                    try {
                        val userDoc =
                            firestoreService.getUserDocument(firebaseUser.uid).get().await()
                        if (userDoc.exists()) {
                            val user =
                                userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
                            if (user != null) {
                                // Check if user is approved
                                if (user.isVpEducation || (user.role == UserRole.MEMBER && user.isApproved)) {
                                    trySend(user)
                                } else {
                                    // User is not approved yet, sign them out
                                    firebaseAuth.signOut()
                                    trySend(null)
                                }
                            } else {
                                trySend(null)
                            }
                        } else {
                            trySend(null)
                        }
                    } catch (e: Exception) {
                        trySend(null)
                    }
                }
            } else {
                trySend(null)
            }
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        // Remove the listener when the flow is cancelled
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
}
