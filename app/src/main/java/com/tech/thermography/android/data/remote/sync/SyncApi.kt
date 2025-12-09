package com.tech.thermography.android.data.remote.sync

import com.tech.thermography.android.data.remote.dto.BusinessUnitDto
import com.tech.thermography.android.data.remote.dto.CompanyDto
import com.tech.thermography.android.data.remote.dto.EquipmentComponentDto
import com.tech.thermography.android.data.remote.dto.EquipmentComponentTemperatureLimitsDto
import com.tech.thermography.android.data.remote.dto.EquipmentDto
import com.tech.thermography.android.data.remote.dto.EquipmentGroupDto
import com.tech.thermography.android.data.remote.dto.EquipmentTypeTranslationDto
import com.tech.thermography.android.data.remote.dto.InspectionRecordDto
import com.tech.thermography.android.data.remote.dto.InspectionRecordGroupDto
import com.tech.thermography.android.data.remote.dto.InspectionRecordGroupEquipmentDto
import com.tech.thermography.android.data.remote.dto.InspectionRouteDto
import com.tech.thermography.android.data.remote.dto.InspectionRouteGroupDto
import com.tech.thermography.android.data.remote.dto.InspectionRouteGroupEquipmentDto
import com.tech.thermography.android.data.remote.dto.PlantDto
import com.tech.thermography.android.data.remote.dto.ROIDto
import com.tech.thermography.android.data.remote.dto.RiskPeriodicityDeadlineDto
import com.tech.thermography.android.data.remote.dto.RiskRecommendationTranslationDto
import com.tech.thermography.android.data.remote.dto.ThermogramDto
import com.tech.thermography.android.data.remote.dto.ThermographicInspectionRecordDto
import com.tech.thermography.android.data.remote.dto.UserInfoDto
import retrofit2.http.GET

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

    @GET("thermographic-inspection-records")
    suspend fun getAllThermographicInspectionRecords(): List<ThermographicInspectionRecordDto>

    @GET("user-infos")
    suspend fun getAllUserInfos(): List<UserInfoDto>
}
