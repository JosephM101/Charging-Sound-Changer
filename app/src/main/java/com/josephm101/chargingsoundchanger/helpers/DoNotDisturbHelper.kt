package com.josephm101.chargingsoundchanger.helpers

import android.app.NotificationManager
import android.content.Context

class DoNotDisturbHelper {
    companion object {
        fun isDoNotDisturbEnabled(context: Context): Boolean {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        }
    }
}