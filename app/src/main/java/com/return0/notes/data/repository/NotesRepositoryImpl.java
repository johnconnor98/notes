package com.return0.notes.data.repository;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.return0.notes.data.mapper.NoteMapper;
import com.return0.notes.data.remote.storage.StorageService;
import com.return0.notes.data.remote.storage.AppwriteStorageService;
import com.return0.notes.data.remote.database.DatabaseService;
import com.return0.notes.data.remote.database.AppwriteDatabaseService;
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

/**
 * Repository implementation following SOLID principles:
 * - Single Responsibility: Coordinates between storage and database services
 * - Dependency Inversion: Depends on abstractions (StorageService, DatabaseService), not concretions
 * - Open/Closed: Open for extension (new services), closed for modification
 */
public class NotesRepositoryImpl implements NotesRepository {
    private static final String TAG = "NotesRepository";
    
    private final Context context;
    private final StorageService storageService;
    private final DatabaseService databaseService;

    /**
     * Constructor with dependency injection.
     * Services can be easily swapped for testing or different implementations.
     */
    public NotesRepositoryImpl(Context context) {
        this(context, 
             AppwriteStorageService.getInstance(context),
             new AppwriteDatabaseService());
    }
    
    /**
     * Constructor for dependency injection - allows easy swapping of implementations.
     * Useful for testing or switching providers.
     */
    public NotesRepositoryImpl(Context context, StorageService storageService, DatabaseService databaseService) {
        this.context = context.getApplicationContext();
        this.storageService = storageService;
        this.databaseService = databaseService;
    }
    

    @Override
    public void loadNotes(LoadNotesCallback callback) {
        Log.d(TAG, "=== LOADING ALL NOTES ===");
        databaseService.loadAllNotes(new DatabaseService.DatabaseCallback<List<NoteDto>>() {
            @Override
            public void onSuccess(List<NoteDto> noteDtos) {
                List<Note> notes = NoteMapper.toDomainList(noteDtos);
                callback.onSuccess(notes);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
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
        
        databaseService.searchNotes(subject, semester, branch, college, 
            new DatabaseService.DatabaseCallback<List<NoteDto>>() {
                @Override
                public void onSuccess(List<NoteDto> noteDtos) {
                    List<Note> notes = NoteMapper.toDomainList(noteDtos);
                    callback.onSuccess(notes);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
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

    /**
     * Saves note metadata to database after file upload.
     * Uses DatabaseService abstraction - can work with any database provider.
     * Follows Dependency Inversion Principle - depends on abstraction, not implementation.
     */
    private void sendMetadataToBackend(String title, String subject, String semester, String branch, 
                                      String college, String fileUrl, String filePath, String thumbUrl, 
                                      String thumbPath, String originalFilename, UploadCallback callback) {
        Log.d(TAG, "=== SENDING DATA TO DATABASE ===");
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Subject: " + (subject != null ? subject : "NULL"));
        Log.d(TAG, "Semester: " + (semester != null ? semester : "NULL"));
        Log.d(TAG, "Branch: " + (branch != null ? branch : "NULL"));
        Log.d(TAG, "College: " + (college != null ? college : "NULL"));
        Log.d(TAG, "File Path: " + filePath);
        Log.d(TAG, "Thumbnail Path: " + thumbPath);
        Log.d(TAG, "=================================");
        
        databaseService.createNote(title, subject, semester, branch, college, filePath, thumbPath,
            new DatabaseService.DatabaseCallback<NoteDto>() {
                @Override
                public void onSuccess(NoteDto noteDto) {
                    Log.d(TAG, "✓ Saved to Database:");
                    Log.d(TAG, "  ID: " + noteDto.getId());
                    Log.d(TAG, "  Title: " + noteDto.getTitle());
                    
                    Note note = NoteMapper.toDomain(noteDto);
                    callback.onSuccess(note);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Database save failed: " + error);
                    callback.onError("Database save failed: " + error);
                }
            });
    }

    @Override
    public void downloadNote(String noteId, String filename, DownloadCallback callback) {
        Log.d(TAG, "Downloading note - Note ID: " + noteId);
        
        // Use DatabaseService to get note metadata
        databaseService.getNoteById(noteId, new DatabaseService.DatabaseCallback<NoteDto>() {
            @Override
            public void onSuccess(NoteDto noteDto) {
                String filePath = noteDto.getFilePath();
                Log.d(TAG, "Retrieved filePath from database: " + (filePath != null ? filePath : "null"));
                
                // Generate filename if not provided
                String finalFilename = filename;
                if (finalFilename == null || finalFilename.isEmpty()) {
                    String title = noteDto.getTitle() != null && !noteDto.getTitle().isEmpty() 
                        ? noteDto.getTitle() : "note";
                    if (filePath != null && filePath.contains("/")) {
                        String fileId = filePath.substring(filePath.lastIndexOf("/") + 1);
                        finalFilename = title + "_" + fileId + ".pdf";
                    } else {
                        finalFilename = title + ".pdf";
                    }
                    Log.d(TAG, "Generated filename: " + finalFilename);
                }
                
                if (filePath != null && !filePath.isEmpty()) {
                    // Download to app's private cache instead of Downloads folder
                    // This provides better security and prevents easy file access
                    File cacheDir = new File(context.getCacheDir(), "pdfs");
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs();
                    }
                    Log.d(TAG, "Starting download from storage path: " + filePath);
                    storageService.downloadFile(filePath, finalFilename, cacheDir, new StorageService.DownloadCallback() {
                        @Override
                        public void onSuccess(String localFilePath) {
                            Log.d(TAG, "Download successful: " + localFilePath);
                            callback.onSuccess(localFilePath);
                        }

                        @Override
                        public void onProgress(double progress) {}

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Storage download error: " + error);
                            callback.onError("Download failed: " + error);
                        }
                    });
                } else {
                    Log.e(TAG, "File path is null or empty in document. Note ID: " + noteId);
                    Log.e(TAG, "NoteDto data - Title: " + noteDto.getTitle() + ", ID: " + noteDto.getId());
                    callback.onError("File path not found in document. Please ensure the note was uploaded correctly.");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to get document from database: " + error);
                callback.onError("Failed to get document: " + error);
            }
        });
    }

    @Override
    public void downloadNote(Note note, DownloadCallback callback) {
        Log.d(TAG, "Downloading note - Note ID: " + note.getId());
        Log.d(TAG, "Note filePath: " + (note.getFilePath() != null ? note.getFilePath() : "null"));
        
        // Always fetch latest data from database to ensure we have the filePath
        // This is more reliable than relying on the Note object which might be stale
        downloadNote(note.getId(), note.getFilename(), callback);
    }
    
    /**
     * Gets the PDF cache directory for secure storage.
     * Files in app's private cache are not easily accessible via file manager.
     */
    private File getPdfCacheDir() {
        File cacheDir = new File(context.getCacheDir(), "pdfs");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
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

