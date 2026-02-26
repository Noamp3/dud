package com.smartboiler.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartboiler.domain.model.BoilerStatus
import com.smartboiler.domain.model.DayType
import com.smartboiler.domain.model.ShowerSchedule
import com.smartboiler.ui.components.HeatingEstimateCard
import com.smartboiler.ui.theme.TempCold
import com.smartboiler.ui.theme.TempHot
import com.smartboiler.ui.theme.TempPerfect
import com.smartboiler.ui.theme.TempWarm
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onScheduleShower: () -> Unit,
    onPlan: () -> Unit,
    onShowerNow: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit = {},
    onFeedback: (Long) -> Unit = {},
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); showContent = true }
    val now = LocalDateTime.now()
    val hasHeating = uiState.asapOneShowerMinutes > 0
    val derivedDayType = when (uiState.weatherEmoji) {
        "☀️" -> DayType.SUNNY
        "⛅" -> DayType.PARTLY_CLOUDY
        else -> DayType.CLOUDY
    }
    val oneShowerPlan = ShowerSchedule(
        date = now.toLocalDate(),
        scheduledTime = now.toLocalTime().plusMinutes(uiState.asapOneShowerMinutes.toLong()),
        dayType = derivedDayType,
        cloudCoverPercent = 0,
        peopleCount = 1,
        heatingRequired = hasHeating,
        heatingDurationMinutes = uiState.asapOneShowerMinutes,
        heatingStartTime = if (hasHeating) now.toLocalTime() else null,
        estimatedSolarTempCelsius = uiState.boilerState.estimatedTempCelsius,
        estimatedFinalTempCelsius = if (hasHeating) {
            uiState.boilerConfig?.desiredTempCelsius?.toDouble() ?: uiState.boilerState.estimatedTempCelsius
        } else {
            uiState.boilerState.estimatedTempCelsius
        },
        waterNeededLiters = uiState.boilerConfig?.avgShowerLiters ?: 50,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Boiler",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                    FilledTonalIconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Temperature Gauge
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -30 },
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surface,
                                    ),
                                ),
                            )
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                    ) {
                        TemperatureGauge(
                            temperature = uiState.boilerState.estimatedTempCelsius,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Cards Row
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, 200)) + slideInVertically(tween(500, 200)) { 40 },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Boiler Status Card
                    StatusCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Power,
                        title = "Status",
                        value = uiState.boilerState.status.label,
                        valueColor = when (uiState.boilerState.status) {
                            BoilerStatus.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                            BoilerStatus.SCHEDULED -> MaterialTheme.colorScheme.primary
                            BoilerStatus.HEATING -> TempHot
                            BoilerStatus.READY -> TempPerfect
                        },
                    )

                    // Weather Card
                    StatusCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.WbSunny,
                        title = "Weather",
                        value = uiState.weatherSummary,
                        emoji = uiState.weatherEmoji,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Feedback Prompt Card (if a past shower needs rating)
            uiState.feedbackScheduleId?.let { scheduleId ->
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(600, 300)) + slideInVertically(tween(500, 300)) { 40 },
                ) {
                    Card(
                        onClick = { onFeedback(scheduleId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🚿", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "How was your shower?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "Tap to rate — helps improve future estimates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Solar Contribution Card
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, 400)) + slideInVertically(tween(500, 400)) { 40 },
            ) {
                SolarContributionCard(
                    percentage = uiState.boilerState.solarContributionPercent,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Next Event Card
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, 500)) + slideInVertically(tween(500, 500)) { 40 },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeatingEstimateCard(
                        plan = oneShowerPlan,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Next Event",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = uiState.boilerState.nextEventDescription
                                        ?: "No events scheduled",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onShowerNow,
                        enabled = !uiState.isShowerNowLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        if (uiState.isShowerNowLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Starting…")
                        } else {
                            Text("1 Person Shower Now")
                        }
                    }

                    FilledTonalButton(
                        onClick = onScheduleShower,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Schedule Shower")
                    }

                    FilledTonalButton(
                        onClick = onPlan,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Smart Plan")
                    }

                    uiState.showerNowMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TemperatureGauge(
    temperature: Double,
    modifier: Modifier = Modifier,
) {
    val tempColor = when {
        temperature < 25 -> TempCold
        temperature < 35 -> TempWarm
        temperature < 45 -> TempPerfect
        else -> TempHot
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (temperature.toFloat() / 70f).coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = LinearEasing),
        label = "temp_gauge",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Background arc
            Canvas(modifier = Modifier.size(180.dp)) {
                val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                val arcSize = Size(size.width, size.height)

                // Track
                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = stroke,
                    topLeft = Offset.Zero,
                    size = arcSize,
                )

                // Progress
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(TempCold, TempWarm, TempPerfect, TempHot),
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = stroke,
                    topLeft = Offset.Zero,
                    size = arcSize,
                )
            }

            // Temperature text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.0f°".format(temperature),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = tempColor,
                )
                Text(
                    text = "1 shower",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emoji: String? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (emoji != null) {
                Text(text = emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
        }
    }
}

@Composable
fun SolarContributionCard(percentage: Int) {
    val animatedWidth by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(1000),
        label = "solar_bar",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("☀️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sun-Powered Heat Share",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TempPerfect,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "How much of your current 1-shower water heating came from the sun (instead of electricity).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedWidth)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(TempWarm, TempPerfect)
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "More grid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "More solar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
