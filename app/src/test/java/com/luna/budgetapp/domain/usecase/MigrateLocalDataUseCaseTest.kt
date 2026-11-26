package com.luna.budgetapp.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.repository.FakeCloudSyncRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.sync.MigrateLocalDataUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MigrateLocalDataUseCaseTest {

    private val cloudSync = FakeCloudSyncRepository()
    private val settings = FakeSettingsRepository()

    @Test
    fun runsUploadThenDownload_thenMarksComplete() = runTest {
        MigrateLocalDataUseCase(cloudSync, settings)()

        assertThat(cloudSync.calls).containsExactly("upload", "download").inOrder()
        assertThat(settings.isMigratedFlow.first()).isTrue()
    }

    @Test
    fun uploadFails_doesNotMarkComplete() = runTest {
        cloudSync.uploadError = RuntimeException()

        val result = runCatching { MigrateLocalDataUseCase(cloudSync, settings)() }

        assertThat(result.isFailure).isTrue()
        assertThat(cloudSync.calls).containsExactly("upload")
        assertThat(settings.isMigratedFlow.first()).isFalse()
    }
}
