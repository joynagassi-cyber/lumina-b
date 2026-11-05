@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.LuminaDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import com.example.export.BackupService
import com.example.ui.FinancialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.util.UUID
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DemoWorkspaceIsolationTest {
    @Test
    fun une_ecriture_d_essai_ne_persiste_jamais_dans_la_base_reelle() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LuminaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val workspaceId = "demo-test-${UUID.randomUUID().toString().replace("-", "")}"
        val repository = TransactionRepository(
            database.transactionDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.cashRegisterDao(),
            database.churchProfileDao(),
            database.memberDao(),
            database.userDao(),
            database.projectDao(),
            workspaceId = workspaceId
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = ViewModelStore()
        Dispatchers.setMain(dispatcher)
        try {
            repository.insert(
                TransactionEntity(
                    title = "Opération réelle avant la démo",
                    type = TransactionType.INCOME,
                    amountCents = 7000,
                    category = "Don"
                )
            )
            val initialTransactions = repository.snapshotAll().transactions
            val viewModel = FinancialViewModel(repository)
            store.put("demo-test", viewModel)
            viewModel.startDemoMode()
            viewModel.addTransaction(
                title = "Écriture d'essai",
                type = TransactionType.INCOME,
                amountCents = 5000,
                category = "Don",
                timestamp = System.currentTimeMillis()
            )
            viewModel.runAutoBackup(context)
            advanceUntilIdle()

            assertEquals(initialTransactions, repository.snapshotAll().transactions)
            assertNull(BackupService.latestAutoFile(context, workspaceId))

            viewModel.exitDemoMode()
            advanceUntilIdle()
            assertEquals(initialTransactions, repository.snapshotAll().transactions)
        } finally {
            store.clear()
            File(context.filesDir, "sauvegardes/$workspaceId").deleteRecursively()
            database.close()
            Dispatchers.resetMain()
        }
    }
}
