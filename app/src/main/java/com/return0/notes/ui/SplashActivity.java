package com.return0.notes.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.return0.notes.R;
import com.return0.notes.ui.viewmodel.NotesViewModel;
import com.return0.notes.ui.viewmodel.NotesViewModelFactory;

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

        animateLogo();
        setupViewModel();
        loadData();
        startMinimumSplashTimer();
    }

    private void animateLogo() {
        View logoContainer = findViewById(R.id.logoContainer);
        View logo = findViewById(R.id.ivLogo);
        View appName = findViewById(R.id.tvAppName);
        View tagline = findViewById(R.id.tvTagline);

        // Logo animation - scale and fade in
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.5f, 1.0f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.5f, 1.0f);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        ObjectAnimator logoRotation = ObjectAnimator.ofFloat(logo, "rotation", -10f, 0f);

        AnimatorSet logoAnimator = new AnimatorSet();
        logoAnimator.playTogether(logoScaleX, logoScaleY, logoAlpha, logoRotation);
        logoAnimator.setDuration(1000);
        logoAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        logoAnimator.start();

        // Container fade in
        ObjectAnimator containerAlpha = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f);
        containerAlpha.setDuration(800);
        containerAlpha.start();

        // App name fade in (delayed)
        ObjectAnimator appNameAlpha = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f);
        appNameAlpha.setStartDelay(600);
        appNameAlpha.setDuration(600);
        appNameAlpha.start();

        // Tagline fade in (delayed)
        ObjectAnimator taglineAlpha = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
        taglineAlpha.setStartDelay(900);
        taglineAlpha.setDuration(600);
        taglineAlpha.start();
    }

    private void setupViewModel() {
        NotesViewModelFactory factory = new NotesViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(NotesViewModel.class);
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

