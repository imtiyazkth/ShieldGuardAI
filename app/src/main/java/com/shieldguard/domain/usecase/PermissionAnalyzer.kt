package com.shieldguard.domain.usecase

import android.Manifest
import com.shieldguard.domain.model.DangerousPermission
import com.shieldguard.domain.model.RiskLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionAnalyzer @Inject constructor() {

    // Complete map of dangerous permissions with Hindi explanations
    private val dangerousPermissionMap = mapOf(
        // === CRITICAL ===
        Manifest.permission.READ_SMS to DangerousPermission(
            permission = Manifest.permission.READ_SMS,
            displayName = "SMS Padhna",
            description = "Aapke saare SMS messages padh sakta hai",
            riskLevel = RiskLevel.CRITICAL,
            whyDangerous = "OTP aur bank messages bhi padh sakta hai — identity theft hone ka darr",
            recommendation = "Sirf messaging apps ko ye permission dein"
        ),
        Manifest.permission.SEND_SMS to DangerousPermission(
            permission = Manifest.permission.SEND_SMS,
            displayName = "SMS Bhejna",
            description = "Aapki taraf se SMS bhej sakta hai",
            riskLevel = RiskLevel.CRITICAL,
            whyDangerous = "Premium SMS services pe charge karwa sakta hai — fraud ka risk",
            recommendation = "Ye permission normal apps ko nahi chahiye"
        ),
        Manifest.permission.READ_CALL_LOG to DangerousPermission(
            permission = Manifest.permission.READ_CALL_LOG,
            displayName = "Call History Padhna",
            description = "Aapki poori call history dekh sakta hai",
            riskLevel = RiskLevel.CRITICAL,
            whyDangerous = "Aapke contacts aur calling patterns ki jaasusi ho sakti hai",
            recommendation = "Sirf dialer apps ko chahiye"
        ),
        Manifest.permission.PROCESS_OUTGOING_CALLS to DangerousPermission(
            permission = Manifest.permission.PROCESS_OUTGOING_CALLS,
            displayName = "Calls Intercept Karna",
            description = "Aapki outgoing calls rok ya redirect kar sakta hai",
            riskLevel = RiskLevel.CRITICAL,
            whyDangerous = "Aapki calls hijack ho sakti hain",
            recommendation = "Ye permission kisi normal app ko nahi chahiye"
        ),

        // === HIGH ===
        Manifest.permission.ACCESS_FINE_LOCATION to DangerousPermission(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            displayName = "Exact Location",
            description = "GPS se aapki exact location jaanta hai",
            riskLevel = RiskLevel.HIGH,
            whyDangerous = "Aapki har movement track ho sakti hai",
            recommendation = "Sirf maps/navigation apps ko dein"
        ),
        Manifest.permission.RECORD_AUDIO to DangerousPermission(
            permission = Manifest.permission.RECORD_AUDIO,
            displayName = "Microphone",
            description = "Aapke phone ka mic use kar sakta hai",
            riskLevel = RiskLevel.HIGH,
            whyDangerous = "Background mein aapki baatein record ho sakti hain",
            recommendation = "Sirf calling/recording apps ko dein"
        ),
        Manifest.permission.CAMERA to DangerousPermission(
            permission = Manifest.permission.CAMERA,
            displayName = "Camera",
            description = "Aapke phone ka camera use kar sakta hai",
            riskLevel = RiskLevel.HIGH,
            whyDangerous = "Background mein photos/videos le sakta hai",
            recommendation = "Sirf camera apps ko dein, baaki ke liye carefully sochein"
        ),
        Manifest.permission.READ_CONTACTS to DangerousPermission(
            permission = Manifest.permission.READ_CONTACTS,
            displayName = "Contacts Padhna",
            description = "Aapki poori contact list dekh sakta hai",
            riskLevel = RiskLevel.HIGH,
            whyDangerous = "Aapke saare contacts spam ya phishing ke liye use ho sakte hain",
            recommendation = "Sirf contacts/social apps ko dein"
        ),
        Manifest.permission.SYSTEM_ALERT_WINDOW to DangerousPermission(
            permission = Manifest.permission.SYSTEM_ALERT_WINDOW,
            displayName = "Screen Pe Kuch Bhi Dikha Sakta Hai",
            description = "Doosre apps ke upar kuch bhi dikhane ka permission",
            riskLevel = RiskLevel.HIGH,
            whyDangerous = "Fake login pages ya overlays dikha ke password chura sakta hai",
            recommendation = "Sirf trusted apps ko dein jaise screen recorders"
        ),

        // === MEDIUM ===
        Manifest.permission.READ_EXTERNAL_STORAGE to DangerousPermission(
            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
            displayName = "Files Padhna",
            description = "Aapki files aur photos dekh sakta hai",
            riskLevel = RiskLevel.MEDIUM,
            whyDangerous = "Private photos aur documents access ho sakte hain",
            recommendation = "Sochke dein — kya is app ko files chahiye?"
        ),
        Manifest.permission.ACCESS_COARSE_LOCATION to DangerousPermission(
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
            displayName = "Approximate Location",
            description = "Approximate location (city level) jaanta hai",
            riskLevel = RiskLevel.MEDIUM,
            whyDangerous = "Aapki general location track ho sakti hai",
            recommendation = "Zyada apps ko ye chahiye nahi"
        ),
        Manifest.permission.WRITE_CONTACTS to DangerousPermission(
            permission = Manifest.permission.WRITE_CONTACTS,
            displayName = "Contacts Badalna",
            description = "Aapki contacts list mein changes kar sakta hai",
            riskLevel = RiskLevel.MEDIUM,
            whyDangerous = "Fake contacts add kar sakta hai ya existing delete kar sakta hai",
            recommendation = "Sirf contacts apps ko dein"
        ),
        Manifest.permission.READ_PHONE_STATE to DangerousPermission(
            permission = Manifest.permission.READ_PHONE_STATE,
            displayName = "Phone State Padhna",
            description = "IMEI aur phone number padh sakta hai",
            riskLevel = RiskLevel.MEDIUM,
            whyDangerous = "Aapka unique device ID track ho sakta hai",
            recommendation = "Bahut kam apps ko ye chahiye"
        ),
        Manifest.permission.GET_ACCOUNTS to DangerousPermission(
            permission = Manifest.permission.GET_ACCOUNTS,
            displayName = "Account List Dekhna",
            description = "Phone pe registered accounts dekh sakta hai (Gmail, etc.)",
            riskLevel = RiskLevel.MEDIUM,
            whyDangerous = "Aapke account names leak ho sakte hain",
            recommendation = "Carefully dein"
        ),
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE to DangerousPermission(
            permission = Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
            displayName = "Accessibility Service",
            description = "Poore phone ko control kar sakta hai",
            riskLevel = RiskLevel.CRITICAL,
            whyDangerous = "Screen kya dikh raha hai padh sakta hai, buttons click kar sakta hai, password chura sakta hai!",
            recommendation = "SIRF trusted apps ko dein — ye sabse dangerous permission hai"
        )
    )

    fun analyzeDangerousPermissions(permissions: List<String>): List<DangerousPermission> {
        return permissions.mapNotNull { permission ->
            dangerousPermissionMap[permission]
        }
    }

    fun getPermissionInfo(permission: String): DangerousPermission? {
        return dangerousPermissionMap[permission]
    }

    fun getAllDangerousPermissions(): Map<String, DangerousPermission> = dangerousPermissionMap
}
