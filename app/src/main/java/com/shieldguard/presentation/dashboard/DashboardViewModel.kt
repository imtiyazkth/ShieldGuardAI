package com.shieldguard.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldguard.data.db.NotificationAlertEntity
import com.shieldguard.data.db.ScannedAppEntity
import com.shieldguard.data.repository.MalwareDbRepository
import com.shieldguard.data.repository.SecurityRepository
import com.shieldguard.domain.model.*
import com.shieldguard.domain.usecase.ScanProgress
import com.shieldguard.domain.usecase.SecurityScanner
import com.shieldguard.domain.usecase.UrlScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val securityScanner: SecurityScanner,
    private val urlScanner: UrlScanner,
    private val securityRepository: SecurityRepository,
    private val malwareDbRepository: MalwareDbRepository
) : ViewModel() {

    // =========================================
    // UI STATE
    // =========================================
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Live data from DB
    val dangerousApps: StateFlow<List<ScannedAppEntity>> = securityRepository
        .getDangerousApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentAlerts: StateFlow<List<NotificationAlertEntity>> = securityRepository
        .getRecentNotificationAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dangerousAppCount: StateFlow<Int> = securityRepository
        .getDangerousAppCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Load initial state
        loadDeviceStatus()
        updateMalwareDb()
    }

    // =========================================
    // FULL SECURITY SCAN
    // =========================================
    fun startFullScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isScanning = true,
                scanProgress = 0f,
                scanStatusText = "Scan shuru ho raha hai...",
                error = null
            )}

            val scannedApps = mutableListOf<ScannedApp>()

            securityScanner.scanAllApps().collect { progress ->
                when (progress) {
                    is ScanProgress.Started -> {
                        _uiState.update { it.copy(
                            scanStatusText = "${progress.totalApps} apps scan hogi..."
                        )}
                    }
                    is ScanProgress.Progress -> {
                        val percent = progress.current.toFloat() / progress.total
                        scannedApps.add(progress.scannedApp)
                        _uiState.update { it.copy(
                            scanProgress = percent,
                            scanStatusText = "Scan kar raha hun: ${progress.currentAppName}",
                            currentScanningApp = progress.currentAppName
                        )}

                        // Save to DB in batches
                        if (scannedApps.size % 10 == 0) {
                            securityRepository.saveScannedApps(scannedApps.takeLast(10))
                        }
                    }
                    is ScanProgress.Completed -> {
                        // Save remaining
                        securityRepository.saveScannedApps(progress.results)

                        // Calculate scores
                        val securityScore = calculateSecurityScore(progress.results)
                        val privacyScore = calculatePrivacyScore(progress.results)

                        _uiState.update { it.copy(
                            isScanning = false,
                            scanProgress = 1f,
                            scanStatusText = "Scan complete!",
                            securityScore = securityScore,
                            privacyScore = privacyScore,
                            lastScanTime = System.currentTimeMillis(),
                            totalAppsScanned = progress.results.size,
                            criticalAppsFound = progress.results.count { it.riskLevel == RiskLevel.CRITICAL },
                            highRiskAppsFound = progress.results.count { it.riskLevel == RiskLevel.HIGH }
                        )}
                    }
                    is ScanProgress.Error -> {
                        _uiState.update { it.copy(
                            isScanning = false,
                            error = progress.message
                        )}
                    }
                }
            }
        }
    }

    // =========================================
    // URL CHECK
    // =========================================
    fun checkUrl(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUrl = true, urlScanResult = null) }
            try {
                val result = urlScanner.scanUrl(url)
                securityRepository.saveUrlScan(result)
                _uiState.update { it.copy(
                    isCheckingUrl = false,
                    urlScanResult = result
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isCheckingUrl = false,
                    error = "URL check fail hua: ${e.message}"
                )}
            }
        }
    }

    // =========================================
    // DEVICE SECURITY
    // =========================================
    fun loadDeviceStatus() {
        viewModelScope.launch {
            val status = securityScanner.checkDeviceSecurity()
            _uiState.update { it.copy(deviceSecurityStatus = status) }
        }
    }

    // =========================================
    // UPDATE MALWARE DB
    // =========================================
    private fun updateMalwareDb() {
        viewModelScope.launch {
            try {
                malwareDbRepository.updateFromRemote()
                val count = malwareDbRepository.getCount()
                _uiState.update { it.copy(malwareDbCount = count) }
            } catch (e: Exception) {
                // DB update failed — offline DB still works
            }
        }
    }

    // =========================================
    // SCORE CALCULATORS
    // =========================================
    private fun calculateSecurityScore(apps: List<ScannedApp>): Int {
        if (apps.isEmpty()) return 100
        val userApps = apps.filter { !it.isSystemApp }
        if (userApps.isEmpty()) return 100

        val criticalCount = userApps.count { it.riskLevel == RiskLevel.CRITICAL }
        val highCount = userApps.count { it.riskLevel == RiskLevel.HIGH }
        val mediumCount = userApps.count { it.riskLevel == RiskLevel.MEDIUM }
        val unknownSourceCount = userApps.count { it.isFromUnknownSource }
        val malwareCount = userApps.count { it.isKnownMalware }

        var score = 100
        score -= criticalCount * 25
        score -= highCount * 15
        score -= mediumCount * 5
        score -= unknownSourceCount * 3
        score -= malwareCount * 30

        // Device security deductions
        val deviceStatus = _uiState.value.deviceSecurityStatus
        if (deviceStatus != null) {
            if (deviceStatus.isUsbDebuggingEnabled) score -= 15
            if (deviceStatus.isUnknownSourcesEnabled) score -= 10
            if (deviceStatus.isRooted) score -= 30
        }

        return score.coerceIn(0, 100)
    }

    private fun calculatePrivacyScore(apps: List<ScannedApp>): Int {
        val userApps = apps.filter { !it.isSystemApp }
        if (userApps.isEmpty()) return 100

        var score = 100
        score -= userApps.count { it.hasCameraPermission } * 3
        score -= userApps.count { it.hasMicrophonePermission } * 3
        score -= userApps.count { it.hasLocationPermission } * 4
        score -= userApps.count { it.hasContactsPermission } * 3
        score -= userApps.count { it.hasCallLogsPermission } * 5
        score -= userApps.count { it.hasSmsPermission } * 5
        score -= userApps.count { it.hasAccessibilityService } * 10
        score -= userApps.count { it.hasUsageStatsAccess } * 5

        return score.coerceIn(0, 100)
    }

    fun dismissUrlResult() {
        _uiState.update { it.copy(urlScanResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// =============================================
// UI STATE
// =============================================
data class DashboardUiState(
    // Scanning
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val scanStatusText: String = "",
    val currentScanningApp: String = "",
    val lastScanTime: Long? = null,
    val totalAppsScanned: Int = 0,
    val criticalAppsFound: Int = 0,
    val highRiskAppsFound: Int = 0,

    // Scores
    val securityScore: Int = 0,
    val privacyScore: Int = 0,

    // URL Check
    val isCheckingUrl: Boolean = false,
    val urlScanResult: UrlScanResult? = null,

    // Device
    val deviceSecurityStatus: DeviceSecurityStatus? = null,

    // DB
    val malwareDbCount: Int = 0,

    // Error
    val error: String? = null
)
