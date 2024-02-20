package com.app.jdcookie.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.jdcookie.R;
import com.app.jdcookie.constant.SettingKey;

public class SetActivity extends AppCompatActivity {

    private String baseUrl = null;
    private String clientId = null;
    private String clientSecret = null;

    private boolean isCleanedCookie = false;
    private boolean isChangeSetting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);

        // 读取服务器地址
        loadSetting();

        findViewById(R.id.set_back_image).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.set_clear_cookie_layout).setOnClickListener(v -> {
            //清除浏览器的cookie
            CookieManager instance = CookieManager.getInstance();
            instance.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    isCleanedCookie = value;
                    Toast.makeText(SetActivity.this, "清除" + (value ? "成功" : "失败"), Toast.LENGTH_SHORT).show();
                }
            });
            instance.flush();
        });

        findViewById(R.id.set_submit_host_layout).setOnClickListener(v -> {
            final EditText input = new EditText(SetActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this)
                    .setTitle("请输入服务器地址")
                    .setMessage("示例：http://127.0.0.1:8080")
                    .setView(input)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String inputText = input.getText().toString();
                            if (TextUtils.isEmpty(inputText)) {
                                return;
                            }
                            if (!URLUtil.isNetworkUrl(inputText)) {
                                Toast.makeText(SetActivity.this.getBaseContext(), "服务器地址格式错误", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            saveBaseUrl(inputText);
                            Toast.makeText(SetActivity.this.getBaseContext(), "保存成功", Toast.LENGTH_SHORT).show();
                        }
                    });
            if (!TextUtils.isEmpty(baseUrl)) {
                input.setText(baseUrl);
            }
            builder.show();
        });

        findViewById(R.id.set_ql_account_layout).setOnClickListener(v -> {
            final EditText input = new EditText(SetActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this)
                    .setTitle("请输入应用ID（Client ID）")
                    .setView(input)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String inputText = input.getText().toString();
                            if (TextUtils.isEmpty(inputText)) {
                                return;
                            }
                            saveClientId(inputText);
                            Toast.makeText(SetActivity.this.getBaseContext(), "保存成功", Toast.LENGTH_SHORT).show();
                        }
                    });
            if (!TextUtils.isEmpty(clientId)) {
                input.setText(clientId);
            }
            builder.show();
        });

        findViewById(R.id.set_ql_password_layout).setOnClickListener(v -> {
            final EditText input = new EditText(SetActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this)
                    .setTitle("请输入应用密钥（Client Secret）")
                    .setView(input)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String inputText = input.getText().toString();
                            if (TextUtils.isEmpty(inputText)) {
                                return;
                            }
                            saveClientSecret(inputText);
                            Toast.makeText(SetActivity.this.getBaseContext(), "保存成功", Toast.LENGTH_SHORT).show();
                        }
                    });
            if (!TextUtils.isEmpty(clientSecret)) {
                input.setText(clientSecret);
            }
            builder.show();
        });
    }

    @Override
    public void onBackPressed() {
        if (isCleanedCookie || isChangeSetting) {
            Intent data = new Intent();
            data.putExtra("isCleanedCookie", isCleanedCookie);
            data.putExtra("isChangeSetting", isChangeSetting);
            setResult(10, data);
            finish();
        }

        super.onBackPressed();
    }

    private void loadSetting() {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);

        this.baseUrl = setting.getString(SettingKey.BASE_URL, "");
        this.clientId = setting.getString(SettingKey.CLIENT_ID, "");
        this.clientSecret = setting.getString(SettingKey.CLIENT_SECRET, "");

        this.showSetting();
    }

    private void showSetting() {
        if (!TextUtils.isEmpty(baseUrl)) {
            TextView textView = findViewById(R.id.set_submit_host_text);
            if (textView != null) {
                textView.setText("服务器：" + baseUrl);
            }
        }
        if (!TextUtils.isEmpty(clientId)) {
            TextView textView = findViewById(R.id.set_ql_account_text);
            if (textView != null) {
                textView.setText("应用ID：" + clientId);
            }
        }
        if (!TextUtils.isEmpty(clientSecret)) {
            TextView textView = findViewById(R.id.set_ql_password_text);
            if (textView != null) {
                textView.setText("应用密钥：" + encode(clientSecret));
            }
        }
    }

    private void saveBaseUrl(String value) {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        if (setting.edit().putString(SettingKey.BASE_URL, value).commit()) {
            baseUrl = value;
            isChangeSetting = true;
        }
        this.showSetting();
    }

    private void saveClientId(String value) {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        if (setting.edit().putString(SettingKey.CLIENT_ID, value).commit()) {
            clientId = value;
            isChangeSetting = true;
        }
        this.showSetting();
    }

    private void saveClientSecret(String value) {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        if (setting.edit().putString(SettingKey.CLIENT_SECRET, value).commit()) {
            clientSecret = value;
            isChangeSetting = true;
        }
        this.showSetting();
    }

    private String encode(String password) {
        if (TextUtils.isEmpty(password)) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < password.length(); i++) {
            result.append("*");
        }
        return result.toString();
    }

}