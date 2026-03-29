package com.epcdiy.filedeleter;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private StringBuilder logBuilder = new StringBuilder();
    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> {
            // 检查并申请权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            } else {
                updateUI("已经有权限了！");
            }
        });
        Button btnStart2 = findViewById(R.id.btnStart2);

        btnStart2.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                updateUI("请先点授权！");
            } else {
                startBypassDeletion();
            }
        });
        updateUI("警告！使用前务必备份照片！");
    }

    // 1. 检查权限 (Android 11+)
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true; // 低版本略过
    }

    // 2. 申请“所有文件访问权限”
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
    }

    // 3. 开始执行删除逻辑
    private void startBypassDeletion() {
        File dcimDir = new File(Environment.getExternalStorageDirectory(), "DCIM");
        if (!dcimDir.exists()) {
            updateUI("未找到 DCIM 目录");
            return;
        }

        new Thread(() -> {
            traverseAndDelete(dcimDir);
            runOnUiThread(() -> Toast.makeText(this, "删除模拟完成", Toast.LENGTH_SHORT).show());
        }).start();
    }

    // 4. 递归枚举并使用底层接口删除
    private void traverseAndDelete(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                traverseAndDelete(file); // 递归进入子文件夹
            } else {
                String fileName = file.getName();
                // 仅模拟删除图片和视频
                if (isMediaFile(fileName)) {
                    // 核心操作：直接调用 java.io.File.delete()
                    // 这不会触发 MediaStore 的数据库更新
                    boolean deleted = file.delete();

                    String msg = (deleted ? "[已抹除] " : "[失败] ") + file.getAbsolutePath();
                    updateUI(msg);
                }
            }
        }
    }

    private boolean isMediaFile(String name) {
        String n = name.toLowerCase();
        if(n.contains("screenrecorder"))
        {
            return false;
        }
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".mp4");
    }

    private void updateUI(String msg) {
        runOnUiThread(() -> {
            logBuilder.append(msg).append("\n");
            tvStatus.setText(logBuilder.toString());
        });
    }
}