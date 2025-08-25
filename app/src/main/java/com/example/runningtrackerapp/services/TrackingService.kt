package com.example.runningtrackerapp.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.runningtrackerapp.R
import com.example.runningtrackerapp.others.Constants.ACTION_PAUSE_SERVICE
import com.example.runningtrackerapp.others.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.example.runningtrackerapp.others.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningtrackerapp.others.Constants.ACTION_STOP_SERVICE
import com.example.runningtrackerapp.others.Constants.FASTEST_LOCATION_INTERVAL
import com.example.runningtrackerapp.others.Constants.LOCATION_UPDATE_INTERVAL
import com.example.runningtrackerapp.others.Constants.NOTIFICATION_CHANNEL_ID
import com.example.runningtrackerapp.others.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.runningtrackerapp.others.Constants.NOTIFICATION_ID
import com.example.runningtrackerapp.others.Constants.TIMER_UPDATE_INTERVAL
import com.example.runningtrackerapp.others.TrackingUtility
import com.example.runningtrackerapp.ui.MainActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.Provider

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {
    private var isFirstRun = true
    private var serviceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val timeRunInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    lateinit var currNotificationBuilder: NotificationCompat.Builder

    companion object {
        val timeRunInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInMillis.postValue(0L)
        timeRunInSeconds.postValue(0L)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()
        currNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        isTracking.observe(this, Observer { tracking ->
            updateLocationTracking(tracking)
            updateNotificationTrackingState(tracking)
        })
    }

    private fun killService(){
      serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                        Timber.d("Service started")
                    } else {
                        Timber.d("Service resumed")
                        startTimer()
                    }
                }

                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Service paused")
                    pauseService()
                }

                ACTION_STOP_SERVICE -> {
                    Timber.d("Service stopped")
                    killService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer(){
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch{
            while(isTracking.value == true){
                lapTime = System.currentTimeMillis() - timeStarted
                timeRunInMillis.postValue(timeRun + lapTime)
                if(timeRunInMillis.value!! >= lastSecondTimestamp + 1000L){
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime

        }
    }

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    private fun updateNotificationTrackingState(isTracking: Boolean){
        val notificationActionText = if(isTracking) "Pause" else "Resume"
        val pendingIntent = if(isTracking){
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
        }else{
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        currNotificationBuilder.javaClass.getDeclaredField("mActions").apply{
            isAccessible = true
            set(currNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
        if(!serviceKilled) {
            currNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_pause, notificationActionText, pendingIntent)
            notificationManager.notify(NOTIFICATION_ID, currNotificationBuilder.build())
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun updateLocationTracking(tracking: Boolean) {
        if (tracking) {
            if (TrackingUtility.hasLocationPermissions(this)) {
                val request = LocationRequest.Builder(LOCATION_UPDATE_INTERVAL)
                    .setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                Timber.d("Requesting location updates")
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            Timber.d("Location updates stopped")
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value == true) {
                for (location in result.locations) {
                    addPathPoint(location)
                    Timber.d("NEW LOCATION: ${location.latitude}, ${location.longitude}")
                }
            }
        }
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            val updatedPath = pathPoints.value ?: mutableListOf()
            if (updatedPath.isEmpty()) updatedPath.add(mutableListOf())
            updatedPath.last().add(pos)
            pathPoints.postValue(updatedPath)
        }
    }

    private fun addEmptyPolyline() {
        val updatedPath = pathPoints.value ?: mutableListOf()
        updatedPath.add(mutableListOf())
        pathPoints.postValue(updatedPath)
    }

    private fun startForegroundService() {
        startTimer()
        isTracking.postValue(true)
        addEmptyPolyline()

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())


        timeRunInSeconds.observe(this, Observer {
            if(!serviceKilled) {
                val notification = currNotificationBuilder
                    .setContentText(TrackingUtility.getFormattedStopWatch(it * 1000L))
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
    }


    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}
