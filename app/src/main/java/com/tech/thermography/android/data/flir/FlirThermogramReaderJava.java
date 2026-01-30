package com.tech.thermography.android.data.flir;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.image.CameraInformation;
import com.flir.thermalsdk.image.ImageFactory;
import com.flir.thermalsdk.image.ImageStatistics;
import com.flir.thermalsdk.image.ThermalImageFile;
import com.flir.thermalsdk.image.ThermalParameters;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.measurements.MeasurementEllipse;
import com.flir.thermalsdk.image.measurements.MeasurementLine;
import com.flir.thermalsdk.image.measurements.MeasurementRectangle;
import com.flir.thermalsdk.image.measurements.MeasurementShapeCollection;
import com.flir.thermalsdk.image.measurements.MeasurementSpot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Java implementation that reads FLIR ThermalImage metadata using the official FLIR Atlas Android SDK.
 * Uses reflection for API differences across SDK versions.
 */
public final class FlirThermogramReaderJava {

    private FlirThermogramReaderJava() {}

    public static ThermogramMetadata readMetadata(@NonNull Context context, @NonNull Uri uri) throws Exception {
        try { ThermalSdkAndroid.init(context.getApplicationContext()); } catch (Throwable ignored) {}

        File file = materializeFile(context, uri);
        ThermalImageFile tif;
        try {
            tif = (ThermalImageFile) ImageFactory.createImage(file.getAbsolutePath());
        } catch (Exception e) {
            throw new Exception("Falha ao abrir imagem térmica: " + e.getMessage(), e);
        }

        // Camera information
        String cameraModel = null;
        String cameraLens = null;
        try {
            CameraInformation camInfo = tif.getCameraInformation();
            if (camInfo != null) {
                cameraModel = camInfo.model;
                cameraLens = camInfo.lens;
            }
        } catch (Throwable ignored) {}

        // Resolution
        String imageResolution = null;
        try {
            int w = tif.getWidth();
            int h = tif.getHeight();
            imageResolution = w + "x" + h;
        } catch (Throwable ignored) {}

        // Statistics (min/avg/max)
        Double minTemp = null, maxTemp = null, avgTemp = null;
        try {
            ImageStatistics stats = tif.getStatistics();
            if (stats != null) {
                minTemp = valueOrNull(stats.min);
                avgTemp = valueOrNull(stats.average);
                maxTemp = valueOrNull(stats.max);
            }
        } catch (Throwable ignored) {}

        // Thermal parameters
        Double emissivity = null,
               reflectedTemp = null,
               atmosphericTemp = null,
               relativeHumidity = null,
               subjectDistance = null,
               referenceTemperature = null;
        try {
            ThermalParameters params = tif.getThermalParameters();
            if (params != null) {
                emissivity = params.getObjectEmissivity();
                subjectDistance = params.getObjectDistance(); // distância do objeto

                ThermalValue reflected = params.getObjectReflectedTemperature(); // temperatura refletida
                reflectedTemp = valueOrNull(reflected);

                ThermalValue atmospheric = params.getAtmosphericTemperature(); // temperatura atmosférica
                atmosphericTemp = valueOrNull(atmospheric);

                double rh = params.getRelativeHumidity(); // umidade relativa
                // RelativeHumidity vem em 0–100, então ajustamos se necessário
                relativeHumidity = (rh <= 1.0) ? rh * 100.0 : rh;
            }
        } catch (Throwable ignored) {}

        // Date/time
        Instant createdAt = null;
        try {
            Date d = tif.getDateTaken();
            if (d != null) createdAt = d.toInstant();
        } catch (Throwable ignored) {}

        Double latitude = null, longitude = null;

        // Measurements / ROIs: spots, rectangles, circles, lines
        List<ROIMetadata> rois = new ArrayList<>();
        try {
            MeasurementShapeCollection measurements = tif.getMeasurements();
            if (measurements != null) {
                int idx = 0;
                // Spots
                List<MeasurementSpot> spots = measurements.getSpots();
                if (spots != null) {
                    for (MeasurementSpot sp : spots) {
                        ThermalValue tv = sp.getValue();
                        double val = valueOrNull(tv);
                        rois.add(new ROIMetadata(UUID.randomUUID(), "Spot", "Sp" + (++idx), val, null, null));
                    }
                }
                // Rectangles
                List<MeasurementRectangle> rects = measurements.getRectangles();
                if (rects != null) {
                    for (MeasurementRectangle r : rects) {
                        ThermalValue max = r.getMaxValue();
                        ThermalValue min = r.getMinValue();
                        ThermalValue avg = r.getAvgValue();
                        rois.add(new ROIMetadata(
                                UUID.randomUUID(), "Rectangle", "Bx" + (++idx),
                                valueOrNull(max) != null ? valueOrNull(max) : 0.0,
                                valueOrNull(min),
                                valueOrNull(avg)
                        ));
                    }
                }
                // Ellipses (círculos)
                List<MeasurementEllipse> ellipses = measurements.getEllipses();
                if (ellipses != null) {
                    for (MeasurementEllipse e : ellipses) {
                        ThermalValue max = e.getMaxValue();
                        ThermalValue min = e.getMinValue();
                        ThermalValue avg = e.getAvgValue();
                        rois.add(new ROIMetadata(
                                UUID.randomUUID(), "Ellipse", "Ex" + (++idx),
                                valueOrNull(max) != null ? valueOrNull(max) : 0.0,
                                valueOrNull(min),
                                valueOrNull(avg)

                        ));
                    }
                }
                // Lines
                List<MeasurementLine> lines = measurements.getLines();
                if (lines != null) {
                    for (MeasurementLine l : lines) {
                        ThermalValue max = l.getMaxValue();
                        ThermalValue min = l.getMinValue();
                        ThermalValue avg = l.getAvgValue();
                        rois.add(new ROIMetadata(
                                UUID.randomUUID(), "Line", "Lx" + (++idx),
                                valueOrNull(max) != null ? valueOrNull(max) : 0.0,
                                valueOrNull(min),
                                valueOrNull(avg)
                        ));
                    }
                }
            }
        } catch (Throwable ignored) {}

        return new ThermogramMetadata(
                cameraModel,
                cameraLens,
                imageResolution,
                minTemp,
                maxTemp,
                avgTemp,
                emissivity,
                reflectedTemp,
                atmosphericTemp,
                relativeHumidity,
                subjectDistance,
                referenceTemperature,
                createdAt,
                latitude,
                longitude,
                rois
        );
    }

    // ===================== helpers =====================

    private static File materializeFile(Context context, Uri uri) throws Exception {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new File(String.valueOf(uri.getPath()));
        }
        File out = new File(context.getCacheDir(), "flir_" + System.currentTimeMillis() + ".jpg");
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            if (in == null) throw new Exception("Não foi possível abrir InputStream para URI");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                fos.write(buf, 0, r);
            }
            fos.flush();
        }
        return out;
    }

    private static Object call(Object target, String name, Class<?>[] types, Object[] args) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(name, types);
            return m.invoke(target, args);
        } catch (Exception ignored) { return null; }
    }

    private static Object call(Object target, String name) { return call(target, name, new Class<?>[]{}, new Object[]{}); }

    private static Double asDouble(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            Method m = v.getClass().getMethod("doubleValue");
            Object nv = m.invoke(v);
            if (nv instanceof Number) return ((Number) nv).doubleValue();
        } catch (Exception ignored) {}
        return null;
    }

    private static Double valueOrNull(ThermalValue tv) {
        return tv == null ? null : tv.asCelsius().value;
    }

}
