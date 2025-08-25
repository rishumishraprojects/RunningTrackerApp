package com.example.runningtrackerapp.ui.fragments

import android.Manifest
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.runningtrackerapp.R
import com.example.runningtrackerapp.others.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.runningtrackerapp.others.Constants.KEY_NAME
import com.example.runningtrackerapp.others.Constants.KEY_WEIGHT
import com.example.runningtrackerapp.others.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.runningtrackerapp.others.TrackingUtility
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.w3c.dom.Text
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup), EasyPermissions.PermissionCallbacks {

    private lateinit var etName: TextView
    private lateinit var etWeight: TextView

    @Inject
    lateinit var sharedPref: SharedPreferences

    @set:Inject
    var isFirstAppOpen = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isFirstAppOpen) {
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment, true)
                .build()
            findNavController().navigate(
                R.id.action_setupFragment_to_runFragment,
                savedInstanceState,
                navOptions
            )
        }
        requestPermissions()
        etName = view.findViewById<TextView>(R.id.et1Name)
        etWeight = view.findViewById<TextView>(R.id.et1Weight)

        val tvContinue = view.findViewById<TextView>(R.id.tvContinue)
        tvContinue.setOnClickListener {
            val success = writePersonalDataToSharedPref()
           if(!success){
               Snackbar.make(requireView(), "Please enter all the fields", Snackbar.LENGTH_SHORT).show()
               return@setOnClickListener
           }
            if (TrackingUtility.hasLocationPermissions(requireContext())) {
                findNavController().navigate(R.id.action_setupFragment_to_runFragment)
            } else {
                requestPermissions()
            }
        }
    }

    private fun requestPermissions() {
        // Step 1 → Foreground first
        if (!TrackingUtility.hasForegroundLocation(requireContext())) {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            // Add notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            EasyPermissions.requestPermissions(
                this,
                "You need to accept location & notification permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                *permissions.toTypedArray()
            )
            return
        }

        // Step 2 → If Android Q+, check background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !TrackingUtility.hasBackgroundLocation(requireContext())
        ) {
            EasyPermissions.requestPermissions(
                this,
                "Background location access is required to track your runs even when the screen is off.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        // After granting foreground, check background if needed
        if (TrackingUtility.hasForegroundLocation(requireContext()) &&
            !TrackingUtility.hasBackgroundLocation(requireContext()) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            requestPermissions()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun writePersonalDataToSharedPref(): Boolean {
        val name = etName.text.toString()
        val weight = etWeight.text.toString()
        if (name.isEmpty() || weight.isEmpty()) {
            return false
        }
        sharedPref.edit()
            .putString(KEY_NAME, name)
            .putFloat(KEY_WEIGHT, weight.toFloat())
            .putBoolean(KEY_FIRST_TIME_TOGGLE, false)
            .apply()
        val toolbarText = "Let's go, $name!"
        requireActivity().findViewById<TextView>(R.id.tvToolbarTitle).text = toolbarText
        return true

    }
}
