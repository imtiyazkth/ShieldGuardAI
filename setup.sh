#!/bin/bash
# ================================================
# ShieldGuard AI — Termux Setup Script
# Run: bash setup.sh
# ================================================

set -e
echo "🛡️ ShieldGuard AI — Project setup ho raha hai..."

# ------------------------------------------------
# STEP 1: FOLDER STRUCTURE
# ------------------------------------------------
mkdir -p app/src/main/java/com/shieldguard/data/db
mkdir -p app/src/main/java/com/shieldguard/data/repository
mkdir -p app/src/main/java/com/shieldguard/domain/model
mkdir -p app/src/main/java/com/shieldguard/domain/usecase
mkdir -p app/src/main/java/com/shieldguard/presentation/dashboard
mkdir -p app/src/main/java/com/shieldguard/presentation/scanner
mkdir -p app/src/main/java/com/shieldguard/presentation/permissions
mkdir -p app/src/main/java/com/shieldguard/presentation/alerts
mkdir -p app/src/main/java/com/shieldguard/presentation/urlcheck
mkdir -p app/src/main/java/com/shieldguard/service
mkdir -p app/src/main/java/com/shieldguard/worker
mkdir -p app/src/main/java/com/shieldguard/di
mkdir -p app/src/main/java/com/shieldguard/util
mkdir -p app/src/main/java/com/shieldguard/ui/theme
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/xml
mkdir -p app/src/main/res/drawable
mkdir -p app/src/main/assets
mkdir -p .github/workflows
echo "✅ Folders created"

# ------------------------------------------------
# STEP 2: ROOT build.gradle
# ------------------------------------------------
cat > build.gradle << 'EOF'
buildscript {
    ext {
        kotlin_version = '1.9.22'
        hilt_version = '2.50'
        room_version = '2.6.1'
    }
}
plugins {
    id 'com.android.application' version '8.2.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.22' apply false
    id 'com.google.dagger.hilt.android' version '2.50' apply false
    id 'com.google.devtools.ksp' version '1.9.22-1.0.17' apply false
}
EOF
echo "✅ build.gradle (root)"

# ------------------------------------------------
# STEP 3: settings.gradle
# ------------------------------------------------
cat > settings.gradle << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ShieldGuardAI"
include ':app'
EOF
echo "✅ settings.gradle"

# ------------------------------------------------
# STEP 4: app/build.gradle
# ------------------------------------------------
cat > app/build.gradle << 'EOF'
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.dagger.hilt.android'
    id 'com.google.devtools.ksp'
}
android {
    namespace 'com.shieldguard'
    compileSdk 34
    defaultConfig {
        applicationId "com.shieldguard.ai"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField "String", "SAFE_BROWSING_KEY", '"YOUR_API_KEY_HERE"'
        buildConfigField "String", "MALWARE_DB_URL", '"https://raw.githubusercontent.com/imtiyazkth/ShieldGuardAI/main/app/src/main/assets/malware_db.json"'
    }
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = '17' }
    buildFeatures {
        compose true
        buildConfig true
    }
    composeOptions {
        kotlinCompilerExtensionVersion '1.5.8'
    }
    packaging {
        resources { excludes += '/META-INF/{AL2.0,LGPL2.1}' }
    }
}
dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.navigation:navigation-compose:2.7.6'
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
    implementation platform('androidx.compose:compose-bom:2024.01.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.material:material-icons-extended'
    implementation 'androidx.compose.animation:animation'
    debugImplementation 'androidx.compose.ui:ui-tooling'
    implementation "com.google.dagger:hilt-android:$hilt_version"
    ksp "com.google.dagger:hilt-compiler:$hilt_version"
    implementation 'androidx.hilt:hilt-navigation-compose:1.1.0'
    implementation 'androidx.hilt:hilt-work:1.1.0'
    ksp 'androidx.hilt:hilt-compiler:1.1.0'
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"
    ksp "androidx.room:room-compiler:$room_version"
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'com.google.code.gson:gson:2.10.1'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
}
EOF
echo "✅ app/build.gradle"

# ------------------------------------------------
# STEP 5: AndroidManifest.xml
# ------------------------------------------------
cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <application
        android:name=".ShieldGuardApp"
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.ShieldGuard"
        android:networkSecurityConfig="@xml/network_security_config"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        tools:targetApi="34">
        <activity
            android:name=".presentation.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.ShieldGuard">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="http" />
                <data android:scheme="https" />
            </intent-filter>
        </activity>
        <service
            android:name=".service.ShieldNotificationListenerService"
            android:exported="false"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
        <receiver android:name=".service.PackageMonitorReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.PACKAGE_ADDED" />
                <action android:name="android.intent.action.PACKAGE_REPLACED" />
                <action android:name="android.intent.action.PACKAGE_REMOVED" />
                <data android:scheme="package" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
    </application>
