package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LuminaDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ChurchDatabaseIsolationTest {
    @Test
    fun deux_dossiers_ouvrent_des_bases_independantes() = runBlocking(Dispatchers.IO) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val nameA = "lumina_test_${suffix}_a.db"
        val nameB = "lumina_test_${suffix}_b.db"
        val databaseA = LuminaDatabase.getDatabaseNamed(context, nameA)
        val databaseB = LuminaDatabase.getDatabaseNamed(context, nameB)

        try {
            databaseA.transactionDao().insertTransaction(
                TransactionEntity(
                    title = "Opération dossier A",
                    type = TransactionType.EXPENSE,
                    amountCents = 1234,
                    category = "Test"
                )
            )

            assertEquals(1, databaseA.transactionDao().getAllTransactions().first().size)
            assertEquals(0, databaseB.transactionDao().getAllTransactions().first().size)
        } finally {
            LuminaDatabase.closeDatabaseNamed(nameA)
            LuminaDatabase.closeDatabaseNamed(nameB)
            context.deleteDatabase(nameA)
            context.deleteDatabase(nameB)
        }
    }
}
