package com.shieldguard.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.shieldguard.domain.model.RiskLevel

// =============================================
// MAIN DASHBOARD SCREEN
// =============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToUrlCheck: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dangerousApps by viewModel.dangerousApps.collectAsState()
    val recentAlerts by viewModel.recentAlerts.collectAsState()
    val dangerousCount by viewModel.dangerousAppCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ShieldGuard AI",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDeviceStatus() }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === SECURITY SCORE CARD ===
            item {
                SecurityScoreCard(
                    securityScore = uiState.securityScore,
                    privacyScore = uiState.privacyScore,
                    dangerousAppsCount = dangerousCount,
                    isScanning = uiState.isScanning,
                    scanProgress = uiState.scanProgress,
                    scanStatusText = uiState.scanStatusText,
                    onScanClick = { viewModel.startFullScan() }
                )
            }

            // === DEVICE SECURITY STATUS ===
            item {
                val deviceStatus = uiState.deviceSecurityStatus
                if (deviceStatus != null) {
                    DeviceSecurityCard(
                        isUsbDebugging = deviceStatus.isUsbDebuggingEnabled,
                        isUnknownSources = deviceStatus.isUnknownSourcesEnabled,
                        isRooted = deviceStatus.isRooted,
                        isDeveloperOptions = deviceStatus.isDeveloperOptionsEnabled,
                        accessibilityApps = deviceStatus.appsWithAccessibility,
                        deviceAdminApps = deviceStatus.activeDeviceAdminApps,
                        issues = deviceStatus.issues
                    )
                }
            }

            // === QUICK ACTIONS ===
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                QuickActionsRow(
                    onCheckUrl = onNavigateToUrlCheck,
                    onScanApps = onNavigateToApps,
                    onCheckPermissions = onNavigateToPermissions,
                    onCheckNotifications = onNavigateToNotifications
                )
            }

            // === DANGEROUS APPS ===
            if (dangerousApps.isNotEmpty()) {
                item {
                    Text(
                        "⚠️ Dangerous Apps (${dangerousApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(dangerousApps.take(5)) { app ->
                    DangerousAppItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        riskLevel = RiskLevel.valueOf(app.riskLevel),
                        riskReason = app.riskReasons
                    )
                }
                if (dangerousApps.size > 5) {
                    item {
                        TextButton(
                            onClick = onNavigateToApps,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aur ${dangerousApps.size - 5} apps dekho →")
                        }
                    }
                }
            }

            // === RECENT ALERTS ===
            if (recentAlerts.isNotEmpty()) {
                item {
                    Text(
                        "🔔 Recent Notification Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(recentAlerts.take(3)) { alert ->
                    AlertItem(
                        appName = alert.appName,
                        title = alert.title ?: "Unknown",
                        riskLevel = alert.riskLevel,
                        reason = alert.riskReason,
                        timestamp = alert.timestamp
                    )
                }
            }

            // Bottom spacing
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
}

