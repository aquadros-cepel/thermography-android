package com.tech.thermography.android.data.local.repository

/**
 * Define um contrato para repositórios que podem ter suas entidades sincronizadas.
 */
interface SyncableRepository {
    /**
     * Executa a lógica de sincronização para as entidades do repositório.
     */
    suspend fun syncEntities()
    suspend fun insertCached()
}
