package com.return0.notes.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.return0.notes.R;
import com.return0.notes.ui.viewmodel.NotesViewModel;
import com.return0.notes.util.FileUtils;

public class UploadActivity extends AppCompatActivity {
    private NotesViewModel viewModel;
    private Uri selectedFileUri;
    private Uri selectedThumbnailUri;
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<String> thumbnailPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupToolbar();
        setupFilePickers();
        setupViewModel();
        setupClickListeners();
        observeViewModel();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Upload Note");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFilePickers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        TextView tvFileName = findViewById(R.id.tvFileName);
                        String fileName = FileUtils.getFileName(this, uri);
                        tvFileName.setText("Selected: " + fileName);
                    }
                });

        thumbnailPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedThumbnailUri = uri;
                        TextView tvThumbnailName = findViewById(R.id.tvThumbnailName);
                        String thumbName = FileUtils.getFileName(this, uri);
                        tvThumbnailName.setText("Selected: " + thumbName);
                    }
                });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);
    }

    private void setupClickListeners() {
        MaterialButton btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSelectFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        MaterialButton btnSelectThumbnail = findViewById(R.id.btnSelectThumbnail);
        btnSelectThumbnail.setOnClickListener(v -> thumbnailPickerLauncher.launch("image/*"));

        MaterialButton btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        MaterialButton btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(v -> uploadNote());
    }

    private void observeViewModel() {
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
                finish(); // Close activity on success
            }
        });
    }

    private void uploadNote() {
        TextInputEditText etTitle = findViewById(R.id.etTitle);
        TextInputEditText etSubject = findViewById(R.id.etSubject);
        TextInputEditText etSemester = findViewById(R.id.etSemester);
        TextInputEditText etBranch = findViewById(R.id.etBranch);
        TextInputEditText etCollege = findViewById(R.id.etCollege);

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
    }
}

