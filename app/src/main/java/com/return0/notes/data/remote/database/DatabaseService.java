package com.return0.notes.data.remote.database;

import com.return0.notes.data.remote.dto.NoteDto;
import java.util.List;

/**
 * Interface for database operations.
 * This abstraction allows easy replacement of database providers (Appwrite, Firebase, Backend API, etc.)
 * Follows Dependency Inversion Principle - depend on abstractions, not concretions.
 */
public interface DatabaseService {
    
    /**
     * Callback for database operations
     */
    interface DatabaseCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    /**
     * Load all notes from the database
     */
    void loadAllNotes(DatabaseCallback<List<NoteDto>> callback);
    
    /**
     * Search notes by filters
     * @param subject Filter by subject (can be null)
     * @param semester Filter by semester (can be null)
     * @param branch Filter by branch (can be null)
     * @param college Filter by college (can be null)
     */
    void searchNotes(String subject, String semester, String branch, String college, 
                    DatabaseCallback<List<NoteDto>> callback);
    
    /**
     * Get a single note by ID
     */
    void getNoteById(String noteId, DatabaseCallback<NoteDto> callback);
    
    /**
     * Create a new note document in the database
     * @param title Note title
     * @param subject Note subject
     * @param semester Note semester
     * @param branch Note branch
     * @param college Note college
     * @param filePath Storage path for the file
     * @param thumbnailPath Storage path for the thumbnail
     * @return The created note DTO
     */
    void createNote(String title, String subject, String semester, String branch, 
                   String college, String filePath, String thumbnailPath,
                   DatabaseCallback<NoteDto> callback);
}

