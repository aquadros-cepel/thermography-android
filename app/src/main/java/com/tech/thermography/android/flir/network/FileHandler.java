/*******************************************************************
 * @title FLIR Atlas Android SDK
 * @file FileHandler.java
 * @Author Teledyne FLIR
 *
 * @brief Provide a directory where camera imported images files can be saved
 *
 * Copyright 2023:    Teledyne FLIR
 *******************************************************************/
package com.tech.thermography.android.flir.network;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Provide a directory where camera imported images files can be saved.
 * Uses the same folder as ACE snapshots: getExternalFilesDir(PICTURES)/thermalEnergy/
 */
public class FileHandler {
    private final File filesDir;

    public FileHandler(Context applicationContext) {
        File picturesDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (picturesDir == null) {
            // fallback para armazenamento interno se o externo não estiver disponível
            picturesDir = applicationContext.getFilesDir();
        }
        filesDir = new File(picturesDir, "thermalEnergy");
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }
    }

    public String getImageStoragePathStr() {
        return filesDir.getAbsolutePath();
    }

    public File getImageStoragePath() {
        return filesDir;
    }

}
