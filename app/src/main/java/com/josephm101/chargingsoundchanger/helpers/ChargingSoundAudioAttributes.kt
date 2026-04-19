package com.josephm101.chargingsoundchanger.helpers

import android.media.AudioAttributes

fun getChargingSoundAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        //.setFlags(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        //.setLegacyStreamType(AudioManager.STREAM_RING)
        //.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}