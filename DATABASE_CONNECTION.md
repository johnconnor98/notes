# Database Connection Explanation

## How SQLite Database Connection Works

### SQLite vs Cloud Databases

**SQLite (What we're using):**
- **File-based database** - No server needed
- **No database ID required** - Just a filename
- **No collection ID required** - Uses tables instead
- **Local file** - Database is a single file on disk

**Cloud Databases (Firebase, MongoDB, etc.):**
- **Server-based** - Requires connection to remote server
- **Database ID required** - To identify which database
- **Collection ID required** - To identify which collection/table

### Current Implementation

In `server/app.py`:

```python
DATABASE = 'notes.db'  # Just a filename, not an ID
```

**Connection is made by:**
```python
conn = sqlite3.connect(DATABASE)  # Connects to 'notes.db' file
```

**How it works:**
1. SQLite looks for a file named `notes.db` in the `server/` directory
2. If the file doesn't exist, SQLite creates it automatically
3. The connection is established - no IDs needed!

**Database Location:**
- File path: `server/notes.db`
- Created automatically when you first run the server
- All data is stored in this single file

**Table Access:**
- We access the `notes` table directly by name: `SELECT * FROM notes`
- No collection ID needed - tables are accessed by their name

### Why No IDs Are Needed

SQLite is a **self-contained, file-based database**:
- ✅ No server to connect to
- ✅ No authentication required
- ✅ No database ID needed
- ✅ No collection ID needed
- ✅ Just a filename: `notes.db`

The database file is created in the same directory where you run `python app.py`.

