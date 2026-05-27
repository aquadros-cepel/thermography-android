/*******************************************************************
 * @title FLIR Atlas Android SDK
 * @file CameraHandler.java
 * @Author Teledyne FLIR
 *
 * @brief Utility class for handling a FLIR network camera
 *
 * Copyright 2023:    Teledyne FLIR
 ********************************************************************/
package com.tech.thermography.android.flir.network;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.helpers.ShowMessage;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.importing.CollisionOption;
import com.flir.thermalsdk.live.importing.FileReference;
import com.flir.thermalsdk.live.importing.FileInfo;
import com.flir.thermalsdk.live.importing.ListFlag;
import com.flir.thermalsdk.live.importing.OnFileCompletion;
import com.flir.thermalsdk.live.importing.OnFileError;
import com.flir.thermalsdk.live.importing.ProgressCallback;
import com.flir.thermalsdk.log.ThermalLog;
import com.flir.thermalsdk.live.AuthenticationResponse;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulates the handling of a network camera, discovery, connecting, list files and import first file found.
 * Please note all listeners are called from Atlas Android SDK on a non-ui thread
 * <p/>
 * Usage:
 * <pre>
 * Start discovery of FLIR network cameras
 * {@linkplain #startDiscovery(DiscoveryEventListener, DiscoveryStatus)}
 * Use a discovered Camera {@linkplain Identity} and connect to the Camera
 * (note that calling connect is blocking and it is mandatory to call this function from a background thread):
 * {@linkplain #connect(Identity, ConnectionStatusListener)}
 * Once connected to a camera, import images
 * {@linkplain #startImport(ImportInformation)}
 * </pre>
 */
public class CameraHandler {

    private static final String TAG = "CameraHandler";

    //Discovered FLIR cameras
    List<Identity> foundCameraIdentities = new LinkedList<>();

    //Handler for providing a place to store imported files
    private FileHandler fileHandler;

    private ImportInformation importInformation;

    //A FLIR Camera
    private Camera camera;


    private final ShowMessage showMessage;

    public interface DiscoveryStatus {
        void started();

        void stopped();
    }

    public void startDiscovery(DiscoveryEventListener cameraDiscoveryListener, DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().scan(cameraDiscoveryListener, CommunicationInterface.NETWORK);
        discoveryStatus.started();
    }

    public void stopDiscovery(DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().stop(CommunicationInterface.NETWORK);
        discoveryStatus.stopped();
    }

    public AuthenticationResponse authenticate(Identity identity,String name) throws IOException{
        return camera.authenticate(identity, name, 41*1001);
    }

    public AuthenticationResponse authenticate(InetAddress ipAddress, String name) throws IOException{
        return camera.authenticate(ipAddress, name,41*1001);
    }

    public void connect(Identity identity, ConnectionStatusListener connectionStatusListener) throws IOException {
        camera.connect(identity, connectionStatusListener,new ConnectParameters());
    }

    public void disconnect() {
        if (camera == null) {
            return;
        }
        if (camera.isGrabbing()) {
            camera.unsubscribeAllStreams();
        }
        camera.disconnect();
    }

    /**
     * Interface for information when importing images
     */
    public interface ImportInformation {
        void importedFile(FileReference file, File savedInDir);

        void importedFileError(FileReference file, ErrorCode errorCode);
    }

    public CameraHandler(ShowMessage showMessage, FileHandler fileHandler) {
        this.fileHandler = fileHandler;
        this.showMessage = showMessage;
        this.camera = new Camera();
    }

    /**
     * List all image files available on the connected camera.
     *
     * @return list of FileInfo or empty list if camera not connected or no files
     * @throws IOException on camera communication error
     */
    public List<FileInfo> listAllImages() throws IOException {
        List<FileInfo> files = camera.getImporter().listImages(null, null);
        ThermalLog.d(TAG, "listAllImages() found: " + files.size() + " files");
        return files;
    }

    /**
     * Import a specific set of files from the camera.
     *
     * @param references files to import
     * @param importInformation callback for results
     * @throws IOException on camera communication error
     */
    public void importFiles(List<FileReference> references, ImportInformation importInformation) throws IOException {
        this.importInformation = importInformation;
        camera.getImporter().importFiles(
                references,
                fileHandler.getImageStoragePathStr(),
                CollisionOption.OVERWRITE,
                onFileCompletion,
                onFileError,
                progressCallback);
    }

    /**
     * Imports images from a already connected network camera
     *
     * @throws IOException if it's not possible to import images from the camera.
     */
    public void startImport(ImportInformation importInformation) throws IOException {
        this.importInformation = importInformation;

        try {
            //List all available files on camera
            List<FileInfo> fileObjects = camera.getImporter().listImages(null, null);

            int size = fileObjects.size();
            if (size <= 0) {
                ThermalLog.w(TAG, "startImport() no files to import");
                return;
            }

            ThermalLog.d(TAG, "startImport, found nr of files in camera:" + size + " will import nr of files:" + size);

            //We just import the first image found
            List<FileReference> fewImages = getReferences(fileObjects.subList(0, 1));

            //Import a subset of files into a folder
            camera.getImporter().importFiles(fewImages, fileHandler.getImageStoragePathStr(), CollisionOption.OVERWRITE, onFileCompletion, onFileError, progressCallback);

        } catch (IOException e) {
            ThermalLog.e(TAG, "startImport(), failed:" + e);
            throw e;
        }

    }

    /**
     * Public accessor to FileHandler path for external use
     */
    public File getStoragePath() {
        return fileHandler.getImageStoragePath();
    }

    /**
     * Add a found camera to the list of known cameras
     */
    public void add(Identity identity) {
        if (!foundCameraIdentities.contains(identity)) {
            foundCameraIdentities.add(identity);
        }
    }

    /**
     * Get a read only list of all found cameras
     */
    public List<Identity> getCameraList() {
        return Collections.unmodifiableList(foundCameraIdentities);
    }

    /**
     * Clear all known network cameras
     */
    public void clear() {
        foundCameraIdentities.clear();
    }

    /**
     * Handle information when a file has been imported from a network camera correctly
     */
    private OnFileCompletion onFileCompletion = new OnFileCompletion() {
        @Override
        public void onFileCompletion(FileReference fileRef) {
            ThermalLog.d(TAG, "onFileCompletion" + fileRef);
            importInformation.importedFile(fileRef, fileHandler.getImageStoragePath());
        }
    };

    /**
     * Handle information when a file has failed to be imported from a network camera correctly
     */
    private OnFileError onFileError = new OnFileError() {
        @Override
        public void onFileError(FileReference fileRef, ErrorCode errorCode) {
            ThermalLog.d(TAG, "onFileError() called with: fileRef = [" + fileRef + "], errorCode = [" + errorCode + "]");
            importInformation.importedFileError(fileRef, errorCode);
        }
    };

    /**
     * Handle progress information for file is in progress of being imported from a network camera
     */
    private ProgressCallback progressCallback = new ProgressCallback() {
        @Override
        public void progressCallback(FileReference file, long currentSize, long totalSize) {
            ThermalLog.d(TAG, "progressCallback() called with: file = [" + file + "], currentSize = [" + currentSize + "], totalSize = [" + totalSize + "]");
        }
    };


    private static List<FileReference> getReferences(@NotNull List<FileInfo> files) {
        List<FileReference> fileRefs = new ArrayList<>();
        for (FileInfo fi : files) {
            fileRefs.add(fi.reference);
        }
        return fileRefs;
    }
}
