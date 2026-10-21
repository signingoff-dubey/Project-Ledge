package com.cedarworks.ledge.demo.onboarding

import android.content.Context

/**
 * Remembers that first-launch setup finished, and which permission prompts
 * have already been shown, so a denial is not requested again on its own.
 */
class SetupStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isFinished(): Boolean = prefs.getBoolean(FINISHED, false)

    fun finish() {
        prefs.edit().putBoolean(FINISHED, true).apply()
    }

    fun markOutsideAccessExplained() {
        prefs.edit().putBoolean(OUTSIDE, true).apply()
    }

    fun explainedOutsideAccess(): Boolean = prefs.getBoolean(OUTSIDE, false)

    fun reset() {
        prefs.edit().putBoolean(FINISHED, false).apply()
    }

    fun wasAsked(id: DockPermissionId): Boolean = asked().contains(id.name)

    fun markAsked(id: DockPermissionId) {
        val next = asked().toMutableSet()
        next += id.name
        prefs.edit().putStringSet(ASKED, next).apply()
    }

    private fun asked(): Set<String> = prefs.getStringSet(ASKED, emptySet()).orEmpty()

    private companion object {
        const val FILE = "ledge_setup"
        const val FINISHED = "finished"
        const val OUTSIDE = "outside_access"
        const val ASKED = "asked"
    }
}
