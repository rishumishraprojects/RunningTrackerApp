package com.example.runningtrackerapp.ui.fragments

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.runningtrackerapp.R
import com.example.runningtrackerapp.others.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningtrackerapp.services.TrackingService
import com.example.runningtrackerapp.ui.viewModels.MainViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.room.InvalidationTracker
import com.example.runningtrackerapp.db.Run
import com.example.runningtrackerapp.others.Constants.ACTION_PAUSE_SERVICE
import com.example.runningtrackerapp.others.Constants.ACTION_STOP_SERVICE
import com.example.runningtrackerapp.others.Constants.MAP_ZOOM
import com.example.runningtrackerapp.others.Constants.POLYLINE_COLOR
import com.example.runningtrackerapp.others.TrackingUtility
import com.example.runningtrackerapp.services.Polyline
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.round

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking), OnMapReadyCallback {

    private val viewModel: MainViewModel by viewModels()
    private var map: GoogleMap? = null
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private lateinit var mapView: MapView
    private lateinit var btnToggleRun: Button
    private lateinit var btnFinishRun: Button
    private var currentLocationMarker: com.google.android.gms.maps.model.Marker? = null
    private var currTimeInMillis = 0L
    private lateinit var tvTimer: com.google.android.material.textview.MaterialTextView

    private var menu: Menu? = null

    @set:Inject
    var weight = 80f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnToggleRun = view.findViewById(R.id.btnToggleRun)
        btnFinishRun = view.findViewById(R.id.btnFinishRun)
        mapView = view.findViewById(R.id.mapView)
        tvTimer = view.findViewById(R.id.tvTimer)
        mapView.onCreate(savedInstanceState)
        btnToggleRun.setOnClickListener {
            toggleRun()
        }
        btnFinishRun.setOnClickListener {
            zoomToSeeWholeTrack()
            endRunAndSaveInDb()
        }
        mapView.getMapAsync {
            map = it
            addAllPolyLines()
        }

        subscribeToObservers()
    }

    private fun subscribeToObservers(){
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer{ newPathPoints ->
            pathPoints = newPathPoints.map{
                it.toMutableList()
            }.toMutableList()
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            currTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatch(currTimeInMillis, true)
            tvTimer.text = formattedTime
        })
    }

    private fun toggleRun(){
        if(isTracking){
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }else{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if(currTimeInMillis > 0L){
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun showCancelTrackingDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the Run?")
            .setMessage("Are you sure to cancel the current run and delete all its data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") {
                    _, _ ->
                stopRun()
            }
            .setNegativeButton("No") {
                    dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }
    private fun stopRun(){
        tvTimer.text = "00:00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking && currTimeInMillis > 0L) {
            btnToggleRun.text = "Start"
            btnFinishRun.visibility = View.VISIBLE
        } else if(isTracking){
            menu?.getItem(0)?.isVisible = true
            btnToggleRun.text = "Stop"
            btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser(){
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
          map?.animateCamera(
              CameraUpdateFactory.newLatLngZoom(
                  pathPoints.last().last(),
                  MAP_ZOOM
              )
          )
            val lastLatLng = pathPoints.last().last()
            if (currentLocationMarker == null) {
                currentLocationMarker = map?.addMarker(
                    com.google.android.gms.maps.model.MarkerOptions()
                        .position(lastLatLng)
                        .title("You are here")
                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                            com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE))
                )
            } else {
                currentLocationMarker?.position = lastLatLng
            }
        }
    }

    private fun zoomToSeeWholeTrack(){
        if (pathPoints.isEmpty() || pathPoints.all { it.isEmpty() }) {
            return
        }
        val bounds = LatLngBounds.Builder()
        for(polyline in pathPoints){
            for(pos in polyline){
                bounds.include(pos)
            }
        }

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
            bounds.build(),
            mapView.width,
            mapView.height,
            (mapView.width * 0.05f).toInt()
        )
        )
    }

    private fun endRunAndSaveInDb() {
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for (polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }
            val avgSpeed = round((distanceInMeters / 1000f) / (currTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val MET = 9.8f
            val hours = currTimeInMillis / 1000f / 3600f
            val caloriesBurned = (hours * MET * weight).toInt()
            val run = Run(
                bmp,
                dateTimestamp,
                avgSpeed,
                distanceInMeters,
                currTimeInMillis,
                caloriesBurned
            )
            viewModel.insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()
        }
    }
    private fun addAllPolyLines(){
        for(polyline in pathPoints){
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(8f)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }
    private fun addLatestPolyline(){
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(8f)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)

        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::mapView.isInitialized) {
            mapView.onSaveInstanceState(outState)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }
}


