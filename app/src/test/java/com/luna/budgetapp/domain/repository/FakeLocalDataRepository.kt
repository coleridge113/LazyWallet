package com.luna.budgetapp.domain.repository

class FakeLocalDataRepository(private val onClear: () -> Unit = {}) : LocalDataRepository {

    var clearCount: Int = 0

    override suspend fun clearAll() {
        onClear()
        clearCount++
    }
}
