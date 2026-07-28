package com.shieldguard.presentation.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldguard.domain.model.DeviceSecurityIssue
import com.shieldguard.domain.model.RiskLevel
import com.shieldguard.presentation.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val deviceStatus = uiState.deviceSecurityStatus

    LaunchedEffect(Unit) { viewModel.loadDeviceStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Security & Permissions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDeviceStatus() }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (deviceStatus == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SCORE
                item {
                    DeviceScoreCard(score = deviceStatus.securityScore)
                }

                // ISSUES
                if (deviceStatus.issues.isNotEmpty()) {
                    item {
                        Text("⚠️ Issues Found (${deviceStatus.issues.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error)
                    }
                    items(deviceStatus.issues) { issue ->
                        DeviceIssueCard(issue = issue, onFix = {
                            // Open relevant settings
                            try {
                                val intent = when {
                                    issue.title.contains("USB") || issue.title.contains("Developer") ->
                                        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                    issue.title.contains("Unknown Sources") ->
                                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    else -> Intent(Settings.ACTION_SECURITY_SETTINGS)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        })
                    }
                }

                // ACCESSIBILITY APPS
                if (deviceStatus.appsWithAccessibility.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "🔴 Accessibility Access (${deviceStatus.appsWithAccessibility.size})",
                            subtitle = "Ye apps poore phone ko control kar sakti hain — bahut dangerous!",
                            color = Color(0xFFF44336)
                        )
                    }
                    items(deviceStatus.appsWithAccessibility) { pkg ->
                        SpecialPermissionItem(
                            packageName = pkg,
                            risk = "CRITICAL",
                            onOpenSettings = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        )
                    }
                }

                // DEVICE ADMIN APPS
                if (deviceStatus.activeDeviceAdminApps.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "🔴 Device Admin Apps (${deviceStatus.activeDeviceAdminApps.size})",
                            subtitle = "Ye apps aapka phone lock ya wipe kar sakti hain!",
                            color = Color(0xFFF44336)
                        )
                    }
                    items(deviceStatus.activeDeviceAdminApps) { pkg ->
                        SpecialPermissionItem(
                            packageName = pkg,
                            risk = "CRITICAL",
                            onOpenSettings = {
                                context.startActivity(
                                    Intent().setClassName("com.android.settings",
                                        "com.android.settings.DeviceAdminSettings")
                                )
                            }
                        )
                    }
                }

                // OVERLAY APPS
                if (deviceStatus.appsWithOverlay.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "🟠 Draw Over Apps (${deviceStatus.appsWithOverlay.size})",
                            subtitle = "Ye apps screen ke upar kuch bhi dikha sakti hain",
                            color = Color(0xFFFF9800)
                        )
                    }
                    items(deviceStatus.appsWithOverlay) { pkg ->
                        SpecialPermissionItem(
                            packageName = pkg,
                            risk = "HIGH",
                            onOpenSettings = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:$pkg"))
                                    )
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                }
                            }
                        )
                    }
                }

                // NOTIFICATION LISTENERS
                if (deviceStatus.appsWithNotificationAccess.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "🟡 Notification Access (${deviceStatus.appsWithNotificationAccess.size})",
                            subtitle = "Ye apps aapke saare notifications padh sakti hain",
                            color = Color(0xFFFF9800)
                        )
                    }
                    items(deviceStatus.appsWithNotificationAccess) { pkg ->
                        SpecialPermissionItem(
                            packageName = pkg,
                            risk = "MEDIUM",
                            onOpenSettings = {
                                context.startActivity(
                                    Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                )
                            }
                        )
                    }
                }

                // ALL CLEAR
                if (deviceStatus.issues.isEmpty() &&
                    deviceStatus.appsWithAccessibility.isEmpty() &&
                    deviceStatus.activeDeviceAdminApps.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(0.12f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, null,
                                    tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Sab theek hai! Koi suspicious permissions nahi mili.",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun DeviceScoreCard(score: Int) {
    val color = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(0.12f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$score", style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold, color = color)
            Text("/100", style = MaterialTheme.typography.titleLarge,
                color = color.copy(0.7f))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Device Security Score", fontWeight = FontWeight.Bold)
                Text(
                    when {
                        score >= 80 -> "Aapka device secure hai ✅"
                        score >= 60 -> "Kuch issues hain — fix karo ⚠️"
                        else -> "Device khatray mein hai! 🔴"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}

@Composable
fun DeviceIssueCard(issue: DeviceSecurityIssue, onFix: () -> Unit) {
    val color = when (issue.riskLevel) {
        RiskLevel.CRITICAL -> Color(0xFFF44336)
        RiskLevel.HIGH -> Color(0xFFFF5722)
        else -> Color(0xFFFF9800)
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(issue.title, fontWeight = FontWeight.Bold, color = color)
            Text(issue.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onFix,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Fix Karo: ${issue.howToFix.take(50)}...")
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String, color: Color) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = color)
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SpecialPermissionItem(packageName: String, risk: String, onOpenSettings: () -> Unit) {
    val color = when (risk) {
        "CRITICAL" -> Color(0xFFF44336)
        "HIGH" -> Color(0xFFFF5722)
        else -> Color(0xFFFF9800)
    }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Apps, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(packageName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall)
            FilledTonalButton(
                onClick = onOpenSettings,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Manage", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
