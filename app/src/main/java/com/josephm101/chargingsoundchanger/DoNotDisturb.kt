package com.josephm101.chargingsoundchanger

import android.app.NotificationManager
import android.content.Context

class DoNotDisturb {
    companion object {
        fun isDndEnabled(context: Context): Boolean {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        }
    }
}