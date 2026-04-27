import com.tech.thermography.android.ui.camera.ThermogramCameraViewModel.MeasurementSquareState
import com.tech.thermography.android.ui.camera.ThermogramCameraViewModel.MeasurementSpotState
// ...existing code...

    // Substitui as data classes locais pelas do ViewModel
    private var measurementSquareStates: List<MeasurementSquareState> = emptyList()
    private var measurementSpotState: MeasurementSpotState = MeasurementSpotState()

    fun setMeasurementSpotState(state: MeasurementSpotState) {
        measurementSpotState = state
        ThermalLog.d(TAG, "Measurement spot state updated: $state")
    }

    fun setMeasurementSquareStates(states: List<MeasurementSquareState>) {
        measurementSquareStates = states
        ThermalLog.d(TAG, "Measurement square states updated: $states")
    }
// ...existing code...

