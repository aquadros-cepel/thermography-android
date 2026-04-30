package com.tech.thermography.android.ui.camera

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flir.thermalsdk.image.TemperatureUnit
import com.flir.thermalsdk.image.ThermalValue
import com.flir.thermalsdk.image.fusion.FusionMode
import com.tech.thermography.android.ui.camera.ThermalParametersConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import javax.inject.Inject
import com.flir.thermalsdk.live.remote.StoredImage
import com.tech.thermography.android.flir.AceController
import com.tech.thermography.android.flir.AceController.TemperatureRange
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ThermogramCameraViewModel @Inject constructor(
    private val controller: AceController,
    @ApplicationContext private val context: Context
) : ViewModel() {
    // Palettes disponíveis
    private val _palettes = MutableStateFlow<List<com.flir.thermalsdk.image.Palette>>(emptyList())
    val palettes: StateFlow<List<com.flir.thermalsdk.image.Palette>> = _palettes.asStateFlow()

    // Palette selecionada
    private val _currentPalette = MutableStateFlow<com.flir.thermalsdk.image.Palette?>(null)
    val currentPalette: StateFlow<com.flir.thermalsdk.image.Palette?> = _currentPalette.asStateFlow()

    // Modos de fusão disponíveis
    val fusionModesWithLabel = listOf(
        FusionMode.THERMAL_ONLY to "Térmica",
        FusionMode.MSX to "MSX",
        FusionMode.VISUAL_ONLY to "Câmera Digital"
    )

    private val _currentFusionMode = kotlinx.coroutines.flow.MutableStateFlow(FusionMode.THERMAL_ONLY)
    val currentFusionMode: kotlinx.coroutines.flow.StateFlow<FusionMode> = _currentFusionMode.asStateFlow()

    companion object {
        private const val PREFS_NAME = "recent_thermograms"
        private const val KEY_RECENT = "recent_paths"
        // Mapa de nomes de palette para labels amigáveis
        val PALETTE_LABELS = mapOf(
            "iron" to "Ferro",
            "lava" to "Lava",
            "rainbow" to "Arco-Íris",
            "rainhc" to "Elevado Contraste de Arco-Íris",
            "artic" to "Ártico",
            "whitehot" to "Incandescente Branco",
            "blackhot" to "Incandescente Preto"
        )
    }

    private val _recentThermograms = MutableStateFlow<List<String>>(emptyList())
    val recentThermograms: StateFlow<List<String>> = _recentThermograms.asStateFlow()

    // Lista reduzida de palettes com label amigável, ordenada conforme a ordem do mapa
    val filteredPalettesWithLabel: List<Pair<com.flir.thermalsdk.image.Palette, String>>
        get() {
            val paletteOrder = PALETTE_LABELS.keys.toList()
            val filtered = _palettes.value.mapNotNull { palette ->
                val nameKey = palette.name?.replace(" ", "")?.lowercase() ?: return@mapNotNull null
                val label = PALETTE_LABELS[nameKey]
                if (label != null) palette to label else null
            }
            // Ordena conforme a ordem do mapa
            return paletteOrder.flatMap { key ->
                filtered.filter { (palette, _) ->
                    palette.name?.replace(" ", "")?.lowercase() == key
                }
            }
        }


    //Atualização do estado de temperatura
    private val _measurementTemperatures = MutableStateFlow(MeasurementTemperatures())
    val measurementTemperatures: StateFlow<MeasurementTemperatures> = _measurementTemperatures.asStateFlow()

    //Atualização do range de temperatura
    private val _temperatureRange = MutableStateFlow(getTemperatureRange())
    val temperatureRange: StateFlow<TemperatureRange?> = _temperatureRange.asStateFlow()

    // Parâmetros térmicos UI
    data class ThermalParametersUi(
        val distance: Double = 1.0,
        val emissivity: Double = 0.95,
        val reflectedTemperature: ThermalValue = ThermalValue(20.0, TemperatureUnit.CELSIUS),
        val atmosphericTemperature: ThermalValue = ThermalValue(20.0, TemperatureUnit.CELSIUS),
        val relativeHumidity: Double = 50.0
    )

    private val _thermalParametersUi = MutableStateFlow(ThermalParametersUi())
    val thermalParametersUi: StateFlow<ThermalParametersUi> = _thermalParametersUi

    init {
        // Inicializa lista de palettes e seleciona a primeira como padrão
        val defaultPalettes = com.flir.thermalsdk.image.PaletteManager.getDefaultPalettes()
        _palettes.value = defaultPalettes
        _currentPalette.value = defaultPalettes.firstOrNull()
        _currentPalette.value?.let { controller.setCurrentPalette(it) }
        _recentThermograms.value = loadRecentThermograms().filter { File(it).exists() }
        persistRecentThermograms(_recentThermograms.value)

        startTemperaturesRangeUpdates()
        startMeasurementTemperaturesUpdates()

        // Inicializa modo de fusão padrão
        controller.setCurrentFusionMode(FusionMode.THERMAL_ONLY)
    }

    fun selectPalette(palette: com.flir.thermalsdk.image.Palette) {
        _currentPalette.value = palette
        controller.setCurrentPalette(palette)
    }

    fun selectFusionMode(mode: FusionMode) {
        _currentFusionMode.value = mode
        controller.setCurrentFusionMode(mode)
    }

    private fun startTemperaturesRangeUpdates() {
        viewModelScope.launch {
            while (isActive) {
                _temperatureRange.value = getTemperatureRange()
                delay(500)
            }
        }
    }

    private fun startMeasurementTemperaturesUpdates() {
        viewModelScope.launch {
            while (isActive) {
                _measurementTemperatures.value = getMeasurementTemperatures()!!
                delay(500)
            }
        }
    }

    fun attachGlSurface(glView: GLSurfaceView) {
        controller.attachSurface(glView)
    }

    fun onSurfaceSizeChanged(width: Int, height: Int) {
        controller.onSurfaceSizeChanged(width, height)
    }

    fun start() {
        controller.startCamera()
    }

    fun stop() {
        controller.disconnect()
    }

    override fun onCleared() {
        controller.disconnect()
    }


    /**
     * Requests the camera to store a snapshot WITH screen overlay (saved as separate PNG).
     * Creates 2 files: snapshot_XXX.jpg (thermal) + snapshot_XXX_overlay.png (UI)
     * The callback returns (success, message, storedImage?)
     */
    fun takeSnapshotWithOverlay(
        activity: Activity,
        callback: (Boolean, String?, StoredImage?) -> Unit = { _, _, _ -> }
    ) {
        controller.takeSnapshotWithOverlay(activity) { success, msg, storedImage ->
            if (success && !msg.isNullOrBlank()) {
                onSnapshotSaved(msg)
            }
            callback(success, msg, storedImage)
        }
    }

    fun onSnapshotSaved(path: String) {
        _recentThermograms.value = buildList {
            add(path)
            addAll(_recentThermograms.value.filterNot { it == path })
        }.filter { File(it).exists() }.take(20)
        persistRecentThermograms(_recentThermograms.value)
    }

    fun removeRecentThermogram(path: String, deleteFile: Boolean = true) {
        _recentThermograms.value = _recentThermograms.value.filterNot { it == path }
        persistRecentThermograms(_recentThermograms.value)

        if (deleteFile) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { File(path).delete() }
            }
        }
    }

    fun pruneMissingRecentThermograms() {
        val filtered = _recentThermograms.value.filter { File(it).exists() }
        if (filtered != _recentThermograms.value) {
            _recentThermograms.value = filtered
            persistRecentThermograms(filtered)
        }
    }

    private fun persistRecentThermograms(paths: List<String>) {
        val payload = JSONArray().apply { paths.forEach { put(it) } }.toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT, payload)
            .apply()
    }

    private fun loadRecentThermograms(): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECENT, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val path = array.optString(i)
                    if (path.isNotBlank()) add(path)
                }
            }
        }.getOrElse { emptyList() }
    }

    fun toggleLaser(callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        controller.toggleLaser(callback)
    }

    fun toggleFlash(callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        controller.toggleLamp(callback)
    }

    fun getTemperatureRange(): AceController.TemperatureRange? {
        return controller.getTemperatureRange()
    }

    fun getMeasurementTemperatures(): MeasurementTemperatures? {
        return controller.getMeasurementTemperatures()
    }

    fun setMeasurementStates(states: List<MeasurementState>) {
        controller.setMeasurementStates(states)
    }

    // Atualiza os parâmetros térmicos UI
    fun updateThermalParametersUi(update: (ThermalParametersUi) -> ThermalParametersUi) {
        _thermalParametersUi.value = update(_thermalParametersUi.value)
    }

    // Salva os parâmetros térmicos no controlador
    fun saveThermalParameters() {
        val params = _thermalParametersUi.value
        controller.setThermalParameters(
            distance = params.distance,
            emissivity = params.emissivity,
            reflectedTemperature = params.reflectedTemperature,
            atmosphericTemperature = params.atmosphericTemperature,
            relativeHumidity = params.relativeHumidity
        )
    }
}
