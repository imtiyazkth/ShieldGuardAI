package com.shieldguard.presentation.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldguard.data.db.NotificationAlertEntity
import com.shieldguard.domain.model.RiskLevel
import com.shieldguard.presentation.dashboard.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationAlertsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val recentAlerts by viewModel.recentAlerts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Alerts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (recentAlerts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.NotificationsNone, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("Koi suspicious notification nahi aayi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("ShieldGuard background mein notifications monitor kar raha hai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.Info, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${recentAlerts.size} suspicious notifications detect hui hain. Ye notifications scam ya phishing link le jaane ki koshish kar rahi thi.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                items(recentAlerts) { alert ->
                    NotificationAlertCard(alert = alert)
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun NotificationAlertCard(alert: NotificationAlertEntity) {
    val risk = try { RiskLevel.valueOf(alert.riskLevel) } catch (e: Exception) { RiskLevel.MEDIUM }
    val riskColor = when (risk) {
        RiskLevel.CRITICAL -> Color(0xFFF44336)
        RiskLevel.HIGH -> Color(0xFFFF5722)
        RiskLevel.MEDIUM -> Color(0xFFFF9800)
        else -> Color(0xFF2196F3)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = riskColor.copy(0.07f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(alert.appName, fontWeight = FontWeight.Bold)
                    Text(
                        alert.title ?: "No title",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(containerColor = riskColor) {
                    Text(risk.displayName, color = Color.White,
                        style = MaterialTheme.typography.labelSmall)
                }
            }

            if (!alert.content.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    alert.content.take(150) + if ((alert.content.length) > 150) "..." else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!alert.riskReason.isBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "⚠️ ${alert.riskReason.take(120)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = riskColor
                )
            }

            if (!alert.containedUrl.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = riskColor.copy(0.15f)
                ) {
                    Text(
                        "🔗 ${alert.containedUrl.take(60)}...",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(Date(alert.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
