package com.tech.thermography.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tech.thermography.android.data.local.dao.*
import com.tech.thermography.android.data.local.entity.*
import com.tech.thermography.android.data.local.util.Converters

@Database(
    entities = [
        CompanyEntity::class,
        BusinessUnitEntity::class,
        PlantEntity::class,
        UserInfoEntity::class,
        EquipmentEntity::class,
        EquipmentGroupEntity::class,
        EquipmentComponentEntity::class,
        EquipmentComponentTemperatureLimitsEntity::class,
        EquipmentTypeTranslationEntity::class,
        InspectionRouteEntity::class,
        InspectionRouteGroupEntity::class,
        InspectionRouteGroupEquipmentEntity::class,
        InspectionRecordEntity::class,
        InspectionRecordGroupEntity::class,
        InspectionRecordGroupEquipmentEntity::class,
        ThermogramEntity::class,
        ROIEntity::class,
        ThermographicInspectionRecordEntity::class,
        RiskPeriodicityDeadlineEntity::class,
        RiskRecommendationTranslationEntity::class,
        SyncEntityState::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun businessUnitDao(): BusinessUnitDao
    abstract fun plantDao(): PlantDao
    abstract fun userInfoDao(): UserInfoDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun equipmentGroupDao(): EquipmentGroupDao
    abstract fun equipmentComponentDao(): EquipmentComponentDao
    abstract fun equipmentComponentTemperatureLimitsDao(): EquipmentComponentTemperatureLimitsDao
    abstract fun equipmentTypeTranslationDao(): EquipmentTypeTranslationDao
    abstract fun inspectionRouteDao(): InspectionRouteDao
    abstract fun inspectionRouteGroupDao(): InspectionRouteGroupDao
    abstract fun inspectionRouteGroupEquipmentDao(): InspectionRouteGroupEquipmentDao
    abstract fun inspectionRecordDao(): InspectionRecordDao
    abstract fun inspectionRecordGroupDao(): InspectionRecordGroupDao
    abstract fun inspectionRecordGroupEquipmentDao(): InspectionRecordGroupEquipmentDao
    abstract fun thermogramDao(): ThermogramDao
    abstract fun roiDao(): ROIDao
    abstract fun thermographicInspectionRecordDao(): ThermographicInspectionRecordDao
    abstract fun riskPeriodicityDeadlineDao(): RiskPeriodicityDeadlineDao
    abstract fun riskRecommendationTranslationDao(): RiskRecommendationTranslationDao
    abstract fun syncEntityStateDao(): SyncEntityStateDao
}
