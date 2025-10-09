package com.bntsoft.toastmasters

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

open class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check authentication state for all activities except MainActivity
        if (this !is MainActivity) {
            checkAuthenticationState()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Double-check authentication state when activity resumes
        if (this !is MainActivity) {
            checkAuthenticationState()
        }
    }

    private fun checkAuthenticationState() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val isLoggedInPrefs = preferenceManager.isLoggedIn
        val userRole = preferenceManager.getUserRole()
        
        Log.d("BaseActivity", "Auth check in ${this::class.simpleName} - Firebase user: ${firebaseUser != null}, Prefs logged in: $isLoggedInPrefs, Role: $userRole")

        // If user is not properly authenticated, redirect to MainActivity
        if (firebaseUser == null || !isLoggedInPrefs || userRole == null) {
            Log.d("BaseActivity", "User not authenticated (Firebase: ${firebaseUser != null}, Prefs: $isLoggedInPrefs, Role: $userRole), redirecting to MainActivity")
            
            // Clear any stale data
            if (firebaseUser == null && isLoggedInPrefs) {
                Log.d("BaseActivity", "Firebase user null but prefs indicate logged in - clearing preferences")
                preferenceManager.clearUserData()
            }
            
            if (firebaseUser != null && !isLoggedInPrefs) {
                Log.d("BaseActivity", "Firebase user exists but prefs indicate not logged in - signing out Firebase")
                FirebaseAuth.getInstance().signOut()
            }
            
            // Redirect to MainActivity which will handle authentication
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
