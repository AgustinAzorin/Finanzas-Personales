package com.agustinazorin.finanzas.core.preferences

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "quick_add_prefs"
private const val KEY_LAST_ACCOUNT_ID = "last_account_id"

/**
 * Recuerda la última cuenta usada en la Alta rápida (CLAUDE.md, sección 28), puramente local.
 * No es información financiera sensible, así que no requiere cifrado (a diferencia de la
 * base de datos, ver Fase 8).
 */
@Singleton
class QuickAddPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastAccountId: Long?
        get() = prefs.getLong(KEY_LAST_ACCOUNT_ID, -1).takeIf { it != -1L }
        set(value) = prefs.edit { if (value == null) remove(KEY_LAST_ACCOUNT_ID) else putLong(KEY_LAST_ACCOUNT_ID, value) }
}
