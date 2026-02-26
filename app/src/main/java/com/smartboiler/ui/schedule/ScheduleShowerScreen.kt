@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smartboiler.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartboiler.ui.components.HeatingEstimateCard
import com.smartboiler.ui.theme.SolarOrange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ScheduleShowerScreen(
    uiState: ScheduleUiState,
    onPeopleCountChange: (Int) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onRecurringDayChange: (DayOfWeek, Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.selectedHour,
        initialMinute = uiState.selectedMinute,
        is24Hour = true,
    )
    var showTimeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        if (timePickerState.hour != uiState.selectedHour ||
            timePickerState.minute != uiState.selectedMinute
        ) {
            onTimeChange(timePickerState.hour, timePickerState.minute)
        }
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Schedule Shower",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InputsCard(
                count = uiState.peopleCount,
                onCountChange = onPeopleCountChange,
                selectedDate = uiState.selectedDate,
                onDateChange = onDateChange,
                selectedTime = LocalTime.of(uiState.selectedHour, uiState.selectedMinute),
                onTimeClick = { showTimeDialog = true },
                recurring = uiState.isRecurring,
                recurringDays = uiState.recurringDays,
                onRecurringChange = onRecurringChange,
                onRecurringDayChange = onRecurringDayChange,
            )

            AnimatedVisibility(
                visible = uiState.heatingPlan != null && !uiState.isCalculating,
                enter = fadeIn() + slideInVertically { 40 },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                uiState.heatingPlan?.let { plan ->
                    HeatingEstimateCard(
                        plan = plan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            StatusPanel(
                isCalculating = uiState.isCalculating,
                warning = uiState.warning,
                error = uiState.error,
                hasPlan = uiState.heatingPlan != null,
            )

            Button(
                onClick = onConfirm,
                enabled = uiState.heatingPlan != null && !uiState.isSaving && uiState.warning == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Schedule", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (showTimeDialog) {
            AlertDialog(
                onDismissRequest = { showTimeDialog = false },
                confirmButton = {
                    Button(onClick = { showTimeDialog = false }) {
                        Text("Done")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showTimeDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Select shower time") },
                text = {
                    TimeInput(state = timePickerState)
                },
            )
        }
    }
}

@Composable
private fun InputsCard(
    count: Int,
    onCountChange: (Int) -> Unit,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    selectedTime: LocalTime,
    onTimeClick: () -> Unit,
    recurring: Boolean,
    recurringDays: Set<DayOfWeek>,
    onRecurringChange: (Boolean) -> Unit,
    onRecurringDayChange: (DayOfWeek, Boolean) -> Unit,
) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val selectedMillis = selectedDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
    var showDateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMillis) {
        datePickerState.selectedDateMillis = selectedMillis
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Shower Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                "$count people",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = count.toFloat(),
                onValueChange = { onCountChange(it.roundToInt()) },
                valueRange = 1f..8f,
                steps = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onDateChange(today) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedDate == today) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    ),
                ) {
                    Text("Today")
                }
                Button(
                    onClick = { onDateChange(tomorrow) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedDate == tomorrow) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    ),
                ) {
                    Text("Tomorrow")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { showDateDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(selectedDate.format(formatter))
                }
                OutlinedButton(
                    onClick = onTimeClick,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(selectedTime.format(timeFormatter))
                }
            }

            RecurringCompact(
                recurring = recurring,
                recurringDays = recurringDays,
                onRecurringChange = onRecurringChange,
                onRecurringDayChange = onRecurringDayChange,
            )
        }
    }

    if (showDateDialog) {
        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val pickedMillis = datePickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            val pickedDate = java.time.Instant.ofEpochMilli(pickedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            if (!pickedDate.isBefore(today)) {
                                onDateChange(pickedDate)
                            }
                        }
                        showDateDialog = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDateDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringCompact(
    recurring: Boolean,
    recurringDays: Set<DayOfWeek>,
    onRecurringChange: (Boolean) -> Unit,
    onRecurringDayChange: (DayOfWeek, Boolean) -> Unit,
) {
    val weekdays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Recurring",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "No expiration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = recurring,
                onCheckedChange = onRecurringChange,
            )
        }

        if (recurring) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 7,
            ) {
                weekdays.forEach { day ->
                    DayChip(
                        label = day.name.take(3),
                        selected = day in recurringDays,
                        onClick = { onRecurringDayChange(day, day !in recurringDays) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedLabel = label.lowercase().replaceFirstChar { it.uppercase() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 34.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Text(normalizedLabel)
    }
}

@Composable
private fun StatusPanel(
    isCalculating: Boolean,
    warning: String?,
    error: String?,
    hasPlan: Boolean,
) {
    when {
        isCalculating -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Calculating...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        warning != null -> {
            Text(
                warning,
                color = SolarOrange,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        error != null -> {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        !hasPlan -> {
            Text(
                "Set date/time to preview estimation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
