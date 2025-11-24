package com.return0.notes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.return0.notes.R;
import com.return0.notes.domain.model.Note;
import com.return0.notes.domain.model.SearchFilters;
import com.return0.notes.ui.adapter.NoteAdapter;
import com.return0.notes.ui.viewmodel.NotesViewModel;
import com.return0.notes.util.DocumentOpener;
import java.util.List;

public class SearchResultsActivity extends AppCompatActivity {
    private NotesViewModel viewModel;
    private NoteAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private SearchFilters searchFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_results);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get search filters from intent
        searchFilters = (SearchFilters) getIntent().getSerializableExtra("searchFilters");
        if (searchFilters == null) {
            searchFilters = new SearchFilters();
        }

        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        observeViewModel();
        
        // Perform search
        viewModel.searchNotes(searchFilters);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Results");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        
        adapter = new NoteAdapter();
        adapter.setOnDownloadClickListener(note -> {
            // Download and open document with restrictions
            openDocumentWithRestrictions(note);
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // Make items clickable to open documents
        adapter.setOnItemClickListener(note -> {
            openDocumentWithRestrictions(note);
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getNotes().observe(this, notes -> {
            if (notes != null) {
                adapter.submitList(notes);
                if (notes.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
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
                // File downloaded successfully - open it
                DocumentOpener.openDocument(this, path);
            }
        });
    }

    private void openDocumentWithRestrictions(Note note) {
        // Check restrictions before opening
        if (canOpenDocument(note)) {
            // Download and open the document
            viewModel.downloadNote(note);
        } else {
            Toast.makeText(this, "You don't have permission to open this document", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canOpenDocument(Note note) {
        // TODO: Add your restrictions logic here
        // Examples:
        // - Check user permissions
        // - Check subscription status
        // - Check document access level
        // - Check time-based restrictions
        // - Check download limits
        
        // For now, allow all documents
        return true;
    }
}

