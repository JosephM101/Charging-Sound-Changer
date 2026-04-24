package com.josephm101.chargingsoundchanger.helpers.soundmanager

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.josephm101.chargingsoundchanger.helpers.DoNotDisturbHelper
import com.josephm101.chargingsoundchanger.preferences.ServicePreferences
import java.io.File


class SoundManager {
    companion object {
        /**
         * Returns a new, preset instance of AudioAttributes configured to play sounds using Android's notification sound channel
         * Used by default for playSound()
         */
        fun getChargingSoundDefaultAudioAttributes(): AudioAttributes {
            return AudioAttributes.Builder()
                //.setFlags(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                //.setLegacyStreamType(AudioManager.STREAM_RING)
                //.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }

        /**
         * The maximum length, in seconds, that a user-selected sound can be
         */
        const val MAX_SOUND_DURATION_IN_SECONDS = 5

        /**
         * The maximum length, in milliseconds, that a user-selected sound can be.
         * Automatically calculated using MAX_SOUND_DURATION_IN_SECONDS
         */
        const val MAX_SOUND_DURATION_IN_MILLISECONDS = MAX_SOUND_DURATION_IN_SECONDS * 1000
    }

    /**
     * Plays either the "ChargingStarted" sound or "ChargingStopped" sound using service settings
     *
     * @param context The context of the calling activity, used to check the status of Do Not Disturb
     * @param servicePreferences An instance of servicePreferences to load settings from such as sound volume
     * @param soundToPlay The sound to play
     * @param logTag The tag that the logger should use (should match the log tag used by the calling class/activity. e.g. "ChargingSoundService")
     * @param logMessagePrefix The text to put at the beginning of the log message, formatted as "%s: <message>". In this instance, this should be the name of the calling function. For example, "testChargingSound()"
     * @param ignoreSoundEnableSetting When true, disregard the "Enable Sound" setting. Even if the "Enable Sound" setting is disabled, the requested sound will play anyway.
     * @param onSoundCompleted The function to run when the sound has finished playing
     *
     */
    fun playSound(
        context: Context,
        servicePreferences: ServicePreferences,
        soundToPlay: Sounds,
        logTag: String = "SoundManager",
        logMessagePrefix: String? = "playSound()",
        ignoreSoundEnableSetting: Boolean = false,
        onSoundStarted: (duration: Int) -> Unit = {},
        onSoundCompleted: () -> Unit = {},
        //progressListener: ((progress: Float, pos: Int, duration: Int) -> Unit)? = null
    ): SoundPlaybackResult {
        val uniqueCallID =
            (100..500).random() // Assign this sound playback request with a unique ID for logging purposes

        fun makeLogMessage(message: String): String {
            // Merge the uniqueCallID and log message together
            val logMessageBody = "[$uniqueCallID] $message"

            // If logMessagePrefix is not null or blank, join it and the logMessageBody string (respectively) together.
            // Otherwise, just return "logMessageBody"
            return logMessagePrefix?.ifBlank { null }?.let { "$it: $logMessageBody" }
                ?: logMessageBody
        }

        // Check if sounds are disabled
        if (!servicePreferences.soundsEnabled && !ignoreSoundEnableSetting) {
            Log.d(logTag, makeLogMessage("chargingSoundEnabled=false, not playing sound"))
            return SoundPlaybackResult.SoundsDisabled // abort
        }

        // Check if Do Not Disturb is enabled
        if (DoNotDisturbHelper.isDoNotDisturbEnabled(context)) {
            Log.d(
                logTag,
                makeLogMessage("DND is enabled, not playing sound")
            )
            return SoundPlaybackResult.DoNotDisturbIsEnabled // abort
        }

        // Figure out which sound we're supposed to play
        Log.d(logTag, makeLogMessage("${soundToPlay.name} requested"))
        val soundFilePath: String = when (soundToPlay) {
            Sounds.ChargingStarted -> {
                servicePreferences.chargingStartedSoundFilePath
            }

            Sounds.ChargingStopped -> {
                servicePreferences.chargingStoppedSoundFilePath
            }
        }

        // If no sound file has been set for the selected sound
        if (soundFilePath == "") {
            when (soundToPlay) {
                Sounds.ChargingStarted -> {
                    Log.e(logTag, makeLogMessage("ChargingStartedSoundFilePath not initialized"))
                }

                Sounds.ChargingStopped -> {
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
        /// TODO: The time between trigger and sound playback could theoretically be made quicker by preemptively loading the audio file and possibly using two separate media players
        mediaPlayer.setDataSource(soundFile.absolutePath)
        mediaPlayer.prepare()
        Log.d(logTag, makeLogMessage("Loaded audio asset"))

        // Set playback volume from preferences.
        val playbackVolume = servicePreferences.soundPlaybackVolume
        mediaPlayer.setVolume(playbackVolume, playbackVolume)
        Log.d(logTag, makeLogMessage("Set media player volume ($playbackVolume)"))

        // Play the sound!
        Log.i(logTag, makeLogMessage("Playing audio"))
        try {
            mediaPlayer.start()
        } catch (e: IllegalStateException) {
            Log.e(logTag, makeLogMessage("ERROR: mediaPlayer.start() threw IllegalStateException"))
            e.printStackTrace()
            return SoundPlaybackResult.IllegalStateException
        }

        onSoundStarted(mediaPlayer.duration)

        Log.d(logTag, makeLogMessage("duration: ${mediaPlayer.duration}"))

        /*
        if (progressListener != null) {
            val loopHandler = Handler(Looper.getMainLooper())
            val refreshMs: Long = 10
            val reportProgressTimer = object : Runnable {
                override fun run() {
                    try {
                        Log.d(logTag, makeLogMessage("pos: ${mediaPlayer.currentPosition}"))
                        progressListener.invoke(
                            (mediaPlayer.currentPosition.toFloat() / mediaPlayer.duration.toFloat()),
                            mediaPlayer.currentPosition,
                            mediaPlayer.duration
                        )
                        if (mediaPlayer.isPlaying) {
                            loopHandler.postDelayed(this, refreshMs)
                        }
                    } catch (e: java.lang.IllegalStateException) {
                        Log.i(
                            logTag,
                            makeLogMessage("reportProgress: Got IllegalStateException. Whatever, it's probably a race condition.")
                        )
                    }
                }
            }
            loopHandler.postDelayed(reportProgressTimer, refreshMs)
        }
         */

        // Set what we should do when playback is finished
        mediaPlayer.setOnCompletionListener {
            Log.i(logTag, makeLogMessage("Playback complete"))

            // When we're done, release resources currently held by the media player
            mediaPlayer.release()
            Log.d(logTag, makeLogMessage("mediaPlayer.release()"))
            onSoundCompleted()
        }

        return SoundPlaybackResult.Success
    }
}