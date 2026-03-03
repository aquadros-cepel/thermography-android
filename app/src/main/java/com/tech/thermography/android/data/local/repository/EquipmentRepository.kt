package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.remote.mapper.EquipmentMapper.dtoToEntity
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<EquipmentEntity>() {
    private val equipmentDao = db.equipmentDao()

    fun getAllEquipments(): Flow<List<EquipmentEntity>> = equipmentDao.getAllEquipments()

    fun getEquipmentsByPlantId(plantId: UUID): Flow<List<EquipmentEntity>> = equipmentDao.getEquipmentsByPlantId(plantId)

    suspend fun getEquipmentById(id: UUID): EquipmentEntity? = equipmentDao.getEquipmentById(id)

    suspend fun insertEquipment(equipment: EquipmentEntity) = equipmentDao.insertEquipment(equipment)

    suspend fun deleteEquipment(equipment: EquipmentEntity) = equipmentDao.deleteEquipment(equipment)

    override suspend fun syncEntities() {
        try {
            // Busca os dados da API
            val remoteEquipments = syncApi.getAllEquipments()            // Mapeia os DTOs para Entidades, ignorando os que derem erro no mapeamento
            val entities = remoteEquipments.mapNotNull { dto ->
                try {
                    dtoToEntity(dto)
                } catch (e: Exception) {
                    android.util.Log.e("EquipmentRepository", "Erro ao mapear equipamento DTO: $dto", e)
                    null // Retorna null para o mapNotNull filtrar este item
                }
            }

            // Atualiza o cache com a lista de entidades válidas
            setCache(entities)

        } catch (e: Exception) {
            // Captura erros de rede ou outros problemas na sincronização geral
            android.util.Log.e("EquipmentRepository", "Falha geral na sincronização de equipamentos", e)
        }
    }

    override suspend fun insertCached() {
//        for (equipment in cache) {
//            try {
//                equipmentDao.insertEquipment(equipment)
//            } catch (e: Exception) {
//                android.util.Log.e("EquipmentRepository", "Erro ao inserir equipamento no banco: $equipment", e)
//            }
//        }
        equipmentDao.insertEquipments(cache)
    }
}
