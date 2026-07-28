package com.shieldguard.util

import android.content.pm.PackageInfo
import android.os.Build
import java.security.MessageDigest

object SignatureUtils {
    /**
     * Get SHA-256 hash of app's signing certificate.
     * This is used to detect tampered/fake apps.
     */
    fun getSha256(packageInfo: PackageInfo): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val signature = signatures?.firstOrNull() ?: return "unknown"
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(signature.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Format SHA256 for display (with colons like certificate fingerprints)
     */
    fun formatSha256(hash: String): String {
        return hash.chunked(2).joinToString(":")
    }
}
