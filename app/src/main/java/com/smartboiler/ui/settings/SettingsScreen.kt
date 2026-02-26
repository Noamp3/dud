package com.smartboiler.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartboiler.domain.model.DayType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdateBoilerSize: (Int) -> Unit,
    onUpdateHeatingPower: (Double) -> Unit,
    onUpdateTargetTemp: (Int) -> Unit,
    onSave: () -> Unit,
    onManageRecurring: () -> Unit = {},
    onDeviceSetup: () -> Unit = {},
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); showContent = true }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Settings saved ✓")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isSaved && uiState.config != null) {
                FloatingActionButton(onClick = onSave) {
                    Icon(Icons.Filled.Check, "Save")
                }
            }
        },
    ) { padding ->
        val config = uiState.config ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // Boiler Configuration Section
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { 20 },
            ) {
                SectionCard(title = "⚙️ Boiler Configuration") {
                    SettingsRow(
                        label = "Tank size",
                        value = config.capacityLiters.toString(),
                        suffix = "liters",
                        onValueChange = { it.toIntOrNull()?.let(onUpdateBoilerSize) },
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsRow(
                        label = "Heating power",
                        value = config.heatingPowerKw.toString(),
                        suffix = "kW",
                        onValueChange = { it.toDoubleOrNull()?.let(onUpdateHeatingPower) },
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsRow(
                        label = "Target temp",
                        value = config.desiredTempCelsius.toString(),
                        suffix = "°C",
                        onValueChange = { it.toIntOrNull()?.let(onUpdateTargetTemp) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Location Section (read-only)
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { 30 },
            ) {
                SectionCard(title = "📍 Location") {
                    Text(
                        "Lat: %.4f  Lon: %.4f".format(config.latitude, config.longitude),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Re-run onboarding to change location",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Current Baselines (read-only, adjusted by feedback)
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { 40 },
            ) {
                SectionCard(title = "📊 Learned Baselines") {
                    Text(
                        "These adjust automatically from your feedback",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    uiState.baselines.forEach { baseline ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                when (baseline.dayType) {
                                    DayType.SUNNY -> "☀️ Sunny"
                                    DayType.PARTLY_CLOUDY -> "⛅ Partly cloudy"
                                    DayType.CLOUDY -> "☁️ Cloudy"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "${baseline.durationMinutes} min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Connected Device Section
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { 45 },
            ) {
                SectionCard(title = "🔌 Connected Device") {
                    Text(
                        "Control your boiler via Google Home",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedButton(
                        onClick = onDeviceSetup,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Manage Device")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { 48 },
            ) {
                SectionCard(title = "🔁 Recurring Schedules") {
                    Text(
                        "Enable, disable, or delete recurring shower schedules",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedButton(
                        onClick = onManageRecurring,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Manage Recurring Schedules")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // App Info
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { 50 },
            ) {
                SectionCard(title = "ℹ️ About") {
                    Text(
                        "Smart Boiler v1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Intelligent solar/electric hybrid boiler controller",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(80.dp)) // FAB clearance
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    suffix: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(100.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            trailingIcon = {
                Text(
                    suffix,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}
