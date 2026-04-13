package com.josephm101.chargingsoundchanger.preferences

import android.content.Context
import com.squareup.moshi.Moshi
import hu.autsoft.krate.SimpleKrate
import hu.autsoft.krate.booleanPref
import hu.autsoft.krate.default.withDefault
import hu.autsoft.krate.moshi.moshi

class AppPreferences(context: Context) : SimpleKrate(context) {
    init {
        moshi = Moshi.Builder().build()
    }

    var hideOverviewDevMessageCard by booleanPref().withDefault(false)
    var hideOverviewInfoMessageCard by booleanPref().withDefault(false)
}