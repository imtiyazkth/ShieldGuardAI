package com.shieldguard.service

import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.shieldguard.data.db.NotificationAlertDao
import com.shieldguard.data.db.NotificationAlertEntity
import com.shieldguard.domain.model.RiskLevel
import com.shieldguard.domain.usecase.UrlScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class ShieldNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var urlScanner: UrlScanner
    @Inject lateinit var notificationAlertDao: NotificationAlertDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // URL pattern to extract from notification text
    private val urlPattern = Pattern.compile(
        "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
    )

    // Scam keywords in Hindi + English
    private val scamKeywords = listOf(
        // English
        "won", "winner", "lottery", "prize", "free recharge", "congratulations",
        "claim now", "click here", "verify now", "account suspended",
        "your account", "urgent", "limited time", "expire", "blocked",
        "free gift", "lucky draw", "reward", "cashback",
        // Hindi
        "jeeta", "inam", "muft", "turant", "aapka account",
        "verify karein", "click karein", "abhi lo", "reward milega",
        // Crypto scams
        "bitcoin", "crypto", "invest now", "double your money", "guaranteed return",
        // Fake banking
        "sbi", "hdfc", "icici", "paytm kyc", "phonepe", "gpay alert",
        "upi blocked", "bank account", "debit card",
        // Fake government
        "pm kisan", "aadhaar", "pan card", "income tax", "epf",
        "ration card", "bijli bill", "jal board"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        // Skip our own notifications
        if (packageName == "com.shieldguard.ai") return

        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val fullText = "$title $text $bigText"

        serviceScope.launch {
            analyzeNotification(packageName, title, fullText, sbn)
        }
    }

    private suspend fun analyzeNotification(
        packageName: String,
        title: String,
        fullText: String,
        sbn: StatusBarNotification
    ) {
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) { packageName }

        // --- Extract URLs ---
        val urls = mutableListOf<String>()
        val matcher = urlPattern.matcher(fullText)
        while (matcher.find()) {
            urls.add(matcher.group())
        }

        // --- Check for scam keywords ---
        val textLower = fullText.lowercase()
        val matchedScamWords = scamKeywords.filter { textLower.contains(it.lowercase()) }
        val hasScamKeywords = matchedScamWords.isNotEmpty()

        // --- Scan URLs ---
        var worstUrlRisk = RiskLevel.SAFE
        var detectedUrl: String? = null
        var urlRiskReason = ""

        for (url in urls.take(3)) { // Scan first 3 URLs
            try {
                val result = urlScanner.scanUrl(url)
                if (result.riskLevel.score > worstUrlRisk.score) {
                    worstUrlRisk = result.riskLevel
                    detectedUrl = url
                    urlRiskReason = result.aiExplanation
                }
            } catch (e: Exception) {
                Log.e("ShieldGuard", "URL scan failed: ${e.message}")
            }
        }

        // --- Calculate final risk ---
        val finalRisk = when {
            worstUrlRisk == RiskLevel.CRITICAL -> RiskLevel.CRITICAL
            worstUrlRisk == RiskLevel.HIGH -> RiskLevel.HIGH
            hasScamKeywords && worstUrlRisk != RiskLevel.SAFE -> RiskLevel.HIGH
            hasScamKeywords -> RiskLevel.MEDIUM
            worstUrlRisk == RiskLevel.MEDIUM -> RiskLevel.MEDIUM
            urls.isNotEmpty() && worstUrlRisk == RiskLevel.LOW -> RiskLevel.LOW
            else -> return // Safe notification — ignore
        }

        // --- Build risk reason ---
        val riskReason = buildString {
            if (hasScamKeywords) {
                append("Scam keywords milei: ${matchedScamWords.take(3).joinToString(", ")}. ")
            }
            if (detectedUrl != null) {
                append("Suspicious link mila: $detectedUrl. $urlRiskReason")
            }
        }

        // --- Save to DB ---
        notificationAlertDao.insert(
            NotificationAlertEntity(
                packageName = packageName,
                appName = appName,
                title = title,
                content = fullText.take(500),
                containedUrl = detectedUrl,
                riskLevel = finalRisk.name,
                riskReason = riskReason,
                timestamp = System.currentTimeMillis(),
                wasBlocked = false // We alert, don't auto-block
            )
        )

        // --- Show alert notification ---
        if (finalRisk.score >= RiskLevel.MEDIUM.score) {
            sendAlertNotification(appName, finalRisk, riskReason)
        }

        Log.w("ShieldGuard", "⚠️ Suspicious notification from $appName: $riskReason")
    }

    private fun sendAlertNotification(appName: String, risk: RiskLevel, reason: String) {
        val intent = Intent("com.shieldguard.NOTIFICATION_ALERT").apply {
            putExtra("app_name", appName)
            putExtra("risk_level", risk.name)
            putExtra("reason", reason)
        }
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}
