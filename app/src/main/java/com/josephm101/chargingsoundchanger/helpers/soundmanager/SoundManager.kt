package com.josephm101.chargingsoundchanger.helpers.soundmanager

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat.createAttributionContext
import com.josephm101.chargingsoundchanger.DoNotDisturb
import com.josephm101.chargingsoundchanger.preferences.ServicePreferences
import java.io.File


class SoundManager {
    companion object {
        fun getChargingSoundDefaultAudioAttributes(): AudioAttributes {
            return AudioAttributes.Builder()
                //.setFlags(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                //.setLegacyStreamType(AudioManager.STREAM_RING)
                //.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }

        val maxSoundDurationInSeconds = 5
        val maxSoundDurationInMilliseconds = maxSoundDurationInSeconds * 1000
    }

    /**
     * Plays either the "ChargingStarted" sound or "ChargingStopped" sound using service settings
     *
     * @param soundToPlay The sound to play
     * @param logTag The tag that the logger should use (should match the log tag used by the calling class/activity. e.g. "ChargingSoundService")
     * @param logMessagePrefix The text to put at the beginning of the log message, formatted as "%s: <message>". In this instance, this should be the name of the calling function. For example, "testChargingSound()"
     * @param ignoreSoundEnableSetting Disregard the Enable sound setting. If true, even if the setting is disabled, the sound will play anyway.
     */
    fun playSound(
        context: Context,
        servicePreferences: ServicePreferences,
        soundToPlay: Sounds,
        logTag: String = "SoundManager",
        logMessagePrefix: String? = "playSound()",
        ignoreSoundEnableSetting: Boolean = false
    ): SoundPlaybackResult {

        val uniqueCallID = (100..500).random()
        fun makeLogMessage(message: String): String {
            val root = "[$uniqueCallID] $message"
            return logMessagePrefix?.ifBlank { null }?.let { "$it: $root" } ?: root
        }

        // Check if sounds are disabled
        if (!servicePreferences.soundsEnabled && !ignoreSoundEnableSetting) {
            Log.d(logTag, makeLogMessage("chargingSoundEnabled=false, not playing sound"))
            return SoundPlaybackResult.SoundsDisabled // abort
        }

        // Check if Do Not Disturb is enabled
        if (DoNotDisturb.isDndEnabled(context)) {
            Log.d(
                logTag,
                makeLogMessage("DND is enabled, not playing sound")
            )
            return SoundPlaybackResult.DoNotDisturbIsEnabled // abort
        }

        // Figure out which sound we're supposed to play
        Log.d(logTag, makeLogMessage("${soundToPlay.name} requested"))
        val soundFilePath: String = when (soundToPlay) {
            Sounds.ChargingStartedSound -> {
                servicePreferences.chargingStartedSoundFilePath
            }

            Sounds.ChargingStoppedSound -> {
                servicePreferences.chargingStoppedSoundFilePath
            }
        }

        // If no sound file has been set for the selected sound
        if (soundFilePath == "") {
            when (soundToPlay) {
                Sounds.ChargingStartedSound -> {
                    Log.e(logTag, makeLogMessage("ChargingStartedSoundFilePath not initialized"))
                }

                Sounds.ChargingStoppedSound -> {
                    Log.e(logTag, makeLogMessage("ChargingStoppedSoundFilePath not initialized"))
                }
            }
            return SoundPlaybackResult.FilePathNeverInitialized
        }

        // Check to make sure that the sound file exists
        val soundFile = File(soundFilePath)
        if (!soundFile.exists()) {
            // Log error
            Log.e(
                logTag,
                makeLogMessage("Could not find the sound file '${soundFilePath}'!")
            )
            return SoundPlaybackResult.FileNotFound // abort
        }

        // Set up a media player that uses the notifications & alerts audio channel
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioAttributes(getChargingSoundDefaultAudioAttributes())
        Log.d(logTag, makeLogMessage("Created media player"))

        // Load the sound file and prepare the media player
        /// TODO: The time between trigger and sound playback could be made quicker by preemptively loading the audio file and possibly using two separate media players
        mediaPlayer.setDataSource(soundFile.absolutePath)
        mediaPlayer.prepare()
        Log.d(logTag, makeLogMessage("Loaded audio asset"))

        // Set playback volume from preferences.
        val playbackVolume = servicePreferences.chargingStartedSoundPlaybackVolume
        mediaPlayer.setVolume(playbackVolume, playbackVolume)
        Log.d(logTag, makeLogMessage("Set media player volume ($playbackVolume)"))

        // Play the sound!
        Log.i(logTag, makeLogMessage("Playing audio"))
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            Log.i(logTag, makeLogMessage("Playback complete"))
            mediaPlayer.release() // Release resources associated with the media player
        }

        return SoundPlaybackResult.Success
    }
}