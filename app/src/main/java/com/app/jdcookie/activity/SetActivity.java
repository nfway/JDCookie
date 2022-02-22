package com.app.jdcookie.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.jdcookie.R;

public class SetActivity extends AppCompatActivity {

    private boolean isClearCookie = false;
    private String submitHost = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);

        // 读取服务器IP
        loadSubmitHost();

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
                    .setTitle("请输入服务器IP地址")
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

    private void loadSubmitHost() {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        String host = setting.getString("submit_host", "");
        submitHost = host;

        showSubmitHost();
    }

    private void saveSubmitHost(String host) {
        SharedPreferences setting = getSharedPreferences("setting", Context.MODE_PRIVATE);
        setting.edit().putString("submit_host", host).commit();
        submitHost = host;

        showSubmitHost();
    }

    private void showSubmitHost() {
        if (TextUtils.isEmpty(submitHost)) {
            return;
        }
        TextView textView = findViewById(R.id.set_submit_host_text);
        if (textView != null) {
            textView.setText("服务器IP：" + submitHost);
        }
    }


}