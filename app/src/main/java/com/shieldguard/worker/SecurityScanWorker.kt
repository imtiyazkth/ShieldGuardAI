package com.shieldguard.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.shieldguard.R
import com.shieldguard.data.repository.SecurityRepository
import com.shieldguard.domain.model.RiskLevel
import com.shieldguard.domain.usecase.ScanProgress
import com.shieldguard.domain.usecase.SecurityScanner
import com.shieldguard.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Runs in background every 6 hours.
 * Scans all apps and alerts if something dangerous is found.
 */
@HiltWorker
class SecurityScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val securityScanner: SecurityScanner,
    private val securityRepository: SecurityRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "ShieldGuard_PeriodicScan"
        const val NOTIFICATION_CHANNEL_ID = "shieldguard_alerts"
        const val NOTIFICATION_CHANNEL_NAME = "ShieldGuard Security Alerts"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SecurityScanWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline!
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannel()

            val scannedApps = mutableListOf<com.shieldguard.domain.model.ScannedApp>()

            // Collect scan results
            securityScanner.scanAllApps().collect { progress ->
                if (progress is ScanProgress.Completed) {
                    scannedApps.addAll(progress.results)
                }
            }

            // Save results
            securityRepository.saveScannedApps(scannedApps)

            // Find threats
            val criticalApps = scannedApps.filter { it.riskLevel == RiskLevel.CRITICAL }
            val highApps = scannedApps.filter { it.riskLevel == RiskLevel.HIGH }
            val newThreats = criticalApps + highApps

            // Alert user if threats found
            if (newThreats.isNotEmpty()) {
                sendThreatNotification(newThreats.size, newThreats.first().appName)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure()
        }
    }

    private fun sendThreatNotification(threatCount: Int, topThreatName: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "scan_results")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ ShieldGuard: $threatCount Threat${if (threatCount > 1) "s" else ""} Found!")
            .setContentText("$topThreatName aur aur ${threatCount - 1} apps suspicious hain")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Aapke phone mein $threatCount suspicious apps hain. $topThreatName sabse dangerous hai. Turant dekho!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ShieldGuard security threat alerts"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

/**
 * Scans a single newly installed app instantly
 */
@HiltWorker
class SingleAppScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val securityScanner: SecurityScanner,
    private val securityRepository: SecurityRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val packageName = inputData.getString("package_name") ?: return Result.failure()
        val isNewInstall = inputData.getBoolean("is_new_install", true)

        return try {
            // Force a device scan to get this new app
            val scannedApps = mutableListOf<com.shieldguard.domain.model.ScannedApp>()
            securityScanner.scanAllApps().collect { progress ->
                if (progress is ScanProgress.Completed) {
                    scannedApps.addAll(progress.results)
                }
            }

            val newApp = scannedApps.find { it.packageName == packageName }

            if (newApp != null && isNewInstall) {
                securityRepository.saveScannedApps(listOf(newApp))

                // Alert if dangerous
                if (newApp.riskLevel == RiskLevel.CRITICAL || newApp.riskLevel == RiskLevel.HIGH) {
                    sendInstallAlert(newApp.appName, newApp.riskLevel, newApp.riskReasons.firstOrNull() ?: "")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendInstallAlert(appName: String, riskLevel: RiskLevel, reason: String) {
        val channelId = "shieldguard_install_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Install Alerts", NotificationManager.IMPORTANCE_HIGH)
            val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 New ${riskLevel.displayName} App Installed!")
            .setContentText("$appName install hua — ye suspicious hai!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$appName naya install hua hai aur ye suspicious lagta hai. Reason: $reason"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(1002, notification)
    }
}
