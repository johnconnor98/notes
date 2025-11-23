package com.return0.notes.data.remote.database;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.return0.notes.data.remote.dto.NoteDto;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Appwrite implementation of DatabaseService.
 * This can be easily replaced with another implementation (Firebase, REST API, etc.)
 * Follows Single Responsibility Principle - only handles Appwrite database operations.
 */
public class AppwriteDatabaseService implements DatabaseService {
    private static final String TAG = "AppwriteDatabaseService";
    
    private static final String ENDPOINT = "https://fra.cloud.appwrite.io/v1";
    private static final String PROJECT_ID = "6922970a001ab5faef56";
    private static final String DATABASE_ID = "69231061001de8342953";
    private static final String COLLECTION_ID = "notes_list";
    
    private final Gson gson = new Gson();
    
    public AppwriteDatabaseService() {
        Log.d(TAG, "AppwriteDatabaseService initialized");
        Log.d(TAG, "Endpoint: " + ENDPOINT);
        Log.d(TAG, "Database ID: " + DATABASE_ID);
        Log.d(TAG, "Collection ID: " + COLLECTION_ID);
    }
    
    @Override
    public void loadAllNotes(DatabaseCallback<List<NoteDto>> callback) {
        searchNotes(null, null, null, null, callback);
    }
    
