package com.return0.notes.data.repository;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.return0.notes.data.mapper.NoteMapper;
import com.return0.notes.data.remote.storage.StorageService;
import com.return0.notes.data.remote.storage.AppwriteStorageService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.return0.notes.data.remote.dto.NoteDto;
import com.return0.notes.domain.model.Note;
import com.return0.notes.domain.model.SearchFilters;
import com.return0.notes.domain.repository.NotesRepository;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class NotesRepositoryImpl implements NotesRepository {
    private static final String TAG = "NotesRepository";
    private static final String ENDPOINT = "https://fra.cloud.appwrite.io/v1";
    private static final String PROJECT_ID = "6922970a001ab5faef56";
    private static final String DATABASE_ID = "69231061001de8342953";
    private static final String COLLECTION_ID = "notes_list";
    
    private final Context context;
    private final StorageService storageService;
    private final Gson gson = new Gson();

    public NotesRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.storageService = AppwriteStorageService.getInstance(context);
    }

    @Override
    public void loadNotes(LoadNotesCallback callback) {
        Log.d(TAG, "=== LOADING ALL NOTES FROM APPWRITE DATABASE ===");
        loadNotesFromAppwrite(null, null, null, null, callback);
    }

    @Override
    public void searchNotes(SearchFilters filters, LoadNotesCallback callback) {
        Map<String, String> filterMap = filters.getFilters();
        String subject = filterMap.get("subject");
        String semester = filterMap.get("semester");
        String branch = filterMap.get("branch");
        String college = filterMap.get("college");
        
        Log.d(TAG, "=== SEARCH REQUEST ===");
        Log.d(TAG, "Subject: " + (subject != null ? subject : "null"));
        Log.d(TAG, "Semester: " + (semester != null ? semester : "null"));
        Log.d(TAG, "Branch: " + (branch != null ? branch : "null"));
        Log.d(TAG, "College: " + (college != null ? college : "null"));
        
        loadNotesFromAppwrite(subject, semester, branch, college, callback);
    }
    
    private void loadNotesFromAppwrite(String subject, String semester, String branch, String college, LoadNotesCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Build query parameters
                List<String> queries = new ArrayList<>();
                if (subject != null && !subject.isEmpty()) {
                    queries.add("equal(\"subject\",\"" + subject + "\")");
                }
                if (semester != null && !semester.isEmpty()) {
                    queries.add("equal(\"semester\",\"" + semester + "\")");
                }
                if (branch != null && !branch.isEmpty()) {
                    queries.add("equal(\"branch\",\"" + branch + "\")");
                }
                if (college != null && !college.isEmpty()) {
                    queries.add("equal(\"college\",\"" + college + "\")");
                }
                
                String queryParam = String.join(",", queries);
                String url = ENDPOINT + "/databases/" + DATABASE_ID + "/collections/" + COLLECTION_ID + "/documents";
                boolean hasQuery = false;
                if (!queryParam.isEmpty()) {
                    url += "?queries=[" + queryParam + "]";
                    hasQuery = true;
                }
                // Add project parameter - use ? if no query params, & if query params exist
                url += (hasQuery ? "&" : "?") + "project=" + PROJECT_ID;
                
                Log.d(TAG, "Appwrite Database URL: " + url);
                
                java.net.URL appwriteUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) appwriteUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Appwrite Database Response Code: " + responseCode);
                
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    inputStream.close();
                    
                    String jsonResponse = response.toString();
                    Log.d(TAG, "Raw JSON response from Appwrite: " + jsonResponse);
                    
                    // Parse JSON response
                    JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
                    JsonArray documentsArray = jsonObject.getAsJsonArray("documents");
                    
                    List<NoteDto> noteDtos = new ArrayList<>();
                    if (documentsArray != null) {
                        for (JsonElement element : documentsArray) {
                            JsonObject doc = element.getAsJsonObject();
                            NoteDto noteDto = new NoteDto();
                            noteDto.setId(doc.has("$id") ? doc.get("$id").getAsString() : "");
                            noteDto.setNotesid(doc.has("notesid") ? doc.get("notesid").getAsString() : "");
                            noteDto.setTitle(doc.has("title") ? doc.get("title").getAsString() : "");
                            noteDto.setSubject(doc.has("subject") ? doc.get("subject").getAsString() : "");
                            noteDto.setSemester(doc.has("semester") ? doc.get("semester").getAsString() : "");
                            noteDto.setBranch(doc.has("branch") ? doc.get("branch").getAsString() : "");
                            noteDto.setCollege(doc.has("college") ? doc.get("college").getAsString() : "");
                            noteDto.setFilePath(doc.has("filePath") ? doc.get("filePath").getAsString() : "");
                            noteDto.setThumbnailPath(doc.has("thumbnailPath") ? doc.get("thumbnailPath").getAsString() : "");
                            noteDto.setThumbnailUrl(doc.has("thumbnailPath") ? doc.get("thumbnailPath").getAsString() : "");
                            noteDtos.add(noteDto);
                        }
                    }
                    
                    Log.d(TAG, "Parsed " + noteDtos.size() + " documents from Appwrite");
                    logAllDatabaseData(noteDtos, "Appwrite Database Results");
                    
                    List<Note> notes = NoteMapper.toDomainList(noteDtos);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(notes);
                    });
                } else {
                    InputStream errorStream = connection.getErrorStream();
                    final String errorMessage;
                    if (errorStream != null) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                        StringBuilder error = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            error.append(line);
                        }
                        errorMessage = "HTTP error code: " + responseCode + " - " + error.toString();
                        reader.close();
                    } else {
                        errorMessage = "HTTP error code: " + responseCode;
                    }
                    Log.e(TAG, "Appwrite Database error: " + errorMessage);
                    final String finalErrorMessage = errorMessage;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Database error: " + finalErrorMessage);
                    });
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error loading notes from Appwrite Database", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Error: " + e.getMessage());
                });
            }
        });
    }
    
    private void logAllDatabaseData(List<NoteDto> notes, String context) {
        Log.d(TAG, "==================================================================================");
        Log.d(TAG, "ALL DATABASE DATA - " + context);
        Log.d(TAG, "==================================================================================");
        Log.d(TAG, "Total records: " + notes.size());
        Log.d(TAG, "----------------------------------------------------------------------------------");
        
        if (notes.isEmpty()) {
            Log.w(TAG, "⚠️ NO RECORDS FOUND IN DATABASE!");
            Log.d(TAG, "==================================================================================");
            return;
        }
        
        for (int i = 0; i < notes.size(); i++) {
            NoteDto note = notes.get(i);
            Log.d(TAG, "Record #" + (i + 1) + ":");
            Log.d(TAG, "  ID: " + (note.getId() != null ? note.getId() : "NULL"));
            Log.d(TAG, "  NotesID: " + (note.getNotesid() != null ? note.getNotesid() : "NULL"));
            Log.d(TAG, "  Title: " + (note.getTitle() != null ? note.getTitle() : "NULL"));
            Log.d(TAG, "  Subject: " + (note.getSubject() != null && !note.getSubject().isEmpty() ? note.getSubject() : "NULL/EMPTY"));
            Log.d(TAG, "  Semester: " + (note.getSemester() != null && !note.getSemester().isEmpty() ? note.getSemester() : "NULL/EMPTY"));
            Log.d(TAG, "  Branch: " + (note.getBranch() != null && !note.getBranch().isEmpty() ? note.getBranch() : "NULL/EMPTY"));
            Log.d(TAG, "  College: " + (note.getCollege() != null && !note.getCollege().isEmpty() ? note.getCollege() : "NULL/EMPTY"));
            Log.d(TAG, "----------------------------------------------------------------------------------");
        }
        
        Log.d(TAG, "==================================================================================");
    }

    @Override
    public void uploadNote(String title, String subject, String semester, String branch, 
                          String college, String filePath, String thumbnailPath, UploadCallback callback) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                callback.onError("File not found");
                return;
            }

            String filename = file.getName();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String storageFilename = timestamp + "_" + filename;

            storageService.uploadFile(file, storageFilename, new StorageService.UploadCallback() {
                @Override
                public void onSuccess(String fileDownloadUrl, String fileStoragePath) {
                    if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                        File thumbFile = new File(thumbnailPath);
                        if (thumbFile.exists()) {
                            String thumbFilename = timestamp + "_" + thumbFile.getName();
                            storageService.uploadThumbnail(thumbFile, thumbFilename, new StorageService.UploadCallback() {
                                @Override
                                public void onSuccess(String thumbDownloadUrl, String thumbStoragePath) {
                                    sendMetadataToBackend(title, subject, semester, branch, college, 
                                            fileDownloadUrl, fileStoragePath, thumbDownloadUrl, thumbStoragePath, filename, callback);
                                }

                                @Override
                                public void onProgress(double progress) {}

                                @Override
                                public void onError(String error) {
                                    sendMetadataToBackend(title, subject, semester, branch, college, 
                                            fileDownloadUrl, fileStoragePath, null, null, filename, callback);
                                }
                            });
                        } else {
                            sendMetadataToBackend(title, subject, semester, branch, college, 
                                    fileDownloadUrl, fileStoragePath, null, null, filename, callback);
                        }
                    } else {
                        sendMetadataToBackend(title, subject, semester, branch, college, 
                                fileDownloadUrl, fileStoragePath, null, null, filename, callback);
                    }
                }

                @Override
                public void onProgress(double progress) {}

                @Override
                public void onError(String error) {
                    callback.onError("Storage upload failed: " + error);
                }
            });
        } catch (Exception e) {
            callback.onError("Error preparing upload: " + e.getMessage());
        }
    }

    private void sendMetadataToBackend(String title, String subject, String semester, String branch, 
                                      String college, String fileUrl, String filePath, String thumbUrl, 
                                      String thumbPath, String originalFilename, UploadCallback callback) {
        Log.d(TAG, "=== SENDING DATA TO APPWRITE DATABASE ===");
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Subject: " + (subject != null ? subject : "NULL"));
        Log.d(TAG, "Semester: " + (semester != null ? semester : "NULL"));
        Log.d(TAG, "Branch: " + (branch != null ? branch : "NULL"));
        Log.d(TAG, "College: " + (college != null ? college : "NULL"));
        Log.d(TAG, "File Path: " + filePath);
        Log.d(TAG, "Thumbnail Path: " + thumbPath);
        Log.d(TAG, "=================================");
        
        CompletableFuture.runAsync(() -> {
            try {
                String documentId = java.util.UUID.randomUUID().toString();
                // Appwrite Database API format: /v1/databases/{databaseId}/collections/{collectionId}/documents
                String url = ENDPOINT + "/databases/" + DATABASE_ID + "/collections/" + COLLECTION_ID + "/documents";
                
                Log.d(TAG, "Creating document in Appwrite Database");
                Log.d(TAG, "URL: " + url);
                Log.d(TAG, "Database ID: " + DATABASE_ID);
                Log.d(TAG, "Collection ID: " + COLLECTION_ID);
                Log.d(TAG, "Project ID: " + PROJECT_ID);
                Log.d(TAG, "Document ID: " + documentId);
                
                // Build data object
                // Note: Appwrite automatically provides "$id" as the document ID
                // Only include attributes that exist in your Appwrite collection schema
                JsonObject data = new JsonObject();
                data.addProperty("title", title);
                data.addProperty("subject", subject != null ? subject : "");
                data.addProperty("semester", semester != null ? semester : "");
                data.addProperty("branch", branch != null ? branch : "");
                data.addProperty("college", college != null ? college : "");
                // Note: filePath and thumbnailPath are not in the collection schema
                // If you need them, add them as attributes in Appwrite Console first
                
                // Appwrite expects form-urlencoded with "documentId" and "data" parameters
                String dataJson = gson.toJson(data);
                Log.d(TAG, "Document data JSON: " + dataJson);
                
                // Appwrite expects: documentId and data as form fields, project as query param or header
                String formData = "documentId=" + java.net.URLEncoder.encode(documentId, "UTF-8") + 
                                 "&data=" + java.net.URLEncoder.encode(dataJson, "UTF-8");
                
                // Add project as query parameter
                String urlWithProject = url + "?project=" + PROJECT_ID;
                
                java.net.URL appwriteUrl = new java.net.URL(urlWithProject);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) appwriteUrl.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
                connection.setRequestProperty("X-Appwrite-Response-Format", "1.0.0");
                connection.setDoOutput(true);
                
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(formData.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Appwrite Database Create Response Code: " + responseCode);
                
                if (responseCode == java.net.HttpURLConnection.HTTP_CREATED || responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    inputStream.close();
                    
                    String jsonResponse = response.toString();
                    Log.d(TAG, "Document created - Response: " + jsonResponse);
                    
                    // Parse response
                    JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
                    NoteDto noteDto = new NoteDto();
                    String docId = responseObj.has("$id") ? responseObj.get("$id").getAsString() : documentId;
                    noteDto.setId(docId);
                    noteDto.setNotesid(docId); // Use document ID as notesid
                    noteDto.setTitle(responseObj.has("title") ? responseObj.get("title").getAsString() : title);
                    noteDto.setSubject(responseObj.has("subject") ? responseObj.get("subject").getAsString() : (subject != null ? subject : ""));
                    noteDto.setSemester(responseObj.has("semester") ? responseObj.get("semester").getAsString() : (semester != null ? semester : ""));
                    noteDto.setBranch(responseObj.has("branch") ? responseObj.get("branch").getAsString() : (branch != null ? branch : ""));
                    noteDto.setCollege(responseObj.has("college") ? responseObj.get("college").getAsString() : (college != null ? college : ""));
                    // Store filePath from the upload result (not from database response)
                    noteDto.setFilePath(filePath != null ? filePath : "");
                    
                    Log.d(TAG, "✓ Saved to Appwrite Database:");
                    Log.d(TAG, "  ID: " + noteDto.getId());
                    Log.d(TAG, "  NotesID: " + noteDto.getNotesid());
                    Log.d(TAG, "  Title: " + noteDto.getTitle());
                    Log.d(TAG, "  Subject: " + noteDto.getSubject());
                    Log.d(TAG, "  Semester: " + noteDto.getSemester());
                    Log.d(TAG, "  Branch: " + noteDto.getBranch());
                    Log.d(TAG, "  College: " + noteDto.getCollege());
                    
                    Note note = NoteMapper.toDomain(noteDto);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(note);
                    });
                } else {
                    InputStream errorStream = connection.getErrorStream();
                    final String errorMessage;
                    if (errorStream != null) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                        StringBuilder error = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            error.append(line);
                        }
                        errorMessage = "HTTP error code: " + responseCode + " - " + error.toString();
                        reader.close();
                        
                        // Log detailed error information
                        Log.e(TAG, "Failed to create document in Appwrite Database: " + errorMessage);
                        Log.e(TAG, "Attempted to send attributes: title, subject, semester, branch, college");
                        Log.e(TAG, "If you see 'Unknown attribute' errors, please add these attributes in Appwrite Console:");
                        Log.e(TAG, "  Go to: Database -> Your Collection -> Attributes -> Create Attribute");
                        Log.e(TAG, "  Required attributes: title (String), subject (String), semester (String), branch (String), college (String)");
                    } else {
                        errorMessage = "HTTP error code: " + responseCode;
                    }
                    final String finalErrorMessage = errorMessage;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Database save failed: " + finalErrorMessage);
                    });
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error saving to Appwrite Database", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Error: " + e.getMessage());
                });
            }
        });
    }

    @Override
    public void downloadNote(String noteId, String filename, DownloadCallback callback) {
        Log.d(TAG, "Downloading note from Appwrite - Note ID: " + noteId);
        
        // First, get the document from Appwrite Database to get filePath
        CompletableFuture.runAsync(() -> {
            try {
                String url = ENDPOINT + "/databases/" + DATABASE_ID + "/collections/" + COLLECTION_ID + "/documents/" + noteId + "?project=" + PROJECT_ID;
                
                java.net.URL appwriteUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) appwriteUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    inputStream.close();
                    
                    String jsonResponse = response.toString();
                    Log.d(TAG, "Document retrieved: " + jsonResponse);
                    
                    // Parse document to get filePath
                    JsonObject doc = JsonParser.parseString(jsonResponse).getAsJsonObject();
                    String filePath = doc.has("filePath") ? doc.get("filePath").getAsString() : null;
                    
                    if (filePath != null && !filePath.isEmpty()) {
                        // Download from Appwrite Storage
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        storageService.downloadFile(filePath, filename, downloadsDir, new StorageService.DownloadCallback() {
                            @Override
                            public void onSuccess(String localFilePath) {
                                callback.onSuccess(localFilePath);
                            }

                            @Override
                            public void onProgress(double progress) {}

                            @Override
                            public void onError(String error) {
                                callback.onError("Download failed: " + error);
                            }
                        });
                    } else {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onError("File path not found in document");
                        });
                    }
                } else {
                    InputStream errorStream = connection.getErrorStream();
                    final String errorMessage;
                    if (errorStream != null) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                        StringBuilder error = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            error.append(line);
                        }
                        errorMessage = "HTTP error code: " + responseCode + " - " + error.toString();
                        reader.close();
                    } else {
                        errorMessage = "HTTP error code: " + responseCode;
                    }
                    final String finalErrorMessage = errorMessage;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Failed to get document: " + finalErrorMessage);
                    });
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error downloading note", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Error: " + e.getMessage());
                });
            }
        });
    }

    @Override
    public void downloadNote(Note note, DownloadCallback callback) {
        String storagePath = extractStoragePathFromNote(note);
        if (storagePath != null && !storagePath.isEmpty()) {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            storageService.downloadFile(storagePath, note.getFilename(), downloadsDir, new StorageService.DownloadCallback() {
                @Override
                public void onSuccess(String filePath) {
                    callback.onSuccess(filePath);
                }

                @Override
                public void onProgress(double progress) {}

                @Override
                public void onError(String error) {
                    callback.onError("Storage download failed: " + error);
                }
            });
        } else {
            downloadNote(note.getId(), note.getFilename(), callback);
        }
    }

    private String extractStoragePathFromNote(Note note) {
        if (note.getFilePath() != null && !note.getFilePath().isEmpty()) {
            return note.getFilePath();
        }
        return null;
    }

    private void downloadFromStorage(String jsonResponse, String filename, DownloadCallback callback) {
        try {
            String storagePath = extractStoragePath(jsonResponse);
            if (storagePath != null && !storagePath.isEmpty()) {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                storageService.downloadFile(storagePath, filename, downloadsDir, new StorageService.DownloadCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        callback.onSuccess(filePath);
                    }

                    @Override
                    public void onProgress(double progress) {}

                    @Override
                    public void onError(String error) {
                        callback.onError("Storage download failed: " + error);
                    }
                });
            } else {
                callback.onError("Storage path not found");
            }
        } catch (Exception e) {
            callback.onError("Error extracting storage path: " + e.getMessage());
        }
    }

    private String extractStoragePath(String jsonResponse) {
        int pathIndex = jsonResponse.indexOf("\"file_path\":\"");
        if (pathIndex != -1) {
            int start = pathIndex + 13;
            int end = jsonResponse.indexOf("\"", start);
            if (end != -1) {
                return jsonResponse.substring(start, end);
            }
        }
        int urlIndex = jsonResponse.indexOf("\"file_url\":\"");
        if (urlIndex != -1) {
            int start = urlIndex + 12;
            int end = jsonResponse.indexOf("\"", start);
            if (end != -1) {
                return jsonResponse.substring(start, end);
            }
        }
        return null;
    }

    private void saveFile(ResponseBody body, String filename, DownloadCallback callback) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, filename);

            InputStream inputStream = body.byteStream();
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            callback.onSuccess(file.getAbsolutePath());
        } catch (Exception e) {
            callback.onError("Error saving file: " + e.getMessage());
        }
    }
}

