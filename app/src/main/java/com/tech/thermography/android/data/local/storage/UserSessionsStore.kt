package com.tech.thermography.android.data.local.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "session")

@Singleton
class UserSessionStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("jwt_token")
        val REMEMBER = booleanPreferencesKey("remember_me")
    }

    suspend fun saveToken(token: String, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.REMEMBER] = remember
        }
    }

    val token: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.TOKEN] }
    val remember: Flow<Boolean?> = context.dataStore.data.map { prefs -> prefs[Keys.REMEMBER] }
}