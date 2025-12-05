# TODO - Atualização de Entidades e Repositórios baseado no JDL

## Phase 1: Create Missing Enums ✅
- [x] Create `Periodicity.kt` enum
- [x] Create `EquipmentInspectionStatus.kt` enum

## Phase 2: Create Missing Entities ✅
- [x] Create `InspectionRouteGroupEquipmentEntity.kt` (junction table)
- [x] Create `InspectionRecordEntity.kt`
- [x] Create `InspectionRecordGroupEntity.kt`
- [x] Create `InspectionRecordGroupEquipmentEntity.kt` (junction table)

## Phase 3: Update Existing Entities ✅
- [x] Update `CompanyEntity` - add `code` field
- [x] Update `BusinessUnitEntity` - add `code` field (change from required to optional)
- [x] Update `EquipmentEntity` - remove `title`, add `code`
- [x] Update `EquipmentGroupEntity` - remove `title`, add `code`
- [x] Update `EquipmentComponentEntity` - remove `title`, add `code`
- [x] Update `InspectionRouteEntity` - restructure fields according to JDL
- [x] Update `InspectionRouteGroupEntity` - remove `title`, add `code`, `included`, `orderIndex`

## Phase 4: Create/Update DAOs (Repositories) ✅
- [x] Create `InspectionRouteGroupEquipmentDao.kt`
- [x] Create `InspectionRecordDao.kt`
- [x] Create `InspectionRecordGroupDao.kt`
- [x] Create `InspectionRecordGroupEquipmentDao.kt`
- [x] Update existing DAOs if needed

## Phase 5: Update AppDatabase ✅
- [x] Add all new entities to the database
- [x] Add all new DAOs
- [x] Increment database version (v1 → v2)
- [x] Add migration strategy (if needed)

## Phase 6: SKIPPED (as per user request)

---

## ✅ TAREFA CONCLUÍDA!

Todas as fases foram completadas com sucesso:
- 2 novos enums criados
- 4 novas entidades criadas
- 7 entidades existentes atualizadas
- 4 novos DAOs criados
- AppDatabase atualizado com todas as entidades e DAOs
- Versão do banco de dados incrementada de 1 para 2
