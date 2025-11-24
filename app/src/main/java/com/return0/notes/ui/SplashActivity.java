package com.return0.notes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.return0.notes.R;
import com.return0.notes.ui.viewmodel.NotesViewModel;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 2000; // 2 seconds minimum
    private NotesViewModel viewModel;
    private boolean dataLoaded = false;
    private boolean minTimeElapsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupViewModel();
        loadData();
        startMinimumSplashTimer();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);
    }

    private void loadData() {
        // Load notes data in the background
        viewModel.loadNotes();
        
        // Observe when data is loaded
        viewModel.getNotes().observe(this, notes -> {
            if (notes != null) {
                dataLoaded = true;
                checkAndNavigate();
            }
        });
        
        // Also check for errors
        viewModel.getErrorMessage().observe(this, error -> {
            // Even if there's an error, proceed to main activity
            dataLoaded = true;
            checkAndNavigate();
        });
    }

    private void startMinimumSplashTimer() {
        // Ensure splash screen shows for at least 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            minTimeElapsed = true;
            checkAndNavigate();
        }, SPLASH_DURATION);
    }

    private void checkAndNavigate() {
        // Navigate only when both conditions are met:
        // 1. Minimum splash time has elapsed
        // 2. Data loading is complete (or failed)
        if (minTimeElapsed && dataLoaded) {
            navigateToMain();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

