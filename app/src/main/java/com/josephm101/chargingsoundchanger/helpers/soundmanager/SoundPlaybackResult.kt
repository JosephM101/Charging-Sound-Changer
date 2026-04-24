package com.josephm101.chargingsoundchanger.helpers.soundmanager

enum class SoundPlaybackResult {
    Success,
    IllegalStateException,
    FileNotFound,
    FilePathNeverInitialized,
    SoundsDisabled,
    DoNotDisturbIsEnabled
}