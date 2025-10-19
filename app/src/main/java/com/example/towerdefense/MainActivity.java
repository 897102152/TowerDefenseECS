package com.example.towerdefense;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 主活动 - 应用程序的入口界面
 * 显示游戏主菜单，提供开始游戏的入口
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Activity创建时的回调方法 - 初始化界面和组件
     * @param savedInstanceState 保存的实例状态，用于Activity重建
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类 onCreate 方法，执行必要的初始化
        super.onCreate(savedInstanceState);
        // 设置Activity的布局文件为 activity_main.xml
        setContentView(R.layout.activity_main);

        // 初始化用户界面组件和事件监听器
        setupUI();
    }

    /**
     * 设置用户界面 - 初始化按钮并设置点击事件
     * 这个方法负责配置主菜单中的所有交互元素
     */
    private void setupUI() {
        // 从布局文件中找到开始游戏按钮
        Button btnStartGame = findViewById(R.id.btn_start_game);

        // 设置按钮的点击事件监听器
        btnStartGame.setOnClickListener(v -> {
            // 创建意图(Intent)用于启动游戏活动(GameActivity)
            // MainActivity.this 表示当前Activity的上下文
            // com.example.towerdefense.view.GameActivity.class 表示要启动的目标Activity
            Intent intent = new Intent(MainActivity.this, com.example.towerdefense.SelectActivity.class);

            // 启动目标Activity，开始游戏
            startActivity(intent);

            // 注意：这里没有调用finish()，所以用户按返回键可以回到主菜单
        });
    }
}
//commit_test_10.06 - 版本控制提交标记