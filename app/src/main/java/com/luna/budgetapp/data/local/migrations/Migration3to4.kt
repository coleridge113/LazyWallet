package com.luna.budgetapp.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `budget` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `remote_id` TEXT,
                `limit` REAL NOT NULL,
                `name` TEXT NOT NULL,
                `frequency` TEXT NOT NULL,
                `start_date` INTEGER NOT NULL,
                `end_date` INTEGER
            )
        """.trimIndent())
    }
}
