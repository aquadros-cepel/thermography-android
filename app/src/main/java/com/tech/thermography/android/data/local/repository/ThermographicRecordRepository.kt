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
import com.tech.thermography.android.data.local.dao.ThermographicRecordWithRelations
import com.tech.thermography.android.data.local.entity.ThermographicRecord
import com.tech.thermography.android.data.remote.mapper.ThermographicRecordMapper
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
class ThermographicRecordRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<ThermographicRecord>() {
    private val thermographicRecordDao = db.thermographicRecordDao()
    private val thermogramDao = db.thermogramDao()
    private val roiDao = db.roiDao()

    fun getAllThermographicRecords(): Flow<List<ThermographicRecord>> = 
        thermographicRecordDao.getAllThermographicRecords()

    fun getThermographicRecordsByPlantId(plantId: UUID): Flow<List<ThermographicRecord>> =
        thermographicRecordDao.getThermographicRecordsByPlantId(plantId)

    fun getThermographicRecordsByPlantAndEquipment(plantId: UUID, equipmentId: UUID): Flow<List<ThermographicRecord>> =
        thermographicRecordDao.getThermographicRecordsByPlantAndEquipment(plantId, equipmentId)

    suspend fun getThermographicRecordById(id: UUID): ThermographicRecord? = 
        thermographicRecordDao.getThermographicRecordById(id)

    suspend fun getThermographicRecordFull(id: UUID): ThermographicRecordWithRelations {
        return thermographicRecordDao.getThermographicRecordWithRelations(id)
    }

    suspend fun insertThermographicRecord(context: Context, record: ThermographicRecord) {
        // Persiste a imagem localmente antes de salvar para garantir acesso posterior
        persistThermogramImageLocally(record.thermogramId)
        record.thermogramRefId?.let { persistThermogramImageLocally(it) }
        
        val newRecord = record.copy(syncStatus = com.tech.thermography.android.data.local.entity.enumeration.RecordSyncStatus.NEW)
        thermographicRecordDao.insertThermographicRecord(newRecord)
        triggerSync(context)
    }

    suspend fun updateThermographicRecord(context: Context, record: ThermographicRecord) {
        // Persiste a imagem localmente antes de salvar
        persistThermogramImageLocally(record.thermogramId)
        record.thermogramRefId?.let { persistThermogramImageLocally(it) }
        
        val updated = record.copy(syncStatus = com.tech.thermography.android.data.local.entity.enumeration.RecordSyncStatus.EDITED)
        thermographicRecordDao.updateThermographicRecord(updated)
        triggerSync(context)
    }

    suspend fun deleteThermographicRecord(context: Context, record: ThermographicRecord) {
        val updated = record.copy(syncStatus = com.tech.thermography.android.data.local.entity.enumeration.RecordSyncStatus.DELETED)
        thermographicRecordDao.updateThermographicRecord(updated)
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
        val remoteEntities: List<com.tech.thermography.android.data.remote.dto.ThermographicRecordCreateDTO> = syncApi.getAllThermographicRecords()
        val entities: List<ThermographicRecord> = remoteEntities.map { dto -> ThermographicRecordMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        thermographicRecordDao.insertThermographicRecords(cache)
    }

    suspend fun syncPendingRecords(): Boolean {
        val records = thermographicRecordDao.getRecordsToSync()
        Log.d("SyncDebug", "syncPendingRecords: Encontrados ${records.size} registros para sincronizar")
        
        var allSynced = true
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

                val fullRecord = getThermographicRecordFull(record.id)
                
                // Buscar ROIs
                val mainRois = fullRecord.thermogram?.let { t -> 
                    roiDao.getRoisByThermogramId(t.id).first().map { ROIMapper.entityToDto(it) }
                }
                val refRois = fullRecord.thermogramRef?.let { t ->
                    roiDao.getRoisByThermogramId(t.id).first().map { ROIMapper.entityToDto(it) }
                }

                val dto = ThermographicRecordMapper.entityToDto(
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
                        val response = syncApi.postThermographicRecord(dto)
                        if (response.isSuccessful) {
                            Log.d("SyncDebug", "Sucesso no POST do registro: ${record.id}")
                            thermographicRecordDao.updateStatus(record.id, "SYNCED")
                        } else {
                            Log.e("SyncDebug", "Erro no POST do registro: ${record.id}. Código: ${response.code()}")
                            allSynced = false
                        }
                    }
                    "EDITED" -> {
                        Log.d("SyncDebug", "Enviando POST (Update) para registro EDITED: ${record.id}")
                        val response = syncApi.updateThermographicRecord(dto)
                        if (response.isSuccessful) {
                            Log.d("SyncDebug", "Sucesso no Update do registro: ${record.id}")
                            thermographicRecordDao.updateStatus(record.id, "SYNCED")
                        } else {
                            Log.e("SyncDebug", "Erro no Update do registro: ${record.id}. Código: ${response.code()}")
                            allSynced = false
                        }
                    }
                    "DELETED" -> {
                        Log.d("SyncDebug", "Enviando DELETE para registro: ${record.id}")
                        val response = syncApi.deleteThermographicRecord(record.id.toString())
                        if (response.isSuccessful) {
                            Log.d("SyncDebug", "Sucesso no DELETE do registro: ${record.id}")
                            thermographicRecordDao.deleteById(record.id)
                        } else {
                            Log.e("SyncDebug", "Erro no DELETE do registro: ${record.id}. Código: ${response.code()}")
                            allSynced = false
                        }
                    }
                    else -> {
                        Log.d("SyncDebug", "Status desconhecido ou já sincronizado: ${record.syncStatus}")
                        allSynced = false
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncDebug", "Exceção ao processar registro: ${record.id}", e)
                allSynced = false
            }
        }
        return allSynced
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
