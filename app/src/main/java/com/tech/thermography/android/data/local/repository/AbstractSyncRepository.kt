package com.tech.thermography.android.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
/**
 * Repositório abstrato que implementa a interface SyncableRepository e fornece
 * funcionalidades básicas de cache para repositórios específicos.
 */
abstract class AbstractSyncRepository<T> : SyncableRepository {

    protected val cache = mutableListOf<T>()

    fun setCache(data: List<T>) {
        cache.clear()
        cache.addAll(data)
    }

    fun isCacheEmpty() = cache.isEmpty()

    fun clearCache() = cache.clear()
}
