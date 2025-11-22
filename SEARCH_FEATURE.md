# Search Feature Documentation

## Overview
The search feature allows users to filter notes by multiple criteria with a scalable architecture that makes it easy to add new filter options in the future.

## Current Filters
- **Subject**: Filter by subject name
- **Semester**: Filter by semester (e.g., "1st", "2nd", "Fall 2024")
- **Branch**: Filter by branch/department (e.g., "Computer Science", "Electrical")
- **College**: Filter by college name

## Architecture

### Scalable Design
The search system uses a `SearchFilters` class that stores filters in a `Map<String, String>`, making it easy to add new filters without changing the core architecture.

### Adding New Filters

#### Backend (Python/Flask)
1. Add the new field to the database schema in `server/app.py`:
```python
c.execute('ALTER TABLE notes ADD COLUMN new_field TEXT')
```

2. Update the search endpoint to accept the new parameter:
```python
new_field = request.args.get('new_field', '')
if new_field:
    query += ' AND new_field LIKE ?'
    params.append(f'%{new_field}%')
```

3. Update the upload endpoint to save the new field:
```python
new_field = request.form.get('new_field', '')
# Add to INSERT statement
```

#### Android App

1. **Domain Layer**: No changes needed - `SearchFilters` already supports any filter key

2. **Data Layer**: Update `ApiService.java` to accept the new query parameter:
```java
@GET("api/notes")
Call<List<NoteDto>> getNotes(
    @retrofit2.http.Query("subject") String subject,
    @retrofit2.http.Query("semester") String semester,
    @retrofit2.http.Query("branch") String branch,
    @retrofit2.http.Query("college") String college,
    @retrofit2.http.Query("new_field") String newField  // Add this
);
```

3. **Repository**: Update `NotesRepositoryImpl.java` to pass the new filter:
```java
apiService.getNotes(
    filterMap.get("subject"),
    filterMap.get("semester"),
    filterMap.get("branch"),
    filterMap.get("college"),
    filterMap.get("new_field")  // Add this
)
```

4. **UI Layer**: Add input field to `dialog_search.xml`:
```xml
<com.google.android.material.textfield.TextInputLayout
    android:hint="New Field">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/etNewField" />
</com.google.android.material.textfield.TextInputLayout>
```

5. **Activity**: Update `MainActivity.java` to read the new field:
```java
filters.setFilter("new_field", etNewField.getText().toString());
```

## Thumbnail Support

- Thumbnails are optional during upload
- Stored separately in `thumbnails/` directory
- Displayed in the note list using Glide image loading library
- Fallback to default icon if thumbnail is not available

## Search Flow

1. User opens search dialog
2. Enters filter criteria (all optional)
3. Clicks "Search"
4. `SearchFilters` object created with entered values
5. ViewModel calls repository's `searchNotes()`
6. Repository makes API call with query parameters
7. Backend filters database results
8. Results returned and displayed in RecyclerView with thumbnails

## Benefits of Current Design

1. **Scalable**: Easy to add new filters without major refactoring
2. **Flexible**: All filters are optional - user can search with any combination
3. **Maintainable**: Clear separation between layers
4. **User-Friendly**: Simple UI with clear labels
5. **Performance**: Server-side filtering reduces data transfer


