package com.return0.notes.data.remote.storage;

import java.io.File;

public interface StorageService {
    interface UploadCallback {
        void onSuccess(String downloadUrl, String storagePath);
        void onProgress(double progress);
        void onError(String error);
    }

    interface DownloadCallback {
        void onSuccess(String filePath);
        void onProgress(double progress);
        void onError(String error);
    }

    void uploadFile(File file, String filename, UploadCallback callback);
    void uploadThumbnail(File file, String filename, UploadCallback callback);
    void downloadFile(String storagePath, String localFilename, java.io.File downloadsDir, DownloadCallback callback);
}

