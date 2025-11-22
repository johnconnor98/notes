package com.return0.notes.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.return0.notes.domain.model.Note;
import com.return0.notes.domain.model.SearchFilters;
import com.return0.notes.domain.repository.NotesRepository;
import com.return0.notes.data.repository.NotesRepositoryImpl;
import java.util.List;

public class NotesViewModel extends AndroidViewModel {
    private final NotesRepository repository;
    private final MutableLiveData<List<Note>> notes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> downloadPath = new MutableLiveData<>();

    public NotesViewModel(@NonNull Application application) {
        super(application);
        this.repository = new NotesRepositoryImpl(application);
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

    public void uploadNote(String title, String subject, String semester, String branch, 
                          String college, String filePath, String thumbnailPath) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        repository.uploadNote(title, subject, semester, branch, college, filePath, thumbnailPath, new NotesRepository.UploadCallback() {
            @Override
            public void onSuccess(Note note) {
                isLoading.setValue(false);
                successMessage.setValue("Note uploaded successfully");
                loadNotes();
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

