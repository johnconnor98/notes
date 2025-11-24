package com.return0.notes.ui.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.return0.notes.domain.model.Note;
import com.return0.notes.domain.model.SearchFilters;
import com.return0.notes.domain.repository.NotesRepository;
import java.util.List;

/**
 * ViewModel following MVVM architecture principles:
 * - Manages UI-related data in a lifecycle-aware way
 * - Survives configuration changes
 * - No reference to View (Activity/Fragment)
 * - Exposes LiveData for reactive UI updates
 * - Contains business logic for UI state management
 */
public class NotesViewModel extends ViewModel {
    private final NotesRepository repository;
    private final MutableLiveData<List<Note>> notes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> downloadPath = new MutableLiveData<>();

    /**
     * Constructor with dependency injection.
     * Repository is injected via ViewModelFactory for testability.
     */
    public NotesViewModel(@NonNull NotesRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Note>> getNotes() {
        return notes;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<String> getDownloadPath() {
        return downloadPath;
    }

    public void loadNotes() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.loadNotes(new NotesRepository.LoadNotesCallback() {
            @Override
            public void onSuccess(List<Note> noteList) {
                isLoading.setValue(false);
                notes.setValue(noteList);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void searchNotes(SearchFilters filters) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.searchNotes(filters, new NotesRepository.LoadNotesCallback() {
            @Override
            public void onSuccess(List<Note> noteList) {
                isLoading.setValue(false);
                notes.setValue(noteList);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Uploads a note with validation.
     * Business logic validation is handled here (MVVM pattern).
     */
    public void uploadNote(String title, String subject, String semester, String branch, 
                          String college, String filePath, String thumbnailPath) {
        // Validation
        if (title == null || title.trim().isEmpty()) {
            errorMessage.setValue("Please enter a title");
            return;
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            errorMessage.setValue("Please select a file");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        repository.uploadNote(title, subject, semester, branch, college, filePath, thumbnailPath, new NotesRepository.UploadCallback() {
            @Override
            public void onSuccess(Note note) {
                isLoading.setValue(false);
                successMessage.setValue("Note uploaded successfully");
                loadNotes(); // Refresh list after upload
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void downloadNote(Note note) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.downloadNote(note, new NotesRepository.DownloadCallback() {
            @Override
            public void onSuccess(String filePath) {
                isLoading.setValue(false);
                downloadPath.setValue(filePath);
                successMessage.setValue("Downloaded to Downloads folder");
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }
}

