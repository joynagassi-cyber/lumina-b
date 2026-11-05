package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Church
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ChurchWorkspace

/** Sélecteur de dossiers séparés : chaque choix ouvre un fichier Room différent. */
@Composable
fun WorkspacePickerDialog(
    workspaces: List<ChurchWorkspace>,
    activeWorkspaceId: String,
    onDismiss: () -> Unit,
    onSelectWorkspace: (String) -> Unit,
    onCreateWorkspace: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { androidx.compose.material3.Icon(Icons.Default.Church, contentDescription = null) },
        title = { Text("Dossiers d'église") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Chaque dossier conserve ses propres opérations, membres, budgets, utilisateurs et sauvegardes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                workspaces.forEach { workspace ->
                    TextButton(
                        onClick = { onSelectWorkspace(workspace.id) },
                        modifier = Modifier.fillMaxWidth().testTag("select_workspace_${workspace.id}")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(workspace.displayName, fontWeight = FontWeight.Bold)
                            Text(
                                if (workspace.id == activeWorkspaceId) "Dossier actuellement ouvert" else "Ouvrir ce dossier",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCreateWorkspace, modifier = Modifier.testTag("create_church_workspace")) {
                androidx.compose.material3.Icon(Icons.Default.AddBusiness, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Créer un dossier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}
