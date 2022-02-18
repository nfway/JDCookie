package com.app.jdcookie.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import android.os.Build;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.app.jdcookie.R;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String LOGIN_URL = "https://plogin.m.jd.com/login/login?appid=300&returnurl=https%3A%2F%2Fhome.m.jd.com%2FmyJd%2Fnewhome.action";
    private static final String SUBMIT_URL = "http://49.232.79.109:8090/jd/cookie/put";

    private WebView webView;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> setLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initWebView();

        setLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            int resultCode = result.getResultCode();
            if (resultCode == 10) {
                findViewById(R.id.main_clear).performClick();
                webView.loadUrl(LOGIN_URL);
            }
        });
    }

    private void initView() {
        // 返回按钮
        findViewById(R.id.main_back).setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        // 设置按钮
        findViewById(R.id.main_set).setOnClickListener(v -> setLauncher.launch(new Intent(MainActivity.this, SetActivity.class)));
        // 刷新按钮
        findViewById(R.id.main_refresh).setOnClickListener(v -> webView.reload());
        // 清除Cookie
        findViewById(R.id.main_clear).setOnClickListener(v -> {
            TextView textView = (TextView) findViewById(R.id.jd_cookie_text);
            textView.setText("");
            Toast.makeText(v.getContext(), "清除成功", Toast.LENGTH_SHORT).show();
        });
        // 复制Cookie
        findViewById(R.id.jd_cookie_text).setOnClickListener(v -> {
            TextView textView = (TextView) v;
            if (TextUtils.isEmpty(textView.getText())) {
                return;
            }

            // 复制到剪切板
            ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData clipData = ClipData.newPlainText("Label", textView.getText());
            // 将ClipData内容放到系统剪贴板里。
            manager.setPrimaryClip(clipData);
            Toast.makeText(MainActivity.this, "复制成功", Toast.LENGTH_SHORT).show();
        });
        // 提交Cookie
        findViewById(R.id.submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    TextView textView = findViewById(R.id.jd_cookie_text);
                    if (TextUtils.isEmpty(textView.getText())) {
                        return;
                    }

                    JSONObject requestJson = new JSONObject();
                    requestJson.put("cookie", textView.getText());
                    MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
                    RequestBody requestBody = RequestBody.create(requestJson.toString(), mediaType);
                    Request request = new Request.Builder().url(SUBMIT_URL).post(requestBody).build();
                    OkHttpClient client = new OkHttpClient();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            runOnUiThread(() -> Toast.makeText(v.getContext(), "提交失败，出现异常", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            try {
                                if (response.code() != 200) {
                                    runOnUiThread(() -> Toast.makeText(v.getContext(), "提交失败，状态码：" + response.code(), Toast.LENGTH_SHORT).show());
                                }
                                final String responseBody = response.body().string();
                                JSONObject responseJson = new JSONObject(responseBody);
                                if (responseJson.length() == 0) {
                                    runOnUiThread(() -> Toast.makeText(v.getContext(), "提交失败，返回数据错误", Toast.LENGTH_SHORT).show());
                                }
                                if (responseJson.getBoolean("status")) {
                                    runOnUiThread(() -> Toast.makeText(v.getContext(), "提交成功", Toast.LENGTH_SHORT).show());
                                } else {
                                    String message = responseJson.getString("message");
                                    runOnUiThread(() -> Toast.makeText(v.getContext(), "提交失败，" + message, Toast.LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(v.getContext(), "提交失败，出现异常", Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(v.getContext(), "提交失败，出现异常", Toast.LENGTH_SHORT).show());
                }
            }
        });

        progressBar = findViewById(R.id.main_progress_bar);
    }

    private void initWebView() {
        webView = findViewById(R.id.main_web_view);
        WebSettings webSetting = webView.getSettings();
        webSetting.setJavaScriptEnabled(true);

        webSetting.setBuiltInZoomControls(true);
        webSetting.setDisplayZoomControls(false);
        webSetting.setUseWideViewPort(true);

        webSetting.setBlockNetworkImage(false);
        //缓存模式
        webSetting.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSetting.setDatabaseEnabled(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setAppCacheMaxSize(1024 * 1024 * 8);
        webSetting.setAppCachePath(getFilesDir().getAbsolutePath());
        webSetting.setDatabasePath(getFilesDir().getAbsolutePath());
        webSetting.setAllowFileAccess(true);
        webSetting.setAppCacheEnabled(true);
        webSetting.setTextZoom(100);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSetting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.INVISIBLE);
                }
                super.onPageFinished(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request1) {
                CookieManager cookieManager = CookieManager.getInstance();
                String url = request1.getUrl().toString();
                String cookie = cookieManager.getCookie(url);
                if (cookie == null || cookie.isEmpty()) {
                    return super.shouldInterceptRequest(view, request1);
                }

                if (!cookie.contains("pt_key") || !cookie.contains("pt_pin")) {
                    return super.shouldInterceptRequest(view, request1);
                }

                final String pt_key = parseCookie(cookie);
                if (pt_key == null || pt_key.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(view.getContext(), "解析Cookie失败", Toast.LENGTH_SHORT).show());
                    return super.shouldInterceptRequest(view, request1);
                }

                TextView textView = findViewById(R.id.jd_cookie_text);
                if (!TextUtils.isEmpty(textView.getText()) && Objects.equals(pt_key, textView.getText())) {
                    return super.shouldInterceptRequest(view, request1);
                }

                textView.setText(pt_key);
                runOnUiThread(() -> Toast.makeText(view.getContext(), "解析Cookie成功", Toast.LENGTH_SHORT).show());
                return super.shouldInterceptRequest(view, request1);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (null != progressBar) {
                    progressBar.setProgress(newProgress);
                }
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });

        webView.loadUrl(LOGIN_URL);
    }

    private String parseCookie(String cookie) {
        StringBuffer stringBuffer = new StringBuffer();
        for (String item : cookie.split(";")) {
            item = item.trim();
            if (item.startsWith("pt_pin") || item.startsWith("pt_key")) {
                stringBuffer.append(item).append(";");
            }
        }
        return stringBuffer.toString();
    }

}