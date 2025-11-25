package com.return0.notes.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.return0.notes.R;
import java.io.File;
import java.io.FileInputStream;
import android.util.Base64;

public class PdfViewerActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pdf_viewer);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupToolbar();
        setupWebView();
        loadPdf();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("PDF Viewer");
        }
    }

    private void setupWebView() {
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
    }

    private void loadPdf() {
        String pdfPath = getIntent().getStringExtra("pdf_path");
        if (pdfPath != null && !pdfPath.isEmpty()) {
            File file = new File(pdfPath);
            if (file.exists()) {
                try {
                    // Read PDF file and convert to base64
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] pdfBytes = new byte[(int) file.length()];
                    fileInputStream.read(pdfBytes);
                    fileInputStream.close();
                    
                    // Encode to base64
                    String base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP);
                    
                    // Load PDF in WebView using data URI
                    String dataUri = "data:application/pdf;base64," + base64Pdf;
                    webView.loadUrl(dataUri);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Fallback: try loading file directly
                    webView.loadUrl("file://" + file.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

