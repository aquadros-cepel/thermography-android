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
---

# 📝 Implementação da Camada de Dados para Anomalias Térmicas (13/01/2026) ✅

## Fase 1: Atualização de DAOs e Repositories ✅
- [x] Atualizar `EquipmentDao` - adicionar query `getEquipmentsByPlantId`
- [x] Atualizar `EquipmentRepository` - adicionar método `getEquipmentsByPlantId`
- [x] Verificar `PlantDao` e `PlantRepository` (já existentes)
- [x] Verificar `ThermographicInspectionRecordDao` e `ThermographicInspectionRecordRepository` (já existentes)

## Fase 2: Implementação do ViewModel ✅
- [x] Criar `ThermalAnomalyEvent.kt` - eventos do formulário
- [x] Criar `ThermalAnomalyUiState.kt` - estado da UI
- [x] Criar `ThermalAnomalyViewModel.kt` - lógica de negócios e integração com repositórios

## Fase 3: Componentes de UI ✅
- [x] Criar `AppExposedDropdownMenu.kt` - dropdown genérico com Material 3
- [x] Criar `AppDatePickerField.kt` - campo de data com DatePicker do Material 3

## Fase 4: Tela de Formulário ✅
- [x] Criar `ThermalAnomalyForm.kt` - formulário completo integrado com ViewModel
- [x] Integrar `ThermalAnomalyForm` no `AppNavHost.kt`

## Estrutura de Arquivos Criados:
```
app/src/main/java/com/tech/thermography/android/
├── ui/thermal_anomaly/
│   ├── ThermalAnomalyEvent.kt
│   ├── ThermalAnomalyUiState.kt
│   ├── ThermalAnomalyViewModel.kt
│   ├── ThermalAnomalyForm.kt
│   └── components/
│       ├── AppExposedDropdownMenu.kt
│       └── AppDatePickerField.kt
└── data/local/
    ├── dao/
    │   └── EquipmentDao.kt (atualizado)
    └── repository/
        └── EquipmentRepository.kt (atualizado)
```

## Recursos Implementados:
✅ Dropdowns reais alimentados pelo banco de dados Room
✅ Filtro de equipamentos por instalação (planta)
✅ DatePicker nativo do Material 3 para prazo de execução
✅ Integração completa com ViewModel usando StateFlow
✅ Validação de campos e tratamento de erros
✅ Salvamento de registros termográficos no banco de dados
✅ UI responsiva com Material 3 Design

## Próximos Passos Sugeridos:
- [ ] Implementar captura de imagens (termogramas)
- [ ] Adicionar visualização de ROIs (Regions of Interest)
- [ ] Implementar edição de registros existentes
- [ ] Adicionar sincronização com servidor remoto
- [ ] Implementar listagem de anomalias registradas