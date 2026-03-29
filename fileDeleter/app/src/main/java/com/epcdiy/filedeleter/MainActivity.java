package com.epcdiy.filedeleter;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;


import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private StringBuilder logBuilder = new StringBuilder();
    // 1. 定义文件夹选择启动器
    private final ActivityResultLauncher<Uri> openTreeLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    // 获取永久访问权限（重启App后依然有效）
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    // 开始遍历删除
                    startSafeDeletion(uri);
                }
            }
    );

    private void startSafeDeletion(Uri treeUri) {
        DocumentFile rootDir = DocumentFile.fromTreeUri(this, treeUri);
        if (rootDir == null) return;

        new Thread(() -> {
            traverseAndRemove(rootDir);
            runOnUiThread(() -> tvStatus.append("\n--- 模拟操作完成 ---"));
        }).start();
    }

    // 3. 递归遍历并删除
    private void traverseAndRemove(DocumentFile dir) {
        DocumentFile[] files = dir.listFiles();
        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                traverseAndRemove(file);
            } else {
                String name = file.getName();
                if (isMedia(name)) {
                    // 核心：使用 SAF 的删除接口
                    boolean success = file.delete();

                    String msg = (success ? "[SAF删除了] " : "[失败] ") + name;
                    runOnUiThread(() -> {
                        logBuilder.append(msg).append("\n");
                        tvStatus.setText(logBuilder.toString());
                    });
                }
            }
        }
    }

    private boolean isMedia(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        if(n.indexOf("screenrecorder")>0)
        {
            return false;
        }
        return n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".mp4");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> {
            openTreeLauncher.launch(null);
        });
        Button btnStart2 = findViewById(R.id.btnStart2);

        btnStart2.setOnClickListener(v -> {
            if (!checkStoragePermission()) {
                requestStoragePermission();
            } else {
                startBypassDeletion();
            }
        });

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
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".mp4");
    }

    private void updateUI(String msg) {
        runOnUiThread(() -> {
            logBuilder.append(msg).append("\n");
            tvStatus.setText(logBuilder.toString());
        });
    }
}