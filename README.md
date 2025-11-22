# Notes Sharing App

A college notes sharing application with Android client and Python backend server.

## Architecture

This app follows **Clean Architecture** principles with **MVVM (Model-View-ViewModel)** pattern for scalability and maintainability. See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed documentation.

### Key Design Patterns
- **MVVM**: Separation of UI, business logic, and data
- **Repository Pattern**: Abstract data source layer
- **Observer Pattern**: LiveData for reactive UI updates
- **Data Mapper**: Converts between DTOs and Domain models

## Backend Setup

1. Navigate to the `server` directory:
```bash
cd server
```

2. Install Python dependencies:
```bash
pip install -r requirements.txt
```

3. Run the server:
```bash
python app.py
```

The server will run on `http://localhost:5000`

**Note:** For Android emulator, the server URL is configured as `http://10.0.2.2:5000/` (which maps to localhost). For a physical device, you'll need to:
- Change the `BASE_URL` in `Constants.java` to your computer's IP address (e.g., `http://192.168.1.100:5000/`)
- Ensure your phone and computer are on the same network

## Android App Setup

1. Open the project in Android Studio
2. Sync Gradle files
3. Run the app on an emulator or physical device

## Features

- **Upload Notes**: Students can upload notes with title and subject
- **Browse Notes**: View all shared notes in a list
- **Download Notes**: Download any shared note to the device
- **Database Storage**: All note metadata is stored in SQLite database
- **File Storage**: Note files are stored on the server

## API Endpoints

- `GET /api/notes` - Get all notes
- `POST /api/notes` - Upload a new note (multipart/form-data)
- `GET /api/notes/<note_id>/download` - Download a note file

## Permissions

The app requires:
- Internet permission (for API calls)
- Read/Write external storage (for file operations)

## Architecture Highlights

- **Clean Architecture**: Separated into domain, data, and UI layers
- **MVVM Pattern**: ViewModels manage UI state with LiveData
- **Repository Pattern**: Abstracted data access layer
- **Scalable Structure**: Easy to add features, test, and maintain
- **Reactive UI**: LiveData observers for automatic UI updates

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture documentation.

