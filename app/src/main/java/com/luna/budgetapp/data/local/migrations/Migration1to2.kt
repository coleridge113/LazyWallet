package com.luna.budgetapp.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS category_filter (
                profile_name TEXT NOT NULL,
                category TEXT NOT NULL,
                is_active INTEGER NOT NULL,
                PRIMARY KEY(profile_name, category)
            )
        """.trimIndent())
    }
}
