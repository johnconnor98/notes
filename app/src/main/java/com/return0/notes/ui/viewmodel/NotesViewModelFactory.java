package com.return0.notes.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.return0.notes.data.repository.NotesRepositoryImpl;
import com.return0.notes.domain.repository.NotesRepository;

/**
 * ViewModelFactory for dependency injection.
 * Follows MVVM best practices by injecting dependencies into ViewModel.
 * This makes the code testable and follows Dependency Inversion Principle.
 */
public class NotesViewModelFactory implements ViewModelProvider.Factory {
    private final NotesRepository repository;

    public NotesViewModelFactory(Application application) {
        this.repository = new NotesRepositoryImpl(application);
    }

    /**
     * Constructor for testing - allows injecting mock repository.
     */
    public NotesViewModelFactory(NotesRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NotesViewModel.class)) {
            return (T) new NotesViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}

