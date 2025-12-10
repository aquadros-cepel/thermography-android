package com.tech.thermography.android.di

import com.tech.thermography.android.data.local.repository.*
import com.tech.thermography.android.data.remote.auth.AuthRepository
import com.tech.thermography.android.data.remote.auth.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    // --- Agrupando todos os repositórios sincronizáveis ---

    @Binds @IntoMap @IntKey(1)
    abstract fun bindCompanyRepository(impl: CompanyRepository): SyncableRepository

    @Binds @IntoMap @IntKey(2)
    abstract fun bindBusinessUnitRepository(impl: BusinessUnitRepository): SyncableRepository

    @Binds @IntoMap @IntKey(3)
    abstract fun bindPlantRepository(impl: PlantRepository): SyncableRepository

    @Binds @IntoMap @IntKey(4)
    abstract fun bindEquipmentRepository(impl: EquipmentRepository): SyncableRepository

    @Binds @IntoMap @IntKey(5)
    abstract fun bindEquipmentGroupRepository(impl: EquipmentGroupRepository): SyncableRepository

    @Binds @IntoMap @IntKey(6)
    abstract fun bindEquipmentTypeTranslationRepository(impl: EquipmentTypeTranslationRepository): SyncableRepository

    @Binds @IntoMap @IntKey(7)
    abstract fun bindRiskPeriodicityDeadlineRepository(impl: RiskPeriodicityDeadlineRepository): SyncableRepository

    @Binds @IntoMap @IntKey(8)
    abstract fun bindRiskRecommendationTranslationRepository(impl: RiskRecommendationTranslationRepository): SyncableRepository

    @Binds @IntoMap @IntKey(9)
    abstract fun bindROIRepository(impl: ROIRepository): SyncableRepository

    @Binds @IntoMap @IntKey(10)
    abstract fun bindThermogramRepository(impl: ThermogramRepository): SyncableRepository

    @Binds @IntoMap @IntKey(11)
    abstract fun bindUserInfoRepository(impl: UserInfoRepository): SyncableRepository
    
    @Binds @IntoMap @IntKey(12)
    abstract fun bindInspectionRouteRepository(impl: InspectionRouteRepository): SyncableRepository

    @Binds @IntoMap @IntKey(13)
    abstract fun bindInspectionRecordRepository(impl: InspectionRecordRepository): SyncableRepository

    @Binds @IntoMap @IntKey(14)
    abstract fun bindEquipmentComponentRepository(impl: EquipmentComponentRepository): SyncableRepository

    @Binds @IntoMap @IntKey(15)
    abstract fun bindInspectionRouteGroupRepository(impl: InspectionRouteGroupRepository): SyncableRepository

    @Binds @IntoMap @IntKey(16)
    abstract fun bindInspectionRecordGroupRepository(impl: InspectionRecordGroupRepository): SyncableRepository

    @Binds @IntoMap @IntKey(17)
    abstract fun bindInspectionRouteGroupEquipmentRepository(impl: InspectionRouteGroupEquipmentRepository): SyncableRepository

    @Binds @IntoMap @IntKey(18)
    abstract fun bindInspectionRecordGroupEquipmentRepository(impl: InspectionRecordGroupEquipmentRepository): SyncableRepository

    @Binds @IntoMap @IntKey(19)
    abstract fun bindThermographicInspectionRecordRepository(impl: ThermographicInspectionRecordRepository): SyncableRepository

    @Binds @IntoMap @IntKey(20)
    abstract fun bindEquipmentComponentTemperatureLimitsRepository(impl: EquipmentComponentTemperatureLimitsRepository): SyncableRepository

}
