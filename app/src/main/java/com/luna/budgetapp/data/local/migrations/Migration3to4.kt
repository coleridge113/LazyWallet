package com.luna.budgetapp.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create the clean parent budget table
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

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `budget_interactor_categories` (
                `budgetId` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                PRIMARY KEY(`budgetId`, `category`),
                FOREIGN KEY(`budgetId`) REFERENCES `budget`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_budget_interactor_categories_budgetId` 
            ON `budget_interactor_categories` (`budgetId`)
        """.trimIndent())
    }
}
