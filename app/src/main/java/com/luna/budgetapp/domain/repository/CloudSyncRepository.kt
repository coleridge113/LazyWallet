package com.luna.budgetapp.domain.repository

interface CloudSyncRepository {

    suspend fun uploadLocalData()

    suspend fun downloadCloudData()
}
