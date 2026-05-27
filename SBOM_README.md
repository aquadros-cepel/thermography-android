# Software Bill of Materials (SBOM)

## Visão Geral

Este documento descreve o Software Bill of Materials (SBOM) gerado para o aplicativo Thermography Android, em conformidade com os requisitos de documentação de componentes.

## Formato e Localização

- **Formato**: CycloneDX JSON (versão 1.5)
- **Arquivo**: `app/build/reports/sbom.json`
- **Plugin utilizado**: `org.cyclonedx.bom` versão 1.8.2

## Como Gerar o SBOM

Para gerar ou atualizar o SBOM, execute o seguinte comando na raiz do projeto:

```bash
./gradlew cyclonedxBom
```

O arquivo `sbom.json` será criado/atualizado em `app/build/reports/sbom.json`.

## Conteúdo do SBOM

O SBOM gerado contém:

### Componentes Identificados
- **Total de componentes**: 117 dependências diretas e transitivas
- **Configuração**: `releaseRuntimeClasspath` (dependências de runtime da versão release)

### Informações por Componente
Para cada componente, o SBOM inclui:
1. **Nome** e **versão** do componente
2. **Grupo/Publisher** (quando disponível)
3. **Tipo** (library, framework, etc.)
4. **PURL** (Package URL) para identificação única
5. **Licença** - informações de licenciamento
6. **Hashes** - MD5, SHA-1, SHA-256, SHA-384, SHA-512
7. **Referências externas** - links para repositórios, documentação, etc.
8. **Descrição** do componente

### Dependências Principais

O SBOM inclui todas as dependências do projeto, incluindo:

#### Frameworks Android
- AndroidX (Core, Lifecycle, Activity, etc.)
- Jetpack Compose (UI, Material3, Navigation)
- Room Database
- WorkManager

#### Bibliotecas de Terceiros
- Hilt (Dependency Injection)
- Kotlin Coroutines
- Retrofit e OkHttp (Networking)
- Coil (Image Loading)
- OSMDroid (Maps)
- Gson (JSON Parsing)

### Componentes Proprietários (Não incluídos automaticamente)

Os seguintes componentes proprietários da FLIR não são incluídos automaticamente no SBOM devido à sua natureza de arquivos .aar locais:

1. **androidsdk-release.aar**
   - Tipo: Biblioteca proprietária
   - Fornecedor: FLIR Systems
   - Localização: `app/libs/androidsdk-release.aar`
   - Licença: Proprietária FLIR

2. **thermalsdk-release.aar**
   - Tipo: Biblioteca proprietária
   - Fornecedor: FLIR Systems
   - Localização: `app/libs/thermalsdk-release.aar`
   - Licença: Proprietária FLIR

> **Nota**: Estes componentes proprietários devem ser documentados separadamente na submissão, incluindo informações de licenciamento fornecidas pela FLIR.

## Estrutura do Arquivo SBOM

```json
{
  "bomFormat": "CycloneDX",
  "specVersion": "1.5",
  "serialNumber": "urn:uuid:...",
  "version": 1,
  "metadata": {
    "timestamp": "...",
    "tools": [...],
    "component": {...}
  },
  "components": [
    {
      "publisher": "...",
      "group": "...",
      "name": "...",
      "version": "...",
      "description": "...",
      "hashes": [...],
      "licenses": [...],
      "purl": "...",
      "type": "library",
      "bom-ref": "..."
    }
  ],
  "dependencies": [...]
}
```

## Configuração do Plugin

A configuração do plugin CycloneDX está em `app/build.gradle.kts`:

```kotlin
tasks.cyclonedxBom {
    setIncludeConfigs(listOf("releaseRuntimeClasspath"))
    setOutputFormat("json")
    setOutputName("sbom")
    setSchemaVersion("1.5")
    setIncludeBomSerialNumber(true)
}
```

### Parâmetros de Configuração

- **includeConfigs**: Inclui apenas as dependências de runtime da versão release
- **outputFormat**: JSON (formato requerido)
- **outputName**: Nome do arquivo gerado (sbom.json)
- **schemaVersion**: Versão 1.5 do CycloneDX
- **includeBomSerialNumber**: Inclui número serial único para rastreabilidade

## Atualizações

O SBOM deve ser regenerado sempre que:
- Novas dependências forem adicionadas ao projeto
- Versões de dependências existentes forem atualizadas
- Antes de cada submissão de release

## Licenciamento

O SBOM inclui informações de licenciamento para todos os componentes open-source. A maioria das dependências Android/AndroidX utiliza a **licença Apache 2.0**.

Para visualizar as licenças específicas de cada componente, consulte o campo `licenses` no arquivo JSON ou use ferramentas de análise de SBOM compatíveis com CycloneDX.

## Conformidade

Este SBOM está em conformidade com:
- Formato CycloneDX (padrão da indústria)
- Especificação versão 1.5
- Formato JSON conforme requerido
- Inclui dependências diretas e transitivas
- Contém informações de licenciamento
- Fornece hashes criptográficos para verificação de integridade

## Ferramentas de Análise

O arquivo SBOM pode ser analisado com diversas ferramentas compatíveis com CycloneDX:
- CycloneDX CLI
- OWASP Dependency-Track
- Snyk
- Sonatype Nexus
- JFrog Xray

## Contato e Manutenção

Para questões sobre o SBOM ou atualizações de dependências, consulte a equipe de desenvolvimento do projeto.
