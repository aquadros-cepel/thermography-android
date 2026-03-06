package com.tech.thermography.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.storage.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionStore: UserSessionStore
) : ViewModel() {

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            sessionStore.clearSession()
            onComplete()
        }
    }
}
