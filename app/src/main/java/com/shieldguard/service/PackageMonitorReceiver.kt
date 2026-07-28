package com.shieldguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shieldguard.worker.SingleAppScanWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Monitors for newly installed/updated apps and triggers an instant scan.
 * This is HOW we catch malicious APKs right after install!
 */
@AndroidEntryPoint
class PackageMonitorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                Log.i("ShieldGuard", "New app installed: $packageName")
                triggerInstantScan(context, packageName, isNewInstall = true)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.i("ShieldGuard", "App updated: $packageName")
                triggerInstantScan(context, packageName, isNewInstall = false)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Schedule background scans after boot
                scheduleBackgroundScans(context)
            }
        }
    }

    private fun triggerInstantScan(context: Context, packageName: String, isNewInstall: Boolean) {
        // Skip scanning our own app
        if (packageName == "com.shieldguard.ai") return

        val work = OneTimeWorkRequestBuilder<SingleAppScanWorker>()
            .setInputData(workDataOf(
                "package_name" to packageName,
                "is_new_install" to isNewInstall
            ))
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    private fun scheduleBackgroundScans(context: Context) {
        // This schedules periodic scans — defined in WorkManager setup
        val intent = Intent("com.shieldguard.SCHEDULE_SCANS")
        context.sendBroadcast(intent)
    }
}
