package com.example.data

import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

/** Jeu synthétique en mémoire uniquement. Il n'est jamais inséré dans Room. */
object DemoDataset {
    private fun day(monthOffset: Int, day: Int): Long = Calendar.getInstance().apply {
        add(Calendar.MONTH, monthOffset)
        set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val profile = ChurchProfileEntity(
        churchName = "Église Lumière — Démonstration",
        denomination = "Communauté de démonstration",
        city = "Kandi",
        address = "Données fictives pour explorer Lumina",
        pastorName = "Profil exemple",
        treasurerName = "Compte lecture seule",
        accentColorHex = "#0F766E",
        onboardingCompleted = true
    )

    val user = UserEntity(
        id = -1L,
        uid = "lumina-demo-read-only",
        displayName = "Visiteur — démonstration",
        role = UserRole.LECTURE.name,
        isActive = true
    )

    val registers = listOf(
        CashRegisterEntity(id = 1L, uid = "demo-cash", name = "Caisse principale", description = "Espèces", iconType = "cash", isDefault = true),
        CashRegisterEntity(id = 2L, uid = "demo-bank", name = "Compte bancaire", description = "Compte de démonstration", iconType = "bank")
    )

    val categories = listOf(
        CategoryEntity(id = 1L, uid = "demo-cat-dime", name = "Dîmes", type = TransactionType.INCOME, colorHex = "#0F766E", isDefault = true),
        CategoryEntity(id = 2L, uid = "demo-cat-offrande", name = "Offrandes", type = TransactionType.INCOME, colorHex = "#047857", isDefault = true),
        CategoryEntity(id = 3L, uid = "demo-cat-aide", name = "Aides & secours", type = TransactionType.EXPENSE, colorHex = "#B9382E", isDefault = true),
        CategoryEntity(id = 4L, uid = "demo-cat-activite", name = "Activités", type = TransactionType.EXPENSE, colorHex = "#1D4ED8", isDefault = true)
    )

    private val monthKey = SimpleDateFormat("yyyy-MM", Locale.FRANCE).format(java.util.Date())
    val budgets = listOf(
        BudgetEntity(id = 1L, categoryName = "Aides & secours", amountCents = 180_000L, monthYearKey = monthKey, alertThresholdPercent = 80),
        BudgetEntity(id = 2L, categoryName = "Activités", amountCents = 120_000L, monthYearKey = monthKey, alertThresholdPercent = 75)
    )

    val projects = listOf(
        ProjectEntity(id = 1L, uid = "demo-campaign", name = "Campagne de solidarité", description = "Collecte fictive pour illustrer les fonds affectés.", targetCents = 600_000L, kind = "CAMPAIGN"),
        ProjectEntity(id = 2L, uid = "demo-event", name = "Journée communautaire", description = "Événement exemple avec dons et dépenses rattachés.", targetCents = 250_000L, kind = "EVENT")
    )

    val members = listOf(
        MemberEntity(id = 1L, uid = "demo-member-1", firstName = "Awa", lastName = "Bio", phone = "+229 90 00 00 01", profession = "Enseignante"),
        MemberEntity(id = 2L, uid = "demo-member-2", firstName = "Moussa", lastName = "Sabi", phone = "+229 90 00 00 02", profession = "Artisan"),
        MemberEntity(id = 3L, uid = "demo-member-3", firstName = "Hadiza", lastName = "Yaro", phone = "+229 90 00 00 03", profession = "Commerçante")
    )

    val transactions = listOf(
        TransactionEntity(id = 1L, uid = "demo-tx-1", title = "Dîmes — dimanche", type = TransactionType.INCOME, amountCents = 185_000L, category = "Dîmes", timestamp = day(-5, 5), cashRegisterId = 1L, cashRegisterName = "Caisse principale"),
        TransactionEntity(id = 2L, uid = "demo-tx-2", title = "Offrandes", type = TransactionType.INCOME, amountCents = 95_000L, category = "Offrandes", timestamp = day(-4, 12), cashRegisterId = 2L, cashRegisterName = "Compte bancaire"),
        TransactionEntity(id = 3L, uid = "demo-tx-3", title = "Aide alimentaire", type = TransactionType.EXPENSE, amountCents = 48_000L, category = "Aides & secours", timestamp = day(-3, 18), cashRegisterId = 1L, cashRegisterName = "Caisse principale"),
        TransactionEntity(id = 4L, uid = "demo-tx-4", title = "Don campagne solidarité", type = TransactionType.INCOME, amountCents = 130_000L, category = "Offrandes", timestamp = day(-2, 8), cashRegisterId = 2L, cashRegisterName = "Compte bancaire", projectId = 1L),
        TransactionEntity(id = 5L, uid = "demo-tx-5", title = "Location salle", type = TransactionType.EXPENSE, amountCents = 70_000L, category = "Activités", timestamp = day(-1, 16), cashRegisterId = 1L, cashRegisterName = "Caisse principale", projectId = 2L),
        TransactionEntity(id = 6L, uid = "demo-tx-6", title = "Dîmes — dimanche", type = TransactionType.INCOME, amountCents = 205_000L, category = "Dîmes", timestamp = day(0, 4), cashRegisterId = 1L, cashRegisterName = "Caisse principale"),
        TransactionEntity(id = 7L, uid = "demo-tx-7", title = "Denrées pour la communauté", type = TransactionType.EXPENSE, amountCents = 32_000L, category = "Aides & secours", timestamp = day(0, 9), cashRegisterId = 1L, cashRegisterName = "Caisse principale", projectId = 1L),
        TransactionEntity(id = 8L, uid = "demo-tx-8", title = "Participation événement", type = TransactionType.INCOME, amountCents = 85_000L, category = "Offrandes", timestamp = day(0, 15), cashRegisterId = 2L, cashRegisterName = "Compte bancaire", projectId = 2L)
    )
}
