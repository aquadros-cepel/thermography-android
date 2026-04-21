package com.tech.thermography.android.ui.camera

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.flir.thermalsdk.live.remote.StoredImage
import com.tech.thermography.android.flir.AceController
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import org.json.JSONArray
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ThermogramCameraViewModel @Inject constructor(
    private val controller: AceController,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "recent_thermograms"
        private const val KEY_RECENT = "recent_paths"
    }

    private val _recentThermograms = MutableStateFlow<List<String>>(emptyList())
    val recentThermograms: StateFlow<List<String>> = _recentThermograms.asStateFlow()

    init {
        _recentThermograms.value = loadRecentThermograms().filter { File(it).exists() }
        persistRecentThermograms(_recentThermograms.value)
    }

    data class MeasurementSquareState(
        val label: String = "Bx1",
        val enabled: Boolean = false,
        val centerXFraction: Float = 0.5f,
        val centerYFraction: Float = 0.5f,
        val sizeFraction: Float = 0.3f,
        val initialSizeFraction: Float = 0.3f
    )

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
     * Requests the camera to store a snapshot. The callback returns (success, message, storedImage?)
     */
    fun takeSnapshot(callback: (Boolean, String?, StoredImage?) -> Unit = { _, _, _ -> }) {
        controller.takeSnapshot { success, msg, storedImage ->
            callback(success, msg, storedImage)
        }
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

    fun setMeasurementSquareState(state: MeasurementSquareState) {
        setMeasurementSquareStates(listOf(state))
    }

    fun setMeasurementSquareStates(states: List<MeasurementSquareState>) {
        controller.setMeasurementSquareStates(
            states.map {
                AceController.MeasurementSquareState(
                    label = it.label,
                    enabled = it.enabled,
                    centerXFraction = it.centerXFraction,
                    centerYFraction = it.centerYFraction,
                    sizeFraction = it.sizeFraction,
                    initialSizeFraction = it.initialSizeFraction
                )
            }
        )
    }
}
