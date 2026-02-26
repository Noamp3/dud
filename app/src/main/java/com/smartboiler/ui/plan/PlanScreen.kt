@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smartboiler.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.smartboiler.ui.components.HeatingEstimateCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PlanScreen(
    uiState: PlanUiState,
    onDesiredTempChange: (Int) -> Unit,
    onShowersCountChange: (Int) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onBack: () -> Unit,
    onUsePlan: () -> Unit = {},
) {
    var showTimeDialog by remember { mutableStateOf(false) }
    var showAdjustments by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val showerTime = LocalTime.of(uiState.selectedHour, uiState.selectedMinute)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Plan Shower",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlanEstimateHero(
                uiState = uiState,
                onUsePlan = onUsePlan,
            )

            PlanClockCard(
                showerTime = showerTime,
                startTime = uiState.heatingPlan?.heatingStartTime,
                heatingDurationMinutes = uiState.heatingPlan?.heatingDurationMinutes,
                heatingRequired = uiState.heatingPlan?.heatingRequired == true,
                onShowerTimeChange = onTimeChange,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Adjust Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedButton(onClick = { showAdjustments = !showAdjustments }) {
                            Icon(
                                if (showAdjustments) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (showAdjustments) "Hide" else "Show")
                        }
                    }

                    AnimatedVisibility(visible = showAdjustments) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = { onDateChange(today) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.selectedDate == today) {
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.selectedDate == tomorrow) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                    ),
                                ) {
                                    Text("Tomorrow")
                                }
                            }

                            OutlinedButton(
                                onClick = { showTimeDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.AccessTime, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Shower time: ${showerTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                            }

                            Text(
                                "Desired temp: ${uiState.desiredTempCelsius}°C",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Slider(
                                value = uiState.desiredTempCelsius.toFloat(),
                                onValueChange = { onDesiredTempChange(it.roundToInt()) },
                                valueRange = 35f..60f,
                                steps = 24,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Text(
                                "Showers: ${uiState.showersCount}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Slider(
                                value = uiState.showersCount.toFloat(),
                                onValueChange = { onShowersCountChange(it.roundToInt()) },
                                valueRange = 1f..6f,
                                steps = 4,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (uiState.weatherSummary.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        uiState.weatherSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (showTimeDialog) {
            val timePickerState = rememberTimePickerState(
                initialHour = uiState.selectedHour,
                initialMinute = uiState.selectedMinute,
                is24Hour = true,
            )

            LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                if (timePickerState.hour != uiState.selectedHour ||
                    timePickerState.minute != uiState.selectedMinute
                ) {
                    onTimeChange(timePickerState.hour, timePickerState.minute)
                }
            }

            AlertDialog(
                onDismissRequest = { showTimeDialog = false },
                confirmButton = {
                    Button(onClick = { showTimeDialog = false }) { Text("Done") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showTimeDialog = false }) { Text("Cancel") }
                },
                title = { Text("Select shower time") },
                text = { TimeInput(state = timePickerState) },
            )
        }
    }
}

@Composable
private fun PlanEstimateHero(
    uiState: PlanUiState,
    onUsePlan: () -> Unit,
) {
    when {
        uiState.isCalculating -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Calculating best plan…")
                }
            }
        }

        uiState.warning != null || uiState.error != null || uiState.heatingPlan == null -> {
            val message = uiState.warning ?: uiState.error ?: "Set shower time to see guidance"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Your Best Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        message,
                        color = if (uiState.warning != null || uiState.error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Your Best Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HeatingEstimateCard(
                    plan = uiState.heatingPlan,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onUsePlan,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Use This Plan")
                }
            }
        }
    }
}

@Composable
private fun PlanClockCard(
    showerTime: LocalTime,
    startTime: LocalTime?,
    heatingDurationMinutes: Int?,
    heatingRequired: Boolean,
    onShowerTimeChange: (Int, Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Plan Clock",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            PlanClock(
                showerTime = showerTime,
                startTime = startTime,
                onShowerTimeChange = onShowerTimeChange,
            )

            Text(
                text = if (heatingRequired && startTime != null && heatingDurationMinutes != null) {
                    "Turn on at ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} for $heatingDurationMinutes min → ready by ${showerTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                } else {
                    "No electric heating needed → shower at ${showerTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanClock(
    showerTime: LocalTime,
    startTime: LocalTime?,
    onShowerTimeChange: (Int, Int) -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val showerColor = MaterialTheme.colorScheme.secondary
    val startColor = MaterialTheme.colorScheme.error
    var clockSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = onSurface.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style = Stroke(width = 10.dp.toPx()),
            )

            repeat(12) { i ->
                val hour24 = i * 2
                val angleDeg = (hour24 / 24f) * 360f - 90f
                val angleRad = angleDeg * (PI / 180f).toFloat()
                val outer = Offset(
                    x = center.x + cos(angleRad) * radius,
                    y = center.y + sin(angleRad) * radius,
                )
                val inner = Offset(
                    x = center.x + cos(angleRad) * (radius - if (hour24 % 6 == 0) 16.dp.toPx() else 10.dp.toPx()),
                    y = center.y + sin(angleRad) * (radius - if (hour24 % 6 == 0) 16.dp.toPx() else 10.dp.toPx()),
                )
                drawLine(
                    color = onSurface.copy(alpha = 0.45f),
                    start = inner,
                    end = outer,
                    strokeWidth = if (hour24 % 6 == 0) 3.dp.toPx() else 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            fun hand(time: LocalTime, lengthRatio: Float): Offset {
                val minuteOfDay = time.hour * 60f + time.minute
                val angleDeg = (minuteOfDay / (24f * 60f)) * 360f - 90f
                val angleRad = angleDeg * (PI / 180f).toFloat()
                return Offset(
                    x = center.x + cos(angleRad) * (radius * lengthRatio),
                    y = center.y + sin(angleRad) * (radius * lengthRatio),
                )
            }

            val showerHand = hand(showerTime, 0.78f)
            drawLine(
                color = showerColor,
                start = center,
                end = showerHand,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )

            if (startTime != null) {
                val startHand = hand(startTime, 0.6f)
                drawLine(
                    color = startColor,
                    start = center,
                    end = startHand,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            drawCircle(
                color = onSurface,
                radius = 4.dp.toPx(),
                center = center,
            )
        }

        Canvas(
            modifier = Modifier
                .size(190.dp)
                .onSizeChanged { clockSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        if (clockSize.width == 0 || clockSize.height == 0) return@detectTapGestures

                        val centerX = clockSize.width / 2f
                        val centerY = clockSize.height / 2f
                        val angleRadians = atan2(tapOffset.y - centerY, tapOffset.x - centerX)
                        val angleDegrees = ((Math.toDegrees(angleRadians.toDouble()) + 90.0) + 360.0) % 360.0
                        val minuteOfDay = ((angleDegrees / 360.0) * 24.0 * 60.0).roundToInt()
                        val normalizedMinuteOfDay = ((minuteOfDay % 1440) + 1440) % 1440
                        val snappedMinuteOfDay = ((normalizedMinuteOfDay / 5.0).roundToInt() * 5) % 1440
                        val hour = snappedMinuteOfDay / 60
                        val minute = snappedMinuteOfDay % 60

                        onShowerTimeChange(hour, minute)
                    }
                },
        ) {}
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(color = showerColor)
        Text(
            text = "Shower ${showerTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LegendDot(color = startColor)
        Text(
            text = if (startTime != null) {
                "Start ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            } else {
                "No start needed"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color = color, shape = CircleShape),
    )
}
