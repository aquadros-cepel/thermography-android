package com.tech.thermography.android.data.local.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import com.tech.thermography.android.work.ThermographicSyncWorker
import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.dao.ThermographicInspectionRecordWithRelations
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import com.tech.thermography.android.data.remote.mapper.ThermographicInspectionRecordMapper
import com.tech.thermography.android.data.remote.mapper.ROIMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermographicInspectionRecordRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<ThermographicInspectionRecordEntity>() {
    private val thermographicInspectionRecordDao = db.thermographicInspectionRecordDao()
    private val thermogramDao = db.thermogramDao()
    private val roiDao = db.roiDao()

    fun getAllThermographicInspectionRecords(): Flow<List<ThermographicInspectionRecordEntity>> = 
        thermographicInspectionRecordDao.getAllThermographicInspectionRecords()

    fun getThermographicInspectionRecordsByPlantId(plantId: UUID): Flow<List<ThermographicInspectionRecordEntity>> =
        thermographicInspectionRecordDao.getThermographicInspectionRecordsByPlantId(plantId)

    fun getThermographicInspectionRecordsByPlantAndEquipment(plantId: UUID, equipmentId: UUID): Flow<List<ThermographicInspectionRecordEntity>> =
        thermographicInspectionRecordDao.getThermographicInspectionRecordsByPlantAndEquipment(plantId, equipmentId)

    suspend fun getThermographicInspectionRecordById(id: UUID): ThermographicInspectionRecordEntity? = 
        thermographicInspectionRecordDao.getThermographicInspectionRecordById(id)

    suspend fun getThermographicInspectionRecordFull(id: UUID): ThermographicInspectionRecordWithRelations {
        return thermographicInspectionRecordDao.getThermographicInspectionRecordWithRelations(id)
    }

    suspend fun insertThermographicInspectionRecord(context: Context, record: ThermographicInspectionRecordEntity) {
        // Persiste a imagem localmente antes de salvar para garantir acesso posterior
        persistThermogramImageLocally(record.thermogramId)
        record.thermogramRefId?.let { persistThermogramImageLocally(it) }
        
        val newRecord = record.copy(syncStatus = com.tech.thermography.android.data.local.entity.enumeration.RecordSyncStatus.NEW)
        thermographicInspectionRecordDao.insertThermographicInspectionRecord(newRecord)
        triggerSync(context)
    }

    suspend fun updateThermographicInspectionRecord(context: Context, record: ThermographicInspectionRecordEntity) {
        // Persiste a imagem localmente antes de salvar
        persistThermogramImageLocally(record.thermogramId)
        record.thermogramRefId?.let { persistThermogramImageLocally(it) }
        
        val updated = record.copy(syncStatus = com.tech.thermography.android.data.local.entity.enumeration.RecordSyncStatus.EDITED)
        thermographicInspectionRecordDao.updateThermographicInspectionRecord(updated)
        triggerSync(context)
    }

    suspend fun deleteThermographicInspectionRecord(context: Context, record: ThermographicInspectionRecordEntity) {
        val updated = record.copy(syncStatus = com.tech.thermography.android.data.local.entity.enumeration.RecordSyncStatus.DELETED)
        thermographicInspectionRecordDao.updateThermographicInspectionRecord(updated)
        triggerSync(context)
    }

    private suspend fun persistThermogramImageLocally(thermogramId: UUID) {
        val thermogram = thermogramDao.getThermogramById(thermogramId) ?: return
        val localPath = thermogram.localImagePath
        
        // Se for uma URI de conteúdo (como do Photo Picker), copiamos para o armazenamento interno
        if (localPath.startsWith("content://")) {
            try {
                Log.d("SyncDebug", "Persistindo imagem do picker localmente: $localPath")
                val uri = Uri.parse(localPath)
                val directory = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "thermalEnergy"
                )
                if (!directory.exists()) directory.mkdirs()
                
                val fileName = "thermogram_${thermogram.id}.jpg"
                val destFile = File(directory, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Atualiza o localImagePath para o caminho absoluto do arquivo físico
                val updatedThermogram = thermogram.copy(localImagePath = destFile.absolutePath)
                thermogramDao.updateThermogram(updatedThermogram)
                Log.d("SyncDebug", "Imagem persistida com sucesso em: ${destFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("SyncDebug", "Erro ao persistir imagem localmente", e)
            }
        }
    }

    fun triggerSync(context: Context) {
        Log.d("SyncDebug", "triggerSync chamado")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<ThermographicSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "ThermographicSync",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    override suspend fun syncEntities() {
        val remoteEntities: List<com.tech.thermography.android.data.remote.dto.ThermographicInspectionRecordCreateDTO> = syncApi.getAllThermographicInspectionRecords()
        val entities: List<ThermographicInspectionRecordEntity> = remoteEntities.map { dto -> ThermographicInspectionRecordMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        thermographicInspectionRecordDao.insertThermographicInspectionRecords(cache)
    }

    suspend fun syncPendingRecords(): Boolean {
        val records = thermographicInspectionRecordDao.getRecordsToSync()
        Log.d("SyncDebug", "syncPendingRecords: Encontrados ${records.size} registros para sincronizar")
        
        var anySynced = false
        for (record in records) {
            try {
                Log.d("SyncDebug", "Processando registro: ${record.id} com status: ${record.syncStatus}")
                
                // Sincroniza imagem do termograma principal
                syncThermogramImage(record.thermogramId)
                // Sincroniza imagem real de referência do termograma principal
                syncRealImageRef(record.thermogramId)

                // Sincroniza imagem do termograma de referência (se existir)
                record.thermogramRefId?.let { 
                    syncThermogramImage(it)
                    syncRealImageRef(it)
                }

                val fullRecord = getThermographicInspectionRecordFull(record.id)
                
                // Buscar ROIs
                val mainRois = fullRecord.thermogram?.let { t -> 
                    roiDao.getRoisByThermogramId(t.id).first().map { ROIMapper.entityToDto(it) }
                }
                val refRois = fullRecord.thermogramRef?.let { t ->
                    roiDao.getRoisByThermogramId(t.id).first().map { ROIMapper.entityToDto(it) }
                }

                val dto = ThermographicInspectionRecordMapper.entityToDto(
                    fullRecord.record,
                    fullRecord.plant,
                    fullRecord.thermogram,
                    fullRecord.thermogramRef,
                    mainRois,
                    refRois
                )
                
                when (record.syncStatus.name) {
                    "NEW" -> {
                        Log.d("SyncDebug", "Enviando POST para registro NEW: ${record.id}")
                        val response = syncApi.postThermographicInspectionRecord(dto)
                        if (response.isSuccessful) {
                            Log.d("SyncDebug", "Sucesso no POST do registro: ${record.id}")
                            thermographicInspectionRecordDao.updateStatus(record.id, "SYNCED")
                            anySynced = true
                        } else {
                            Log.e("SyncDebug", "Erro no POST do registro: ${record.id}. Código: ${response.code()}")
                        }
                    }
                    "EDITED" -> {
                        Log.d("SyncDebug", "Enviando POST (Update) para registro EDITED: ${record.id}")
                        val response = syncApi.updateThermographicInspectionRecord(dto)
                        if (response.isSuccessful) {
                            Log.d("SyncDebug", "Sucesso no Update do registro: ${record.id}")
                            thermographicInspectionRecordDao.updateStatus(record.id, "SYNCED")
                            anySynced = true
                        } else {
                            Log.e("SyncDebug", "Erro no Update do registro: ${record.id}. Código: ${response.code()}")
                        }
                    }
                    "DELETED" -> {
                        Log.d("SyncDebug", "Enviando DELETE para registro: ${record.id}")
                        val response = syncApi.deleteThermographicInspectionRecord(record.id.toString())
                        if (response.isSuccessful) {
                            Log.d("SyncDebug", "Sucesso no DELETE do registro: ${record.id}")
                            thermographicInspectionRecordDao.deleteById(record.id)
                            anySynced = true
                        } else {
                            Log.e("SyncDebug", "Erro no DELETE do registro: ${record.id}. Código: ${response.code()}")
                        }
                    }
                    else -> {
                        Log.d("SyncDebug", "Status desconhecido ou já sincronizado: ${record.syncStatus}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncDebug", "Exceção ao processar registro: ${record.id}", e)
            }
        }
        return anySynced
    }

    private suspend fun syncThermogramImage(thermogramId: UUID) {
        val thermogram = thermogramDao.getThermogramById(thermogramId) ?: return
        if (thermogram.imagePath.isNullOrEmpty() || thermogram.imagePath.isBlank()) {
            Log.d("SyncDebug", "Iniciando upload de imagem térmica para o termograma: ${thermogram.id}")
            
            val localPath = thermogram.localImagePath
            val body = prepareMultipartFile(localPath, "thermogram_${thermogram.id}.jpg") ?: return
            
            val uploadResponse = syncApi.uploadThermogram(body)
            if (uploadResponse.isSuccessful && uploadResponse.body() != null) {
                val result = uploadResponse.body()!!
                Log.d("SyncDebug", "Upload térmico concluído. Novo imagePath: ${result.imagePath}")
                
                val updatedThermogram = thermogram.copy(
                    imagePath = result.imagePath, 
                    audioPath = result.audioPath
                )
                thermogramDao.updateThermogram(updatedThermogram)
            } else {
                Log.e("SyncDebug", "Falha no upload da imagem térmica. Código: ${uploadResponse.code()}")
            }
        }
    }

    private suspend fun syncRealImageRef(thermogramId: UUID) {
        val thermogram = thermogramDao.getThermogramById(thermogramId) ?: return
        
        // Verifica se existe caminho local para a imagem real e se o caminho remoto ainda não foi preenchido
        // Obs: Usamos imageRefPath como o campo remoto para a imagem real/visual
        if (!thermogram.localImageRefPath.isNullOrBlank() && thermogram.imageRefPath.isNullOrBlank()) {
            Log.d("SyncDebug", "Iniciando upload de imagem REAL para o termograma: ${thermogram.id}")
            
            val localPath = thermogram.localImageRefPath
            val body = prepareMultipartFile(localPath, "real_${thermogram.id}.jpg") ?: return
            
            val uploadResponse = syncApi.uploadImage(body)
            if (uploadResponse.isSuccessful && uploadResponse.body() != null) {
                val result = uploadResponse.body()!!
                Log.d("SyncDebug", "Upload de imagem REAL concluído. Novo imageRefPath: ${result.imagePath}")
                
                val updatedThermogram = thermogram.copy(
                    imageRefPath = result.imagePath
                )
                thermogramDao.updateThermogram(updatedThermogram)
            } else {
                Log.e("SyncDebug", "Falha no upload da imagem REAL. Código: ${uploadResponse.code()}")
            }
        }
    }

    private fun prepareMultipartFile(localPath: String, defaultName: String): MultipartBody.Part? {
        return try {
            if (localPath.startsWith("/")) {
                val file = File(localPath)
                if (!file.exists()) {
                    Log.e("SyncDebug", "Arquivo não encontrado: $localPath")
                    return null
                }
                val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
                MultipartBody.Part.createFormData("file", file.name, requestFile)
            } else {
                val imageUri = Uri.parse(localPath)
                val requestBody = object : RequestBody() {
                    override fun contentType() = "image/jpeg".toMediaTypeOrNull()
                    override fun writeTo(sink: BufferedSink) {
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            sink.writeAll(input.source())
                        } ?: throw java.io.IOException("Não foi possível abrir a URI: $imageUri")
                    }
                }
                MultipartBody.Part.createFormData("file", defaultName, requestBody)
            }
        } catch (e: Exception) {
            Log.e("SyncDebug", "Erro ao preparar arquivo multipart", e)
            null
        }
    }
}
