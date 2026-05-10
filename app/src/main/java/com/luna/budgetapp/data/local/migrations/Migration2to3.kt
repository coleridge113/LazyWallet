package com.luna.budgetapp.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN remote_id TEXT")
        db.execSQL("ALTER TABLE expense_presets ADD COLUMN remote_id TEXT")
    }
}
