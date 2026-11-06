package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Les mêmes six chapitres alimentent l'accueil initial et le guide des paramètres. */
data class LuminaFeatureChapter(
    val title: String,
    val description: String,
    val benefit: String,
    val icon: ImageVector,
    val detailIcon: ImageVector
)

val LuminaFeatureChapters = listOf(
    LuminaFeatureChapter(
        title = "Vos finances en un regard",
        description = "Le tableau de bord rassemble soldes, entrées, dépenses et tendances pour la période choisie.",
        benefit = "Une situation claire, sans calcul manuel",
        icon = Icons.Default.AccountBalance,
        detailIcon = Icons.Default.PieChart
    ),
    LuminaFeatureChapter(
        title = "Chaque opération bien suivie",
        description = "Enregistrez recettes et dépenses, choisissez une caisse, ajoutez une note et joignez le reçu.",
        benefit = "Une piste d'audit facile à relire",
        icon = Icons.Default.Payments,
        detailIcon = Icons.Default.Description
    ),
    LuminaFeatureChapter(
        title = "Des budgets sous contrôle",
        description = "Fixez un plafond par catégorie et comparez le prévu au réalisé avant qu'un poste ne dérape.",
        benefit = "Vous voyez les écarts à temps",
        icon = Icons.Default.PieChart,
        detailIcon = Icons.Default.VerifiedUser
    ),
    LuminaFeatureChapter(
        title = "Projets, collectes et événements",
        description = "Affectez les dons et les dépenses à un objectif pour savoir ce qui est collecté, engagé et disponible.",
        benefit = "Les fonds affectés restent traçables",
        icon = Icons.Default.Groups,
        detailIcon = Icons.Default.Payments
    ),
    LuminaFeatureChapter(
        title = "Des rapports prêts à présenter",
        description = "Générez des états lisibles, avec les mouvements, les soldes de caisse et les contrôles utiles.",
        benefit = "Une information claire pour l'équipe",
        icon = Icons.Default.Description,
        detailIcon = Icons.Default.AccountBalance
    ),
    LuminaFeatureChapter(
        title = "Une équipe et des données protégées",
        description = "Chacun utilise son accès local. Travaillez hors ligne et partagez une sauvegarde chiffrée quand vous le décidez.",
        benefit = "Vous gardez la maîtrise du dossier",
        icon = Icons.Default.Groups,
        detailIcon = Icons.Default.Lock
    )
)

/**
 * Visite guidée unique, réutilisée avant la configuration et dans Paramètres.
 * Les illustrations sont entièrement vectorielles (ImageVector + formes Compose).
 */
@Composable
fun LuminaFeatureTour(
    page: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onJoinFromFile: (() -> Unit)? = null,
    finalActionLabel: String = "Terminer",
    closeLabel: String = "Passer",
    modifier: Modifier = Modifier
) {
    val safePage = page.coerceIn(0, LuminaFeatureChapters.lastIndex)
    val chapter = LuminaFeatureChapters[safePage]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("lumina_feature_tour"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LUMINA  ·  DÉCOUVRIR",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onSkip, modifier = Modifier.testTag("feature_tour_skip")) {
                Text(closeLabel)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LuminaFeatureChapters.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= safePage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            FeatureIllustration(
                chapter = chapter,
                page = safePage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )
            Spacer(Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = "CHAPITRE 0${safePage + 1}  /  06",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = chapter.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = chapter.description,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = chapter.benefit,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            if (safePage == 0 && onJoinFromFile != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onJoinFromFile, modifier = Modifier.testTag("join_with_lumina_file")) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("J'ai déjà un fichier Lumina")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPrevious,
                enabled = safePage > 0,
                modifier = Modifier.testTag("feature_tour_previous")
            ) {
                Text("Retour")
            }
            Text(
                text = "${safePage + 1} sur ${LuminaFeatureChapters.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onNext,
                modifier = Modifier.height(52.dp).testTag("feature_tour_next"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (safePage == LuminaFeatureChapters.lastIndex) finalActionLabel else "Continuer",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeatureIllustration(
    chapter: LuminaFeatureChapter,
    page: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
                    )
                )
            )
            .semantics { contentDescription = "Illustration vectorielle : ${chapter.title}" }
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 24.dp)
                .size(148.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.08f))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 25.dp, bottom = 12.dp)
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.09f))
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(136.dp),
            shape = RoundedCornerShape(38.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            contentColor = primary,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.88f), primary.copy(alpha = 0.30f))
                )
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(chapter.icon, contentDescription = null, modifier = Modifier.size(76.dp), tint = primary)
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 17.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
            contentColor = onSurface,
            shadowElevation = 5.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(chapter.detailIcon, contentDescription = null, modifier = Modifier.size(18.dp), tint = primary)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = when (page) {
                        0 -> "Période"
                        1 -> "Justificatif"
                        2 -> "Prévu / réel"
                        3 -> "Fonds dédiés"
                        4 -> "Rapport"
                        else -> "Accès local"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 15.dp),
            shape = RoundedCornerShape(16.dp),
            color = primary.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 7.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Simple & fiable", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
