package com.example.towerdefense;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class SelectActivity extends AppCompatActivity {

    public static final int level_training = 0;  // 训练关id
    public static final int LEVEL_01 = 1;  // 正式关01 id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类 onCreate 方法，执行必要的初始化
        super.onCreate(savedInstanceState);
        // 设置Activity的布局文件为 activity_selectlevel.xml
        setContentView(R.layout.activity_selectlevel);

        // 初始化用户界面组件和事件监听器
        setupUI();
        hideSystemUI();
    }

    private void setupUI() {
        // 从布局文件中找到按钮
        Button btntrainlevel = findViewById(R.id.btn_train);
        Button btnlevel01 = findViewById(R.id.btn_level01);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // 设置返回按钮的点击事件监听器
        btnBack.setOnClickListener(v -> {
            returnToMainMenu();
        });

        // 设置训练关卡按钮的点击事件监听器
        btntrainlevel.setOnClickListener(v -> {
            Intent intent = new Intent(SelectActivity.this, com.example.towerdefense.view.GameActivity.class);
            intent.putExtra("LEVEL_ID", level_training);
            intent.putExtra("LEVEL_NAME", "教学关");
            startActivity(intent);

            // 添加过渡动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 设置第一关按钮的点击事件监听器 - 修正关卡ID
        btnlevel01.setOnClickListener(v -> {
            Intent intent = new Intent(SelectActivity.this, com.example.towerdefense.view.GameActivity.class);
            intent.putExtra("LEVEL_ID", LEVEL_01);  // 修正为 LEVEL_01 而不是 level_training
            intent.putExtra("LEVEL_NAME", "第一关");
            startActivity(intent);

            // 添加过渡动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    /**
     * 返回主菜单
     */
    private void returnToMainMenu() {
        // 结束当前Activity，返回上一个Activity（MainActivity）
        finish();

        // 添加返回动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 处理返回键
     */
    @Override
    public void onBackPressed() {
        returnToMainMenu();
    }

    /**
     * 窗口焦点变化回调
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY        // 沉浸式粘性模式
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE     // 保持布局稳定
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // 布局隐藏导航栏
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // 布局全屏
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION   // 隐藏导航栏
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);      // 全屏显示
    }
}