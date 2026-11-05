package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity
import com.example.data.UserRole
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.LuminaBranding
import com.example.ui.designsystem.LuminaBrandMark

/**
 * Écran d'ouverture de session locale.
 *
 * Chaque personne de l'équipe choisit son nom puis saisit son code PIN.
 * Aucun compte distant : tout est vérifié sur l'appareil.
 */
@Composable
fun LoginScreen(
    churchName: String,
    brandBackgroundColor: Color = MaterialTheme.colorScheme.primary,
    users: List<UserEntity>,
    onLogin: (Long, String) -> Unit,
    onCreateFirstUser: (String, UserRole, String) -> Unit,
    onStartDemo: () -> Unit = {},
    onChooseWorkspace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var pin by remember { mutableStateOf("") }

    // Aucun utilisateur : création du premier (trésorier)
    var firstUserFirstName by remember { mutableStateOf("") }
    var firstUserLastName by remember { mutableStateOf("") }
    var firstUserPin by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LuminaBrandMark(
                    modifier = Modifier.size(56.dp),
                    backgroundColor = brandBackgroundColor
                )
                Text(
                    text = churchName.ifBlank { "Mon église" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = if (users.isEmpty()) {
                    "Créez votre premier utilisateur : le trésorier administre l'application."
                } else {
                    "Qui utilise l'application ?"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = LuminaBranding.SLOGAN,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp).testTag("lumina_slogan")
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (users.isEmpty()) {
                OutlinedTextField(
                    value = firstUserFirstName,
                    onValueChange = { firstUserFirstName = it },
                    label = { Text("Prénom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("first_user_first_name")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = firstUserLastName,
                    onValueChange = { firstUserLastName = it },
                    label = { Text("Nom de famille") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("first_user_last_name")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = firstUserPin,
                    onValueChange = { firstUserPin = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Code PIN (4 à 6 chiffres)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("first_user_pin")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val personalName = listOf(firstUserFirstName.trim(), firstUserLastName.trim())
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                        if (firstUserFirstName.isNotBlank() && firstUserLastName.isNotBlank() && firstUserPin.length >= 4) {
                            onCreateFirstUser(personalName, UserRole.TRESORIER, firstUserPin)
                            firstUserPin = ""
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("create_first_user")
                ) {
                    Text("Créer mon accès trésorier", fontWeight = FontWeight.Bold)
                }
            } else {
                users.forEach { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { selectedUser = user; pin = "" }
                            .testTag("login_user_${user.id}"),
                        shape = LuminaTokens.ShapeSubCard,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedUser?.id == user.id) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.initials,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.displayName, fontWeight = FontWeight.Bold)
                                Text(
                                    text = user.roleValue.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                selectedUser?.let { user ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Code de ${user.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("PIN") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("login_pin_field")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (!user.hasPin || pin.length >= 4) onLogin(user.id, pin)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("login_submit")
                    ) {
                        Text("Ouvrir mon dossier", fontWeight = FontWeight.Bold)
                    }
                    if (!user.hasPin) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cet utilisateur n'a pas encore de code : appuyez simplement sur ouvrir.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { selectedUser = null; pin = "" }) { Text("Changer d'utilisateur") }
                }
            }

            TextButton(
                onClick = onStartDemo,
                modifier = Modifier.fillMaxWidth().testTag("start_read_only_demo")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Explorer la démonstration (essais temporaires)")
            }
            Text(
                "Seules des opérations fictives peuvent être testées ; vos données réelles ne sont jamais modifiées.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onChooseWorkspace,
                modifier = Modifier.fillMaxWidth().testTag("open_workspace_picker_login")
            ) {
                Text("Changer ou créer un dossier d'église")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Les données restent sur cet appareil. Le partage entre téléphones se fait " +
                    "par un fichier chiffré, jamais par internet.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}
