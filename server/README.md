# Notes Sharing Backend Server

## Setup

1. Install Python dependencies:
```bash
pip install -r requirements.txt
```

2. Run the server:
```bash
python app.py
```

The server will run on `http://localhost:5000`

## API Endpoints

- `GET /api/notes` - Get all notes
- `POST /api/notes` - Upload a new note (multipart/form-data with 'file', 'title', 'subject')
- `GET /api/notes/<note_id>/download` - Download a note file

## Database

SQLite database (`notes.db`) stores note metadata. Files are stored in the `uploads/` directory.




