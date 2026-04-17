package com.tech.thermography.android.flir;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.flir.thermalsdk.image.Scale;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.measurements.MeasurementRectangle;
import com.flir.thermalsdk.image.measurements.MeasurementShapeCollection;
import com.flir.thermalsdk.log.ThermalLog;

import java.util.List;

/**
 * Service that reads FLIR thermal image information using direct SDK calls.
 */
public final class FlirCameraService {

    private static final String TAG = "FlirCameraService";
    private static final long RANGE_THROTTLE_MS = 400L;

    private AceController.TemperatureRange latestTemperatureRange;
    private long lastRangeUpdateMs;
    private String lastMeasurementSquaresSignature = "";

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

    public void resetMeasurementSquaresCache() {
        lastMeasurementSquaresSignature = "";
    }

    public void applyMeasurementSquares(@NonNull ThermalImage thermalImage, @NonNull List<AceController.MeasurementSquareState> states) {
        try {
            MeasurementShapeCollection measurements = thermalImage.getMeasurements();
            if (measurements == null) {
                ThermalLog.w(TAG, "ThermalImage measurements collection is null");
                return;
            }

            String signature = buildMeasurementSquaresSignature(states);
            if (signature.equals(lastMeasurementSquaresSignature)) {
                return;
            }

            clearRectangles(measurements);

            for (AceController.MeasurementSquareState state : states) {
                if (state != null && state.getEnabled()) {
                    applyMeasurementSquare(measurements, thermalImage, state);
                }
            }

            lastMeasurementSquaresSignature = signature;
        } catch (Exception e) {
            ThermalLog.e(TAG, "Failed to apply measurement squares: " + e.getMessage());
        }
    }

    @NonNull
    private String buildMeasurementSquaresSignature(@NonNull List<AceController.MeasurementSquareState> states) {
        StringBuilder builder = new StringBuilder();
        for (AceController.MeasurementSquareState state : states) {
            if (state == null) {
                continue;
            }
            builder.append(state.getLabel())
                    .append(':')
                    .append(state.getEnabled())
                    .append(':')
                    .append(state.getCenterXFraction())
                    .append(':')
                    .append(state.getCenterYFraction())
                    .append(':')
                    .append(state.getSizeFraction())
                    .append('|');
        }
        return builder.toString();
    }

    private void clearRectangles(@NonNull MeasurementShapeCollection measurements) {
        try {
            boolean cleared = false;
            List<MeasurementRectangle> rects = measurements.getRectangles();
            if (rects != null) {
                try {
                    rects.clear();
                    cleared = true;
                } catch (Throwable ignored) {
                    // fall through to iterative removal
                }

                if (!cleared) {
                    try {
                        for (MeasurementRectangle rect : rects.toArray(new MeasurementRectangle[0])) {
                            rects.remove(rect);
                        }
                        cleared = true;
                    } catch (Throwable ignored) {
                        // ignore and continue with the fallback below
                    }
                }
            }

            if (!cleared) {
                ThermalLog.w(TAG, "Unable to fully clear measurement rectangles");
            }
        } catch (Exception e) {
            ThermalLog.w(TAG, "Unable to clear measurement rectangles: " + e.getMessage());
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

