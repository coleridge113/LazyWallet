package com.luna.budgetapp.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expense_presets` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "UPDATE `expense_presets` SET `position` = " +
                "(SELECT COUNT(*) FROM `expense_presets` p2 WHERE p2.`id` < `expense_presets`.`id`)"
        )
    }
}