</manifest>
EOF
echo "✅ AndroidManifest.xml"

# ------------------------------------------------
# STEP 6: res files
# ------------------------------------------------
cat > app/src/main/res/values/strings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">ShieldGuard AI</string>
</resources>
EOF

cat > app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ShieldGuard" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
EOF

cat > app/src/main/res/xml/network_security_config.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
EOF
echo "✅ res/ files"

# ------------------------------------------------
# STEP 7: malware_db.json (offline database)
# ------------------------------------------------
cat > app/src/main/assets/malware_db.json << 'EOF'
{
  "version": 1,
  "updated": "2024-01-01",
  "malicious_packages": [
    {"packageName":"com.android.cleaner.boost","category":"adware","description":"Fake cleaner with intrusive ads","severity":"HIGH","source":"ShieldGuard"},
    {"packageName":"com.system.security.update","category":"spyware","description":"Fake system update stealing data","severity":"CRITICAL","source":"ShieldGuard"},
    {"packageName":"com.free.vpn.master","category":"spyware","description":"Fake VPN intercepting traffic","severity":"CRITICAL","source":"ShieldGuard"},
    {"packageName":"com.battery.saver.pro","category":"adware","description":"Fake battery saver with ads","severity":"HIGH","source":"ShieldGuard"},
    {"packageName":"com.phone.booster.speed","category":"adware","description":"Fake booster showing overlay ads","severity":"HIGH","source":"ShieldGuard"}
  ],
  "malicious_domains": [
    {"domain":"sbi-secure-login.xyz","category":"phishing","description":"Fake SBI login","severity":"CRITICAL","source":"ShieldGuard"},
    {"domain":"hdfc-verify.tk","category":"phishing","description":"Fake HDFC verification","severity":"CRITICAL","source":"ShieldGuard"},
    {"domain":"paytm-kyc-update.ml","category":"phishing","description":"Fake Paytm KYC","severity":"CRITICAL","source":"ShieldGuard"},
    {"domain":"amazon-prize-winner.xyz","category":"scam","description":"Fake Amazon prize","severity":"HIGH","source":"ShieldGuard"},
    {"domain":"free-recharge-jio.tk","category":"scam","description":"Fake Jio recharge","severity":"HIGH","source":"ShieldGuard"},
    {"domain":"pm-kisan-help.ml","category":"phishing","description":"Fake PM Kisan portal","severity":"CRITICAL","source":"ShieldGuard"},
    {"domain":"aadhaar-update-online.xyz","category":"phishing","description":"Fake Aadhaar update","severity":"CRITICAL","source":"ShieldGuard"},
    {"domain":"bitcoin-double.top","category":"scam","description":"Crypto doubling scam","severity":"CRITICAL","source":"ShieldGuard"}
  ]
}
EOF
echo "✅ malware_db.json"

# ------------------------------------------------
# STEP 8: proguard-rules.pro
# ------------------------------------------------
cat > app/proguard-rules.pro << 'EOF'
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.shieldguard.domain.model.** { *; }
-keep class com.shieldguard.data.db.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class * extends androidx.work.CoroutineWorker
-keep class com.shieldguard.service.ShieldNotificationListenerService { *; }
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
EOF
echo "✅ proguard-rules.pro"

# ------------------------------------------------
# STEP 9: GitHub Actions CI/CD
# ------------------------------------------------
cat > .github/workflows/build.yml << 'EOF'
name: ShieldGuard AI Build

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Setup Gradle Cache
        uses: gradle/actions/setup-gradle@v3
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: ShieldGuard-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
EOF
echo "✅ GitHub Actions workflow"

# ------------------------------------------------
# STEP 10: .gitignore
# ------------------------------------------------
cat > .gitignore << 'EOF'
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
app/build/
EOF
echo "✅ .gitignore"

# ------------------------------------------------
# STEP 11: gradle wrapper properties
# ------------------------------------------------
mkdir -p gradle/wrapper
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
echo "✅ gradle-wrapper.properties"

# ------------------------------------------------
# DONE
# ------------------------------------------------
echo ""
echo "🎉 ShieldGuard AI project structure ready!"
echo ""
echo "Ab ye commands chalaao:"
echo "  git add ."
echo "  git commit -m 'Initial commit: ShieldGuard AI'"
echo "  git push origin main"
