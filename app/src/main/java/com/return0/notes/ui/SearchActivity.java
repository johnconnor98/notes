package com.return0.notes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.return0.notes.R;
import com.return0.notes.domain.model.SearchFilters;
import com.return0.notes.ui.viewmodel.NotesViewModel;

public class SearchActivity extends AppCompatActivity {
    private NotesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupToolbar();
        setupViewModel();
        setupClickListeners();
        observeViewModel();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Notes");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);
    }

    private void setupClickListeners() {
        MaterialButton btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> {
            TextInputEditText etSubject = findViewById(R.id.etSubject);
            TextInputEditText etSemester = findViewById(R.id.etSemester);
            TextInputEditText etBranch = findViewById(R.id.etBranch);
            TextInputEditText etCollege = findViewById(R.id.etCollege);
            
            etSubject.setText("");
            etSemester.setText("");
            etBranch.setText("");
            etCollege.setText("");
        });

        MaterialButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> performSearch());
    }

    private void observeViewModel() {
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });
    }

    private void performSearch() {
        TextInputEditText etSubject = findViewById(R.id.etSubject);
        TextInputEditText etSemester = findViewById(R.id.etSemester);
        TextInputEditText etBranch = findViewById(R.id.etBranch);
        TextInputEditText etCollege = findViewById(R.id.etCollege);

        SearchFilters filters = new SearchFilters();
        filters.setFilter("subject", etSubject.getText().toString());
        filters.setFilter("semester", etSemester.getText().toString());
        filters.setFilter("branch", etBranch.getText().toString());
        filters.setFilter("college", etCollege.getText().toString());

        // Navigate to SearchResultsActivity
        Intent intent = new Intent(SearchActivity.this, SearchResultsActivity.class);
        intent.putExtra("searchFilters", filters);
        startActivity(intent);
    }
}

