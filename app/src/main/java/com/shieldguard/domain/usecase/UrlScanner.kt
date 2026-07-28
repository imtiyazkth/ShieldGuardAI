package com.shieldguard.domain.usecase

import android.util.Patterns
import com.shieldguard.BuildConfig
import com.shieldguard.data.repository.MalwareDbRepository
import com.shieldguard.domain.model.RiskLevel
import com.shieldguard.domain.model.UrlRecommendation
import com.shieldguard.domain.model.UrlScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.IDN
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlScanner @Inject constructor(
    private val malwareDbRepository: MalwareDbRepository
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false) // We track redirects manually
        .build()

    // URL Shorteners list
    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly",
        "short.link", "rebrand.ly", "buff.ly", "is.gd", "shorturl.at",
        "cutt.ly", "tiny.cc", "rb.gy", "yourls.org", "s.id"
    )

    // Suspicious TLDs often used for phishing
    private val suspiciousTlds = setOf(
        ".xyz", ".tk", ".ml", ".ga", ".cf", ".gq", ".top",
        ".click", ".download", ".zip", ".mov", ".men"
    )

    // Known phishing keywords
    private val phishingKeywords = listOf(
        "secure-login", "verify-account", "update-payment", "account-suspended",
        "confirm-identity", "bank-alert", "sbi-alert", "hdfc-secure",
        "paytm-kyc", "google-prize", "amazon-winner", "free-recharge",
        "aadhaar-update", "pan-verify", "income-tax-refund", "epf-claim",
        "whatsapp-video", "lottery-winner", "covid-relief", "pm-kisan",
        "jio-free", "airtel-reward", "lic-policy", "credit-card-verify"
    )

    // =========================================
    // MAIN SCAN FUNCTION
    // =========================================
    suspend fun scanUrl(rawUrl: String): UrlScanResult = withContext(Dispatchers.IO) {
        val url = normalizeUrl(rawUrl)
        val domain = extractDomain(url)

        val redirectChain = mutableListOf<String>()
        var finalUrl = url
        var isHttps = url.startsWith("https://")
        var sslValid = false

        // --- Follow redirects manually ---
        try {
            var currentUrl = url
            var hops = 0
            while (hops < 5) {
                redirectChain.add(currentUrl)
                val request = Request.Builder().url(currentUrl).head().build()
                val response = httpClient.newCall(request).execute()
                val location = response.header("Location")
                if (location != null && response.code in 300..399) {
                    currentUrl = resolveUrl(currentUrl, location)
                    hops++
                } else {
                    finalUrl = currentUrl
                    isHttps = finalUrl.startsWith("https://")
                    sslValid = isHttps && response.code != -1
                    break
                }
            }
        } catch (e: javax.net.ssl.SSLException) {
            sslValid = false
        } catch (e: Exception) {
            // Network error — still analyze the URL itself
        }

        val finalDomain = extractDomain(finalUrl)
        val reasons = mutableListOf<String>()
        var riskScore = 0

        // --- Check 1: Known bad domain (offline DB) ---
        val isBadDomain = malwareDbRepository.checkDomain(domain) != null ||
                          malwareDbRepository.checkDomain(finalDomain) != null
        if (isBadDomain) {
            riskScore += 100
            reasons.add("⚠️ Ye domain malware database mein hai!")
        }

        // --- Check 2: SSL ---
        if (!isHttps) {
            riskScore += 15
            reasons.add("🟠 HTTP use ho raha hai — connection secure nahi hai")
        } else if (!sslValid) {
            riskScore += 25
            reasons.add("🔴 SSL Certificate invalid hai — fake website ho sakta hai")
        }

        // --- Check 3: URL shortener ---
        val isUrlShortener = urlShorteners.any { domain.contains(it) }
        if (isUrlShortener) {
            riskScore += 10
            reasons.add("🟡 Short URL hai — asli destination hide ho sakta hai")
        }

        // --- Check 4: Suspicious TLD ---
        val hasSuspiciousTld = suspiciousTlds.any { domain.endsWith(it) }
        if (hasSuspiciousTld) {
            riskScore += 20
            reasons.add("🟠 Suspicious domain extension (.xyz, .tk, .ml etc.) — scam sites yahi use karte hain")
        }

        // --- Check 5: Phishing keywords ---
        val urlLower = url.lowercase()
        val matchedKeywords = phishingKeywords.filter { urlLower.contains(it) }
        if (matchedKeywords.isNotEmpty()) {
            riskScore += 35
            reasons.add("🔴 URL mein phishing keywords hain: ${matchedKeywords.take(3).joinToString(", ")}")
        }

        // --- Check 6: Homograph/IDN attack ---
        val hasHomograph = detectHomographAttack(domain)
        if (hasHomograph) {
            riskScore += 40
            reasons.add("🔴 URL mein fake characters hain (Homograph attack) — ye naqli website hai!")
        }

        // --- Check 7: Multiple redirects ---
        if (redirectChain.size > 3) {
            riskScore += 15
            reasons.add("🟠 Bahut zyada redirects (${redirectChain.size}) — suspicious hai")
        }

        // --- Check 8: IP address as domain ---
        if (isIpAddress(domain)) {
            riskScore += 20
            reasons.add("🟠 IP address URL hai — real website naam use nahi kar raha")
        }

        // --- Check 9: Google Safe Browsing (online) ---
        val isSafeBrowsingClean = checkGoogleSafeBrowsing(url)
        if (!isSafeBrowsingClean) {
            riskScore += 50
            reasons.add("🔴 Google Safe Browsing ne flag kiya hai — DANGEROUS!")
        }

        // --- Determine final risk ---
        val isPhishing = matchedKeywords.isNotEmpty() || hasHomograph || !isSafeBrowsingClean
        val isMalware = isBadDomain && !isSafeBrowsingClean
        val isScam = riskScore in 30..59

        val riskLevel = when {
            riskScore >= 80 || isMalware -> RiskLevel.CRITICAL
            riskScore >= 60 || isPhishing -> RiskLevel.HIGH
            riskScore >= 30 -> RiskLevel.MEDIUM
            riskScore >= 10 -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        val recommendation = when (riskLevel) {
            RiskLevel.CRITICAL -> UrlRecommendation.BLOCKED_MALWARE
            RiskLevel.HIGH -> if (isPhishing) UrlRecommendation.BLOCKED_PHISHING
                             else UrlRecommendation.BLOCK_AND_WARN
            RiskLevel.MEDIUM -> UrlRecommendation.PROCEED_WITH_CAUTION
            else -> UrlRecommendation.SAFE_TO_VISIT
        }

        val aiExplanation = generateExplanation(url, riskLevel, reasons, isPhishing, isMalware)

        UrlScanResult(
            url = url,
            finalUrl = finalUrl,
            isHttps = isHttps,
            sslValid = sslValid,
            domainAge = null, // WHOIS lookup can be added
            isSafeBrowsingClean = isSafeBrowsingClean,
            isPhishing = isPhishing,
            isScam = isScam,
            isMalware = isMalware,
            isKnownBadDomain = isBadDomain,
            isUrlShortener = isUrlShortener,
            hasHomographAttack = hasHomograph,
            redirectChain = redirectChain,
            riskLevel = riskLevel,
            riskReasons = reasons,
            aiExplanation = aiExplanation,
            recommendation = recommendation
        )
    }

    // =========================================
    // GOOGLE SAFE BROWSING CHECK
    // =========================================
    private fun checkGoogleSafeBrowsing(url: String): Boolean {
        return try {
            val apiKey = BuildConfig.SAFE_BROWSING_KEY
            if (apiKey == "YOUR_API_KEY_HERE") return true // Skip if not configured

            val apiUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$apiKey"
            val requestBody = """
                {
                  "client": {"clientId": "shieldguard", "clientVersion": "1.0"},
                  "threatInfo": {
                    "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"],
                    "platformTypes": ["ANDROID"],
                    "threatEntryTypes": ["URL"],
                    "threatEntries": [{"url": "$url"}]
                  }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)
            !json.has("matches") // No matches = clean
        } catch (e: Exception) {
            true // If API fails, don't block
        }
    }

    // =========================================
    // HELPER FUNCTIONS
    // =========================================

    private fun normalizeUrl(url: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "https://$url"
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            URL(url).host.removePrefix("www.")
        } catch (e: Exception) { url }
    }

    private fun detectHomographAttack(domain: String): Boolean {
        return try {
            val ascii = IDN.toASCII(domain)
            ascii != domain && (ascii.contains("xn--"))
        } catch (e: Exception) { false }
    }

    private fun isIpAddress(domain: String): Boolean {
        return Patterns.IP_ADDRESS.matcher(domain).matches()
    }

    private fun resolveUrl(base: String, relative: String): String {
        return if (relative.startsWith("http")) relative
        else URL(URL(base), relative).toString()
    }

    private fun generateExplanation(
        url: String,
        riskLevel: RiskLevel,
        reasons: List<String>,
        isPhishing: Boolean,
        isMalware: Boolean
    ): String {
        return when (riskLevel) {
            RiskLevel.CRITICAL -> if (isMalware)
                "Ye link MALWARE distribute karta hai. Isko bilkul mat kholo — aapka phone hack ho sakta hai ya personal data chori ho sakta hai."
            else
                "Ye link PHISHING attack hai. Ye aapke passwords, bank details ya personal information churaane ki koshish kar raha hai."
            RiskLevel.HIGH -> "Ye link suspicious hai aur dangerous ho sakta hai. ${reasons.firstOrNull() ?: ""}. Recommend hai ki ye link mat kholo."
            RiskLevel.MEDIUM -> "Ye link thoda suspicious lagta hai. Proceed karo to sochke — agar kisi unknown ne bheja hai to mat kholo."
            RiskLevel.LOW -> "Ye link mostly safe lagta hai lekin ek choti risk hai. Dhyan rakho."
            RiskLevel.SAFE -> "Ye link safe lagta hai. Aap isko khol sakte ho."
        }
    }
}
