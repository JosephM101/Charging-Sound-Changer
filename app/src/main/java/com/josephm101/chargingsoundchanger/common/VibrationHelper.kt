package com.josephm101.chargingsoundchanger.common

import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationHelper(context: Context) {
    var vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrateMs(ms: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortVibration = VibrationEffect.createOneShot(
                ms,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(shortVibration)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }
}