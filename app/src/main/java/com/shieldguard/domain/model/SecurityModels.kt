package com.shieldguard.domain.model

import android.graphics.drawable.Drawable

// =============================================
// RISK LEVELS
// =============================================
enum class RiskLevel(val displayName: String, val score: Int) {
    SAFE("Safe", 0),
    LOW("Low Risk", 25),
    MEDIUM("Medium Risk", 50),
    HIGH("High Risk", 75),
    CRITICAL("Critical Threat", 100)
}

// =============================================
// SCANNED APP MODEL
// =============================================
data class ScannedApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val installerSource: String?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val sha256Signature: String,
    val isSystemApp: Boolean,
    val isFromUnknownSource: Boolean,
    val isDebugBuild: Boolean,
    val requestedPermissions: List<String>,
    val dangerousPermissions: List<DangerousPermission>,
    val riskLevel: RiskLevel,
    val riskReasons: List<String>,
    val riskScore: Int,
    val isKnownMalware: Boolean,
    val malwareCategory: String? = null,
    val hasAccessibilityService: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasDeviceAdmin: Boolean = false,
    val hasVpnService: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val hasCallLogsPermission: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val hasMicrophonePermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val hasUsageStatsAccess: Boolean = false,
    val scanTime: Long = System.currentTimeMillis()
)

// =============================================
// DANGEROUS PERMISSION
// =============================================
data class DangerousPermission(
    val permission: String,
    val displayName: String,
    val description: String,
    val riskLevel: RiskLevel,
    val whyDangerous: String,
    val recommendation: String
)

// =============================================
// URL/LINK SCAN RESULT
// =============================================
data class UrlScanResult(
    val url: String,
    val finalUrl: String,           // after redirects
    val isHttps: Boolean,
    val sslValid: Boolean,
    val domainAge: Int?,            // days, null if unknown
    val isSafeBrowsingClean: Boolean,
    val isPhishing: Boolean,
    val isScam: Boolean,
    val isMalware: Boolean,
    val isKnownBadDomain: Boolean,
    val isUrlShortener: Boolean,
    val hasHomographAttack: Boolean,
    val redirectChain: List<String>,
    val riskLevel: RiskLevel,
    val riskReasons: List<String>,
    val aiExplanation: String,
    val recommendation: UrlRecommendation,
    val scanTime: Long = System.currentTimeMillis()
)

enum class UrlRecommendation {
    SAFE_TO_VISIT,
    PROCEED_WITH_CAUTION,
    BLOCK_AND_WARN,
    BLOCKED_MALWARE,
    BLOCKED_PHISHING
}

// =============================================
// NOTIFICATION ALERT
// =============================================
data class NotificationAlert(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String?,
    val content: String?,
    val containedUrl: String?,
    val urlScanResult: UrlScanResult?,
    val riskLevel: RiskLevel,
    val riskReason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wasBlocked: Boolean = false
)

// =============================================
// DEVICE SECURITY STATUS
// =============================================
data class DeviceSecurityStatus(
    val isDeveloperOptionsEnabled: Boolean,
    val isUsbDebuggingEnabled: Boolean,
    val isUnknownSourcesEnabled: Boolean,
    val isPlayProtectEnabled: Boolean,
    val isRooted: Boolean,
    val isBootloaderUnlocked: Boolean,
    val hasActiveVpn: Boolean,
    val hasPrivateDns: Boolean,
    val hasActiveDeviceAdmin: Boolean,
    val activeDeviceAdminApps: List<String>,
    val appsWithAccessibility: List<String>,
    val appsWithOverlay: List<String>,
    val appsWithNotificationAccess: List<String>,
    val securityScore: Int,
    val issues: List<DeviceSecurityIssue>
)

data class DeviceSecurityIssue(
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
    val howToFix: String
)

// =============================================
// SCAN REPORT
// =============================================
data class ScanReport(
    val id: Long = 0,
    val scanTime: Long = System.currentTimeMillis(),
    val totalAppsScanned: Int,
    val dangerousApps: List<ScannedApp>,
    val highRiskApps: List<ScannedApp>,
    val mediumRiskApps: List<ScannedApp>,
    val urlsScanned: Int,
    val blockedUrls: Int,
    val notificationAlerts: List<NotificationAlert>,
    val deviceSecurityStatus: DeviceSecurityStatus,
    val overallSecurityScore: Int,
    val overallPrivacyScore: Int,
    val recommendations: List<String>
)

// =============================================
// MALWARE DB ENTRY
// =============================================
data class MalwareEntry(
    val packageName: String? = null,
    val domain: String? = null,
    val sha256: String? = null,
    val category: String,           // adware, spyware, ransomware, phishing, etc
    val description: String,
    val severity: RiskLevel,
    val source: String              // who reported it
)

// =============================================
// BROWSER APP INFO
// =============================================
data class BrowserSecurity(
    val packageName: String,
    val browserName: String,
    val isDefault: Boolean,
    val hasNotificationAbuse: Boolean,
    val hasPopupAbuse: Boolean,
    val isHijacked: Boolean,
    val riskLevel: RiskLevel,
    val issues: List<String>,
    val recommendations: List<String>
)
