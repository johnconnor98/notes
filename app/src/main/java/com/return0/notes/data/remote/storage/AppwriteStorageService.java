package com.return0.notes.data.remote.storage;

import android.content.Context;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class AppwriteStorageService implements StorageService {
    private static final String TAG = "AppwriteStorageService";
    private static AppwriteStorageService instance;
    private static final String NOTES_BUCKET_ID = "notes";
    private static final String THUMBNAILS_BUCKET_ID = "thumbnails";
    
    private static final String ENDPOINT = "https://fra.cloud.appwrite.io/v1";
    private static final String PROJECT_ID = "6922970a001ab5faef56";
    private static final String API_KEY = "standard_c50102f470efafa6715e49beab3390411ad890f3f9e5fed0efb299054a373a95b21e7d44158f900d89876e2a796910ea1653624350a9c73b072498605c0f1794f6c348a30471f968d9886e4d1821aabc0ac91ccea5bd1101d65da3af26fbe10e31229a2b19f437af22230583301353fd166783a783a90f11c91f5d0c4cae2a72";

    private Context context;
    
    private AppwriteStorageService(Context context) {
        this.context = context.getApplicationContext();
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
                    String fileId = java.util.UUID.randomUUID().toString();
                    String uploadUrl = ENDPOINT + "/storage/buckets/" + NOTES_BUCKET_ID + "/files";
                    
                    String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                    URL url = new URL(uploadUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
                    connection.setRequestProperty("X-Appwrite-Key", API_KEY);
                    
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
                    String fileId = java.util.UUID.randomUUID().toString();
                    String uploadUrl = ENDPOINT + "/storage/buckets/" + THUMBNAILS_BUCKET_ID + "/files";
                    
                    String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                    URL url = new URL(uploadUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
                    connection.setRequestProperty("X-Appwrite-Key", API_KEY);
                    
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

