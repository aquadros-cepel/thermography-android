package com.tech.thermography.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    fun clearLocalData(onFinished: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Limpa todas as tabelas do Room Database
                database.clearAllTables()
            }
            onFinished()
        }
    }
}
