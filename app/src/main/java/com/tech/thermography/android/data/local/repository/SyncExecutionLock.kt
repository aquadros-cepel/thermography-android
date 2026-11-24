package com.tech.thermography.android.data.local.repository

import kotlinx.coroutines.sync.Mutex

internal class SyncExecutionLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T {
        mutex.lock()
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}