package com.app.jdcookie.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.jdcookie.R;
import com.app.jdcookie.constant.SettingKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String JD_LOGIN_URL = "https://plogin.m.jd.com/login/login?appid=300&returnurl=https%3A%2F%2Fhome.m.jd.com%2FmyJd%2Fnewhome.action";
    private static final String QL_ENV_NAME = "JD_COOKIE";

    private WebView webView;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> setLauncher;
    private OkHttpClient httpClient;

    private volatile String token;
    private volatile String baseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initWebView();

        setLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            int resultCode = result.getResultCode();
            switch (resultCode) {
                case 10:
                    boolean isCleanedCookie = result.getData().getBooleanExtra("isCleanedCookie", false);
                    boolean isChangeSetting = result.getData().getBooleanExtra("isChangeSetting", false);
                    if (isCleanedCookie) {
                        TextView textView = findViewById(R.id.jd_cookie_text);
                        textView.setText("");
                        webView.loadUrl(JD_LOGIN_URL);
                    }
                    if (isChangeSetting) {
                        token = null;
                        baseUrl = null;
                    }
                default:
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
            TextView textView = findViewById(R.id.jd_cookie_text);
            if (TextUtils.isEmpty(textView.getText())) {
                return;
            }
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

                    // 获取服务器地址
                    if (TextUtils.isEmpty(baseUrl)) {
                        baseUrl = getBaseUrl();
                    }
                    if (TextUtils.isEmpty(baseUrl)) {
                        runOnUiThread(() -> Toast.makeText(v.getContext(), "未设置服务器地址", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    // 获取应用ID
                    String clientId = getClientId();
                    if (TextUtils.isEmpty(clientId)) {
                        runOnUiThread(() -> Toast.makeText(v.getContext(), "未设置应用ID", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    // 获取应用密钥
                    String clientSecret = getClientSecret();
                    if (TextUtils.isEmpty(clientSecret)) {
                        runOnUiThread(() -> Toast.makeText(v.getContext(), "未设置应用密钥", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    new Thread(() -> {
                        try {
                            // 青龙应用认证
                            if (TextUtils.isEmpty(token)) {
                                token = doAuth(clientId, clientSecret);
                            }
                            if (TextUtils.isEmpty(token)) {
                                return;
                            }

                            // 提交Cookie
                            JSONObject resBody = doSubmit(String.valueOf(textView.getText()));
                            if (resBody == null || resBody.length() == 0) {
                                runOnUiThread(() -> Toast.makeText(v.getContext(), "提交失败", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            int code = resBody.getInt("code");
                            if (code == 200) {
                                runOnUiThread(() -> Toast.makeText(v.getContext(), "提交成功", Toast.LENGTH_SHORT).show());
                            } else {
                                String msg = resBody.getString("message");
                                runOnUiThread(() -> Toast.makeText(v.getContext(), ("提交失败，" + msg), Toast.LENGTH_SHORT).show());
                            }

                        } catch (Exception e) {
                            String msg = ("程序异常：" + e.getClass().getSimpleName());
                            runOnUiThread(() -> Toast.makeText(v.getContext(), msg, Toast.LENGTH_SHORT).show());
                        }
                    }).start();

                } catch (Exception e) {
                    String msg = ("程序异常：" + e.getClass().getSimpleName());
                    runOnUiThread(() -> Toast.makeText(v.getContext(), msg, Toast.LENGTH_SHORT).show());
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

        webView.loadUrl(JD_LOGIN_URL);
    }

    private String parseCookie(String cookie) {
        StringBuffer stringBuffer = new StringBuffer();
        String[] split = cookie.split(";");
        for (String item : split) {
            item = item.trim();
            if (item.startsWith("pt_pin")) {
                stringBuffer.append(item).append(";");
                break;
            }
        }
        for (String item : split) {
            item = item.trim();
            if (item.startsWith("pt_key")) {
                stringBuffer.append(item).append(";");
                break;
            }
        }
        return stringBuffer.toString();
    }

    private String getBaseUrl() {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        return setting.getString(SettingKey.BASE_URL, "");
    }

    private String getClientId() {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        return setting.getString(SettingKey.CLIENT_ID, "");
    }

    private String getClientSecret() {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        return setting.getString(SettingKey.CLIENT_SECRET, "");
    }

    private String doAuth(String clientId, String clientSecret) {
        try {
            if (httpClient == null) {
                httpClient = new OkHttpClient();
            }
            String url = (baseUrl + "/open/auth/token?client_id=" + clientId + "&client_secret=" + clientSecret);
            Request request = new Request.Builder().url(url).get().build();
            Response response = httpClient.newCall(request).execute();
            if (response.code() != 200) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), "认证失败，状态码错误：" + response.code(), Toast.LENGTH_SHORT).show());
                return null;
            }
            String resBody = response.body().string();
            if (TextUtils.isEmpty(resBody)) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), "认证失败，返回数据为空", Toast.LENGTH_SHORT).show());
                return null;
            }
            JSONObject resJson = new JSONObject(resBody);
            if (resJson == null || resJson.length() == 0) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), "认证失败，返回数据为空", Toast.LENGTH_SHORT).show());
                return null;
            }
            int code = resJson.getInt("code");
            if (code != 200) {
                String msg = resJson.getString("message");
                runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), ("认证失败，" + msg), Toast.LENGTH_SHORT).show());
                return null;
            }
            JSONObject data = resJson.getJSONObject("data");
            if (data == null || data.length() == 0) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), "认证失败，返回数据[data]为空", Toast.LENGTH_SHORT).show());
                return null;
            }
            return data.getString("token");
        } catch (ConnectException e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), "认证失败，服务器无法连接！", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            String msg = ("认证失败，程序异常：" + e.getClass().getSimpleName());
            runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), msg, Toast.LENGTH_SHORT).show());
        }
        return null;
    }

    private JSONObject doSubmit(String jdCookie) {
        try {
            String submitUrl = (baseUrl + "/open/envs");
            JSONArray envArray = getEnvArray(jdCookie);
            if (envArray == null || envArray.length() == 0) {
                // 新增
                JSONObject body = new JSONObject();
                body.put("name", QL_ENV_NAME);
                body.put("value", jdCookie);
                return doPost(submitUrl, body);
            } else {
                // 更新
                JSONObject env = envArray.getJSONObject(0);

                JSONObject body = new JSONObject();
                body.put("id", env.getString("id"));
                body.put("name", QL_ENV_NAME);
                body.put("value", jdCookie);
                return doPut(submitUrl, body);
            }
        } catch (ConnectException e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), "提交失败，服务器无法连接！", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            String msg = ("提交失败，程序异常：" + e.getClass().getSimpleName());
            runOnUiThread(() -> Toast.makeText(MainActivity.this.getBaseContext(), msg, Toast.LENGTH_SHORT).show());
        }
        return null;
    }

    private JSONArray getEnvArray(String jdCookie) throws IOException, JSONException {
        if (TextUtils.isEmpty(jdCookie)) {
            return null;
        }
        String ptPin = getPtPin(jdCookie);
        if (TextUtils.isEmpty(ptPin)) {
            return null;
        }
        JSONObject resBody = doGet(baseUrl + "/open/envs?searchValue=" + ptPin);
        if (resBody == null) {
            return null;
        }
        return resBody.getJSONArray("data");
    }

    private JSONObject doGet(String url) throws IOException, JSONException {
        if (httpClient == null) {
            httpClient = new OkHttpClient();
        }
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();
        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();
        if (TextUtils.isEmpty(responseBody)) {
            return null;
        }
        return new JSONObject(responseBody);
    }

    private JSONObject doPost(String url, JSONObject body) throws IOException, JSONException {
        if (httpClient == null) {
            httpClient = new OkHttpClient();
        }
        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
        RequestBody reqBody = RequestBody.create(body.toString(), mediaType);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .post(reqBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();
        if (TextUtils.isEmpty(responseBody)) {
            return null;
        }
        return new JSONObject(responseBody);
    }

    private JSONObject doPut(String url, JSONObject body) throws IOException, JSONException {
        if (httpClient == null) {
            httpClient = new OkHttpClient();
        }
        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
        RequestBody reqBody = RequestBody.create(body.toString(), mediaType);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .put(reqBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();
        if (TextUtils.isEmpty(responseBody)) {
            return null;
        }
        return new JSONObject(responseBody);
    }

    public static String getPtPin(String jdCookie) {
        if (TextUtils.isEmpty(jdCookie)) {
            return null;
        }
        String[] split = jdCookie.split(";");
        if (split == null || split.length == 0) {
            return null;
        }
        for (String item : split) {
            if (item != null && item.trim().startsWith("pt_pin")) {
                return item.replace("pt_pin=", "");
            }
        }
        return null;
    }

}