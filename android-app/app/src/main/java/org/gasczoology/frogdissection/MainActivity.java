package org.gasczoology.frogdissection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final String LOCAL_URL = "file:///android_asset/index.html";
    private static final int FILE_PICK_REQUEST = 7001;
    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        webView.addJavascriptInterface(new FileExportBridge(), "AndroidFileBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(Uri.parse(url));
            }

            private boolean handleNavigation(Uri url) {
                final String value = url.toString();
                if (value.startsWith("file:///android_asset/") || value.equals("about:blank")) {
                    return false;
                }
                if ("http".equalsIgnoreCase(url.getScheme()) ||
                    "https".equalsIgnoreCase(url.getScheme())) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, url));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, "No browser installed", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                try {
                    startActivityForResult(Intent.createChooser(pick, "Choose frog specimen photograph"),
                                           FILE_PICK_REQUEST);
                } catch (ActivityNotFoundException e) {
                    fileChooserCallback.onReceiveValue(null);
                    fileChooserCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) return;
        webView.loadUrl(LOCAL_URL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_PICK_REQUEST) return;
        if (fileChooserCallback != null) {
            Uri[] picked = (resultCode == RESULT_OK && data != null && data.getData() != null)
                    ? new Uri[]{data.getData()} : null;
            fileChooserCallback.onReceiveValue(picked);
            fileChooserCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = null;
        }
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidFileBridge");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    // Only fixed, user-triggered HTML/JSON exports are accepted; no arbitrary file paths.
    private final class FileExportBridge {
        @JavascriptInterface
        public void saveBase64(String filename, String dataUrl) {
            if (filename == null || dataUrl == null) return;
            final boolean htmlFile = "index.html".equals(filename);
            final boolean jsonFile = "frog-calibration-review.json".equals(filename);
            if (!htmlFile && !jsonFile) return;
            if (dataUrl.length() > 32_000_000) {
                showMessage("Export exceeds the 24 MB Android limit");
                return;
            }
            final int comma = dataUrl.indexOf(',');
            if (comma < 0 || !dataUrl.substring(0, comma).contains(";base64")) return;
            final byte[] bytes;
            try {
                bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                showMessage("Could not decode the exported file");
                return;
            }

            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    values.put(MediaStore.Downloads.MIME_TYPE,
                            htmlFile ? "text/html" : "application/json");
                    values.put(MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/FrogDissection");
                    Uri item = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (item == null) throw new IllegalStateException("Cannot create download");
                    try (OutputStream output = getContentResolver().openOutputStream(item)) {
                        if (output == null) throw new IllegalStateException("Download inaccessible");
                        output.write(bytes);
                    }
                } else {
                    File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    if (directory == null) throw new IllegalStateException("No documents directory");
                    if (!directory.exists() && !directory.mkdirs())
                        throw new IllegalStateException("Cannot create documents directory");
                    try (OutputStream output = new FileOutputStream(new File(directory, filename))) {
                        output.write(bytes);
                    }
                }
                showMessage("Saved " + filename + " to FrogDissection downloads");
            } catch (Exception e) {
                showMessage("Export failed: " + e.getMessage());
            }
        }

        private void showMessage(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
        }
    }
}
