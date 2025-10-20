package com.example.towerdefense.view;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.towerdefense.GameEngine;
import com.example.towerdefense.R;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.ecs.World;

/**
 * 游戏主活动 - 负责游戏界面的显示和用户交互
 * 实现GameUpdateListener接口，接收游戏状态更新回调
 */
public class GameActivity extends AppCompatActivity implements GameEngine.GameUpdateListener {

    // 游戏引擎实例，负责游戏逻辑处理
    private GameEngine gameEngine;
    // 游戏视图，负责游戏画面的渲染
    private GameView gameView;
    private int currentLevelId;
    private String currentLevelName;

    // ========== 建造模式控制 ==========
    private boolean isBuildMode = false;
    private LinearLayout buildMenuLayout;

    /**
     * Activity创建时的回调方法
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentLevelId = extras.getInt("LEVEL_ID", 0);
            currentLevelName = extras.getString("LEVEL_NAME", "教学关");
        } else {
            currentLevelId = 0;
            currentLevelName = "训练模式";
        }

        // 设置全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 隐藏系统UI
        hideSystemUI();

        // 设置游戏界面的布局文件
        setContentView(R.layout.activity_game);

        // 初始化视图组件
        initViews();
        // 设置游戏引擎
        setupGameEngine();

        // 自动开始游戏
        startGame();
    }

    /**
     * 隐藏系统UI - 实现全屏沉浸式体验
     */
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
     * 窗口焦点变化回调
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    /**
     * 初始化所有视图组件和事件监听器
     */
    private void initViews() {
        // 查找并绑定视图组件
        gameView = findViewById(R.id.gameView);
        buildMenuLayout = findViewById(R.id.buildMenuLayout);

        // ========== 建造模式主按钮 ==========
        findViewById(R.id.btnBuildMode).setOnClickListener(v -> {
            toggleBuildMode();
        });

        // ========== 建造菜单中的塔选择按钮 ==========
        // 弓箭塔选择按钮
        findViewById(R.id.btnArcherTower).setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.ARCHER);
            Toast.makeText(this, "选中弓箭塔", Toast.LENGTH_SHORT).show();
        });

        // 炮塔选择按钮
        findViewById(R.id.btnCannonTower).setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.CANNON);
            Toast.makeText(this, "选中炮塔", Toast.LENGTH_SHORT).show();
        });

        // 法师塔选择按钮
        findViewById(R.id.btnMageTower).setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.MAGE);
            Toast.makeText(this, "选中法师塔", Toast.LENGTH_SHORT).show();
        });

        // 初始状态：建造模式关闭
        setBuildMode(false);
    }

    /**
     * 切换建造模式
     */
    private void toggleBuildMode() {
        isBuildMode = !isBuildMode;
        setBuildMode(isBuildMode);

        String message = isBuildMode ? "建造模式开启" : "建造模式关闭";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置建造模式状态
     */
    private void setBuildMode(boolean buildMode) {
        this.isBuildMode = buildMode;

        if (gameView != null) {
            // 设置建造模式和网格显示（建造模式下显示网格）
            gameView.setBuildMode(buildMode);
            gameView.setShowGrid(buildMode);
        }

        // 显示/隐藏建造菜单
        if (buildMenuLayout != null) {
            buildMenuLayout.setVisibility(buildMode ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 设置游戏引擎并建立关联
     */
    private void setupGameEngine() {
        gameEngine = new GameEngine(this, currentLevelId);
        gameEngine.setUpdateListener(this);
        gameView.setGameEngine(gameEngine);

        Toast.makeText(this, "当前关卡: " + currentLevelName, Toast.LENGTH_SHORT).show();
    }

    /**
     * 开始游戏
     */
    private void startGame() {
        if (gameEngine == null) return;
        gameEngine.startGame();
    }

    /**
     * 游戏状态更新回调
     */
    @Override
    public void onGameStateUpdated(World world) {
        runOnUiThread(() -> {
            gameView.invalidate();
        });
    }

    /**
     * Activity恢复时的回调
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (gameEngine != null && !gameEngine.isRunning()) {
            gameEngine.resumeGame();
        }
        hideSystemUI();
    }

    /**
     * Activity暂停时的回调
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (gameEngine != null) {
            gameEngine.pauseGame();
        }
    }

    /**
     * Activity销毁时的回调
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameEngine != null) {
            gameEngine.stopGame();
        }
    }
}