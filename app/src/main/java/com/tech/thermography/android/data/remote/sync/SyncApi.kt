package com.tech.thermography.android.data.remote.sync

import com.tech.thermography.android.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import com.tech.thermography.android.util.Constants

interface SyncApi {
    
    @GET("plants")
    suspend fun getAllPlants(): List<PlantDto>

    @GET("business-units")
    suspend fun getAllBusinessUnits(): List<BusinessUnitDto>

    @GET("companies")
    suspend fun getAllCompanies(): List<CompanyDto>

    @GET("equipment")
    suspend fun getAllEquipments(): List<EquipmentDto>

    @GET("equipment-groups")
    suspend fun getAllEquipmentGroups(): List<EquipmentGroupDto>

    @GET("equipment-components")
    suspend fun getAllEquipmentComponents(): List<EquipmentComponentDto>

    @GET("equipment-component-temperature-limits")
    suspend fun getAllEquipmentComponentTemperatureLimits(): List<EquipmentComponentTemperatureLimitsDto>

    @GET("equipment-type-translations")
    suspend fun getAllEquipmentTypeTranslations(): List<EquipmentTypeTranslationDto>

    @GET("inspection-records")
    suspend fun getAllInspectionRecords(): List<InspectionRecordDto>

    @GET("inspection-record-groups")
    suspend fun getAllInspectionRecordGroups(): List<InspectionRecordGroupDto>

    @GET("inspection-record-group-equipments")
    suspend fun getAllInspectionRecordGroupEquipments(): List<InspectionRecordGroupEquipmentDto>

    @GET("inspection-routes")
    suspend fun getAllInspectionRoutes(): List<InspectionRouteDto>

    @GET("inspection-route-groups")
    suspend fun getAllInspectionRouteGroups(): List<InspectionRouteGroupDto>

    @GET("inspection-route-group-equipments")
    suspend fun getAllInspectionRouteGroupEquipments(): List<InspectionRouteGroupEquipmentDto>

    @GET("risk-periodicity-deadlines")
    suspend fun getAllRiskPeriodicityDeadlines(): List<RiskPeriodicityDeadlineDto>

    @GET("risk-recommendation-translations")
    suspend fun getAllRiskRecommendationTranslations(): List<RiskRecommendationTranslationDto>

    @GET("rois")
    suspend fun getAllROIs(): List<ROIDto>

    @GET("thermograms")
    suspend fun getAllThermograms(): List<ThermogramDto>

    @GET
    @Streaming
    suspend fun downloadFile(@Url url: String): Response<ResponseBody>

    @Multipart
    @POST("${Constants.IMAGE_BASE_URL}/thermogram/upload")
    suspend fun uploadThermogram(@Part file: MultipartBody.Part): Response<UploadResponse>

    @Multipart
    @POST("${Constants.IMAGE_BASE_URL}/py-api/flir/uploadImage")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<UploadResponse>

    @GET("thermographic-records")
    suspend fun getAllThermographicRecords(): List<ThermographicRecordCreateDTO>

    @GET("user-infos")
    suspend fun getAllUserInfos(): List<UserInfoDto>

    @POST("thermographic-records/actions/create")
    suspend fun postThermographicRecord(@Body record: ThermographicRecordCreateDTO): Response<Unit>

    @POST("thermographic-records/actions/update")
    suspend fun updateThermographicRecord(@Body record: ThermographicRecordCreateDTO): Response<Unit>

    @DELETE("thermographic-records/{id}")
    suspend fun deleteThermographicRecord(@Path("id") id: String): Response<Unit>
}