// =============================================
// SECURITY SCORE CARD
// =============================================
@Composable
fun SecurityScoreCard(
    securityScore: Int,
    privacyScore: Int,
    dangerousAppsCount: Int,
    isScanning: Boolean,
    scanProgress: Float,
    scanStatusText: String,
    onScanClick: () -> Unit
) {
    val scoreColor = when {
        securityScore >= 80 -> Color(0xFF4CAF50)  // Green
        securityScore >= 60 -> Color(0xFFFF9800)  // Orange
        securityScore >= 40 -> Color(0xFFFF5722)  // Deep Orange
        else -> Color(0xFFF44336)                  // Red
    }

    val statusText = when {
        securityScore == 0 -> "Scan karo pehle"
        securityScore >= 80 -> "Aapka phone safe hai ✅"
        securityScore >= 60 -> "Kuch risks hain ⚠️"
        securityScore >= 40 -> "Multiple threats! 🔴"
        else -> "Phone khatray mein! 🚨"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Score circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreCircle(
                    label = "Security",
                    score = securityScore,
                    color = scoreColor
                )
                ScoreCircle(
                    label = "Privacy",
                    score = privacyScore,
                    color = Color(0xFF2196F3)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$dangerousAppsCount",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dangerousAppsCount > 0) MaterialTheme.colorScheme.error
                                else Color(0xFF4CAF50)
                    )
                    Text(
                        "Threats",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Scan progress or button
            if (isScanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = scanProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .height(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        scanStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Full Security Scan Karo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =============================================
// SCORE CIRCLE (Animated)
// =============================================
@Composable
fun ScoreCircle(label: String, score: Int, color: Color) {
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1000, easing = EaseOut),
        label = "score"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                // Background track
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, style = stroke
                )
                // Score arc
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedScore,
                    useCenter = false, style = stroke
                )
            }
            Text(
                "$score",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================
// DEVICE SECURITY CARD
// =============================================
@Composable
fun DeviceSecurityCard(
    isUsbDebugging: Boolean,
    isUnknownSources: Boolean,
    isRooted: Boolean,
    isDeveloperOptions: Boolean,
    accessibilityApps: List<String>,
    deviceAdminApps: List<String>,
    issues: List<com.shieldguard.domain.model.DeviceSecurityIssue>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (isRooted || isUsbDebugging)
            BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📱 Device Security",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            SecurityCheckItem("USB Debugging", !isUsbDebugging, "OFF rakho hamesha")
            SecurityCheckItem("Unknown Sources", !isUnknownSources, "Play Store se hi install karo")
            SecurityCheckItem("Developer Options", !isDeveloperOptions, "Normal users ko chahiye nahi")
            SecurityCheckItem("Root Detected", !isRooted, "Rooted phone unsafe hai")

            if (accessibilityApps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ Accessibility Access wali apps: ${accessibilityApps.size}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (deviceAdminApps.isNotEmpty()) {
                Text(
                    "⚠️ Device Admin wali apps: ${deviceAdminApps.size}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SecurityCheckItem(label: String, isGood: Boolean, tip: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGood) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (isGood) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (!isGood) {
                Text(
                    tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (isGood) "✅ OK" else "⚠️ Risk",
            style = MaterialTheme.typography.labelSmall,
            color = if (isGood) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

// =============================================
// QUICK ACTIONS
// =============================================
@Composable
fun QuickActionsRow(
    onCheckUrl: () -> Unit,
    onScanApps: () -> Unit,
    onCheckPermissions: () -> Unit,
    onCheckNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            icon = Icons.Filled.Link,
            label = "URL\nCheck",
            color = Color(0xFF2196F3),
            onClick = onCheckUrl,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Filled.Apps,
            label = "App\nScan",
            color = Color(0xFF4CAF50),
            onClick = onScanApps,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Filled.AdminPanelSettings,
            label = "Perms",
            color = Color(0xFFFF9800),
            onClick = onCheckPermissions,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Filled.Notifications,
            label = "Alerts",
            color = Color(0xFF9C27B0),
            onClick = onCheckNotifications,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =============================================
// DANGEROUS APP ITEM
// =============================================
@Composable
fun DangerousAppItem(
    appName: String,
    packageName: String,
    riskLevel: RiskLevel,
    riskReason: String
) {
    val (bgColor, textColor) = when (riskLevel) {
        RiskLevel.CRITICAL -> Pair(Color(0xFFF44336).copy(alpha = 0.12f), Color(0xFFF44336))
        RiskLevel.HIGH -> Pair(Color(0xFFFF5722).copy(alpha = 0.12f), Color(0xFFFF5722))
        RiskLevel.MEDIUM -> Pair(Color(0xFFFF9800).copy(alpha = 0.12f), Color(0xFFFF9800))
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appName, fontWeight = FontWeight.Bold, color = textColor)
                Text(
                    packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                if (riskReason.isNotEmpty()) {
                    Text(
                        riskReason.take(80) + if (riskReason.length > 80) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
            Badge(containerColor = textColor) {
                Text(
                    riskLevel.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// =============================================
// ALERT ITEM
// =============================================
@Composable
fun AlertItem(
    appName: String,
    title: String,
    riskLevel: String,
    reason: String,
    timestamp: Long
) {
    val risk = try { RiskLevel.valueOf(riskLevel) } catch (e: Exception) { RiskLevel.MEDIUM }
    val color = when (risk) {
        RiskLevel.CRITICAL -> Color(0xFFF44336)
        RiskLevel.HIGH -> Color(0xFFFF5722)
        RiskLevel.MEDIUM -> Color(0xFFFF9800)
        else -> Color(0xFF2196F3)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
                    .align(Alignment.CenterVertically)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$appName — $title",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    reason.take(100),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                        .format(java.util.Date(timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
