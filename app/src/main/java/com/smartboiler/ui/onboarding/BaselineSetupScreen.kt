package com.smartboiler.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.smartboiler.ui.theme.SecondaryLight
import com.smartboiler.ui.theme.TempWarm
import kotlinx.coroutines.delay

@Composable
fun BaselineSetupScreen(
    uiState: OnboardingUiState,
    onSunnyChange: (Int) -> Unit,
    onPartlyCloudyChange: (Int) -> Unit,
    onCloudyChange: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); showContent = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SecondaryLight.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
        ) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { -30 },
            ) {
                Column {
                    Text(
                        text = "Heating Baselines",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "How long do you usually turn on the electric boiler to get enough hot water?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sunny day
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(initialAlpha = 0.3f) + slideInVertically { 40 },
            ) {
                BaselineCard(
                    icon = Icons.Filled.WbSunny,
                    iconTint = Color(0xFFFFA000),
                    title = "☀️  Sunny Day",
                    subtitle = if (uiState.sunnyMinutes == 0) "No heating needed" else "${uiState.sunnyMinutes} minutes",
                    value = uiState.sunnyMinutes,
                    onValueChange = onSunnyChange,
                    accentColor = Color(0xFFFFA000),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Partly cloudy
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(initialAlpha = 0.3f) + slideInVertically { 60 },
            ) {
                BaselineCard(
                    icon = Icons.Filled.FilterDrama,
                    iconTint = Color(0xFF78909C),
                    title = "⛅  Partly Cloudy Day",
                    subtitle = "${uiState.partlyCloudyMinutes} minutes",
                    value = uiState.partlyCloudyMinutes,
                    onValueChange = onPartlyCloudyChange,
                    accentColor = TempWarm,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cloudy / Rainy
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(initialAlpha = 0.3f) + slideInVertically { 80 },
            ) {
                BaselineCard(
                    icon = Icons.Filled.Cloud,
                    iconTint = Color(0xFF546E7A),
                    title = "☁️  Cloudy / Rainy Day",
                    subtitle = "${uiState.cloudyMinutes} minutes",
                    value = uiState.cloudyMinutes,
                    onValueChange = onCloudyChange,
                    accentColor = Color(0xFF546E7A),
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Step indicator + Finish button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StepIndicator(currentStep = 3, totalSteps = 4)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = !uiState.isSaving,
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Saving…", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finish Setup", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BaselineCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentColor,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..240f,
                steps = 23,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text("0 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("4 hrs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
