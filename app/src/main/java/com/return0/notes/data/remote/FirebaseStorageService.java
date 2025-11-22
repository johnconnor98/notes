package com.return0.notes.data.remote;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FirebaseStorageService {
    private static FirebaseStorageService instance;
    private final FirebaseStorage storage;
    private static final String NOTES_FOLDER = "notes";
    private static final String THUMBNAILS_FOLDER = "thumbnails";

    private FirebaseStorageService() {
        storage = FirebaseStorage.getInstance();
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
        StorageReference ref = storage.getReference().child(path);
        
        UploadTask uploadTask = ref.putFile(Uri.fromFile(file));
        
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        });
        
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                callback.onSuccess(uri.toString(), path);
            }).addOnFailureListener(e -> {
                callback.onError("Failed to get download URL: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            callback.onError("Upload failed: " + e.getMessage());
        });
    }

    public void uploadThumbnail(File file, String filename, UploadCallback callback) {
        String path = THUMBNAILS_FOLDER + "/" + filename;
        StorageReference ref = storage.getReference().child(path);
        
        UploadTask uploadTask = ref.putFile(Uri.fromFile(file));
        
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        });
        
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                callback.onSuccess(uri.toString(), path);
            }).addOnFailureListener(e -> {
                callback.onError("Failed to get thumbnail URL: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            callback.onError("Thumbnail upload failed: " + e.getMessage());
        });
    }

    public void downloadFile(String storagePath, String localFilename, java.io.File downloadsDir, DownloadCallback callback) {
        StorageReference ref = storage.getReference().child(storagePath);
        
        ref.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
            try {
                java.io.File file = new java.io.File(downloadsDir, localFilename);
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(bytes);
                outputStream.close();
                callback.onSuccess(file.getAbsolutePath());
            } catch (Exception e) {
                callback.onError("Error saving file: " + e.getMessage());
            }
        }).addOnFailureListener(e -> {
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

