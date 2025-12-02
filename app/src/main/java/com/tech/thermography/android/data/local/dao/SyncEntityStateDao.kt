package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.SyncEntityState
import com.tech.thermography.android.ui.sync.SyncStatus

@Dao
interface SyncEntityStateDao {

    @Query("SELECT * FROM sync_entity_state WHERE entityName = :entityName")
    suspend fun getState(entityName: String): SyncEntityState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncEntityState)

    @Query("UPDATE sync_entity_state SET status = :status, lastErrorMessage = :error WHERE entityName = :entityName")
    suspend fun updateStatus(entityName: String, status: SyncStatus, error: String?)
}
