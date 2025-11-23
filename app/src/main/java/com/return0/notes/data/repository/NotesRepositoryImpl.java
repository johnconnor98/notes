package com.return0.notes.data.repository;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.return0.notes.data.mapper.NoteMapper;
import com.return0.notes.data.remote.ApiService;
import com.return0.notes.data.remote.ApiClient;
import com.return0.notes.data.remote.storage.StorageService;
import com.return0.notes.data.remote.storage.AppwriteStorageService;
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
import java.util.List;
import java.util.Map;

public class NotesRepositoryImpl implements NotesRepository {
    private static final String TAG = "NotesRepository";
    private final ApiService apiService;
    private final Context context;
    private final StorageService storageService;

    public NotesRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getApiService();
        this.storageService = AppwriteStorageService.getInstance(context);
    }

    @Override
    public void loadNotes(LoadNotesCallback callback) {
        Log.d(TAG, "=== LOADING ALL NOTES ===");
        apiService.getNotes(null, null, null, null).enqueue(new Callback<List<NoteDto>>() {
            @Override
            public void onResponse(Call<List<NoteDto>> call, Response<List<NoteDto>> response) {
                Log.d(TAG, "Load Notes - Response code: " + response.code());
                Log.d(TAG, "Load Notes - Response successful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    List<NoteDto> noteDtos = response.body();
                    Log.d(TAG, "Load Notes - Number of notes: " + noteDtos.size());
                    
                    // Log raw response
                    try {
                        okhttp3.Response rawResponse = response.raw();
                        if (rawResponse != null && rawResponse.body() != null) {
                            String responseBody = rawResponse.peekBody(1024 * 1024).string();
                            Log.d(TAG, "Load Notes - Raw JSON: " + responseBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading raw response: " + e.getMessage());
                    }
                    
                    logAllDatabaseData(noteDtos, "Load Notes");
                    List<Note> notes = NoteMapper.toDomainList(noteDtos);
                    callback.onSuccess(notes);
                } else {
                    Log.e(TAG, "Load Notes failed: " + response.message());
                    callback.onError("Failed to load notes: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<NoteDto>> call, Throwable t) {
                Log.e(TAG, "Load Notes network error: " + t.getMessage(), t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
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
        
        apiService.getNotes(subject, semester, branch, college).enqueue(new Callback<List<NoteDto>>() {
            @Override
            public void onResponse(Call<List<NoteDto>> call, Response<List<NoteDto>> response) {
                Log.d(TAG, "=== API RESPONSE RECEIVED ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response successful: " + response.isSuccessful());
                Log.d(TAG, "Response body is null: " + (response.body() == null));
                
                if (response.isSuccessful() && response.body() != null) {
                    List<NoteDto> noteDtos = response.body();
                    Log.d(TAG, "Number of notes received: " + noteDtos.size());
                    
                    // Log raw response body
                    try {
                        okhttp3.Response rawResponse = response.raw();
                        if (rawResponse != null && rawResponse.body() != null) {
                            String responseBody = rawResponse.peekBody(1024 * 1024).string();
                            Log.d(TAG, "Raw JSON response (first 1MB): " + responseBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading raw response: " + e.getMessage());
                    }
                    
                    logAllDatabaseData(noteDtos, "Search Results");
                    List<Note> notes = NoteMapper.toDomainList(noteDtos);
                    Log.d(TAG, "Converted to domain models: " + notes.size());
                    callback.onSuccess(notes);
                } else {
                    Log.e(TAG, "Search failed - Code: " + response.code() + ", Message: " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body: " + e.getMessage());
                        }
                    }
                    callback.onError("Search failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<NoteDto>> call, Throwable t) {
                Log.e(TAG, "Network error during search: " + t.getMessage(), t);
                callback.onError("Network error: " + t.getMessage());
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
            Log.d(TAG, "  Title: " + (note.getTitle() != null ? note.getTitle() : "NULL"));
            Log.d(TAG, "  Filename: " + (note.getFilename() != null ? note.getFilename() : "NULL"));
            Log.d(TAG, "  Upload Date: " + (note.getUploadDate() != null ? note.getUploadDate() : "NULL"));
            Log.d(TAG, "  File Size: " + note.getFileSize());
            Log.d(TAG, "  Subject: " + (note.getSubject() != null && !note.getSubject().isEmpty() ? note.getSubject() : "NULL/EMPTY"));
            Log.d(TAG, "  Semester: " + (note.getSemester() != null && !note.getSemester().isEmpty() ? note.getSemester() : "NULL/EMPTY"));
            Log.d(TAG, "  Branch: " + (note.getBranch() != null && !note.getBranch().isEmpty() ? note.getBranch() : "NULL/EMPTY"));
            Log.d(TAG, "  College: " + (note.getCollege() != null && !note.getCollege().isEmpty() ? note.getCollege() : "NULL/EMPTY"));
            Log.d(TAG, "  Thumbnail: " + (note.getThumbnail() != null ? note.getThumbnail() : "NULL"));
            Log.d(TAG, "  File URL: " + (note.getFileUrl() != null && !note.getFileUrl().isEmpty() ? note.getFileUrl() : "NULL/EMPTY"));
            Log.d(TAG, "  File Path: " + (note.getFilePath() != null && !note.getFilePath().isEmpty() ? note.getFilePath() : "NULL/EMPTY"));
            Log.d(TAG, "  Thumbnail URL: " + (note.getThumbnailUrl() != null && !note.getThumbnailUrl().isEmpty() ? note.getThumbnailUrl() : "NULL/EMPTY"));
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
        RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody subjectBody = RequestBody.create(MediaType.parse("text/plain"), subject);
        RequestBody semesterBody = RequestBody.create(MediaType.parse("text/plain"), semester != null ? semester : "");
        RequestBody branchBody = RequestBody.create(MediaType.parse("text/plain"), branch != null ? branch : "");
        RequestBody collegeBody = RequestBody.create(MediaType.parse("text/plain"), college != null ? college : "");
        RequestBody fileUrlBody = RequestBody.create(MediaType.parse("text/plain"), fileUrl);
        RequestBody filePathBody = RequestBody.create(MediaType.parse("text/plain"), filePath);
        RequestBody thumbUrlBody = thumbUrl != null ? RequestBody.create(MediaType.parse("text/plain"), thumbUrl) : RequestBody.create(MediaType.parse("text/plain"), "");

        apiService.uploadNoteMetadata(titleBody, subjectBody, semesterBody, branchBody, collegeBody, 
                fileUrlBody, filePathBody, thumbUrlBody).enqueue(new Callback<NoteDto>() {
            @Override
            public void onResponse(Call<NoteDto> call, Response<NoteDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Note note = NoteMapper.toDomain(response.body());
                    callback.onSuccess(note);
                } else {
                    callback.onError("Upload failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<NoteDto> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void downloadNote(String noteId, String filename, DownloadCallback callback) {
        apiService.downloadNote(noteId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String contentType = response.headers().get("Content-Type");
                    if (contentType != null && contentType.contains("application/json")) {
                        try {
                            String jsonResponse = response.body().string();
                            if (jsonResponse.contains("file_path") || jsonResponse.contains("appwrite") || jsonResponse.contains("storage")) {
                                downloadFromStorage(jsonResponse, filename, callback);
                            } else {
                                saveFile(response.body(), filename, callback);
                            }
                        } catch (Exception e) {
                            callback.onError("Error parsing response: " + e.getMessage());
                        }
                    } else {
                        saveFile(response.body(), filename, callback);
                    }
                } else {
                    callback.onError("Download failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
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