    @Override
    public void searchNotes(String subject, String semester, String branch, String college,
                           DatabaseCallback<List<NoteDto>> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = buildSearchUrl(subject, semester, branch, college);
                Log.d(TAG, "Database URL: " + url);
                
                HttpURLConnection connection = createConnection(url, "GET");
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonResponse = readResponse(connection);
                    Log.d(TAG, "Raw JSON response: " + jsonResponse);
                    
                    List<NoteDto> notes = parseNotesResponse(jsonResponse);
                    Log.d(TAG, "Parsed " + notes.size() + " documents");
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(notes);
                    });
                } else {
                    String error = readErrorResponse(connection, responseCode);
                    Log.e(TAG, "Database error: " + error);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError(error);
                    });
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error loading notes", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Error: " + e.getMessage());
                });
            }
        });
    }
    
    @Override
    public void getNoteById(String noteId, DatabaseCallback<NoteDto> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = ENDPOINT + "/databases/" + DATABASE_ID + "/collections/" + 
                            COLLECTION_ID + "/documents/" + noteId + "?project=" + PROJECT_ID;
                
                HttpURLConnection connection = createConnection(url, "GET");
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonResponse = readResponse(connection);
                    Log.d(TAG, "Document retrieved: " + jsonResponse);
                    
                    NoteDto noteDto = parseNoteResponse(jsonResponse);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(noteDto);
                    });
                } else {
                    String error = readErrorResponse(connection, responseCode);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError(error);
                    });
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting note", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Error: " + e.getMessage());
                });
            }
        });
    }
    
    @Override
    public void createNote(String title, String subject, String semester, String branch,
                          String college, String filePath, String thumbnailPath,
                          DatabaseCallback<NoteDto> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String documentId = UUID.randomUUID().toString();
                String url = ENDPOINT + "/databases/" + DATABASE_ID + "/collections/" + 
                            COLLECTION_ID + "/documents?project=" + PROJECT_ID;
                
                Log.d(TAG, "Creating document: " + url);
                
                JsonObject data = buildNoteData(title, subject, semester, branch, college, filePath, thumbnailPath);
                String dataJson = gson.toJson(data);
                Log.d(TAG, "Document data: " + dataJson);
                
                String formData = "documentId=" + URLEncoder.encode(documentId, "UTF-8") + 
                                "&data=" + URLEncoder.encode(dataJson, "UTF-8");
                
                HttpURLConnection connection = createConnection(url, "POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setDoOutput(true);
                
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(formData.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Create Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonResponse = readResponse(connection);
                    Log.d(TAG, "Document created: " + jsonResponse);
                    
                    NoteDto noteDto = parseNoteResponse(jsonResponse);
                    noteDto.setFilePath(filePath != null ? filePath : "");
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(noteDto);
                    });
                } else {
                    String error = readErrorResponse(connection, responseCode);
                    Log.e(TAG, "Failed to create document: " + error);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError(error);
                    });
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error creating note", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Error: " + e.getMessage());
                });
            }
        });
    }
    
    // Helper methods - following Single Responsibility Principle
    
    private String buildSearchUrl(String subject, String semester, String branch, String college) {
        String url = ENDPOINT + "/databases/" + DATABASE_ID + "/collections/" + COLLECTION_ID + "/documents";
        boolean hasQuery = false;
        
        List<String> queryStrings = new ArrayList<>();
        if (subject != null && !subject.isEmpty()) {
            queryStrings.add("search(\"subject\",\"" + escapeJsonString(subject) + "\")");
        }
        if (semester != null && !semester.isEmpty()) {
            queryStrings.add("search(\"semester\",\"" + escapeJsonString(semester) + "\")");
        }
        if (branch != null && !branch.isEmpty()) {
            queryStrings.add("search(\"branch\",\"" + escapeJsonString(branch) + "\")");
        }
        if (college != null && !college.isEmpty()) {
            queryStrings.add("search(\"college\",\"" + escapeJsonString(college) + "\")");
        }
        
        if (!queryStrings.isEmpty()) {
            StringBuilder queriesJson = new StringBuilder("[");
            for (int i = 0; i < queryStrings.size(); i++) {
                if (i > 0) queriesJson.append(",");
                queriesJson.append("\"").append(queryStrings.get(i)).append("\"");
            }
            queriesJson.append("]");
            
            String encodedQueries = URLEncoder.encode(queriesJson.toString(), "UTF-8");
            url += "?queries=" + encodedQueries;
            hasQuery = true;
        }
        
        url += (hasQuery ? "&" : "?") + "project=" + PROJECT_ID;
        return url;
    }
    
    private HttpURLConnection createConnection(String urlString, String method) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
        return connection;
    }
    
    private String readResponse(HttpURLConnection connection) throws Exception {
        InputStream inputStream = connection.getInputStream();
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        inputStream.close();
        return response.toString();
    }
    
    private String readErrorResponse(HttpURLConnection connection, int responseCode) {
        try {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(errorStream));
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line);
                }
                reader.close();
                return "HTTP error code: " + responseCode + " - " + error.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading error response", e);
        }
        return "HTTP error code: " + responseCode;
    }
    
    private JsonObject buildNoteData(String title, String subject, String semester, 
                                    String branch, String college, String filePath, String thumbnailPath) {
        JsonObject data = new JsonObject();
        data.addProperty("title", title);
        data.addProperty("subject", subject != null ? subject : "");
        data.addProperty("semester", semester != null ? semester : "");
        data.addProperty("branch", branch != null ? branch : "");
        data.addProperty("college", college != null ? college : "");
        data.addProperty("filePath", filePath != null ? filePath : "");
        data.addProperty("thumbnailPath", thumbnailPath != null ? thumbnailPath : "");
        return data;
    }
    
    private List<NoteDto> parseNotesResponse(String jsonResponse) {
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray documentsArray = jsonObject.getAsJsonArray("documents");
        
        List<NoteDto> notes = new ArrayList<>();
        if (documentsArray != null) {
            for (JsonElement element : documentsArray) {
                JsonObject doc = element.getAsJsonObject();
                notes.add(parseNoteFromJson(doc));
            }
        }
        return notes;
    }
    
    private NoteDto parseNoteResponse(String jsonResponse) {
        JsonObject doc = JsonParser.parseString(jsonResponse).getAsJsonObject();
        return parseNoteFromJson(doc);
    }
    
    private NoteDto parseNoteFromJson(JsonObject doc) {
        NoteDto noteDto = new NoteDto();
        noteDto.setId(doc.has("$id") ? doc.get("$id").getAsString() : "");
        noteDto.setNotesid(doc.has("$id") ? doc.get("$id").getAsString() : "");
        noteDto.setTitle(doc.has("title") ? doc.get("title").getAsString() : "");
        noteDto.setSubject(doc.has("subject") ? doc.get("subject").getAsString() : "");
        noteDto.setSemester(doc.has("semester") ? doc.get("semester").getAsString() : "");
        noteDto.setBranch(doc.has("branch") ? doc.get("branch").getAsString() : "");
        noteDto.setCollege(doc.has("college") ? doc.get("college").getAsString() : "");
        
        if (doc.has("filePath") && !doc.get("filePath").isJsonNull()) {
            noteDto.setFilePath(doc.get("filePath").getAsString());
        } else {
            noteDto.setFilePath("");
        }
        
        if (doc.has("thumbnailPath") && !doc.get("thumbnailPath").isJsonNull()) {
            String thumbPath = doc.get("thumbnailPath").getAsString();
            noteDto.setThumbnailPath(thumbPath);
            noteDto.setThumbnailUrl(thumbPath);
        } else {
            noteDto.setThumbnailPath("");
            noteDto.setThumbnailUrl("");
        }
        
        return noteDto;
    }
    
    private String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}

