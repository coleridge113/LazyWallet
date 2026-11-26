package com.luna.budgetapp.data.local.repository

import com.luna.budgetapp.data.local.AppDatabase
import com.luna.budgetapp.domain.repository.LocalDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDataRepositoryImpl(
    private val db: AppDatabase
) : LocalDataRepository {

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        db.clearAllTables()
    }
}
