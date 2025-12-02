package com.tech.thermography.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tech.thermography.android.ui.sync.SyncStatus

@Entity(tableName = "sync_entity_state")
data class SyncEntityState(
    @PrimaryKey val entityName: String,
    val lastFullSync: Long?,             // timestamp do último sync bem-sucedido (PULL completo)
    val lastServerVersion: Long?,        // hash/updatedAt/version do servidor
    val status: SyncStatus,              // PENDING, IN_PROGRESS, COMPLETED, FAILED
    val lastErrorMessage: String? = null // útil para logs/debug
)
