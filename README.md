# 🛡️ ShieldGuard AI

**Advanced Android Security & Privacy Scanner**

> Aapke phone ko malware, phishing, spyware, aur scam links se bachata hai.
> Protects your Android device from malware, phishing, spyware, and scam links.

---

## ⚡ Features

### 🔍 Security Scanner
- Har installed app ko scan karta hai
- Risk score deta hai: Safe / Low / Medium / High / Critical
- Malware database se compare karta hai
- Unknown source installs detect karta hai
- Debug builds detect karta hai

### 🔗 URL / Link Scanner
- Koi bhi suspicious link paste karo — turant result
- Google Safe Browsing API se check
- Phishing keywords detect karta hai (SBI, HDFC, Paytm, Aadhaar, etc.)
- Homograph attacks detect karta hai (fake characters in URL)
- SSL certificate validity check
- Redirect chain track karta hai

### 🔔 Notification Monitor
- Background mein saari notifications monitor karta hai
- Scam keywords detect karta hai (Hindi + English)
- Suspicious links automatically scan karta hai
- Alert deta hai real-time mein

### 📱 Device Security Check
- USB Debugging status
- Unknown Sources status
- Developer Options status
- Root detection
- Accessibility apps list
- Device Admin apps list
- Overlay permission apps

### ⚠️ Permission Analyzer
- Dangerous permissions ka risk level batata hai
- Hindi mein explain karta hai kyu dangerous hai
- Settings tak directly le jaata hai fix karne ke liye

---

## 🏗️ Architecture

```
ShieldGuardAI/
├── presentation/          # Jetpack Compose UI
│   ├── dashboard/         # Main dashboard + ViewModel
│   ├── scanner/           # App scanner screen
│   ├── permissions/       # Permissions & device security
│   ├── alerts/            # Notification alerts
│   └── urlcheck/          # URL checker screen
├── domain/
│   ├── model/             # Data models (ScannedApp, UrlScanResult, etc.)
│   └── usecase/           # Business logic (SecurityScanner, UrlScanner, etc.)
├── data/
│   ├── db/                # Room database (entities + DAOs)
│   └── repository/        # Data access layer
├── service/               # Background services
│   ├── ShieldNotificationListenerService.kt
│   └── PackageMonitorReceiver.kt
├── worker/                # WorkManager background scans
│   ├── SecurityScanWorker.kt
│   └── SingleAppScanWorker.kt
├── di/                    # Hilt dependency injection
└── util/                  # Utilities (SignatureUtils, etc.)
```

**Pattern:** MVVM + Clean Architecture + Repository Pattern

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room (encrypted) |
| Background | WorkManager |
| Networking | OkHttp + Retrofit |
| Security | AndroidX Security Crypto |
| CI/CD | GitHub Actions |

---

## 🚀 Setup & Build

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Min Android 8.0 (API 26)

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/YOUR_USERNAME/ShieldGuardAI.git
cd ShieldGuardAI

# 2. Add your Google Safe Browsing API key in app/build.gradle
# buildConfigField "String", "SAFE_BROWSING_KEY", '"YOUR_KEY_HERE"'
# Get key from: https://developers.google.com/safe-browsing/v4/get-started

# 3. Build debug APK
./gradlew assembleDebug

# 4. Install on device
./gradlew installDebug
```

### Get Google Safe Browsing API Key (Free)
1. Go to https://console.cloud.google.com
2. Enable "Safe Browsing API"
3. Create API Key
4. Paste in `app/build.gradle`

---

## 📲 First-Time Setup (App mein)

Pehli baar app kholo to ye permissions deni hogi:

1. **Notification Access** — Suspicious notifications detect karne ke liye
   - Settings → Notification Access → ShieldGuard ON

2. **Post Notifications** — Security alerts dikhane ke liye
   - App khud maangega

Ye permissions optional hain — app bina bhi kaam karta hai URL check aur app scan ke liye.

---

## 🗄️ Malware Database Update karna

`app/src/main/assets/malware_db.json` mein add karo:

```json
{
  "malicious_packages": [
    {
      "packageName": "com.fake.app",
      "category": "spyware",
      "description": "Fake app stealing data",
      "severity": "CRITICAL",
      "source": "Your Name"
    }
  ],
  "malicious_domains": [
    {
      "domain": "scam-website.xyz",
      "category": "phishing",
      "description": "Fake bank login",
      "severity": "CRITICAL",
      "source": "Your Name"
    }
  ]
}
```

GitHub pe push karo — app automatically update kar lega (24 hours mein).

---

## 🔒 Privacy Policy

ShieldGuard AI:
- ✅ Koi data collect NAHI karta
- ✅ Koi analytics NAHI bhejta
- ✅ Koi ads NAHI dikhata
- ✅ Sab data phone pe locally store hota hai (encrypted)
- ✅ Internet sirf URL check aur DB update ke liye use hota hai

---

## 📜 License

MIT License — Free to use, modify, distribute.

---

## 🤝 Contribute

Issues aur Pull Requests welcome hain!

Khas taur pe ye cheezein add kar sakte ho:
- Aur malware entries malware_db.json mein
- Nayi scam domain entries
- Hindi aur regional language support
- New phishing keyword patterns

---

**Built with ❤️ for Indian users — Apne phone ko safe rakho! 🛡️**
