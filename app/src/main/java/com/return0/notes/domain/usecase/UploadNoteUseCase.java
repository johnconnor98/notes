package com.return0.notes.domain.usecase;

import com.return0.notes.domain.model.Note;
import com.return0.notes.domain.repository.NotesRepository;

/**
 * Use Case for uploading notes.
 * Follows Clean Architecture principles by encapsulating business logic.
 * This makes the code more testable and maintainable.
 */
public class UploadNoteUseCase {
    private final NotesRepository repository;

    public UploadNoteUseCase(NotesRepository repository) {
        this.repository = repository;
    }

    /**
     * Validates upload data and executes upload.
     * Returns validation error message if validation fails, null if valid.
     */
    public String validateUploadData(String title, String filePath) {
        if (title == null || title.trim().isEmpty()) {
            return "Please enter a title";
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            return "Please select a file";
        }
        return null; // Validation passed
    }

    /**
     * Executes the upload operation.
     */
    public void execute(String title, String subject, String semester, String branch,
                       String college, String filePath, String thumbnailPath,
                       NotesRepository.UploadCallback callback) {
        String validationError = validateUploadData(title, filePath);
        if (validationError != null) {
            callback.onError(validationError);
            return;
        }

        repository.uploadNote(title, subject, semester, branch, college, filePath, thumbnailPath, callback);
    }
}

