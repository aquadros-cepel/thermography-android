package com.tech.thermography.android.flir;

import androidx.annotation.NonNull;

import com.flir.thermalsdk.image.Scale;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.measurements.MeasurementShapeCollection;
import com.flir.thermalsdk.log.ThermalLog;

import java.util.List;

/**
 * Service that reads FLIR thermal image information using direct SDK calls.
 */
public final class FlirCameraService {

    private static final String TAG = "FlirCameraService";
    private AceController.TemperatureRange latestTemperatureRange;

    public AceController.TemperatureRange getLatestTemperatureRange() {
        return latestTemperatureRange;
    }

    public AceController.TemperatureRange updateRangeFromThermalImage(@NonNull ThermalImage thermalImage) {
        try {
            Scale scale = thermalImage.getScale();
            if (scale == null) {
                ThermalLog.w(TAG, "ThermalImage scale is null");
                return latestTemperatureRange;
            }

            ThermalValue minValue = scale.getRangeMin();
            ThermalValue maxValue = scale.getRangeMax();
            if (minValue == null || maxValue == null) {
//                ThermalLog.w(TAG, "Scale range values are null: min=" + minValue + ", max=" + maxValue);
                return latestTemperatureRange;
            }

            double min = minValue.asCelsius().value;
            double max = maxValue.asCelsius().value;
            latestTemperatureRange = new AceController.TemperatureRange(min, max);

//            ThermalLog.d(TAG, "Range (Celsius): " + min + "°C - " + max + "°C");
            return latestTemperatureRange;
        } catch (Exception e) {
            ThermalLog.e(TAG, "Failed to read range from thermal image: " + e.getMessage());
            return latestTemperatureRange;
        }
    }

    public void applyMeasurementSquares(@NonNull ThermalImage thermalImage, @NonNull List<AceController.MeasurementSquareState> states) {
        try {
            MeasurementShapeCollection measurements = thermalImage.getMeasurements();
            if (measurements == null) {
                ThermalLog.w(TAG, "ThermalImage measurements collection is null");
                return;
            }

            for (AceController.MeasurementSquareState state : states) {
                if (state != null && state.getEnabled()) {
                    applyMeasurementSquare(measurements, thermalImage, state);
                }
            }
        } catch (Exception e) {
            ThermalLog.e(TAG, "Failed to apply measurement squares: " + e.getMessage());
        }
    }


    private void applyMeasurementSquare(
            @NonNull MeasurementShapeCollection measurements,
            @NonNull ThermalImage thermalImage,
            @NonNull AceController.MeasurementSquareState state
    ) {
        try {
            int imageWidth = thermalImage.getWidth();
            int imageHeight = thermalImage.getHeight();

            int baseSize = Math.min(imageWidth, imageHeight);
            int squareSize = Math.max(24, Math.round(baseSize * state.getSizeFraction()));
            int halfSize = squareSize / 2;

            int centerX = Math.round(imageWidth * state.getCenterXFraction());
            int centerY = Math.round(imageHeight * state.getCenterYFraction());
            int left = Math.max(0, Math.min(centerX - halfSize, imageWidth - squareSize));
            int top = Math.max(0, Math.min(centerY - halfSize, imageHeight - squareSize));

            measurements.addRectangle(left, top, squareSize, squareSize);
            ThermalLog.d(TAG, "Measurement rectangle applied (" + state.getLabel() + ") at [" + left + "," + top + "] size=" + squareSize);
        } catch (Exception e) {
            ThermalLog.w(TAG, "Unable to add rectangle measurement (" + state.getLabel() + "): " + e.getMessage());
        }
    }
}

