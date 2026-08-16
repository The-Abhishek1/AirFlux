package com.xcloak.airflux.core.common

import android.content.Context

object OnboardingPrefs {
    private const val PREFS_NAME = "airflux_prefs"
    private const val KEY_ONBOARDED = "onboarding_complete"

    fun isComplete(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false)
    }

    fun markComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDED, true).apply()
    }
}