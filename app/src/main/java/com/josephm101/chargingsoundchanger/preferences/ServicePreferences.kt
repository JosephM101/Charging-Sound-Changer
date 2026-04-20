package com.josephm101.chargingsoundchanger.preferences

import android.content.Context
import hu.autsoft.krate.SimpleKrate
import hu.autsoft.krate.booleanPref
import hu.autsoft.krate.default.withDefault
import hu.autsoft.krate.floatPref
import hu.autsoft.krate.intPref
import hu.autsoft.krate.stringPref

// Uses Krate library. See: https://github.com/ZenitechSoftware/Krate

class ServicePreferences(context: Context) : SimpleKrate(context) {
    // If false, charging sound will not play.
    var soundsEnabled by booleanPref().withDefault(true)

    var chargingStartedSoundFilePath by stringPref().withDefault("")
    var chargingStartedSoundFileName by stringPref().withDefault("")
    var chargingStartedSoundPlaybackVolume by floatPref().withDefault(1.0f) // Sets how loud the sound will be when played (0.0f to 1.0f)

    var chargingStoppedSoundFilePath by stringPref().withDefault("")
    var chargingStoppedSoundFileName by stringPref().withDefault("")
    //var chargingStoppedSoundPlaybackVolume by floatPref().withDefault(1.0f) // Sets how loud the sound will be when played (0.0f to 1.0f)
    var chargingStoppedSoundEnabled by booleanPref().withDefault(false)

    // If enabled, device will vibrate for an amount of time (defined by vibrationLengthMs) when charging begins.
    // This simulates default Android behavior.
    /// TODO: implement vibration on power connect since built-in Android functionality will be disabled
    var vibrationEnabled by booleanPref().withDefault(false)
    var vibrationLengthMs by intPref().withDefault(400)

    // When sound is played, wait a preset amount of time before it's possible to play the sound again.
    // May be useful for devices with finicky chargers, connections, etc.
    /// TODO: debounceEnabled should be implemented
    //var debounceEnabled by booleanPref().withDefault(false)

    // If Do Not Disturb is enabled, the sound and vibration will not play.
    // This replicates default Android behavior.
    //var respectDoNotDisturb by booleanPref().withDefault(true)
}