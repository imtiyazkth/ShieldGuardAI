package com.shieldguard.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// =============================================
// DATABASE
// =============================================
@Database(
    entities = [
        ScannedAppEntity::class,
        NotificationAlertEntity::class,
        MalwareEntryEntity::class,
        UrlScanEntity::class,
        ScanReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShieldGuardDatabase : RoomDatabase() {
    abstract fun scannedAppDao(): ScannedAppDao
    abstract fun notificationAlertDao(): NotificationAlertDao
    abstract fun malwareEntryDao(): MalwareEntryDao
    abstract fun urlScanDao(): UrlScanDao
    abstract fun scanReportDao(): ScanReportDao
}

// =============================================
// ENTITY: Scanned App
// =============================================
@Entity(tableName = "scanned_apps")
data class ScannedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val installerSource: String?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val targetSdk: Int,
    val sha256Signature: String,
    val isSystemApp: Boolean,
    val isFromUnknownSource: Boolean,
    val isDebugBuild: Boolean,
    val riskLevel: String,          // RiskLevel.name
    val riskScore: Int,
    val riskReasons: String,        // JSON array
    val isKnownMalware: Boolean,
    val malwareCategory: String?,
    val hasAccessibility: Boolean,
    val hasOverlay: Boolean,
    val hasNotificationAccess: Boolean,
    val hasDeviceAdmin: Boolean,
    val dangerousPermissions: String,  // JSON array
    val scanTime: Long
)

@Dao
interface ScannedAppDao {
    @Query("SELECT * FROM scanned_apps ORDER BY riskScore DESC")
    fun getAllApps(): Flow<List<ScannedAppEntity>>

    @Query("SELECT * FROM scanned_apps WHERE riskLevel IN ('HIGH', 'CRITICAL') ORDER BY riskScore DESC")
    fun getDangerousApps(): Flow<List<ScannedAppEntity>>

    @Query("SELECT * FROM scanned_apps WHERE packageName = :packageName")
    suspend fun getApp(packageName: String): ScannedAppEntity?

    @Query("SELECT * FROM scanned_apps WHERE isKnownMalware = 1")
    fun getMalwareApps(): Flow<List<ScannedAppEntity>>

    @Upsert
    suspend fun upsert(app: ScannedAppEntity)

    @Upsert
    suspend fun upsertAll(apps: List<ScannedAppEntity>)

    @Query("DELETE FROM scanned_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT COUNT(*) FROM scanned_apps")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM scanned_apps WHERE riskLevel IN ('HIGH', 'CRITICAL')")
    fun getDangerousCount(): Flow<Int>
}

// =============================================
// ENTITY: Notification Alert
// =============================================
@Entity(tableName = "notification_alerts")
data class NotificationAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String?,
    val content: String?,
    val containedUrl: String?,
    val riskLevel: String,
    val riskReason: String,
    val timestamp: Long,
    val wasBlocked: Boolean
)

@Dao
interface NotificationAlertDao {
    @Query("SELECT * FROM notification_alerts ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAlerts(): Flow<List<NotificationAlertEntity>>

    @Query("SELECT * FROM notification_alerts WHERE riskLevel IN ('HIGH', 'CRITICAL') ORDER BY timestamp DESC")
    fun getCriticalAlerts(): Flow<List<NotificationAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: NotificationAlertEntity)

    @Query("DELETE FROM notification_alerts WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM notification_alerts WHERE timestamp > :since")
    fun getAlertCountSince(since: Long): Flow<Int>
}

// =============================================
// ENTITY: Malware DB
// =============================================
@Entity(
    tableName = "malware_entries",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["domain"]),
        Index(value = ["sha256"])
    ]
)
data class MalwareEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String?,
    val domain: String?,
    val sha256: String?,
    val category: String,
    val description: String,
    val severity: String,
    val source: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface MalwareEntryDao {
    @Query("SELECT * FROM malware_entries WHERE packageName = :packageName LIMIT 1")
    suspend fun findByPackageName(packageName: String): MalwareEntryEntity?

    @Query("SELECT * FROM malware_entries WHERE domain = :domain LIMIT 1")
    suspend fun findByDomain(domain: String): MalwareEntryEntity?

    @Query("SELECT * FROM malware_entries WHERE sha256 = :sha256 LIMIT 1")
    suspend fun findBySha256(sha256: String): MalwareEntryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<MalwareEntryEntity>)

    @Query("SELECT COUNT(*) FROM malware_entries")
    suspend fun getCount(): Int

    @Query("DELETE FROM malware_entries")
    suspend fun clearAll()
}

// =============================================
// ENTITY: URL Scan History
// =============================================
@Entity(tableName = "url_scans")
data class UrlScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val finalUrl: String,
    val riskLevel: String,
    val riskScore: Int,
    val isPhishing: Boolean,
    val isMalware: Boolean,
    val isScam: Boolean,
    val aiExplanation: String,
    val riskReasons: String,        // JSON array
    val recommendation: String,
    val scanTime: Long
)

@Dao
interface UrlScanDao {
    @Query("SELECT * FROM url_scans ORDER BY scanTime DESC LIMIT 50")
    fun getRecentScans(): Flow<List<UrlScanEntity>>

    @Query("SELECT * FROM url_scans WHERE riskLevel IN ('HIGH', 'CRITICAL') ORDER BY scanTime DESC")
    fun getDangerousUrls(): Flow<List<UrlScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: UrlScanEntity)
}

// =============================================
// ENTITY: Scan Reports
// =============================================
@Entity(tableName = "scan_reports")
data class ScanReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanTime: Long,
    val totalAppsScanned: Int,
    val dangerousAppsCount: Int,
    val urlsScanned: Int,
    val blockedUrls: Int,
    val overallSecurityScore: Int,
    val overallPrivacyScore: Int,
    val recommendations: String,    // JSON array
    val reportJson: String          // Full report as JSON
)

@Dao
interface ScanReportDao {
    @Query("SELECT * FROM scan_reports ORDER BY scanTime DESC LIMIT 20")
    fun getRecentReports(): Flow<List<ScanReportEntity>>

    @Query("SELECT * FROM scan_reports ORDER BY scanTime DESC LIMIT 1")
    suspend fun getLatestReport(): ScanReportEntity?

    @Insert
    suspend fun insert(report: ScanReportEntity): Long

    @Query("DELETE FROM scan_reports WHERE scanTime < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
