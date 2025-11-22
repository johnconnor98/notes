# Architecture Documentation

## Overview
This app follows **Clean Architecture** principles with **MVVM (Model-View-ViewModel)** pattern for scalability and maintainability.

## Package Structure

```
com.return0.notes/
├── domain/              # Business logic layer (independent of frameworks)
│   ├── model/          # Domain models
│   └── repository/     # Repository interfaces
├── data/               # Data layer (implementations)
│   ├── remote/         # API services and DTOs
│   ├── repository/     # Repository implementations
│   └── mapper/         # Data mappers (DTO <-> Domain)
├── ui/                 # Presentation layer
│   ├── viewmodel/      # ViewModels
│   ├── adapter/        # RecyclerView adapters
│   └── MainActivity    # Activities
└── util/               # Utility classes
```

## Design Patterns

### 1. MVVM (Model-View-ViewModel)
- **View (Activity)**: Handles UI and user interactions
- **ViewModel**: Manages UI-related data, survives configuration changes
- **Model (Repository)**: Handles data operations

### 2. Repository Pattern
- **Interface** (`NotesRepository`): Defines data operations contract
- **Implementation** (`NotesRepositoryImpl`): Concrete implementation
- Benefits: Easy to swap data sources, testable, single source of truth

### 3. Data Mapper Pattern
- Converts between DTOs (Data Transfer Objects) and Domain models
- Keeps domain layer independent of API structure

### 4. Observer Pattern
- LiveData for reactive UI updates
- ViewModel exposes LiveData, Activity observes changes

### 5. Singleton Pattern
- ApiClient uses singleton for Retrofit instance
- Ensures single HTTP client instance

## Data Flow

1. **User Action** → Activity
2. **Activity** → ViewModel method call
3. **ViewModel** → Repository method
4. **Repository** → API Service (Remote data source)
5. **API Response** → DTO → Mapper → Domain Model
6. **Domain Model** → Repository callback
7. **Repository** → ViewModel (updates LiveData)
8. **LiveData** → Activity observer (updates UI)

## Key Components

### Domain Layer
- **Note**: Domain model (business entity)
- **NotesRepository**: Interface defining data operations

### Data Layer
- **NoteDto**: API response model
- **ApiService**: Retrofit interface for API calls
- **ApiClient**: Retrofit client factory
- **NoteMapper**: Converts DTOs to Domain models
- **NotesRepositoryImpl**: Repository implementation

### UI Layer
- **NotesViewModel**: Manages UI state with LiveData
- **NoteAdapter**: RecyclerView adapter with DiffUtil
- **MainActivity**: Main UI controller

## Benefits

1. **Separation of Concerns**: Each layer has a single responsibility
2. **Testability**: Easy to unit test ViewModels and Repositories
3. **Scalability**: Easy to add new features without affecting existing code
4. **Maintainability**: Clear structure makes code easy to understand
5. **Flexibility**: Easy to swap implementations (e.g., different data sources)

## Future Enhancements

- Add Dependency Injection (Dagger/Hilt)
- Add local database caching (Room)
- Add Use Cases for complex business logic
- Add error handling strategies
- Add loading states management
- Add pagination for large datasets




