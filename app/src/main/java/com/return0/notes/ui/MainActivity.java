package com.return0.notes.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.return0.notes.R;
import com.return0.notes.domain.model.Note;
import com.return0.notes.domain.model.SearchFilters;
import com.return0.notes.ui.adapter.NoteAdapter;
import com.return0.notes.ui.viewmodel.NotesViewModel;
import com.return0.notes.util.FileUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NotesViewModel viewModel;
    private NoteAdapter adapter;
    private RecyclerView recyclerView;
    private Uri selectedFileUri;
    private Uri selectedThumbnailUri;
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<String> thumbnailPickerLauncher;

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

        requestPermissions();
        setupViewModel();
        setupRecyclerView();
        setupFilePicker();
        setupClickListeners();
        observeViewModel();
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 1);
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnDownloadClickListener(note -> viewModel.downloadNote(note));
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                    }
                });
        
        thumbnailPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedThumbnailUri = uri;
                    }
                });
    }

    private void setupClickListeners() {
        MaterialButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> showSearchDialog());

        MaterialButton btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(v -> showUploadDialog());

        FloatingActionButton fabRefresh = findViewById(R.id.fabRefresh);
        fabRefresh.setOnClickListener(v -> viewModel.loadNotes());
    }

    private void observeViewModel() {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload, null);
        builder.setView(dialogView);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etSubject = dialogView.findViewById(R.id.etSubject);
        TextInputEditText etSemester = dialogView.findViewById(R.id.etSemester);
        TextInputEditText etBranch = dialogView.findViewById(R.id.etBranch);
        TextInputEditText etCollege = dialogView.findViewById(R.id.etCollege);
        TextView tvFileName = dialogView.findViewById(R.id.tvFileName);
        TextView tvThumbnailName = dialogView.findViewById(R.id.tvThumbnailName);
        MaterialButton btnSelectFile = dialogView.findViewById(R.id.btnSelectFile);
        MaterialButton btnSelectThumbnail = dialogView.findViewById(R.id.btnSelectThumbnail);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnUpload = dialogView.findViewById(R.id.btnUpload);

        selectedFileUri = null;
        selectedThumbnailUri = null;
        tvFileName.setText("No file selected");
        tvThumbnailName.setText("No thumbnail selected (optional)");

        btnSelectFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnSelectThumbnail.setOnClickListener(v -> thumbnailPickerLauncher.launch("image/*"));

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpload.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String subject = etSubject.getText().toString().trim();
            String semester = etSemester.getText().toString().trim();
            String branch = etBranch.getText().toString().trim();
            String college = etCollege.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = FileUtils.getFileName(this, selectedFileUri);
            String filePath = FileUtils.copyUriToCache(this, selectedFileUri, fileName);

            if (filePath == null) {
                Toast.makeText(this, "Failed to prepare file for upload", Toast.LENGTH_SHORT).show();
                return;
            }

            String thumbnailPath = null;
            if (selectedThumbnailUri != null) {
                String thumbFileName = FileUtils.getFileName(this, selectedThumbnailUri);
                thumbnailPath = FileUtils.copyUriToCache(this, selectedThumbnailUri, thumbFileName);
            }

            viewModel.uploadNote(title, subject, semester, branch, college, filePath, thumbnailPath);
            dialog.dismiss();
        });

        // Update filename display after file selection
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (selectedFileUri != null) {
                String fileName = FileUtils.getFileName(this, selectedFileUri);
                tvFileName.setText("Selected: " + fileName);
            }
            if (selectedThumbnailUri != null) {
                String thumbName = FileUtils.getFileName(this, selectedThumbnailUri);
                tvThumbnailName.setText("Selected: " + thumbName);
            }
        }, 100);
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search, null);
        builder.setView(dialogView);

        TextInputEditText etSubject = dialogView.findViewById(R.id.etSubject);
        TextInputEditText etSemester = dialogView.findViewById(R.id.etSemester);
        TextInputEditText etBranch = dialogView.findViewById(R.id.etBranch);
        TextInputEditText etCollege = dialogView.findViewById(R.id.etCollege);
        MaterialButton btnClear = dialogView.findViewById(R.id.btnClear);
        MaterialButton btnSearch = dialogView.findViewById(R.id.btnSearch);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnClear.setOnClickListener(v -> {
            etSubject.setText("");
            etSemester.setText("");
            etBranch.setText("");
            etCollege.setText("");
        });

        btnSearch.setOnClickListener(v -> {
            SearchFilters filters = new SearchFilters();
            filters.setFilter("subject", etSubject.getText().toString());
            filters.setFilter("semester", etSemester.getText().toString());
            filters.setFilter("branch", etBranch.getText().toString());
            filters.setFilter("college", etCollege.getText().toString());

            viewModel.searchNotes(filters);
            dialog.dismiss();
        });
    }
}

