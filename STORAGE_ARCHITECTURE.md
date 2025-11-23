# Storage Architecture

This document describes the scalable storage architecture implemented in the Notes app.

## Architecture Overview

The storage system uses a **Strategy Pattern** with interface-based abstraction, making it easy to switch between different storage providers (Appwrite, Firebase, AWS S3, etc.) without changing the business logic.

## Components

### 1. StorageService Interface
**Location:** `app/src/main/java/com/return0/notes/data/remote/storage/StorageService.java`

This interface defines the contract for all storage implementations:
- `uploadFile()` - Upload files to storage
- `uploadThumbnail()` - Upload thumbnails to storage
- `downloadFile()` - Download files from storage

### 2. AppwriteStorageService
**Location:** `app/src/main/java/com/return0/notes/data/remote/storage/AppwriteStorageService.java`

Current implementation using Appwrite Storage:
- Endpoint: `https://fra.cloud.appwrite.io/v1`
- Project ID: `6922970a001ab5faef56`
- Buckets: `notes` and `thumbnails`

### 3. NotesRepositoryImpl
**Location:** `app/src/main/java/com/return0/notes/data/repository/NotesRepositoryImpl.java`

The repository uses `StorageService` interface, making it provider-agnostic:
```java
private final StorageService storageService;

public NotesRepositoryImpl(Context context) {
    this.storageService = AppwriteStorageService.getInstance();
}
```

## How to Switch Storage Providers

### Step 1: Create New Implementation
Create a new class implementing `StorageService`:

```java
public class NewStorageService implements StorageService {
    // Implement all interface methods
}
```

### Step 2: Update Repository
Change only one line in `NotesRepositoryImpl`:

```java
// Before
this.storageService = AppwriteStorageService.getInstance();

// After
this.storageService = NewStorageService.getInstance();
```

That's it! No other code changes needed.

## Example: Switching to Firebase

1. Create `FirebaseStorageService.java` implementing `StorageService`
2. Update `NotesRepositoryImpl` constructor to use `FirebaseStorageService.getInstance()`
3. Done!

## Benefits

✅ **Scalable** - Easy to add new storage providers
✅ **Maintainable** - Clear separation of concerns
✅ **Testable** - Can mock `StorageService` for unit tests
✅ **Flexible** - Switch providers without changing business logic

## Current Configuration

- **Provider:** Appwrite Storage
- **Endpoint:** https://fra.cloud.appwrite.io/v1
- **Project ID:** 6922970a001ab5faef56
- **Buckets:** 
  - `notes` - For note files
  - `thumbnails` - For thumbnail images

