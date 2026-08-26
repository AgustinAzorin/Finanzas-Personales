package com.agustinazorin.finanzas.core.database

import androidx.room.migration.Migration

/**
 * Todas las migraciones de [AppDatabase], en orden. `fallbackToDestructiveMigration()` está
 * prohibido (CLAUDE.md, sección "Estrategia de migraciones"): cada cambio de schema agrega acá
 * una [Migration] explícita, con su propio test usando `MigrationTestHelper`.
 *
 * La versión 1 es el schema inicial (Fase 0, 1, 2 y 3, ver comentario en [AppDatabase]): todavía no
 * salió de este repositorio a un dispositivo real, así que no requiere migración. La primera vez
 * que se bumpee la versión, agregar acá la [Migration] correspondiente.
 */
val APP_MIGRATIONS: Array<Migration> = arrayOf()
