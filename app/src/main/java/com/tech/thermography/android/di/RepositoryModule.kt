package com.tech.thermography.android.di

import com.tech.thermography.android.data.local.repository.*
import com.tech.thermography.android.data.remote.auth.AuthRepository
import com.tech.thermography.android.data.remote.auth.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    // --- Agrupando todos os repositórios sincronizáveis ---

    @Binds @IntoSet
    abstract fun bindCompanyRepository(impl: CompanyRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindBusinessUnitRepository(impl: BusinessUnitRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindPlantRepository(impl: PlantRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindEquipmentRepository(impl: EquipmentRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindEquipmentGroupRepository(impl: EquipmentGroupRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindEquipmentTypeTranslationRepository(impl: EquipmentTypeTranslationRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindRiskPeriodicityDeadlineRepository(impl: RiskPeriodicityDeadlineRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindRiskRecommendationTranslationRepository(impl: RiskRecommendationTranslationRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindROIRepository(impl: ROIRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindThermogramRepository(impl: ThermogramRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindUserInfoRepository(impl: UserInfoRepository): SyncableRepository
    
    @Binds @IntoSet
    abstract fun bindInspectionRouteRepository(impl: InspectionRouteRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindInspectionRecordRepository(impl: InspectionRecordRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindEquipmentComponentRepository(impl: EquipmentComponentRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindInspectionRouteGroupRepository(impl: InspectionRouteGroupRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindInspectionRecordGroupRepository(impl: InspectionRecordGroupRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindInspectionRouteGroupEquipmentRepository(impl: InspectionRouteGroupEquipmentRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindInspectionRecordGroupEquipmentRepository(impl: InspectionRecordGroupEquipmentRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindThermographicInspectionRecordRepository(impl: ThermographicInspectionRecordRepository): SyncableRepository

    @Binds @IntoSet
    abstract fun bindEquipmentComponentTemperatureLimitsRepository(impl: EquipmentComponentTemperatureLimitsRepository): SyncableRepository

}
