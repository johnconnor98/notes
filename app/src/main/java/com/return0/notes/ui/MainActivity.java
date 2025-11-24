package com.return0.notes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.return0.notes.R;
import com.return0.notes.domain.model.Note;
import com.return0.notes.ui.adapter.NoteAdapter;
import com.return0.notes.ui.viewmodel.NotesViewModel;
import com.return0.notes.ui.viewmodel.NotesViewModelFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NotesViewModel viewModel;
    private NoteAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupViewModel();
        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupViewModel() {
        NotesViewModelFactory factory = new NotesViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(NotesViewModel.class);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnDownloadClickListener(note -> viewModel.downloadNote(note));
    }

    private void setupClickListeners() {
        MaterialButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> showSearchDialog());

        MaterialButton btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(v -> showUploadDialog());

        // Floating Action Menu
        ExtendedFloatingActionButton fabMenu = findViewById(R.id.fabMenu);
        FloatingActionButton fabRefresh = findViewById(R.id.fabRefresh);
        FloatingActionButton fabSearch = findViewById(R.id.fabSearch);
        
        boolean[] isMenuExpanded = {false};
        
        fabMenu.setOnClickListener(v -> {
            isMenuExpanded[0] = !isMenuExpanded[0];
            if (isMenuExpanded[0]) {
                // Expand menu
                fabRefresh.show();
                fabSearch.show();
                fabMenu.shrink();
            } else {
                // Collapse menu
                fabRefresh.hide();
                fabSearch.hide();
                fabMenu.extend();
            }
        });
        
        fabRefresh.setOnClickListener(v -> {
            viewModel.loadNotes();
            // Collapse menu after action
            fabRefresh.hide();
            fabSearch.hide();
            fabMenu.extend();
            isMenuExpanded[0] = false;
        });
        
        fabSearch.setOnClickListener(v -> {
            showSearchDialog();
            // Collapse menu after action
            fabRefresh.hide();
            fabSearch.hide();
            fabMenu.extend();
            isMenuExpanded[0] = false;
        });
    }

    private void observeViewModel() {
        // Notes are already loaded in SplashActivity, just observe for updates
        viewModel.getNotes().observe(this, notes -> {
            if (notes != null) {
                adapter.submitList(notes);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            // Could show/hide progress indicator here
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });

        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });

        viewModel.getDownloadPath().observe(this, path -> {
            if (path != null && !path.isEmpty()) {
                // File downloaded successfully
            }
        });
    }

    private void showUploadDialog() {
        // Navigate to UploadActivity instead of showing dialog
        Intent intent = new Intent(MainActivity.this, UploadActivity.class);
        startActivity(intent);
    }

    private void showSearchDialog() {
        // Navigate to SearchActivity instead of showing dialog
        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
        startActivity(intent);
    }
}

