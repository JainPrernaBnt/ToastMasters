package com.bntsoft.toastmasters

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.bntsoft.toastmasters.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.status_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle status bar item clicks
        return when (item.itemId) {
            R.id.action_reports -> {
                // TODO
                true
            }

            R.id.action_settings -> {
                // TODO
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}