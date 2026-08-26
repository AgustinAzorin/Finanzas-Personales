package com.agustinazorin.finanzas.core.backup

import android.content.Context
import android.content.Intent
import android.os.Process

/**
 * Reinicia el proceso de la app. Hace falta después de importar un backup (CLAUDE.md, sección
 * 44): [BackupManager.import] cierra la conexión de Room a la base anterior y reemplaza el
 * archivo en disco, así que ningún componente vivo en este proceso puede seguir usándola de forma
 * segura — la única salida limpia es matar el proceso y arrancar de nuevo.
 */
fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Process.killProcess(Process.myPid())
}
