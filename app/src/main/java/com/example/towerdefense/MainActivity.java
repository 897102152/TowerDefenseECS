package com.example.towerdefense;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 主活动 - 应用程序的入口界面
 * 显示游戏主菜单，提供开始游戏和退出游戏的入口
 */
public class MainActivity extends AppCompatActivity {

    private ImageButton btnStartGame;
    private ImageButton btnQuitGame;

    /**
     * Activity创建时的回调方法 - 初始化界面和组件
     * @param savedInstanceState 保存的实例状态，用于Activity重建
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类 onCreate 方法，执行必要的初始化
        super.onCreate(savedInstanceState);

        // 设置全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 隐藏系统UI
        hideSystemUI();
        // 设置Activity的布局文件为 activity_main.xml
        setContentView(R.layout.activity_main);

        // 初始化用户界面组件和事件监听器
        setupUI();
    }
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
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
    /**
     * 设置用户界面 - 初始化按钮并设置点击事件
     * 这个方法负责配置主菜单中的所有交互元素
     */
    private void setupUI() {
        // 从布局文件中找到开始游戏按钮和退出游戏按钮
        btnStartGame = findViewById(R.id.btn_start_game);
        btnQuitGame = findViewById(R.id.btn_quit_game);

        // 设置开始游戏按钮的点击事件监听器
        btnStartGame.setOnClickListener(v -> {
            // 播放点击动画
            animateButtonClick(btnStartGame);

            // 延迟执行跳转逻辑，让动画完成
            new Handler().postDelayed(() -> {
                // 创建意图(Intent)用于启动选择关卡活动(SelectActivity)
                Intent intent = new Intent(MainActivity.this, com.example.towerdefense.SelectActivity.class);
                // 启动目标Activity，进入关卡选择
                startActivity(intent);
            }, 150);
        });

        // 设置退出游戏按钮的点击事件监听器
        btnQuitGame.setOnClickListener(v -> {
            // 播放点击动画
            animateButtonClick(btnQuitGame);

            // 延迟执行退出逻辑，让动画完成
            new Handler().postDelayed(() -> {
                showExitConfirmationDialog();
            }, 150);
        });
    }

    /**
     * 显示退出确认对话框
     */
    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("退出游戏")
                .setMessage("确定要退出游戏吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitApp();
                    }
                })
                .setNegativeButton("取消", null)
                .setCancelable(true)
                .show();
    }

    /**
     * 退出应用程序
     */
    private void exitApp() {
        // 结束所有Activity并退出应用
        finishAffinity();
        // 确保完全退出进程
        System.exit(0);
    }

    /**
     * 按钮点击动画效果
     * @param view 要添加动画的视图
     */
    private void animateButtonClick(View view) {
        // 创建缩放动画
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.button_click);
        view.startAnimation(animation);
    }

    /**
     * 处理返回键按下事件
     */
    @Override
    public void onBackPressed() {
        showExitConfirmationDialog();
    }
}
//commit_test_10.06 - 版本控制提交标记