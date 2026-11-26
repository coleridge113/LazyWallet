package com.luna.budgetapp.domain.repository

class FakeLocalDataRepository : LocalDataRepository {

    var clearCount: Int = 0

    override suspend fun clearAll() {
        clearCount++
    }
}
