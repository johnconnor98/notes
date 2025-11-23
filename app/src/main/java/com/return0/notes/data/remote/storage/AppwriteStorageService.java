package com.return0.notes.data.remote.storage;

import android.content.Context;
import android.util.Log;
import io.appwrite.Client;
import io.appwrite.services.Storage;
import io.appwrite.InputFile;
import io.appwrite.exceptions.AppwriteException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class AppwriteStorageService implements StorageService {
    private static final String TAG = "AppwriteStorageService";
    private static AppwriteStorageService instance;
    private final Storage storage;
    private static final String NOTES_BUCKET_ID = "notes";
    private static final String THUMBNAILS_BUCKET_ID = "thumbnails";
    
    private static final String ENDPOINT = "https://fra.cloud.appwrite.io/v1";
    private static final String PROJECT_ID = "6922970a001ab5faef56";
    private static final String API_KEY = "standard_c50102f470efafa6715e49beab3390411ad890f3f9e5fed0efb299054a373a95b21e7d44158f900d89876e2a796910ea1653624350a9c73b072498605c0f1794f6c348a30471f968d9886e4d1821aabc0ac91ccea5bd1101d65da3af26fbe10e31229a2b19f437af22230583301353fd166783a783a90f11c91f5d0c4cae2a72";

    private Client client;
    private Context context;
    
    private AppwriteStorageService(Context context) {
        this.context = context.getApplicationContext();
        client = new Client(context)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID)
            .setKey(API_KEY);
        
        storage = new Storage(client);
        Log.d(TAG, "AppwriteStorageService initialized");
        Log.d(TAG, "Endpoint: " + ENDPOINT);
        Log.d(TAG, "Project ID: " + PROJECT_ID);
    }

    public static synchronized AppwriteStorageService getInstance(Context context) {
        if (instance == null) {
            instance = new AppwriteStorageService(context);
        }
        return instance;
    }

    @Override
    public void uploadFile(File file, String filename, UploadCallback callback) {
        Log.d(TAG, "Starting file upload - Filename: " + filename + ", Local file: " + file.getAbsolutePath());
        
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
            
            Log.d(TAG, "Starting Appwrite upload - Bucket: " + NOTES_BUCKET_ID + ", Filename: " + filename + ", Size: " + fileSize + " bytes");
            
            CompletableFuture.runAsync(() -> {
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    InputFile inputFile = InputFile.fromFile(fileInputStream, filename);
                    
                    io.appwrite.models.File uploadedFile = storage.createFile(
                        NOTES_BUCKET_ID,
                        "unique()",
                        inputFile
                    );
                    
                    String fileId = uploadedFile.getId();
                    String downloadUrl = ENDPOINT + "/storage/buckets/" + NOTES_BUCKET_ID + "/files/" + fileId + "/view?project=" + PROJECT_ID;
                    String storagePath = NOTES_BUCKET_ID + "/" + fileId;
                    
                    Log.d(TAG, "Upload successful - File ID: " + fileId);
                    Log.d(TAG, "Download URL: " + downloadUrl);
                    Log.d(TAG, "Storage path: " + storagePath);
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(downloadUrl, storagePath);
                    });
                } catch (AppwriteException e) {
                    Log.e(TAG, "Upload failed - Filename: " + filename + ", Error: " + e.getMessage(), e);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Upload failed: " + e.getMessage());
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error during upload - Filename: " + filename, e);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Upload error: " + e.getMessage());
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: " + file.getAbsolutePath(), e);
            callback.onError("Error reading file: " + e.getMessage());
        }
    }

    @Override
    public void uploadThumbnail(File file, String filename, UploadCallback callback) {
        Log.d(TAG, "Starting thumbnail upload - Filename: " + filename + ", Local file: " + file.getAbsolutePath());
        
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
            
            Log.d(TAG, "Starting Appwrite thumbnail upload - Bucket: " + THUMBNAILS_BUCKET_ID + ", Filename: " + filename);
            
            CompletableFuture.runAsync(() -> {
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    InputFile inputFile = InputFile.fromFile(fileInputStream, filename);
                    
                    io.appwrite.models.File uploadedFile = storage.createFile(
                        THUMBNAILS_BUCKET_ID,
                        "unique()",
                        inputFile
                    );
                    
                    String fileId = uploadedFile.getId();
                    String downloadUrl = ENDPOINT + "/storage/buckets/" + THUMBNAILS_BUCKET_ID + "/files/" + fileId + "/view?project=" + PROJECT_ID;
                    String storagePath = THUMBNAILS_BUCKET_ID + "/" + fileId;
                    
                    Log.d(TAG, "Thumbnail upload successful - File ID: " + fileId);
                    Log.d(TAG, "Thumbnail download URL: " + downloadUrl);
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(downloadUrl, storagePath);
                    });
                } catch (AppwriteException e) {
                    Log.e(TAG, "Thumbnail upload failed - Filename: " + filename + ", Error: " + e.getMessage(), e);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Thumbnail upload failed: " + e.getMessage());
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error during thumbnail upload - Filename: " + filename, e);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Thumbnail upload error: " + e.getMessage());
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading thumbnail file: " + file.getAbsolutePath(), e);
            callback.onError("Error reading thumbnail file: " + e.getMessage());
        }
    }

    @Override
    public void downloadFile(String storagePath, String localFilename, java.io.File downloadsDir, DownloadCallback callback) {
        Log.d(TAG, "Starting file download - Storage path: " + storagePath + ", Local filename: " + localFilename);
        
        String[] parts = storagePath.split("/");
        if (parts.length < 2) {
            Log.e(TAG, "Invalid storage path format: " + storagePath);
            callback.onError("Invalid storage path format");
            return;
        }
        
        String bucketId = parts[0];
        String fileId = parts[1];
        
        Log.d(TAG, "Downloading from bucket: " + bucketId + ", File ID: " + fileId);
        
        CompletableFuture.runAsync(() -> {
            try {
                InputStream inputStream = storage.getFileDownload(bucketId, fileId);
                byte[] buffer = new byte[8192];
                java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                byte[] fileBytes = byteArrayOutputStream.toByteArray();
                inputStream.close();
                byteArrayOutputStream.close();
                
                Log.d(TAG, "Download successful - Bytes downloaded: " + fileBytes.length + ", File ID: " + fileId);
                
                java.io.File file = new java.io.File(downloadsDir, localFilename);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(fileBytes);
                fileOutputStream.close();
                
                Log.d(TAG, "File saved to: " + file.getAbsolutePath());
                
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(file.getAbsolutePath());
                });
            } catch (AppwriteException e) {
                Log.e(TAG, "Download failed - Storage path: " + storagePath + ", Error: " + e.getMessage(), e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Download failed: " + e.getMessage());
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving downloaded file: " + localFilename, e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Error saving file: " + e.getMessage());
                });
            }
        });
    }
}

