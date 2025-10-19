package com.example.towerdefense;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;


public class SelectActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类 onCreate 方法，执行必要的初始化
        super.onCreate(savedInstanceState);
        // 设置Activity的布局文件为 activity_main.xml
        setContentView(R.layout.activity_selectlevel);

        // 初始化用户界面组件和事件监听器
        setupUI();
        hideSystemUI();
    }

    private void setupUI() {
        // 从布局文件中找到开始游戏按钮
        Button btntrainlevel = findViewById(R.id.btn_train);
        Button btnlevel01 = findViewById(R.id.btn_level01);

        // 设置按钮的点击事件监听器
        btntrainlevel.setOnClickListener(v -> {
            // 创建意图(Intent)用于启动游戏活动(GameActivity)
            // SelectActivity.this 表示当前Activity的上下文
            // com.example.towerdefense.view.GameActivity.class 表示要启动的目标Activity
            Intent intent = new Intent(SelectActivity.this, com.example.towerdefense.view.GameActivity.class);

            // 启动目标Activity，开始游戏
            startActivity(intent);

            // 注意：这里没有调用finish()，所以用户按返回键可以回到主菜单
        });

        btnlevel01.setOnClickListener(v -> {
            // 创建意图(Intent)用于启动游戏活动(GameActivity)
            // SelectActivity.this 表示当前Activity的上下文
            // com.example.towerdefense.view.GameActivity.class 表示要启动的目标Activity
            Intent intent = new Intent(SelectActivity.this, com.example.towerdefense.view.GameActivity.class);

            // 启动目标Activity，开始游戏
            startActivity(intent);

            // 注意：这里没有调用finish()，所以用户按返回键可以回到主菜单
        });
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
