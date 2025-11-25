package com.return0.notes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.return0.notes.R;
import com.return0.notes.data.remote.ApiClient;
import com.return0.notes.data.remote.ApiService;
import com.return0.notes.data.remote.dto.PreviewResponse;
import com.return0.notes.domain.model.Note;
import com.return0.notes.ui.adapter.PreviewImageAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class PreviewActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private Button btnViewFull;
    private Note note;
    private PreviewResponse previewResponse;
    private PreviewImageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preview);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        note = (Note) getIntent().getSerializableExtra("note");
        if (note == null) {
            finish();
            return;
        }

        setupToolbar();
        setupViews();
        loadPreview();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(note.getTitle() != null ? note.getTitle() : "Preview");
        }
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnViewFull = findViewById(R.id.btnViewFull);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PreviewImageAdapter();
        recyclerView.setAdapter(adapter);
        
        btnViewFull.setOnClickListener(v -> {
            if (previewResponse != null && previewResponse.getAccessControl() != null) {
                if (previewResponse.getAccessControl().canViewFull()) {
                    // User has access, download and show full PDF
                    downloadAndShowFullPdf();
                } else if (previewResponse.getAccessControl().requiresPayment()) {
                    // Show payment dialog
                    showPaymentDialog();
                } else {
                    // Check access via API
                    checkAccessAndDownload();
                }
            } else {
                // Fallback: try to download directly
                downloadAndShowFullPdf();
            }
        });
    }

    private void loadPreview() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        btnViewFull.setVisibility(View.GONE);
        
        ApiService apiService = ApiClient.getApiService();
        Call<PreviewResponse> call = apiService.getPreview(note.getId(), 2, "medium", 800);
        
        call.enqueue(new Callback<PreviewResponse>() {
            @Override
            public void onResponse(Call<PreviewResponse> call, Response<PreviewResponse> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    previewResponse = response.body();
                    if (previewResponse.isSuccess() && previewResponse.getImages() != null) {
                        adapter.submitList(previewResponse.getImages());
                        recyclerView.setVisibility(View.VISIBLE);
                        btnViewFull.setVisibility(View.VISIBLE);
                        
                        // Update button text based on access
                        if (previewResponse.getAccessControl() != null) {
                            if (previewResponse.getAccessControl().requiresPayment()) {
                                double amount = previewResponse.getAccessControl().getPaymentAmount();
                                String currency = previewResponse.getAccessControl().getCurrency();
                                btnViewFull.setText(String.format("View Full Document - %s %.2f", currency, amount));
                            } else if (previewResponse.getAccessControl().canViewFull()) {
                                btnViewFull.setText("View Full Document");
                            }
                        }
                    } else {
                        showError("Failed to load preview");
                    }
                } else {
                    showError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PreviewResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void checkAccessAndDownload() {
        ApiService apiService = ApiClient.getApiService();
        Call<com.return0.notes.data.remote.dto.AccessResponse> call = apiService.checkAccess(note.getId());
        
        call.enqueue(new Callback<com.return0.notes.data.remote.dto.AccessResponse>() {
            @Override
            public void onResponse(Call<com.return0.notes.data.remote.dto.AccessResponse> call, 
                                  Response<com.return0.notes.data.remote.dto.AccessResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.return0.notes.data.remote.dto.AccessResponse accessResponse = response.body();
                    if (accessResponse.canDownload()) {
                        downloadAndShowFullPdf();
                    } else if (accessResponse.getPaymentRequired() != null) {
                        showPaymentDialog();
                    } else {
                        Toast.makeText(PreviewActivity.this, "Access denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<com.return0.notes.data.remote.dto.AccessResponse> call, Throwable t) {
                Toast.makeText(PreviewActivity.this, "Failed to check access", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadAndShowFullPdf() {
        // Use existing download flow
        // This will be handled by ViewModel
        Intent intent = new Intent();
        intent.putExtra("note", note);
        intent.putExtra("action", "download_full");
        setResult(RESULT_OK, intent);
        finish();
    }

    private void showPaymentDialog() {
        // TODO: Implement payment dialog
        Toast.makeText(this, "Payment required to view full document", Toast.LENGTH_LONG).show();
    }

    private void showError(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

