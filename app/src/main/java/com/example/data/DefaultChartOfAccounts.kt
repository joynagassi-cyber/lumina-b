package com.example.data

/**
 * Plan de comptes de départ, commun à la création du dossier et à une remise
 * à zéro : une église retrouve toujours la même liste, sans données fictives.
 */
object DefaultChartOfAccounts {

    /** (nom, couleur, icône) — sorties de caisse. */
    val expenses: List<Triple<String, String, String>> = listOf(
        Triple("Salaires & indemnités", "#3730A3", ""),
        Triple("Loyer & charges", "#4338CA", ""),
        Triple("Électricité & eau", "#4F46E5", ""),
        Triple("Entretien & réparations", "#6366F1", ""),
        Triple("Équipement & matériel", "#818CF8", ""),
        Triple("Transport & carburant", "#52525B", ""),
        Triple("Aide sociale & diaconie", "#71717A", ""),
        Triple("Missions & évangélisation", "#3F3F46", ""),
        Triple("Communication & internet", "#27272A", ""),
        Triple("Fournitures de bureau", "#57534E", ""),
        Triple("Restauration & accueil", "#78716C", ""),
        Triple("Autres sorties", "#A1A1AA", "")
    )

    /** (nom, couleur, icône) — entrées. */
    val incomes: List<Triple<String, String, String>> = listOf(
        Triple("Dîmes", "#1E1B4B", ""),
        Triple("Offrandes", "#3730A3", ""),
        Triple("Dons spéciaux", "#4338CA", ""),
        Triple("Projets & constructions", "#4F46E5", ""),
        Triple("Vente d'objets & littérature", "#6366F1", ""),
        Triple("Subventions & partenariats", "#818CF8", ""),
        Triple("Autres entrées", "#A1A1AA", "")
    )
}
