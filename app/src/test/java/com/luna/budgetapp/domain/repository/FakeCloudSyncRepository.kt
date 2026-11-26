package com.luna.budgetapp.domain.repository

class FakeCloudSyncRepository(
    var uploadError: Throwable? = null,
    var downloadError: Throwable? = null
) : CloudSyncRepository {

    val calls: MutableList<String> = mutableListOf()

    override suspend fun uploadLocalData() {
        calls += "upload"
        uploadError?.let { throw it }
    }

    override suspend fun downloadCloudData() {
        calls += "download"
        downloadError?.let { throw it }
    }
}
