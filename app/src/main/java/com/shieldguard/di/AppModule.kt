package com.shieldguard.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.shieldguard.data.db.*
import com.shieldguard.data.repository.MalwareDbRepository
import com.shieldguard.data.repository.SecurityRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // =========================================
    // ROOM DATABASE (Encrypted)
    // =========================================
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShieldGuardDatabase {
        return Room.databaseBuilder(
            context,
            ShieldGuardDatabase::class.java,
            "shieldguard_db"
        )
        .fallbackToDestructiveMigration()
        .build()
        // Note: For production, use SQLCipher for full encryption:
        // .openHelperFactory(SupportFactory(passphrase))
    }

    // =========================================
    // DAOs
    // =========================================
    @Provides
    fun provideScannedAppDao(db: ShieldGuardDatabase): ScannedAppDao = db.scannedAppDao()

    @Provides
    fun provideNotificationAlertDao(db: ShieldGuardDatabase): NotificationAlertDao = db.notificationAlertDao()

    @Provides
    fun provideMalwareEntryDao(db: ShieldGuardDatabase): MalwareEntryDao = db.malwareEntryDao()

    @Provides
    fun provideUrlScanDao(db: ShieldGuardDatabase): UrlScanDao = db.urlScanDao()

    @Provides
    fun provideScanReportDao(db: ShieldGuardDatabase): ScanReportDao = db.scanReportDao()

    // =========================================
    // HTTP CLIENT
    // =========================================
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // =========================================
    // ENCRYPTED SHARED PREFERENCES
    // =========================================
    @Provides
    @Singleton
    fun provideEncryptedPrefs(@ApplicationContext context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "shieldguard_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
