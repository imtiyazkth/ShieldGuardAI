package com.shieldguard.domain.usecase

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import android.provider.Settings
import com.shieldguard.data.repository.MalwareDbRepository
import com.shieldguard.domain.model.*
import com.shieldguard.util.SignatureUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val malwareDbRepository: MalwareDbRepository,
    private val permissionAnalyzer: PermissionAnalyzer
) {

    // =========================================
    // SCAN ALL INSTALLED APPS
    // Returns Flow so UI can show live progress
    // =========================================
    fun scanAllApps(): Flow<ScanProgress> = flow {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.PackageInfoFlags.of(
                (PackageManager.GET_PERMISSIONS or
                PackageManager.GET_SIGNATURES or
                PackageManager.GET_META_DATA).toLong()
            )
        } else {
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES or PackageManager.GET_META_DATA
        }

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(
                (PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES).toLong()
            ))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)
        }

        val total = packages.size
        val scannedApps = mutableListOf<ScannedApp>()

        emit(ScanProgress.Started(total))

        packages.forEachIndexed { index, packageInfo ->
            val scanned = analyzePackage(pm, packageInfo)
            scannedApps.add(scanned)
            emit(ScanProgress.Progress(
                current = index + 1,
                total = total,
                currentAppName = scanned.appName,
                scannedApp = scanned
            ))
        }

        emit(ScanProgress.Completed(scannedApps))
    }.flowOn(Dispatchers.IO)

    // =========================================
    // ANALYZE A SINGLE PACKAGE
    // =========================================
    private suspend fun analyzePackage(pm: PackageManager, packageInfo: PackageInfo): ScannedApp {
        val appInfo = packageInfo.applicationInfo
        val packageName = packageInfo.packageName
        val appName = pm.getApplicationLabel(appInfo).toString()

        // --- Signature SHA256 ---
        val sha256 = SignatureUtils.getSha256(packageInfo)

        // --- Install source ---
        val installerSource = getInstallerSource(pm, packageName)

        // --- Is from unknown source ---
        val isFromUnknownSource = isInstalledFromUnknownSource(pm, packageInfo, installerSource)

        // --- Is debug build ---
        val isDebugBuild = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // --- Is system app ---
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        // --- Permissions ---
        val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        val dangerousPermissions = permissionAnalyzer.analyzeDangerousPermissions(requestedPermissions)

        // --- Special permissions ---
        val hasAccessibility = hasAccessibilityService(packageName)
        val hasOverlay = hasOverlayPermission(requestedPermissions)
        val hasNotification = hasNotificationAccess(packageName)
        val hasDevAdmin = hasDeviceAdmin(packageName)
        val hasVpn = hasVpnService(pm, packageName)

        // --- Known malware check ---
        val malwareEntry = malwareDbRepository.checkPackageName(packageName)
        val malwareShaEntry = malwareDbRepository.checkSha256(sha256)
        val isKnownMalware = malwareEntry != null || malwareShaEntry != null

        // --- Calculate risk ---
        val (riskLevel, riskScore, riskReasons) = calculateAppRisk(
            isFromUnknownSource = isFromUnknownSource,
            isDebugBuild = isDebugBuild,
            isKnownMalware = isKnownMalware,
            hasAccessibility = hasAccessibility,
            hasOverlay = hasOverlay,
            hasDevAdmin = hasDevAdmin,
            hasVpn = hasVpn,
            dangerousPermissions = dangerousPermissions,
            isSystemApp = isSystemApp,
            packageName = packageName
        )

        return ScannedApp(
            packageName = packageName,
            appName = appName,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
            icon = try { pm.getApplicationIcon(packageName) } catch (e: Exception) { null },
            installerSource = installerSource,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            targetSdk = appInfo.targetSdkVersion,
            minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 0,
            sha256Signature = sha256,
            isSystemApp = isSystemApp,
            isFromUnknownSource = isFromUnknownSource,
            isDebugBuild = isDebugBuild,
            requestedPermissions = requestedPermissions,
            dangerousPermissions = dangerousPermissions,
            riskLevel = riskLevel,
            riskReasons = riskReasons,
            riskScore = riskScore,
            isKnownMalware = isKnownMalware,
            malwareCategory = malwareEntry?.category ?: malwareShaEntry?.category,
            hasAccessibilityService = hasAccessibility,
            hasOverlayPermission = hasOverlay,
            hasNotificationAccess = hasNotification,
            hasDeviceAdmin = hasDevAdmin,
            hasVpnService = hasVpn,
            hasSmsPermission = requestedPermissions.contains(android.Manifest.permission.SEND_SMS) ||
                               requestedPermissions.contains(android.Manifest.permission.READ_SMS),
            hasCallLogsPermission = requestedPermissions.contains(android.Manifest.permission.READ_CALL_LOG),
            hasCameraPermission = requestedPermissions.contains(android.Manifest.permission.CAMERA),
            hasMicrophonePermission = requestedPermissions.contains(android.Manifest.permission.RECORD_AUDIO),
            hasLocationPermission = requestedPermissions.contains(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                                   requestedPermissions.contains(android.Manifest.permission.ACCESS_COARSE_LOCATION),
            hasContactsPermission = requestedPermissions.contains(android.Manifest.permission.READ_CONTACTS),
            hasStoragePermission = requestedPermissions.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE) ||
                                  requestedPermissions.contains(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            hasUsageStatsAccess = hasUsageStatsAccess(packageName)
        )
    }

    // =========================================
    // RISK CALCULATOR — Core logic
    // =========================================
    private fun calculateAppRisk(
        isFromUnknownSource: Boolean,
        isDebugBuild: Boolean,
        isKnownMalware: Boolean,
        hasAccessibility: Boolean,
        hasOverlay: Boolean,
        hasDevAdmin: Boolean,
        hasVpn: Boolean,
        dangerousPermissions: List<DangerousPermission>,
        isSystemApp: Boolean,
        packageName: String
    ): Triple<RiskLevel, Int, List<String>> {
        // System apps get a pass unless known malware
        if (isSystemApp && !isKnownMalware) {
            return Triple(RiskLevel.SAFE, 0, emptyList())
        }

        val reasons = mutableListOf<String>()
        var score = 0

        // CRITICAL: Known malware
        if (isKnownMalware) {
            score += 100
            reasons.add("⚠️ Ye app malware database mein hai — ye DANGEROUS hai!")
        }

        // HIGH: Special permissions
        if (hasAccessibility && !isSystemApp) {
            score += 40
            reasons.add("🔴 Accessibility Service access hai — ye dusre apps ko control kar sakta hai")
        }
        if (hasDevAdmin) {
            score += 35
            reasons.add("🔴 Device Admin access hai — ye phone lock/wipe kar sakta hai")
        }
        if (hasVpn && !isSystemApp) {
            score += 30
            reasons.add("🟠 VPN service hai — ye aapka internet traffic dekh sakta hai")
        }
        if (hasOverlay && !isSystemApp) {
            score += 25
            reasons.add("🟠 Screen pe kuch bhi dikhane ka permission hai (Draw Over Apps)")
        }

        // MEDIUM: Unknown source install
        if (isFromUnknownSource) {
            score += 30
            reasons.add("🟠 Ye app Play Store se install nahi hua — unknown source")
        }

        // MEDIUM: Debug build
        if (isDebugBuild) {
            score += 20
            reasons.add("🟡 Ye debug build hai — production app nahi")
        }

        // MEDIUM: Dangerous permission combinations
        val criticalPermCount = dangerousPermissions.count { it.riskLevel == RiskLevel.CRITICAL }
        val highPermCount = dangerousPermissions.count { it.riskLevel == RiskLevel.HIGH }

        score += criticalPermCount * 15
        score += highPermCount * 8

        if (criticalPermCount > 0) {
            reasons.add("🟠 $criticalPermCount critical permissions hain (SMS, Call Logs, etc.)")
        }

        // Suspicious package name patterns
        val suspiciousPatterns = listOf(
            "com.android.system", "android.system", "system.update",
            "security.scan", "battery.saver", "phone.cleaner",
            "virus.cleaner", "speed.booster"
        )
        if (suspiciousPatterns.any { packageName.contains(it) } && !isSystemApp) {
            score += 20
            reasons.add("🟡 Package name suspicious lagta hai — fake system app ho sakta hai")
        }

        val riskLevel = when {
            score >= 80 -> RiskLevel.CRITICAL
            score >= 60 -> RiskLevel.HIGH
            score >= 35 -> RiskLevel.MEDIUM
            score >= 10 -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        return Triple(riskLevel, score.coerceAtMost(100), reasons)
    }

    // =========================================
    // HELPER FUNCTIONS
    // =========================================

    private fun getInstallerSource(pm: PackageManager, packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) { null }
    }

    private fun isInstalledFromUnknownSource(
        pm: PackageManager,
        packageInfo: PackageInfo,
        installerSource: String?
    ): Boolean {
        if ((packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return false
        val knownSources = setOf(
            "com.android.vending",           // Google Play Store
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.huawei.appmarket",
            "com.oppo.market",
            "com.vivo.appstore"
        )
        return installerSource == null || installerSource !in knownSources
    }

    private fun hasAccessibilityService(packageName: String): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName)
    }

    private fun hasOverlayPermission(permissions: List<String>): Boolean {
        return permissions.contains(android.Manifest.permission.SYSTEM_ALERT_WINDOW)
    }

    private fun hasNotificationAccess(packageName: String): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(packageName)
    }

    private fun hasDeviceAdmin(packageName: String): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.activeAdmins?.any { it.packageName == packageName } ?: false
    }

    private fun hasVpnService(pm: PackageManager, packageName: String): Boolean {
        return try {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            packageInfo.requestedPermissions?.contains("android.permission.BIND_VPN_SERVICE") == true
        } catch (e: Exception) { false }
    }

    private fun hasUsageStatsAccess(packageName: String): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.packageManager.getApplicationInfo(packageName, 0).uid,
                packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    // =========================================
    // DEVICE SECURITY CHECK
    // =========================================
    suspend fun checkDeviceSecurity(): DeviceSecurityStatus {
        val pm = context.packageManager
        val issues = mutableListOf<DeviceSecurityIssue>()
        var score = 100

        val isDeveloperOptions = Settings.Global.getInt(
            context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) == 1

        val isUsbDebugging = Settings.Global.getInt(
            context.contentResolver, Settings.Global.ADB_ENABLED, 0
        ) == 1

        val isUnknownSources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pm.canRequestPackageInstalls()
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        }

        val isRooted = checkRoot()

        // Deduct scores
        if (isDeveloperOptions) {
            score -= 10
            issues.add(DeviceSecurityIssue(
                title = "Developer Options ON hai",
                description = "Developer options ON hone se security risks badh jaate hain",
                riskLevel = RiskLevel.MEDIUM,
                howToFix = "Settings → About Phone → Developer Options → OFF karo"
            ))
        }

        if (isUsbDebugging) {
            score -= 20
            issues.add(DeviceSecurityIssue(
                title = "USB Debugging ON hai",
                description = "USB se koi bhi computer aapke phone ka data access kar sakta hai",
                riskLevel = RiskLevel.HIGH,
                howToFix = "Settings → Developer Options → USB Debugging → OFF karo"
            ))
        }

        if (isUnknownSources) {
            score -= 25
            issues.add(DeviceSecurityIssue(
                title = "Unknown Sources se install allow hai",
                description = "Play Store ke bahar se koi bhi app install ho sakta hai",
                riskLevel = RiskLevel.HIGH,
                howToFix = "Settings → Apps → Special Access → Install Unknown Apps → sab OFF karo"
            ))
        }

        if (isRooted) {
            score -= 40
            issues.add(DeviceSecurityIssue(
                title = "Phone Root hua lag raha hai",
                description = "Root hone se koi bhi app aapki poori device control kar sakta hai",
                riskLevel = RiskLevel.CRITICAL,
                howToFix = "Rooted phone mein maximum security nahi ho sakti — unroot karne ki koshish karein"
            ))
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val activeAdmins = dpm.activeAdmins?.map { it.packageName } ?: emptyList()

        val enabledAccessibility = (Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: "").split(":").filter { it.isNotEmpty() }.map { it.split("/")[0] }

        val enabledOverlay = getAppsWithOverlay()

        val enabledNotification = (Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: "").split(":").filter { it.isNotEmpty() }.map { it.split("/")[0] }

        return DeviceSecurityStatus(
            isDeveloperOptionsEnabled = isDeveloperOptions,
            isUsbDebuggingEnabled = isUsbDebugging,
            isUnknownSourcesEnabled = isUnknownSources,
            isPlayProtectEnabled = true, // Checked via Play Integrity API separately
            isRooted = isRooted,
            isBootloaderUnlocked = checkBootloaderUnlocked(),
            hasActiveVpn = checkActiveVpn(),
            hasPrivateDns = checkPrivateDns(),
            hasActiveDeviceAdmin = activeAdmins.isNotEmpty(),
            activeDeviceAdminApps = activeAdmins,
            appsWithAccessibility = enabledAccessibility,
            appsWithOverlay = enabledOverlay,
            appsWithNotificationAccess = enabledNotification,
            securityScore = score.coerceAtLeast(0),
            issues = issues
        )
    }

    private fun checkRoot(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { java.io.File(it).exists() }
    }

    private fun checkBootloaderUnlocked(): Boolean {
        return try {
            val prop = Runtime.getRuntime().exec(arrayOf("/system/bin/getprop", "ro.boot.verifiedbootstate"))
            val result = prop.inputStream.bufferedReader().readLine()
            result == "orange" || result == "yellow"
        } catch (e: Exception) { false }
    }

    private fun checkActiveVpn(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networks = connectivityManager.allNetworks
        return networks.any { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }

    private fun checkPrivateDns(): Boolean {
        return Settings.Global.getString(context.contentResolver, "private_dns_mode") == "hostname"
    }

    private fun getAppsWithOverlay(): List<String> {
        val pm = context.packageManager
        return pm.getInstalledPackages(0)
            .filter { pkg ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val appInfo = pm.getPackageInfo(pkg.packageName, PackageManager.GET_PERMISSIONS)
                        appInfo.requestedPermissions?.contains(android.Manifest.permission.SYSTEM_ALERT_WINDOW) == true
                    } else false
                } catch (e: Exception) { false }
            }
            .map { it.packageName }
    }
}

// Progress events for UI
sealed class ScanProgress {
    data class Started(val totalApps: Int) : ScanProgress()
    data class Progress(
        val current: Int,
        val total: Int,
        val currentAppName: String,
        val scannedApp: ScannedApp
    ) : ScanProgress()
    data class Completed(val results: List<ScannedApp>) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
}
