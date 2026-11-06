package com.example.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.util.localTimestampToPickerUtcMillis
import com.example.util.pickerUtcMillisToLocalMidday

/** A full calendar date picker with no lower bound, so past dates remain selectable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuminaDatePickerDialog(
    initialLocalDateMillis: Long,
    title: String,
    onDismissRequest: () -> Unit,
    onDateSelected: (localNoonMillis: Long) -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = localTimestampToPickerUtcMillis(initialLocalDateMillis)
    )
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(pickerUtcMillisToLocalMidday(it)) }
                },
                enabled = pickerState.selectedDateMillis != null,
                modifier = Modifier.testTag("lumina_date_picker_confirm")
            ) { Text("Choisir") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Annuler") }
        }
    ) {
        DatePicker(
            state = pickerState,
            title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
            showModeToggle = false,
            modifier = Modifier.testTag("lumina_date_picker")
        )
    }
}
