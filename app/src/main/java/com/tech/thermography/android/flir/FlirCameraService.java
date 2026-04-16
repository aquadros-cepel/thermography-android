package com.tech.thermography.android.flir;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.flir.thermalsdk.image.Scale;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.log.ThermalLog;

/**
 * Service that reads FLIR thermal image information using direct SDK calls.
 */
public final class FlirCameraService {

    private static final String TAG = "FlirCameraService";
    private static final long RANGE_THROTTLE_MS = 400L;

    private AceController.TemperatureRange latestTemperatureRange;
    private long lastRangeUpdateMs;

    public AceController.TemperatureRange getLatestTemperatureRange() {
        return latestTemperatureRange;
    }

    public AceController.TemperatureRange updateRangeFromThermalImage(@NonNull ThermalImage thermalImage) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastRangeUpdateMs < RANGE_THROTTLE_MS) {
            return latestTemperatureRange;
        }

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
            lastRangeUpdateMs = now;

//            ThermalLog.d(TAG, "Range (Celsius): " + min + "°C - " + max + "°C");
            return latestTemperatureRange;
        } catch (Exception e) {
            ThermalLog.e(TAG, "Failed to read range from thermal image: " + e.getMessage());
            return latestTemperatureRange;
        }
    }
}

