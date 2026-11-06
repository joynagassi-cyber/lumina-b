package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.data.ChurchProfileEntity
import com.example.ui.components.LuminaFeatureChapters
import com.example.ui.components.LuminaFeatureTour
import com.example.ui.components.LuminaOnboardingWelcome
import com.example.ui.designsystem.LuminaBranding
import com.example.ui.designsystem.LuminaBrandMark
import com.example.ui.theme.AccentPalettes
import com.example.ui.theme.parseHexColor
import com.example.util.ImageStore
import com.example.export.BackupSnapshot

/**
 * Assistant de premier lancement.
 *
 * Le parcours de découverte des fonctionnalités est fourni par la visite guidée
 * partagée avec Paramètres. L'assistant d'église qui suit conserve ses six étapes
 * de configuration et ses règles de sauvegarde locale ; le nom reste le seul champ
 * obligatoire et aucune donnée ne quitte le téléphone.
 */
@Composable
fun OnboardingScreen(
    initialProfile: ChurchProfileEntity = ChurchProfileEntity(),
    onFinish: (ChurchProfileEntity) -> Unit,
    modifier: Modifier = Modifier,
    startAtStep: Int = 0,
    finishLabel: String = "Ouvrir mon dossier",
    onOpenExistingProfiles: (() -> Unit)? = null,
    onJoinFromFile: (Uri, String, (Result<BackupSnapshot>) -> Unit) -> Unit = { _, _, result ->
        result(Result.failure(IllegalStateException("Import de fichier indisponible")))
    }
) {
    val context = LocalContext.current
    var introPage by remember { mutableIntStateOf(if (startAtStep > 0) LuminaFeatureChapters.size else -1) }
    var step by remember { mutableIntStateOf(startAtStep.coerceIn(0, 5)) }
    var selectedArchiveUri by remember { mutableStateOf<Uri?>(null) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinPassword by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    var churchName by remember { mutableStateOf(initialProfile.churchName) }
    var denomination by remember { mutableStateOf(initialProfile.denomination) }
    var city by remember { mutableStateOf(initialProfile.city) }
    var address by remember { mutableStateOf(initialProfile.address) }
    var phone by remember { mutableStateOf(initialProfile.phone) }
    var email by remember { mutableStateOf(initialProfile.email) }
    var pastorName by remember { mutableStateOf(initialProfile.pastorName) }
    var treasurerName by remember { mutableStateOf(initialProfile.treasurerName) }
    var accentHex by remember { mutableStateOf(initialProfile.accentColorHex) }
    var logoBase64 by remember { mutableStateOf(initialProfile.logoBase64) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageStore.uriToBase64(context, uri)
            if (encoded != null) {
                logoBase64 = encoded
                errorMessage = null
            } else {
                errorMessage = "Image illisible, essayez un autre fichier."
            }
        }
    }

    val archivePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedArchiveUri = uri
            joinPassword = ""
            joinError = null
            showJoinDialog = true
        }
    }

    val accent = parseHexColor(accentHex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("onboarding_screen")
    ) {
        when {
            introPage == -1 -> LuminaOnboardingWelcome(
                onDiscover = { introPage = 0 },
                onConfigure = { introPage = LuminaFeatureChapters.size },
                onJoinFromFile = { archivePicker.launch(arrayOf("*/*")) },
                onOpenExistingProfiles = onOpenExistingProfiles
            )
            introPage < LuminaFeatureChapters.size -> LuminaFeatureTour(
                page = introPage,
                onPrevious = { introPage = (introPage - 1).coerceAtLeast(0) },
                onNext = {
                    introPage = if (introPage == LuminaFeatureChapters.lastIndex) LuminaFeatureChapters.size
                    else introPage + 1
                },
                onSkip = { introPage = LuminaFeatureChapters.size },
                onJoinFromFile = { archivePicker.launch(arrayOf("*/*")) },
                onOpenExistingProfiles = onOpenExistingProfiles,
                finalActionLabel = "Configurer mon église",
                closeLabel = "Passer"
            )
            else -> {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Progression
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index <= step) accent
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (step) {
                    0 -> WelcomeStep(accentColor = accent)
                    1 -> IdentityStep(
                        churchName = churchName,
                        onChurchNameChange = { churchName = it; errorMessage = null },
                        denomination = denomination,
                        onDenominationChange = { denomination = it },
                        city = city,
                        onCityChange = { city = it },
                        address = address,
                        onAddressChange = { address = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        email = email,
                        onEmailChange = { email = it },
                        pastorName = pastorName,
                        onPastorNameChange = { pastorName = it },
                        treasurerName = treasurerName,
                        onTreasurerNameChange = { treasurerName = it }
                    )
                    2 -> OnboardingInfoStep(
                        icon = Icons.Default.Payments,
                        title = "Une gestion financière claire",
                        subtitle = "Lumina réunit les opérations et les prévisions pour vous aider à suivre les finances de votre église.",
                        points = listOf("Entrées et dépenses avec justificatifs", "Caisses et budgets séparés", "Projets, campagnes, événements et collectes affectés")
                    )
                    3 -> OnboardingInfoStep(
                        icon = Icons.Default.VerifiedUser,
                        title = "Vos données restent sous votre contrôle",
                        subtitle = "Le dossier fonctionne localement sur cet appareil ; les rapports et exports ne sont générés qu'à votre demande.",
                        points = listOf("Aucun compte cloud obligatoire", "Sauvegarde chiffrée disponible dans Paramètres", "Vérifiez régulièrement vos sauvegardes")
                    )
                    4 -> OnboardingInfoStep(
                        icon = Icons.Default.Groups,
                        title = "Une équipe avec des rôles",
                        subtitle = "Après la configuration, créez votre accès local et choisissez qui peut saisir, gérer ou consulter.",
                        points = listOf("Code PIN propre à l'utilisateur", "Rôles adaptés aux responsabilités", "Les droits peuvent être ajustés plus tard")
                    )
                    else -> BrandingStep(
                        churchName = churchName,
                        logoBase64 = logoBase64,
                        accentHex = accentHex,
                        onPickLogo = { logoPicker.launch("image/*") },
                        onRemoveLogo = { logoBase64 = "" },
                        onAccentSelected = { accentHex = it }
                    )
                }

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 0) {
                    TextButton(onClick = { step -= 1 }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retour")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        when (step) {
                            0 -> step = 1
                            1 -> {
                                if (churchName.isBlank()) {
                                    errorMessage = "Le nom de l'église est obligatoire."
                                } else {
                                    errorMessage = null
                                    step = 2
                                }
                            }
                            in 2..4 -> step += 1
                            else -> {
                                onFinish(
                                    initialProfile.copy(
                                        churchName = churchName.trim(),
                                        denomination = denomination.trim(),
                                        city = city.trim(),
                                        address = address.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        pastorName = pastorName.trim(),
                                        treasurerName = treasurerName.trim(),
                                        accentColorHex = accentHex,
                                        logoBase64 = logoBase64,
                                        onboardingCompleted = true,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("onboarding_primary_button")
                ) {
                    Text(if (step == 5) finishLabel else "Continuer", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (step == 5) Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Données locales, aucun compte ni serveur requis",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
            }
        }

        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { if (!joining) showJoinDialog = false },
                title = { Text("Rejoindre une église avec un fichier Lumina") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Vous pouvez importer une invitation légère (.lumina-invite) ou une archive Lumina complète. " +
                                "L'invitation ne contient pas les finances : après avoir choisi votre rôle et créé votre profil et PIN, importez séparément un fichier d'échange si l'église vous en transmet un. " +
                                "L'archive complète, elle, apporte aussi les données du dossier. Vos accès restent personnels sur cet appareil."
                        )
                        OutlinedTextField(
                            value = joinPassword,
                            onValueChange = { joinPassword = it },
                            label = { Text("Phrase de passe (obligatoire pour l'invitation légère)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            enabled = !joining,
                            modifier = Modifier.fillMaxWidth().testTag("join_archive_password")
                        )
                        joinError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !joining && selectedArchiveUri != null,
                        onClick = {
                            val uri = selectedArchiveUri ?: return@TextButton
                            joining = true
                            joinError = null
                            onJoinFromFile(uri, joinPassword) { result ->
                                joining = false
                                result.onSuccess {
                                    showJoinDialog = false
                                    selectedArchiveUri = null
                                    joinPassword = ""
                                }.onFailure {
                                    joinError = it.message ?: "Import impossible. Aucune donnée n'a été remplacée."
                                }
                            }
                        }
                    ) { Text(if (joining) "Importation…" else "Importer et continuer") }
                },
                dismissButton = {
                    TextButton(enabled = !joining, onClick = { showJoinDialog = false }) { Text("Annuler") }
                }
            )
        }
    }
}

@Composable
private fun OnboardingInfoStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    points: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(142.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            )
            Box(
                Modifier.size(96.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        points.forEach { point ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(10.dp))
                Text(point, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun WelcomeStep(accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LuminaBrandMark(
            modifier = Modifier.size(72.dp),
            backgroundColor = accentColor
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Bienvenue sur Lumina",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "La gestion financière de votre église, claire et traçable : " +
                "caisses, budgets, membres et bilans prêts à présenter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = LuminaBranding.SLOGAN,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("lumina_slogan")
        )
        Spacer(modifier = Modifier.height(12.dp))
        WelcomePoint(Icons.Default.Payments, "Trésorerie par caisse", "Chaque franc est rattaché à une caisse, avec virements traçables.")
        WelcomePoint(Icons.Default.PieChart, "Budgets maîtrisés", "Plafonds par poste et alertes avant dépassement.")
        WelcomePoint(Icons.Default.VerifiedUser, "Bilans présentables", "Rapports structurés avec l'identité et le logo de votre église.")
    }
}

@Composable
private fun WelcomePoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IdentityStep(
    churchName: String,
    onChurchNameChange: (String) -> Unit,
    denomination: String,
    onDenominationChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    pastorName: String,
    onPastorNameChange: (String) -> Unit,
    treasurerName: String,
    onTreasurerNameChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Votre église",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Ces informations apparaîtront sur vos rapports.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = churchName,
            onValueChange = onChurchNameChange,
            label = { Text("Nom de l'église *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_church_name")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = denomination,
            onValueChange = onDenominationChange,
            label = { Text("Dénomination / réseau (optionnel)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = city,
                onValueChange = onCityChange,
                label = { Text("Ville") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Téléphone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Adresse du lieu de culte") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("E-mail (optionnel)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = pastorName,
                onValueChange = onPastorNameChange,
                label = { Text("Responsable / pasteur") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = treasurerName,
                onValueChange = onTreasurerNameChange,
                label = { Text("Trésorier") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Seul le nom de l'église est obligatoire : tout le reste peut être complété plus tard.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BrandingStep(
    churchName: String,
    logoBase64: String,
    accentHex: String,
    onPickLogo: () -> Unit,
    onRemoveLogo: () -> Unit,
    onAccentSelected: (String) -> Unit
) {
    val bitmap = remember(logoBase64) { ImageStore.base64ToBitmap(logoBase64) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Identité visuelle",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Le logo et la couleur choisie habilleront l'application et les rapports.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Aperçu de l'en-tête du rapport
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(parseHexColor(accentHex)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Logo de l'église",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = churchName.trim().take(2).ifBlank { "L" }.uppercase(),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = churchName.trim().ifBlank { "Nom de votre église" },
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Bilan financier — aperçu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPickLogo, shape = RoundedCornerShape(12.dp)) {
                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (bitmap == null) "Choisir un logo" else "Changer le logo")
            }
            if (bitmap != null) {
                TextButton(onClick = onRemoveLogo) { Text("Retirer") }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text("Couleur d'accent", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(10.dp))

        AccentPalettes.chunked(2).forEach { rowPalettes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPalettes.forEach { palette ->
                    val selected = palette.hex.equals(accentHex, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onAccentSelected(palette.hex) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(palette.hex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = palette.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 2
                        )
                    }
                }
                if (rowPalettes.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Le bleu indigo est proposé par défaut. Vous pourrez changer de couleur à tout moment.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}
