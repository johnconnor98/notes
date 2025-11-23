package com.return0.notes.data.repository;

import android.content.Context;
import android.os.Environment;
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
    private final ApiService apiService;
    private final Context context;
    private final StorageService storageService;

    public NotesRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getApiService();
        this.storageService = AppwriteStorageService.getInstance();
    }

    @Override
    public void loadNotes(LoadNotesCallback callback) {
        apiService.getNotes(null, null, null, null).enqueue(new Callback<List<NoteDto>>() {
            @Override
            public void onResponse(Call<List<NoteDto>> call, Response<List<NoteDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Note> notes = NoteMapper.toDomainList(response.body());
                    callback.onSuccess(notes);
                } else {
                    callback.onError("Failed to load notes: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<NoteDto>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void searchNotes(SearchFilters filters, LoadNotesCallback callback) {
        Map<String, String> filterMap = filters.getFilters();
        apiService.getNotes(
                filterMap.get("subject"),
                filterMap.get("semester"),
                filterMap.get("branch"),
                filterMap.get("college")
        ).enqueue(new Callback<List<NoteDto>>() {
            @Override
            public void onResponse(Call<List<NoteDto>> call, Response<List<NoteDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Note> notes = NoteMapper.toDomainList(response.body());
                    callback.onSuccess(notes);
                } else {
                    callback.onError("Search failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<NoteDto>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
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

