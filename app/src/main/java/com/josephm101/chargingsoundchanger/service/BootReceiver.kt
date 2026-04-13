package com.josephm101.chargingsoundchanger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log

/*
BootReceiver

Starts the ChargingSoundService when Android finishes booting
*/

// https://www.dev2qa.com/how-to-start-android-service-automatically-at-boot-time/

class BootReceiver : BroadcastReceiver() {
    private val tagBootBroadcastReceiver = "BOOT_BROADCAST_RECEIVER"
    private lateinit var internalAppPreferences: SharedPreferences

    // Set logging tag to the simple name of the class
    private val logTag = BootReceiver::class.java.simpleName

    // When we receive the BOOT_COMPLETED message from the Android system, start the ChargingSoundService
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(tagBootBroadcastReceiver, "Starting ChargingSoundService")
            Intent(context, ChargingSoundService::class.java).also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(logTag, "Starting ChargingSoundService (SDK >=26)")
                    context.startForegroundService(it)
                    return
                }
                Log.d(logTag, "Starting ChargingSoundService (SDK <26)")
                context.startService(it)
            }
        }
    }
}