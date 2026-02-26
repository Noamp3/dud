package com.smartboiler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartboiler.domain.model.ShowerSchedule
import com.smartboiler.ui.theme.SolarOrange
import com.smartboiler.ui.theme.WarmRed
import java.time.format.DateTimeFormatter

@Composable
fun HeatingEstimateCard(
    plan: ShowerSchedule,
    modifier: Modifier = Modifier,
) {
    val runtimeText = if (plan.heatingRequired) {
        "Turn boiler ON for ${plan.heatingDurationMinutes} min"
    } else {
        "No boiler ON needed"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (plan.heatingRequired) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            },
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (plan.heatingRequired) Icons.Filled.WaterDrop else Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = if (plan.heatingRequired) MaterialTheme.colorScheme.error else SolarOrange,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (plan.heatingRequired) "Electric Heating Needed" else "Solar Is Enough ☀️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (plan.heatingRequired) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                    },
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = runtimeText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            MetricRow(
                "Boiler ON",
                if (plan.heatingRequired) "${plan.heatingDurationMinutes} min" else "0 min",
                "Final",
                "%.1f°C".format(plan.estimatedFinalTempCelsius),
            )
            MetricRow(
                "Current",
                "%.1f°C".format(plan.estimatedSolarTempCelsius),
                "Water",
                "${plan.waterNeededLiters}L",
            )
            MetricRow(
                "People",
                plan.peopleCount.toString(),
                "Day",
                plan.dayType.name.replace('_', ' '),
            )

            if (plan.heatingRequired) {
                Spacer(modifier = Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    WarmRed.copy(alpha = 0.15f),
                                    SolarOrange.copy(alpha = 0.15f),
                                ),
                            ),
                        )
                        .padding(10.dp),
                ) {
                    MetricRow(
                        "Estimated ON time",
                        "${plan.heatingDurationMinutes} min",
                        "Start heating at",
                        plan.heatingStartTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "-",
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Solar is enough for this shower",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricCell(leftLabel, leftValue, Modifier.weight(1f))
        MetricCell(rightLabel, rightValue, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
