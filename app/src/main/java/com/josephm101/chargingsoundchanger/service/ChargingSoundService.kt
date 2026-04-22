package com.josephm101.chargingsoundchanger.service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_POWER_CONNECTED
import android.content.Intent.ACTION_POWER_DISCONNECTED
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.josephm101.chargingsoundchanger.MainActivity
import com.josephm101.chargingsoundchanger.R
import com.josephm101.chargingsoundchanger.helpers.soundmanager.SoundManager
import com.josephm101.chargingsoundchanger.helpers.soundmanager.Sounds
import com.josephm101.chargingsoundchanger.helpers.VibrationHelper
import com.josephm101.chargingsoundchanger.preferences.ServicePreferences

/* A developer's note:

Android development is kinda weird, especially when it comes to services.
There is still a bunch of boilerplate code (makes up most of this source file) to do the following:
 - Start the service
 - Create a persistent notification that keeps Android from killing the service (hopefully)
 - Restarting if Android decides to kill the service anyway (more likely on low-memory devices)

The only part of this service that achieves the "charging sound" part is the playChargingSound()
function (towards the bottom of this file)
 */

class ChargingSoundService : Service() {
    /* This should ONLY be enabled for debugging purposes. Disable for release builds.
     * Enabling this causes the service to generate toast notifications (they appear at the bottom
     * of the screen) indicating the status of the service regardless of whether the app is in the foreground.
     *
     * Useful if you can't get to a debugger fast enough to diagnose an issue.
     */
    private val showDebugToasts = false

    val soundManager = SoundManager()

    val serviceLogTag =
        "ChargingSoundService" // This is the logging tag that we use for Log.[d/i/w/e]()

    /*
    To ensure this service's survival after we start it (quite a battle in Android), we create a persistent
    service notification. Doesn't need to be anything fancy.
    These values define what the ID of the notification will be, and when we register a notification
    channel for the aforementioned service notification, its ID will be the value stored in persistentNotificationChannelID.

    These values aren't terribly important in this case since this app will only have a single notification,
    but for larger apps that can have multiple notifications in different categories, these parameters
    are required.
     */
    private val persistentNotificationChannelID = "charging_sound_service_channel_id"
    private val persistentNotificationID = 101

    /*
    This BroadcastReceiver is one of the most important parts of the service.
    This is how we detect a charger being plugged in.

    When registered, if Android broadcasts a system-wide action notification that the IntentFilter
    (assigned to the receiver in onStartCommand()) is set up to look for, onReceive() will be
    called within the receiver. *This* is where we call our playChargingSound() function.
     */
    private val onChargingStatusChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_POWER_CONNECTED -> {
                    Log.i(serviceLogTag, "Received \"ACTION_POWER_CONNECTED\"")
                    playChargingStartedSound()
                }

                ACTION_POWER_DISCONNECTED -> {
                    Log.i(serviceLogTag, "Received \"ACTION_POWER_DISCONNECTED\"")
                    playChargingStoppedSound()
                }
            }
        }
    }

    // Handle setting up the broadcast receiver, creating a service notification, and bringing the service online
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        /// TODO: Add permission checks that stop the service from launching if any required permissions are not granted
        //onTaskRemoved(intent)

        if (showDebugToasts) {
            // Create a toast notification letting us know that the sound service has started.
            // This will show up even if the app is not in the foreground.
            Toast.makeText(
                applicationContext,
                "Battery sound service has started.",
                Toast.LENGTH_SHORT
            ).show()
        }

        /* Here, we're going to register our broadcast receiver defined in this service (onChargingStatusChangedReceiver).
         * This broadcast receiver will send a signal to our service to play a sound when it detects that a charger has been attached to the device.
         */
        /* First, we create an IntentFilter for the broadcast receiver to tell it what to listen for. */
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_POWER_CONNECTED) // Subscribe to alerts when power is connected
            addAction(ACTION_POWER_DISCONNECTED) // Subscribe to alerts when power is disconnected
        }
        this.registerReceiver(
            onChargingStatusChangedReceiver,
            intentFilter
        ) // Register our IntentFilter with the BroadcastReceiver
        Log.i(
            serviceLogTag,
            "INIT: Registered receiver (onChargingStatusChangedReceiver)"
        ) // Log what we've done


        // Create a notification channel for our persistent service notification (required for Android 8 Oreo and later; keeps Android from just nuking it when resources are low)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                persistentNotificationChannelID,
                getString(R.string.service_notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )

            // This notification shouldn't get in the way; it's just a service notification. Let's make it so it doesn't.
            channel.setShowBadge(false)
            channel.enableVibration(false)
            channel.enableLights(false)
            channel.setSound(null, null)
            channel.description = getString(R.string.service_notification_channel_description)

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        /* Here, we set up our persistent notification that will launch the MainActivity when tapped. */
        val notificationIntent = Intent( // Create an intent that points to the app's MainActivity
            this,
            MainActivity::class.java
        )

        /* A PendingIntent allows us to trigger a regular intent at a later time. This is what we'll give to our notification. */
        val pendingIntent =
            PendingIntent.getActivity( // Create a PendingIntent that wraps notificationIntent
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        /* Now we'll build the notification */
        val serviceNotification = NotificationCompat.Builder(
            this,
            persistentNotificationChannelID
        ) // Create notification builder, assign channel ID
            .setSmallIcon(R.drawable.baseline_battery_charging_full_24) // Set notification icon
            .setContentTitle(getString(R.string.service_notification_notification_content_text)) // Set the notification content (body) text
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE) // Make this a service notification
            .setOngoing(true) // This makes it persistent
            .setContentIntent(pendingIntent) // When the notification is tapped, launch MainActivity.
            .build() // Put it all together!

        /* Start the service as a foreground service with the notification we just created. */
        startForeground(persistentNotificationID, serviceNotification)

        Log.i(serviceLogTag, "INIT: Foreground service has started")
        return START_STICKY // Tell OS to recreate the service when it has enough memory if the service was killed due to lack of memory
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.e(serviceLogTag, "A bind to service was attempted. onBind is not (yet) implemented.")
        TODO("A bind to service was attempted. onBind is not (yet) implemented")
    }

    /*
    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
*/

    override fun onCreate() {
        super.onCreate()
        Log.i(serviceLogTag, "Service was created")
    }

    override fun onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        //handler.removeCallbacks(runnable)
        if (showDebugToasts) {
            Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show()
        }
        Log.w(serviceLogTag, "Service was destroyed!")

        unregisterReceiver(onChargingStatusChangedReceiver)
    }


    /* This is the magic! */
    fun playChargingStartedSound() {
        // Load the user preferences for the service
        val servicePreferences = ServicePreferences(applicationContext)

        // If vibration is enabled, vibrate the device
        if (servicePreferences.vibrationEnabled) {
            val vibrationHelper = VibrationHelper(applicationContext)
            vibrationHelper.vibrateMs(servicePreferences.vibrationLengthMs.toLong())
        }

        soundManager.playSound(
            context = applicationContext,
            servicePreferences = servicePreferences,
            soundToPlay = Sounds.ChargingStarted,
            logTag = serviceLogTag,
            logMessagePrefix = "playChargingStartedSound()"
        )
    }

    fun playChargingStoppedSound() {
        // Load the user preferences for the service
        val servicePreferences = ServicePreferences(applicationContext)

        soundManager.playSound(
            context = applicationContext,
            servicePreferences = servicePreferences,
            soundToPlay = Sounds.ChargingStopped,
            logTag = serviceLogTag,
            logMessagePrefix = "playChargingStoppedSound()"
        )
    }
}