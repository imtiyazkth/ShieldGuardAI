package com.shieldguard.data.repository

import com.google.gson.Gson
import com.shieldguard.data.db.*
import com.shieldguard.domain.model.ScannedApp
import com.shieldguard.domain.model.UrlScanResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepository @Inject constructor(
    private val scannedAppDao: ScannedAppDao,
    private val notificationAlertDao: NotificationAlertDao,
    private val urlScanDao: UrlScanDao,
    private val scanReportDao: ScanReportDao
) {
    private val gson = Gson()

    fun getDangerousApps(): Flow<List<ScannedAppEntity>> =
        scannedAppDao.getDangerousApps()

    fun getDangerousAppCount(): Flow<Int> =
        scannedAppDao.getDangerousCount()

    fun getRecentNotificationAlerts(): Flow<List<NotificationAlertEntity>> =
        notificationAlertDao.getRecentAlerts()

    suspend fun saveScannedApps(apps: List<ScannedApp>) {
        val entities = apps.map { app ->
            ScannedAppEntity(
                packageName = app.packageName,
                appName = app.appName,
                versionName = app.versionName,
                versionCode = app.versionCode,
                installerSource = app.installerSource,
                firstInstallTime = app.firstInstallTime,
                lastUpdateTime = app.lastUpdateTime,
                targetSdk = app.targetSdk,
                sha256Signature = app.sha256Signature,
                isSystemApp = app.isSystemApp,
                isFromUnknownSource = app.isFromUnknownSource,
                isDebugBuild = app.isDebugBuild,
                riskLevel = app.riskLevel.name,
                riskScore = app.riskScore,
                riskReasons = gson.toJson(app.riskReasons),
                isKnownMalware = app.isKnownMalware,
                malwareCategory = app.malwareCategory,
                hasAccessibility = app.hasAccessibilityService,
                hasOverlay = app.hasOverlayPermission,
                hasNotificationAccess = app.hasNotificationAccess,
                hasDeviceAdmin = app.hasDeviceAdmin,
                dangerousPermissions = gson.toJson(app.dangerousPermissions.map { it.permission }),
                scanTime = app.scanTime
            )
        }
        scannedAppDao.upsertAll(entities)
    }

    suspend fun saveUrlScan(result: UrlScanResult) {
        urlScanDao.insert(UrlScanEntity(
            url = result.url,
            finalUrl = result.finalUrl,
            riskLevel = result.riskLevel.name,
            riskScore = result.riskLevel.score,
            isPhishing = result.isPhishing,
            isMalware = result.isMalware,
            isScam = result.isScam,
            aiExplanation = result.aiExplanation,
            riskReasons = gson.toJson(result.riskReasons),
            recommendation = result.recommendation.name,
            scanTime = result.scanTime
        ))
    }
}
