package com.himu.cyclecare.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.himu.cyclecare.domain.CycleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cycleSettingsDataStore by preferencesDataStore("cycle_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val cycleLength = intPreferencesKey("cycle_length")
        val periodLength = intPreferencesKey("period_length")
        val reminderHour = intPreferencesKey("reminder_hour")
        val periodReminders = booleanPreferencesKey("period_reminders")
        val ovulationReminders = booleanPreferencesKey("ovulation_reminders")
        val premenstrualReminders = booleanPreferencesKey("premenstrual_reminders")
        val privateNotifications = booleanPreferencesKey("private_notifications")
    }

    val settings: Flow<CycleSettings> = context.cycleSettingsDataStore.data.map { values ->
        CycleSettings(
            cycleLength = values[Keys.cycleLength] ?: 28,
            periodLength = values[Keys.periodLength] ?: 5,
            reminderHour = values[Keys.reminderHour] ?: 9,
            periodReminders = values[Keys.periodReminders] ?: true,
            ovulationReminders = values[Keys.ovulationReminders] ?: false,
            premenstrualReminders = values[Keys.premenstrualReminders] ?: false,
            privateNotifications = values[Keys.privateNotifications] ?: true,
        )
    }

    suspend fun update(value: CycleSettings) {
        context.cycleSettingsDataStore.edit { values ->
            values[Keys.cycleLength] = value.cycleLength.coerceIn(21, 45)
            values[Keys.periodLength] = value.periodLength.coerceIn(1, 10)
            values[Keys.reminderHour] = value.reminderHour.coerceIn(0, 23)
            values[Keys.periodReminders] = value.periodReminders
            values[Keys.ovulationReminders] = value.ovulationReminders
            values[Keys.premenstrualReminders] = value.premenstrualReminders
            values[Keys.privateNotifications] = value.privateNotifications
        }
    }
}
