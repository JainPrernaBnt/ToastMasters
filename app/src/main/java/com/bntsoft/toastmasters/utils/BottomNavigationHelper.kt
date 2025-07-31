package com.bntsoft.toastmasters.utils

import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import com.bntsoft.toastmasters.R
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavigationHelper {

    const val ROLE_VP = "vp"
    const val ROLE_MEMBER = "member"

    @MenuRes
    fun getMenuResForRole(role: String): Int {
        return when (role.lowercase()) {
            ROLE_VP -> R.menu.bottom_nav_vp_menu
            ROLE_MEMBER -> R.menu.bottom_nav_member_menu
            else -> throw IllegalArgumentException("Unknown role: $role")
        }
    }

    fun setupBottomNavigation(
        activity: AppCompatActivity,
        bottomNavView: BottomNavigationView,
        onNavigationItemSelected: (Int) -> Boolean
    ) {
        // To show original icon colors if needed
        bottomNavView.itemIconTintList = null
        bottomNavView.setOnItemSelectedListener { item ->
            onNavigationItemSelected(item.itemId)
            true
        }
    }

    fun setupWithNavController(
        bottomNavView: BottomNavigationView,
        role: String = ROLE_MEMBER,
        onNavItemSelected: (Int) -> Unit
    ) {
        // Inflating the appropriate menu based on role
        bottomNavView.menu.clear()
        bottomNavView.inflateMenu(getMenuResForRole(role))

        // Item selection listener
        bottomNavView.setOnItemSelectedListener { item ->
            onNavItemSelected(item.itemId)
            true
        }
    }
}
