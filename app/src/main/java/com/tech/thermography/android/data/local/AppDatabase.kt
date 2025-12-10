package com.tech.thermography.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tech.thermography.android.data.local.dao.BusinessUnitDao
import com.tech.thermography.android.data.local.dao.CompanyDao
import com.tech.thermography.android.data.local.dao.EquipmentComponentDao
import com.tech.thermography.android.data.local.dao.EquipmentComponentTemperatureLimitsDao
import com.tech.thermography.android.data.local.dao.EquipmentDao
import com.tech.thermography.android.data.local.dao.EquipmentGroupDao
import com.tech.thermography.android.data.local.dao.EquipmentTypeTranslationDao
import com.tech.thermography.android.data.local.dao.InspectionRecordDao
import com.tech.thermography.android.data.local.dao.InspectionRecordGroupDao
import com.tech.thermography.android.data.local.dao.InspectionRecordGroupEquipmentDao
import com.tech.thermography.android.data.local.dao.InspectionRouteDao
import com.tech.thermography.android.data.local.dao.InspectionRouteGroupDao
import com.tech.thermography.android.data.local.dao.InspectionRouteGroupEquipmentDao
import com.tech.thermography.android.data.local.dao.PlantDao
import com.tech.thermography.android.data.local.dao.ROIDao
import com.tech.thermography.android.data.local.dao.RiskPeriodicityDeadlineDao
import com.tech.thermography.android.data.local.dao.RiskRecommendationTranslationDao
import com.tech.thermography.android.data.local.dao.SyncEntityStateDao
import com.tech.thermography.android.data.local.dao.ThermogramDao
import com.tech.thermography.android.data.local.dao.ThermographicInspectionRecordDao
import com.tech.thermography.android.data.local.dao.UserInfoDao
import com.tech.thermography.android.data.local.entity.BusinessUnitEntity
import com.tech.thermography.android.data.local.entity.CompanyEntity
import com.tech.thermography.android.data.local.entity.EquipmentComponentEntity
import com.tech.thermography.android.data.local.entity.EquipmentComponentTemperatureLimitsEntity
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.EquipmentGroupEntity
import com.tech.thermography.android.data.local.entity.EquipmentTypeTranslationEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEquipmentEntity
import com.tech.thermography.android.data.local.entity.InspectionRouteEntity
import com.tech.thermography.android.data.local.entity.InspectionRouteGroupEntity
import com.tech.thermography.android.data.local.entity.InspectionRouteGroupEquipmentEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.local.entity.RiskPeriodicityDeadlineEntity
import com.tech.thermography.android.data.local.entity.RiskRecommendationTranslationEntity
import com.tech.thermography.android.data.local.entity.SyncEntityState
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import com.tech.thermography.android.data.local.entity.UserInfoEntity
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
