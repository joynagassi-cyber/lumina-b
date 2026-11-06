package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.localTimestampToPickerUtcMillis
import com.example.util.pickerUtcMillisToLocalMidday

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuminaDateRangePickerDialog(
    initialStartLocalMillis: Long?,
    initialEndLocalMillis: Long?,
    onDismissRequest: () -> Unit,
    onRangeSelected: (startLocalNoon: Long, endLocalNoon: Long) -> Unit
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartLocalMillis?.let(::localTimestampToPickerUtcMillis),
        initialSelectedEndDateMillis = initialEndLocalMillis?.let(::localTimestampToPickerUtcMillis)
    )
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).heightIn(max = 760.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DateRangePicker(
                    state = state,
                    title = { Text("Période de l'événement", modifier = Modifier.padding(start = 24.dp, top = 20.dp)) },
                    showModeToggle = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 440.dp).testTag("project_date_range_picker")
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) { Text("Annuler") }
                    TextButton(
                        enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                        modifier = Modifier.testTag("project_date_range_confirm"),
                        onClick = {
                            val start = state.selectedStartDateMillis ?: return@TextButton
                            val end = state.selectedEndDateMillis ?: return@TextButton
                            onRangeSelected(
                                pickerUtcMillisToLocalMidday(start),
                                pickerUtcMillisToLocalMidday(end)
                            )
                        }
                    ) { Text("Valider la période") }
                }
            }
        }
    }
}
