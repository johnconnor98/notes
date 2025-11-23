package com.return0.notes.data.remote.storage;

import android.content.Context;
import android.util.Log;
import io.appwrite.Client;
import io.appwrite.services.Account;
import io.appwrite.exceptions.AppwriteException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.CookieManager;
import java.net.CookieHandler;
import java.net.CookiePolicy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppwriteStorageService implements StorageService {
    private static final String TAG = "AppwriteStorageService";
    private static AppwriteStorageService instance;
    private static final String NOTES_BUCKET_ID = "notes";
    private static final String THUMBNAILS_BUCKET_ID = "thumbnails";
    
    private static final String ENDPOINT = "https://fra.cloud.appwrite.io/v1";
    private static final String PROJECT_ID = "6922970a001ab5faef56";

    private Context context;
    private Client client;
    private Account account;
    private AtomicBoolean sessionCreated = new AtomicBoolean(false);
    
    private AppwriteStorageService(Context context) {
        this.context = context.getApplicationContext();
        client = new Client(context)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID);
        
        account = new Account(client);
        
        // Set up cookie manager to handle session cookies
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        
        Log.d(TAG, "AppwriteStorageService initialized");
        Log.d(TAG, "Endpoint: " + ENDPOINT);
        Log.d(TAG, "Project ID: " + PROJECT_ID);
    }
    
    private void ensureSessionCreated(Runnable onComplete) {
        if (sessionCreated.get()) {
            onComplete.run();
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            synchronized (sessionCreated) {
                if (sessionCreated.get()) {
                    onComplete.run();
                    return;
                }
                
                try {
                    account.createAnonymousSession(new kotlin.coroutines.Continuation<io.appwrite.models.Session>() {
                        @Override
                        public kotlin.coroutines.CoroutineContext getContext() {
                            return kotlin.coroutines.EmptyCoroutineContext.INSTANCE;
                        }

                        @Override
                        public void resumeWith(Object result) {
                            try {
                                // Check if result indicates success or failure
                                String resultStr = result != null ? result.toString() : "null";
                                
                                // If result contains "Failure" or exception info, it's a failure
                                if (resultStr.contains("Failure") || resultStr.contains("Exception")) {
                                    Log.e(TAG, "Failed to create anonymous session: " + resultStr);
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        // Still try to proceed - session might work anyway
                                        sessionCreated.set(true);
                                        onComplete.run();
                                    });
                                } else {
                                    // Assume success - Appwrite SDK handles session storage
                                    sessionCreated.set(true);
                                    Log.d(TAG, "Anonymous session created successfully");
                                    onComplete.run();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing session result", e);
                                // Proceed anyway - session might still work
                                sessionCreated.set(true);
                                onComplete.run();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error creating anonymous session", e);
                    // Still try to proceed
                    sessionCreated.set(true);
                    onComplete.run();
                }
            }
        });
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
            
            ensureSessionCreated(() -> {
                CompletableFuture.runAsync(() -> {
                    try {
                        String fileId = java.util.UUID.randomUUID().toString();
                        String uploadUrl = ENDPOINT + "/storage/buckets/" + NOTES_BUCKET_ID + "/files";
                        
                        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                        URL url = new URL(uploadUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
                    
                    OutputStream outputStream = connection.getOutputStream();
                    java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(outputStream, "UTF-8"), true);
                    
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"fileId\"").append("\r\n");
                    writer.append("\r\n");
                    writer.append(fileId).append("\r\n");
                    writer.flush();
                    
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"").append("\r\n");
                    writer.append("Content-Type: application/octet-stream").append("\r\n");
                    writer.append("\r\n");
                    writer.flush();
                    
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    fileInputStream.close();
                    
                    writer.append("\r\n");
                    writer.append("--" + boundary + "--").append("\r\n");
                    writer.flush();
                    writer.close();
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream responseStream = connection.getInputStream();
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(responseStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        
                        String uploadedFileId = fileId;
                        String downloadUrl = ENDPOINT + "/storage/buckets/" + NOTES_BUCKET_ID + "/files/" + uploadedFileId + "/view?project=" + PROJECT_ID;
                        String storagePath = NOTES_BUCKET_ID + "/" + uploadedFileId;
                        
                        Log.d(TAG, "Upload successful - File ID: " + uploadedFileId);
                        Log.d(TAG, "Download URL: " + downloadUrl);
                        Log.d(TAG, "Storage path: " + storagePath);
                        
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onSuccess(downloadUrl, storagePath);
                        });
                    } else {
                        InputStream errorStream = connection.getErrorStream();
                        String errorMessage = "HTTP error code: " + responseCode;
                        if (errorStream != null) {
                            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                            StringBuilder error = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                error.append(line);
                            }
                            errorMessage = error.toString();
                            reader.close();
                        }
                        
                        // Check if it's a bucket not found error
                        if (errorMessage.contains("storage_bucket_not_found") || errorMessage.contains("could not be found")) {
                            Log.e(TAG, "Storage bucket '" + NOTES_BUCKET_ID + "' not found in Appwrite. Please create the bucket in Appwrite console.");
                            Log.e(TAG, "Go to: Storage -> Create Bucket -> Bucket ID: " + NOTES_BUCKET_ID);
                        }
                        
                        throw new Exception(errorMessage);
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Error during upload - Filename: " + filename, e);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onError("Upload error: " + e.getMessage());
                        });
                    }
                });
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
            
            ensureSessionCreated(() -> {
                CompletableFuture.runAsync(() -> {
                    try {
                        String fileId = java.util.UUID.randomUUID().toString();
                        String uploadUrl = ENDPOINT + "/storage/buckets/" + THUMBNAILS_BUCKET_ID + "/files";
                        
                        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                        URL url = new URL(uploadUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
                    
                    OutputStream outputStream = connection.getOutputStream();
                    java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(outputStream, "UTF-8"), true);
                    
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"fileId\"").append("\r\n");
                    writer.append("\r\n");
                    writer.append(fileId).append("\r\n");
                    writer.flush();
                    
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"").append("\r\n");
                    writer.append("Content-Type: application/octet-stream").append("\r\n");
                    writer.append("\r\n");
                    writer.flush();
                    
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    fileInputStream.close();
                    
                    writer.append("\r\n");
                    writer.append("--" + boundary + "--").append("\r\n");
                    writer.flush();
                    writer.close();
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream responseStream = connection.getInputStream();
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(responseStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        
                        String uploadedFileId = fileId;
                        String downloadUrl = ENDPOINT + "/storage/buckets/" + THUMBNAILS_BUCKET_ID + "/files/" + uploadedFileId + "/view?project=" + PROJECT_ID;
                        String storagePath = THUMBNAILS_BUCKET_ID + "/" + uploadedFileId;
                        
                        Log.d(TAG, "Thumbnail upload successful - File ID: " + uploadedFileId);
                        Log.d(TAG, "Thumbnail download URL: " + downloadUrl);
                        
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onSuccess(downloadUrl, storagePath);
                        });
                    } else {
                        InputStream errorStream = connection.getErrorStream();
                        String errorMessage = "HTTP error code: " + responseCode;
                        if (errorStream != null) {
                            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                            StringBuilder error = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                error.append(line);
                            }
                            errorMessage = error.toString();
                            reader.close();
                        }
                        
                        // Check if it's a bucket not found error
                        if (errorMessage.contains("storage_bucket_not_found") || errorMessage.contains("could not be found")) {
                            Log.e(TAG, "Storage bucket '" + THUMBNAILS_BUCKET_ID + "' not found in Appwrite. Please create the bucket in Appwrite console.");
                            Log.e(TAG, "Go to: Storage -> Create Bucket -> Bucket ID: " + THUMBNAILS_BUCKET_ID);
                        }
                        
                        throw new Exception(errorMessage);
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Error during thumbnail upload - Filename: " + filename, e);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onError("Thumbnail upload error: " + e.getMessage());
                        });
                    }
                });
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
        
        String downloadUrl = ENDPOINT + "/storage/buckets/" + bucketId + "/files/" + fileId + "/download?project=" + PROJECT_ID;
        Log.d(TAG, "Download URL: " + downloadUrl);
        
        CompletableFuture.runAsync(() -> {
            try {
                java.net.URL url = new java.net.URL(downloadUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                
                if (connection.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP error code: " + connection.getResponseCode());
                }
                
                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[8192];
                java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                byte[] fileBytes = byteArrayOutputStream.toByteArray();
                inputStream.close();
                byteArrayOutputStream.close();
                connection.disconnect();
                
                Log.d(TAG, "Download successful - Bytes downloaded: " + fileBytes.length + ", File ID: " + fileId);
                
                java.io.File file = new java.io.File(downloadsDir, localFilename);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(fileBytes);
                fileOutputStream.close();
                
                Log.d(TAG, "File saved to: " + file.getAbsolutePath());
                
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(file.getAbsolutePath());
                });
            } catch (Exception e) {
                Log.e(TAG, "Download failed - Storage path: " + storagePath + ", Error: " + e.getMessage(), e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Download failed: " + e.getMessage());
                });
            }
        });
    }
}

