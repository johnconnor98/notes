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
                // Format: equal("field","value") - this is the exact format Appwrite expects
                // Based on Appwrite documentation, Query.equal() returns strings in this format
                List<String> queryStrings = new ArrayList<>();
                Log.d(TAG, "Generating queries in Appwrite format...");
                
                // Build query strings manually in the format: equal("field","value")
                // This matches what Query.equal() would return
                if (subject != null && !subject.isEmpty()) {
                    // Format: equal("subject","value") - proper JSON escaping for the value
                    String query = "equal(\"subject\",\"" + escapeJsonString(subject) + "\")";
                    queryStrings.add(query);
                    Log.d(TAG, "  ✓ Subject query: " + query);
                    Log.d(TAG, "    Query length: " + query.length() + " chars");
                }
                if (semester != null && !semester.isEmpty()) {
                    String query = "equal(\"semester\",\"" + escapeJsonString(semester) + "\")";
                    queryStrings.add(query);
                    Log.d(TAG, "  ✓ Semester query: " + query);
                    Log.d(TAG, "    Query length: " + query.length() + " chars");
                }
                if (branch != null && !branch.isEmpty()) {
                    String query = "equal(\"branch\",\"" + escapeJsonString(branch) + "\")";
                    queryStrings.add(query);
                    Log.d(TAG, "  ✓ Branch query: " + query);
                    Log.d(TAG, "    Query length: " + query.length() + " chars");
                }
                if (college != null && !college.isEmpty()) {
                    String query = "equal(\"college\",\"" + escapeJsonString(college) + "\")";
                    queryStrings.add(query);
                    Log.d(TAG, "  ✓ College query: " + query);
                    Log.d(TAG, "    Query length: " + query.length() + " chars");
                }
                Log.d(TAG, "Total queries generated: " + queryStrings.size());
                
                // Build JSON array of query strings (as Appwrite SDK expects)
                // Format: ["equal(\"field\",\"value\")", "equal(\"field2\",\"value2\")"]
                Log.d(TAG, "Building JSON array from query strings...");
                JsonArray queriesArray = new JsonArray();
                for (int i = 0; i < queryStrings.size(); i++) {
                    String query = queryStrings.get(i);
                    queriesArray.add(query);
                    Log.d(TAG, "  Query[" + i + "]: " + query);
                }
                String queriesJson = gson.toJson(queriesArray);
                Log.d(TAG, "Queries JSON array: " + queriesJson);
                Log.d(TAG, "Queries JSON length: " + queriesJson.length() + " chars");
                
                // Build URL with queries parameter
                // Appwrite REST API expects queries as a JSON array string
                // The challenge: URL encoding breaks JSON structure
                // Solution: Use java.net.URI to properly encode, or pass as-is and let HttpURLConnection handle it
                String finalUrl;
                try {
                    Log.d(TAG, "Building final URL...");
                    // Build query string
                    StringBuilder queryBuilder = new StringBuilder("project=" + PROJECT_ID);
                    Log.d(TAG, "  Base query string: " + queryBuilder.toString());
                    
                    if (!queryStrings.isEmpty()) {
                        // Try: Pass the JSON array as-is without encoding
                        // HttpURLConnection should handle special characters in query parameters
                        // Or use URI encoding which might preserve structure better
                        queryBuilder.append("&queries=").append(queriesJson);
                        Log.d(TAG, "  Queries JSON (unencoded): " + queriesJson);
                        Log.d(TAG, "  NOTE: Passing queries without URL encoding - HttpURLConnection will handle it");
                    } else {
                        Log.d(TAG, "  No queries to add (empty query list)");
                    }
                    
                    String queryString = queryBuilder.toString();
                    Log.d(TAG, "  Full query string: " + queryString);
                    
                    // Use URI to properly construct the URL
                    // This will encode special characters but might preserve JSON structure better
                    java.net.URI uri = new java.net.URI(
                        baseUrl.split("://")[0], // scheme (https)
                        baseUrl.split("://")[1].split("/")[0], // authority (host)
                        "/" + baseUrl.substring(baseUrl.indexOf("/", 8)), // path
                        queryString, // query
                        null // fragment
                    );
                    finalUrl = uri.toString();
                    Log.d(TAG, "  Final URL (via URI): " + finalUrl);
                    
                    // Alternative: Construct manually and let HttpURLConnection encode
                    if (finalUrl.contains("%")) {
                        Log.d(TAG, "  NOTE: URI encoded the URL");
                    } else {
                        Log.d(TAG, "  NOTE: URL not encoded by URI constructor");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ERROR: Failed to build URL with URI", e);
                    Log.e(TAG, "Exception type: " + e.getClass().getName());
                    Log.e(TAG, "Exception message: " + e.getMessage());
                    // Fallback: Manual construction without encoding
                    Log.d(TAG, "Using fallback URL construction (no encoding)...");
                    finalUrl = baseUrl + "?project=" + PROJECT_ID;
                    if (!queryStrings.isEmpty()) {
                        // Don't encode - pass JSON array as-is
                        finalUrl += "&queries=" + queriesJson;
                        Log.d(TAG, "  Fallback URL (unencoded queries): " + finalUrl);
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
                    Log.d(TAG, "Response body length: " + jsonResponse.length() + " chars");
                    Log.d(TAG, "Raw JSON response (first 500 chars): " + 
                        (jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse));
                    
                    List<NoteDto> notes = parseNotesResponse(jsonResponse);
                    Log.d(TAG, "✓ Parsed " + notes.size() + " documents successfully");
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(notes);
                    });
                } else {
                    Log.e(TAG, "✗ Request failed with code: " + responseCode);
                    String error = readErrorResponse(connection, responseCode);
                    Log.e(TAG, "Error response body: " + error);
                    Log.e(TAG, "Database error: " + error);
                    
                    // Log the exact URL that failed
                    Log.e(TAG, "Failed URL: " + finalUrl);
                    
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
                
                String formData;
                try {
                    formData = "documentId=" + URLEncoder.encode(documentId, "UTF-8") + 
                              "&data=" + URLEncoder.encode(dataJson, "UTF-8");
                } catch (java.io.UnsupportedEncodingException e) {
                    Log.e(TAG, "Error encoding form data", e);
                    // Fallback: use unencoded data (may cause issues with special chars)
                    formData = "documentId=" + documentId + "&data=" + dataJson;
                }
                
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
            // Appwrite expects queries as a JSON array of strings
            // Format: ["search(\"field\",\"value\")", "search(\"field2\",\"value2\")"]
            // Use Gson to properly serialize the array
            String queriesJson = gson.toJson(queryStrings);
            
            Log.d(TAG, "Query JSON before encoding: " + queriesJson);
            
            try {
                // URL encode the entire JSON array
                String encodedQueries = URLEncoder.encode(queriesJson, "UTF-8");
                url += "?queries=" + encodedQueries;
                hasQuery = true;
                Log.d(TAG, "Encoded queries: " + encodedQueries);
            } catch (java.io.UnsupportedEncodingException e) {
                Log.e(TAG, "Error encoding queries", e);
                // Fallback: use queries without encoding (may cause issues with special chars)
                url += "?queries=" + queriesJson;
                hasQuery = true;
            }
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

