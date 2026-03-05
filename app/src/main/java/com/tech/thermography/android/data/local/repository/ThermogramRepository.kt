package com.tech.thermography.android.data.local.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.remote.mapper.ThermogramMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import com.tech.thermography.android.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermogramRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<ThermogramEntity>() {
    private val thermogramDao = db.thermogramDao()

    fun getAllThermograms(): Flow<List<ThermogramEntity>> = thermogramDao.getAllThermograms()

    suspend fun getThermogramById(id: UUID): ThermogramEntity? = thermogramDao.getThermogramById(id)

    suspend fun insertThermogram(thermogram: ThermogramEntity) = thermogramDao.insertThermogram(thermogram)

    suspend fun updateThermogram(thermogram: ThermogramEntity) = thermogramDao.updateThermogram(thermogram)

    suspend fun deleteThermogram(thermogram: ThermogramEntity) = thermogramDao.deleteThermogram(thermogram)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllThermograms()
        val entities = remoteEntities.map { dto -> 
            val entity = ThermogramMapper.dtoToEntity(dto)
            // Tenta baixar a imagem se houver path remoto
            val localPath = entity.imagePath?.let { remotePath ->
                val fullUrl = if (remotePath.startsWith("http")) {
                    remotePath
                } else {
                    "${Constants.IMAGE_BASE_URL}$remotePath"
                }
                downloadAndSaveImage(fullUrl, entity.id)
            }
            entity.copy(localImagePath = localPath ?: entity.localImagePath)
        }
        setCache(entities)
    }

    override suspend fun insertCached() {
        thermogramDao.insertThermograms(cache)
    }

    private suspend fun downloadAndSaveImage(fullUrl: String, thermogramId: UUID): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("ThermogramRepository", "Baixando imagem de: $fullUrl")
            val response = syncApi.downloadFile(fullUrl)
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext null
                
                val directory = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "thermalEnergy"
                )
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val fileName = "thermogram_${thermogramId}.jpg"
                val file = File(directory, fileName)

                file.sink().buffer().use { sink ->
                    sink.writeAll(body.source())
                }

                Log.d("ThermogramRepository", "Imagem salva em: ${file.absolutePath}")
                return@withContext file.absolutePath
            } else {
                Log.e("ThermogramRepository", "Erro ao baixar imagem: ${response.code()} em $fullUrl")
                null
            }
        } catch (e: Exception) {
            Log.e("ThermogramRepository", "Erro no download/salvamento da imagem para $fullUrl", e)
            null
        }
    }
}
