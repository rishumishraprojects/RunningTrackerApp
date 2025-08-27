package com.example.runningtrackerapp.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.runningtrackerapp.R
import com.example.runningtrackerapp.others.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.example.runningtrackerapp.others.Constants.KEY_NAME
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.view.View

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment

        setSupportActionBar(findViewById(R.id.toolbar))
        bottomNavigationView.setupWithNavController(navHostFragment.findNavController())
        bottomNavigationView.setOnNavigationItemReselectedListener { /* NO-OP */ }
        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.settingsFragment, R.id.runFragment, R.id.statisticsFragment ->
                    bottomNavigationView.visibility = View.VISIBLE
                else -> bottomNavigationView.visibility = View.GONE
            }
        }
        navigateToTrackingFragmentIfNeeded(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
            navHostFragment.findNavController().navigate(R.id.action_global_trackingFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        val name = sharedPref.getString(KEY_NAME, null)
        val toolbarTitle = if (!name.isNullOrEmpty()) {
            "Let's go, $name!"
        } else {
            getString(R.string.app_name)
        }
        findViewById<TextView>(R.id.tvToolbarTitle).text = toolbarTitle
    }
}
