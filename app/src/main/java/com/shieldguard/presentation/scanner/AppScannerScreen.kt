package com.shieldguard.presentation.scanner

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
import com.shieldguard.data.db.ScannedAppEntity
import com.shieldguard.domain.model.RiskLevel
import com.shieldguard.presentation.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScannerScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dangerousApps by viewModel.dangerousApps.collectAsState()

    // Filter tabs: All threats / Critical / High / Medium
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Sabhi Threats", "Critical 🚨", "High Risk 🔴", "Medium ⚠️")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startFullScan() }) {
                        Icon(Icons.Filled.PlayArrow, "Start Scan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // SCAN PROGRESS
            if (uiState.isScanning) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        uiState.scanStatusText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = uiState.scanProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Scan kar raha hai: ${uiState.currentScanningApp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SUMMARY STATS
            if (!uiState.isScanning && uiState.totalAppsScanned > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(
                        label = "Total",
                        value = "${uiState.totalAppsScanned}",
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatChip(
                        label = "Critical",
                        value = "${uiState.criticalAppsFound}",
                        color = Color(0xFFF44336)
                    )
                    StatChip(
                        label = "High Risk",
                        value = "${uiState.highRiskAppsFound}",
                        color = Color(0xFFFF5722)
                    )
                }
            }

            // TABS
            if (dangerousApps.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            // EMPTY STATE
            if (!uiState.isScanning && dangerousApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (uiState.totalAppsScanned == 0)
                                "Scan karo pehle\n'▶' button press karo"
                            else
                                "Koi dangerous app nahi mili! ✅",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // APP LIST
            val filteredApps = when (selectedTab) {
                1 -> dangerousApps.filter { it.riskLevel == "CRITICAL" }
                2 -> dangerousApps.filter { it.riskLevel == "HIGH" }
                3 -> dangerousApps.filter { it.riskLevel == "MEDIUM" }
                else -> dangerousApps
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppRiskCard(app = app)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, color = color,
                style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
fun AppRiskCard(app: ScannedAppEntity) {
    val risk = try { RiskLevel.valueOf(app.riskLevel) } catch (e: Exception) { RiskLevel.MEDIUM }
    val riskColor = when (risk) {
        RiskLevel.CRITICAL -> Color(0xFFF44336)
        RiskLevel.HIGH     -> Color(0xFFFF5722)
        RiskLevel.MEDIUM   -> Color(0xFFFF9800)
        RiskLevel.LOW      -> Color(0xFF2196F3)
        RiskLevel.SAFE     -> Color(0xFF4CAF50)
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // TOP ROW
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    null,
                    tint = riskColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(containerColor = riskColor) {
                    Text(
                        risk.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // RISK FLAGS
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (app.isKnownMalware) RiskFlag("🦠 Malware", Color(0xFFF44336))
                if (app.isFromUnknownSource) RiskFlag("Unknown Source", Color(0xFFFF9800))
                if (app.hasAccessibility) RiskFlag("Accessibility", Color(0xFFF44336))
                if (app.hasDeviceAdmin) RiskFlag("Device Admin", Color(0xFFF44336))
                if (app.hasOverlay) RiskFlag("Overlay", Color(0xFFFF5722))
            }

            // EXPAND FOR DETAILS
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (expanded) "Kam dikhao ▲" else "Details dekho ▼",
                    style = MaterialTheme.typography.labelMedium)
            }

            if (expanded) {
                Divider()
                Spacer(Modifier.height(8.dp))

                // Risk reasons
                val reasons = try {
                    com.google.gson.Gson().fromJson(app.riskReasons, Array<String>::class.java).toList()
                } catch (e: Exception) { listOf(app.riskReasons) }

                reasons.forEach { reason ->
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // App info
                InfoRow("Version", app.versionName)
                InfoRow("Install Source", app.installerSource ?: "Unknown")
                InfoRow("Install Date", java.text.SimpleDateFormat("dd MMM yyyy",
                    java.util.Locale.getDefault()).format(java.util.Date(app.firstInstallTime)))
                InfoRow("SHA-256", app.sha256Signature.take(20) + "...")

                Spacer(Modifier.height(8.dp))

                // RECOMMENDATION
                Card(
                    colors = CardDefaults.cardColors(containerColor = riskColor.copy(0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.Info, null, tint = riskColor,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            getRecommendation(risk, app.isKnownMalware, app.hasAccessibility, app.hasDeviceAdmin),
                            style = MaterialTheme.typography.bodySmall,
                            color = riskColor
                        )
                    }
                }
            }
        }
    }
}

private fun getRecommendation(
    risk: RiskLevel,
    isMalware: Boolean,
    hasAccessibility: Boolean,
    hasDeviceAdmin: Boolean
): String {
    return when {
        isMalware -> "Ye app turant uninstall karo! Settings → Apps → ${"\u2192"} Force Stop → Uninstall"
        hasDeviceAdmin -> "Pehle Device Admin remove karo: Settings → Security → Device Admins → Disable, phir uninstall karo"
        hasAccessibility -> "Accessibility disable karo: Settings → Accessibility → ${"\u2192"} Is app ke liye OFF karo"
        risk == RiskLevel.CRITICAL -> "Ye app bahut dangerous hai — uninstall karne ki koshish karo"
        risk == RiskLevel.HIGH -> "Ye app suspicious hai — usage carefully karo ya uninstall karo"
        else -> "Ye app thoda suspicious hai — permissions check karo aur unnecessary permissions deny karo"
    }
}

@Composable
fun RiskFlag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
