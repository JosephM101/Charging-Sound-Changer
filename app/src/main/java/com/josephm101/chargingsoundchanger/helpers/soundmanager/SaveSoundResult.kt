package com.josephm101.chargingsoundchanger.helpers.soundmanager

enum class SaveSoundResult {
    Success,
    DurationLimitExceeded,
    InvalidOrDamagedFile,
    InputStreamWasNull,
    DurationWasNull
}