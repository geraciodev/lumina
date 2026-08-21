package com.geraciodev.lumina.data

import com.geraciodev.lumina.data.model.AppSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SettingsRepository {
    private val appDataDir = File(System.getProperty("user.home"), ".lumina")
    private val settingsFile = File(appDataDir, "settings.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
    }

    fun loadSettings(): AppSettings {
        return if (settingsFile.exists()) {
            try {
                json.decodeFromString<AppSettings>(settingsFile.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        try {
            settingsFile.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
