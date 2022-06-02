package com.app.jdcookie.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.jdcookie.R;

public class SetActivity extends AppCompatActivity {

    private boolean isClearCookie = false;
    private String submitHost = null;
    private String account = null;
    private String password = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);

        // 读取服务器IP
        loadSetting();

        findViewById(R.id.set_back_image).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.set_clear_cookie_layout).setOnClickListener(v -> {
            //清除浏览器的cookie
            CookieManager instance = CookieManager.getInstance();
            instance.removeAllCookies(value -> {
                SetActivity.this.isClearCookie = value;
                Toast.makeText(SetActivity.this, "清除" + (value ? "成功" : "失败"), Toast.LENGTH_SHORT).show();
            });
            instance.flush();
        });

        findViewById(R.id.set_submit_host_layout).setOnClickListener(v -> {
            final EditText input = new EditText(SetActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this)
                    .setTitle("请输入服务器地址")
                    .setView(input)
                    .setNegativeButton("取消", null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String inputText = input.getText().toString();
                    if (TextUtils.isEmpty(inputText)) {
                        return;
                    }
                    saveSubmitHost(inputText);
                    Toast.makeText(SetActivity.this.getBaseContext(), "保存成功", Toast.LENGTH_SHORT).show();
                }
            });
            if (!TextUtils.isEmpty(submitHost)) {
                input.setText(submitHost);
            }
            builder.show();
        });

        findViewById(R.id.set_ql_account_layout).setOnClickListener(v -> {
            final EditText input = new EditText(SetActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this)
                    .setTitle("请输入账号")
                    .setView(input)
                    .setNegativeButton("取消", null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String inputText = input.getText().toString();
                    if (TextUtils.isEmpty(inputText)) {
                        return;
                    }
                    saveAccount(inputText);
                    Toast.makeText(SetActivity.this.getBaseContext(), "保存成功", Toast.LENGTH_SHORT).show();
                }
            });
            if (!TextUtils.isEmpty(account)) {
                input.setText(account);
            }
            builder.show();
        });

        findViewById(R.id.set_ql_password_layout).setOnClickListener(v -> {
            final EditText input = new EditText(SetActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this)
                    .setTitle("请输入密码")
                    .setView(input)
                    .setNegativeButton("取消", null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String inputText = input.getText().toString();
                    if (TextUtils.isEmpty(inputText)) {
                        return;
                    }
                    savePassword(inputText);
                    Toast.makeText(SetActivity.this.getBaseContext(), "保存成功", Toast.LENGTH_SHORT).show();
                }
            });
            if (!TextUtils.isEmpty(password)) {
                input.setText(password);
            }
            builder.show();
        });
    }


    @Override
    public void onBackPressed() {
        if (isClearCookie) {
            //清楚cookie需要通知刷新
            setResult(10);
            finish();
        }
        super.onBackPressed();
    }

    private void loadSetting() {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);

        this.submitHost = setting.getString("submit_host", "");
        this.account = setting.getString("account", "");
        this.password = setting.getString("password", "");

        this.showSetting();
    }

    private void saveSubmitHost(String value) {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        if (setting.edit().putString("submit_host", value).commit()) {
            submitHost = value;
        }
        this.showSetting();
    }

    private void saveAccount(String value) {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        if (setting.edit().putString("account", value).commit()) {
            account = value;
        }

        this.showSetting();
    }

    private void savePassword(String value) {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        if (setting.edit().putString("password", value).commit()) {
            password = value;
        }

        this.showSetting();
    }

    private void showSetting() {
        if (!TextUtils.isEmpty(submitHost)) {
            TextView textView = findViewById(R.id.set_submit_host_text);
            if (textView != null) {
                textView.setText("服务器：" + submitHost);
            }
        }
        if (!TextUtils.isEmpty(account)) {
            TextView textView = findViewById(R.id.set_ql_account_text);
            if (textView != null) {
                textView.setText("账号：" + account);
            }
        }
        if (!TextUtils.isEmpty(password)) {
            TextView textView = findViewById(R.id.set_ql_password_text);
            if (textView != null) {
                textView.setText("密码：" + password);
            }
        }
    }


}