package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.localTimestampToPickerUtcMillis
import com.example.util.pickerUtcMillisToLocalMidday
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Inline range calendar. Safe to embed in an existing dialog; it opens no second window. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuminaDateRangePickerPanel(
    initialStartLocalMillis: Long?,
    initialEndLocalMillis: Long?,
    onCancel: () -> Unit,
    onRangeSelected: (startLocalNoon: Long, endLocalNoon: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartLocalMillis?.let(::localTimestampToPickerUtcMillis),
        initialSelectedEndDateMillis = initialEndLocalMillis?.let(::localTimestampToPickerUtcMillis)
    )
    val format = SimpleDateFormat("EEE d MMM yyyy", Locale.FRANCE)
    Column(modifier = modifier.fillMaxHeight()) {
        Text("Choisissez séparément les bornes de la période", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Touchez une date de début, puis une date de fin. Le calendrier mettra toute la plage en évidence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RangeDatePill(
                label = "DÉBUT",
                value = state.selectedStartDateMillis?.let { format.format(Date(pickerUtcMillisToLocalMidday(it))) } ?: "À choisir",
                modifier = Modifier.weight(1f)
            )
            RangeDatePill(
                label = "FIN",
                value = state.selectedEndDateMillis?.let { format.format(Date(pickerUtcMillisToLocalMidday(it))) } ?: "À choisir",
                modifier = Modifier.weight(1f)
            )
        }
        DateRangePicker(
            state = state,
            title = { Text("Période de l'événement", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            showModeToggle = false,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("project_date_range_picker")
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("Annuler") }
            TextButton(
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                modifier = Modifier.testTag("project_date_range_confirm"),
                onClick = {
                    val start = state.selectedStartDateMillis ?: return@TextButton
                    val end = state.selectedEndDateMillis ?: return@TextButton
                    onRangeSelected(pickerUtcMillisToLocalMidday(start), pickerUtcMillisToLocalMidday(end))
                }
            ) { Text("Valider la période") }
        }
    }
}

@Composable
private fun RangeDatePill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun LuminaDateRangePickerDialog(
    initialStartLocalMillis: Long?,
    initialEndLocalMillis: Long?,
    onDismissRequest: () -> Unit,
    onRangeSelected: (startLocalNoon: Long, endLocalNoon: Long) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                LuminaDateRangePickerPanel(
                    initialStartLocalMillis = initialStartLocalMillis,
                    initialEndLocalMillis = initialEndLocalMillis,
                    onCancel = onDismissRequest,
                    onRangeSelected = onRangeSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
