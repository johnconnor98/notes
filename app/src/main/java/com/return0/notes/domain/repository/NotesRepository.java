package com.return0.notes.domain.repository;

import com.return0.notes.domain.model.Note;
import com.return0.notes.domain.model.SearchFilters;
import java.util.List;

public interface NotesRepository {
    interface LoadNotesCallback {
        void onSuccess(List<Note> notes);
        void onError(String error);
    }

    interface UploadCallback {
        void onSuccess(Note note);
        void onError(String error);
    }

    interface DownloadCallback {
        void onSuccess(String filePath);
        void onError(String error);
    }

    void loadNotes(LoadNotesCallback callback);
    void searchNotes(SearchFilters filters, LoadNotesCallback callback);
    void uploadNote(String title, String subject, String semester, String branch, 
                    String college, String filePath, String thumbnailPath, UploadCallback callback);
    void downloadNote(String noteId, String filename, DownloadCallback callback);
    void downloadNote(Note note, DownloadCallback callback);
}

