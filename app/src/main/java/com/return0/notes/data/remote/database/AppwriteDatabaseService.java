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
    
    private static final String ENDPOINT = "https://cloud.appwrite.io/v1";
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
                Log.d(TAG, "=== SEARCH NOTES DEBUG ===");
                Log.d(TAG, "Input parameters:");
                Log.d(TAG, "  Subject: " + (subject != null ? subject : "null"));
                Log.d(TAG, "  Semester: " + (semester != null ? semester : "null"));
                Log.d(TAG, "  Branch: " + (branch != null ? branch : "null"));
                Log.d(TAG, "  College: " + (college != null ? college : "null"));
                
                // Build base URL without query parameters first
                String baseUrl = ENDPOINT + "/databases/" + DATABASE_ID + "/collections/" + COLLECTION_ID + "/documents";
                Log.d(TAG, "Base URL: " + baseUrl);
                
                // Build queries array - Appwrite REST API format
                List<String> queryStrings = new ArrayList<>();
                Log.d(TAG, "Generating queries in Appwrite format...");
                
                if (subject != null && !subject.isEmpty()) {
                    String query = "equal(\"subject\",\"" + escapeJsonString(subject) + "\")";
                    queryStrings.add(query);
                    Log.d(TAG, "  ✓ Subject query: " + query);
                }
                if (semester != null && !semester.isEmpty()) {
                    String query = "equal(\"semester\",\"" + escapeJsonString(semester) + "\")";
                    queryStrings.add(query);
                    Log.d(TAG, "  ✓ Semester query: " + query);
                }
                if (branch != null && !branch.isEmpty()) {
                    String query = "equal(\"branch\",\"" + escapeJsonString(branch) + "\")";
                    queryStrings.add(query);
                    Log.d(TAG, "  ✓ Branch query: " + query);
                }
                if (college != null && !college.isEmpty()) {
                    String query = "equal(\"college\",\"" + escapeJsonString(college) + "\")";
                    queryStrings.add(query);
                    Log.d(TAG, "  ✓ College query: " + query);
                }
                
                // Build URL with queries parameter
                String finalUrl = baseUrl + "?project=" + PROJECT_ID;
                
                if (!queryStrings.isEmpty()) {
                    JsonArray queriesArray = new JsonArray();
                    for (String q : queryStrings) {
                        queriesArray.add(q);
                    }
                    String queriesJson = gson.toJson(queriesArray);
                    
                    // Important: URLEncoder.encode the JSON string for query parameter
                    try {
                        String encodedQueries = URLEncoder.encode(queriesJson, "UTF-8");
                        finalUrl += "&queries=" + encodedQueries;
                        Log.d(TAG, "  Added encoded queries: " + encodedQueries);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to encode queries", e);
                    }
                }
                
                Log.d(TAG, "Creating HTTP connection...");
                HttpURLConnection connection = createConnection(finalUrl, "GET");
                Log.d(TAG, "Connecting to: " + finalUrl);
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "=== HTTP RESPONSE ===");
                Log.d(TAG, "Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "✓ Request successful (200 OK)");
                    String jsonResponse = readResponse(connection);
                    
                    List<NoteDto> notes = parseNotesResponse(jsonResponse);
                    Log.d(TAG, "✓ Parsed " + notes.size() + " documents successfully");
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(notes);
                    });
                } else {
                    Log.e(TAG, "✗ Request failed with code: " + responseCode);
                    String error = readErrorResponse(connection, responseCode);
                    Log.e(TAG, "Error response body: " + error);
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError(error);
                    });
                }
                connection.disconnect();
                Log.d(TAG, "=== END SEARCH NOTES DEBUG ===");
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
                
                JsonObject payload = new JsonObject();
                payload.addProperty("documentId", documentId);
                payload.add("data", data);
                
                String jsonPayload = gson.toJson(payload);
                
                HttpURLConnection connection = createConnection(url, "POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(jsonPayload.getBytes("UTF-8"));
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
    
    private HttpURLConnection createConnection(String urlString, String method) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
        connection.setRequestProperty("X-Appwrite-Response-Format", "1.4.0");
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
