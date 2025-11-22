package com.return0.notes.data.remote;

import android.net.Uri;
import android.util.Log;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FirebaseStorageService {
    private static final String TAG = "FirebaseStorageService";
    private static FirebaseStorageService instance;
    private final FirebaseStorage storage;
    private static final String NOTES_FOLDER = "notes";
    private static final String THUMBNAILS_FOLDER = "thumbnails";

    private FirebaseStorageService() {
        storage = FirebaseStorage.getInstance();
        Log.d(TAG, "FirebaseStorageService initialized - Storage bucket: " + storage.getReference().getBucket());
    }

    public static synchronized FirebaseStorageService getInstance() {
        if (instance == null) {
            instance = new FirebaseStorageService();
        }
        return instance;
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl, String storagePath);
        void onProgress(double progress);
        void onError(String error);
    }

    public interface DownloadCallback {
        void onSuccess(String filePath);
        void onProgress(double progress);
        void onError(String error);
    }

    public void uploadFile(File file, String filename, UploadCallback callback) {
        String path = NOTES_FOLDER + "/" + filename;
        Log.d(TAG, "Starting file upload - Path: " + path + ", Local file: " + file.getAbsolutePath());
        
        try {
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
                callback.onError("File does not exist: " + file.getAbsolutePath());
                return;
            }
            
            long fileSize = file.length();
            Log.d(TAG, "File exists - Size: " + fileSize + " bytes");
            
            if (fileSize == 0) {
                Log.e(TAG, "File is empty: " + file.getAbsolutePath());
                callback.onError("File is empty");
                return;
            }
            
            if (fileSize > 100 * 1024 * 1024) {
                Log.w(TAG, "Large file detected: " + fileSize + " bytes (>100MB)");
            }
            
            StorageReference ref = storage.getReference().child(path);
            Log.d(TAG, "Firebase Storage reference created: " + ref.getPath());
            
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[(int) fileSize];
            int bytesRead = fileInputStream.read(buffer);
            fileInputStream.close();
            
            Log.d(TAG, "File read complete - Bytes read: " + bytesRead + ", Expected: " + fileSize);
            
            if (bytesRead != fileSize) {
                Log.e(TAG, "File read incomplete - Read: " + bytesRead + ", Expected: " + fileSize);
                callback.onError("Failed to read entire file");
                return;
            }
            
            Log.d(TAG, "Starting Firebase upload - Path: " + path + ", Size: " + buffer.length + " bytes");
            UploadTask uploadTask = ref.putBytes(buffer);
            
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Upload progress: " + String.format("%.2f", progress) + "% (" + 
                    taskSnapshot.getBytesTransferred() + "/" + taskSnapshot.getTotalByteCount() + " bytes)");
                callback.onProgress(progress);
            });
            
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "Upload successful - Path: " + path + ", Bytes uploaded: " + taskSnapshot.getBytesTransferred());
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Download URL obtained: " + uri.toString());
                    callback.onSuccess(uri.toString(), path);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL - Path: " + path, e);
                    callback.onError("Failed to get download URL: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Upload failed - Path: " + path + ", Error: " + e.getMessage(), e);
                if (e.getCause() != null) {
                    Log.e(TAG, "Upload failure cause: " + e.getCause().getMessage(), e.getCause());
                }
                callback.onError("Upload failed: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: " + file.getAbsolutePath(), e);
            callback.onError("Error reading file: " + e.getMessage());
        }
    }

    public void uploadThumbnail(File file, String filename, UploadCallback callback) {
        String path = THUMBNAILS_FOLDER + "/" + filename;
        Log.d(TAG, "Starting thumbnail upload - Path: " + path + ", Local file: " + file.getAbsolutePath());
        
        try {
            if (!file.exists()) {
                Log.e(TAG, "Thumbnail file does not exist: " + file.getAbsolutePath());
                callback.onError("Thumbnail file does not exist: " + file.getAbsolutePath());
                return;
            }
            
            long fileSize = file.length();
            Log.d(TAG, "Thumbnail file exists - Size: " + fileSize + " bytes");
            
            if (fileSize == 0) {
                Log.e(TAG, "Thumbnail file is empty: " + file.getAbsolutePath());
                callback.onError("Thumbnail file is empty");
                return;
            }
            
            StorageReference ref = storage.getReference().child(path);
            Log.d(TAG, "Firebase Storage reference created for thumbnail: " + ref.getPath());
            
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[(int) fileSize];
            int bytesRead = fileInputStream.read(buffer);
            fileInputStream.close();
            
            Log.d(TAG, "Thumbnail file read complete - Bytes read: " + bytesRead + ", Expected: " + fileSize);
            
            if (bytesRead != fileSize) {
                Log.e(TAG, "Thumbnail file read incomplete - Read: " + bytesRead + ", Expected: " + fileSize);
                callback.onError("Failed to read entire thumbnail file");
                return;
            }
            
            Log.d(TAG, "Starting Firebase thumbnail upload - Path: " + path + ", Size: " + buffer.length + " bytes");
            UploadTask uploadTask = ref.putBytes(buffer);
            
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Thumbnail upload progress: " + String.format("%.2f", progress) + "% (" + 
                    taskSnapshot.getBytesTransferred() + "/" + taskSnapshot.getTotalByteCount() + " bytes)");
                callback.onProgress(progress);
            });
            
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "Thumbnail upload successful - Path: " + path + ", Bytes uploaded: " + taskSnapshot.getBytesTransferred());
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Thumbnail download URL obtained: " + uri.toString());
                    callback.onSuccess(uri.toString(), path);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get thumbnail download URL - Path: " + path, e);
                    callback.onError("Failed to get thumbnail URL: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Thumbnail upload failed - Path: " + path + ", Error: " + e.getMessage(), e);
                if (e.getCause() != null) {
                    Log.e(TAG, "Thumbnail upload failure cause: " + e.getCause().getMessage(), e.getCause());
                }
                callback.onError("Thumbnail upload failed: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error reading thumbnail file: " + file.getAbsolutePath(), e);
            callback.onError("Error reading thumbnail file: " + e.getMessage());
        }
    }

    public void downloadFile(String storagePath, String localFilename, java.io.File downloadsDir, DownloadCallback callback) {
        Log.d(TAG, "Starting file download - Storage path: " + storagePath + ", Local filename: " + localFilename);
        StorageReference ref = storage.getReference().child(storagePath);
        Log.d(TAG, "Firebase Storage reference created for download: " + ref.getPath());
        
        ref.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
            Log.d(TAG, "Download successful - Bytes downloaded: " + bytes.length + ", Storage path: " + storagePath);
            try {
                java.io.File file = new java.io.File(downloadsDir, localFilename);
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(bytes);
                outputStream.close();
                Log.d(TAG, "File saved to: " + file.getAbsolutePath());
                callback.onSuccess(file.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Error saving downloaded file: " + localFilename, e);
                callback.onError("Error saving file: " + e.getMessage());
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Download failed - Storage path: " + storagePath + ", Error: " + e.getMessage(), e);
            if (e.getCause() != null) {
                Log.e(TAG, "Download failure cause: " + e.getCause().getMessage(), e.getCause());
            }
            callback.onError("Download failed: " + e.getMessage());
        });
    }

    public void downloadFileByUrl(String downloadUrl, String localFilename, java.io.File downloadsDir, DownloadCallback callback) {
        try {
            StorageReference ref = storage.getReferenceFromUrl(downloadUrl);
            String path = ref.getPath();
            downloadFile(path, localFilename, downloadsDir, callback);
        } catch (Exception e) {
            callback.onError("Invalid Firebase URL: " + e.getMessage());
        }
    }
}

