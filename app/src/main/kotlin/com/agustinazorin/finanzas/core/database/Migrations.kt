package com.agustinazorin.finanzas.core.database

import androidx.room.migration.Migration

/**
 * Todas las migraciones de [AppDatabase], en orden. `fallbackToDestructiveMigration()` está
 * prohibido (CLAUDE.md, sección "Estrategia de migraciones"): cada cambio de schema agrega acá
 * una [Migration] explícita, con su propio test usando `MigrationTestHelper`.
 *
 * La versión 1 es el schema inicial de Fase 0, no requiere migración.
 */
val APP_MIGRATIONS: Array<Migration> = arrayOf()
