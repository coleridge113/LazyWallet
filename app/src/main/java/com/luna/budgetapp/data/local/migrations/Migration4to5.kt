package com.luna.budgetapp.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Migrate expenses table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expenses_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `remote_id` TEXT,
                `name` TEXT,
                `amount` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `date` INTEGER NOT NULL
            )
        """.trimIndent())
        
        db.execSQL("""
            INSERT INTO `expenses_new` (`id`, `remote_id`, `name`, `amount`, `category`, `type`, `date`)
            SELECT `id`, `remote_id`, `name`, CAST(`amount` * 100 AS INTEGER), `category`, `type`, `date` FROM `expenses`
        """.trimIndent())
        
        db.execSQL("DROP TABLE `expenses`")
        db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")

        // 2. Migrate expense_presets table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expense_presets_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `remote_id` TEXT,
                `amount` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL
            )
        """.trimIndent())
        
        db.execSQL("""
            INSERT INTO `expense_presets_new` (`id`, `remote_id`, `amount`, `category`, `type`, `created_at`)
            SELECT `id`, `remote_id`, CAST(`amount` * 100 AS INTEGER), `category`, `type`, `created_at` FROM `expense_presets`
        """.trimIndent())
        
        db.execSQL("DROP TABLE `expense_presets`")
        db.execSQL("ALTER TABLE `expense_presets_new` RENAME TO `expense_presets`")

        // 3. Migrate budget table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `budget_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `remote_id` TEXT,
                `limit` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `frequency` TEXT NOT NULL,
                `start_date` INTEGER NOT NULL,
                `end_date` INTEGER
            )
        """.trimIndent())
        
        db.execSQL("""
            INSERT INTO `budget_new` (`id`, `remote_id`, `limit`, `name`, `frequency`, `start_date`, `end_date`)
            SELECT `id`, `remote_id`, CAST(`limit` * 100 AS INTEGER), `name`, `frequency`, `start_date`, `end_date` FROM `budget`
        """.trimIndent())
        
        db.execSQL("DROP TABLE `budget`")
        db.execSQL("ALTER TABLE `budget_new` RENAME TO `budget`")
    }
}
