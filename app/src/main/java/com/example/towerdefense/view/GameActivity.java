package com.example.towerdefense.view;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
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
    // UI控件
    private Button btnStart, btnPause;
    private TextView tvGold, tvHealth, tvWave;
    private int currentLevelId;
    private String currentLevelName;
    // 获取从SelectActivity传递过来的关卡信息


    /**
     * Activity创建时的回调方法
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentLevelId = extras.getInt("LEVEL_ID", 0); // 默认训练关
            currentLevelName = extras.getString("LEVEL_NAME", "教学关");
        } else {
            // 如果没有传递关卡信息，使用默认值
            currentLevelId = 0;
            currentLevelName = "训练模式";
        }

        // 设置全屏显示，提供沉浸式游戏体验
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 隐藏系统UI（状态栏和导航栏）
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
     * 隐藏状态栏、导航栏等系统界面元素
     */
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

    /**
     * 窗口焦点变化回调 - 当Activity获得或失去焦点时调用
     * @param hasFocus 是否获得焦点
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 如果获得焦点，重新隐藏系统UI
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
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        tvGold = findViewById(R.id.tvGold);
        tvHealth = findViewById(R.id.tvHealth);
        tvWave = findViewById(R.id.tvWave);

        // 设置"出怪"按钮点击事件
        btnStart.setOnClickListener(v -> {
            if (gameEngine != null) {
                // 手动生成敌人
                gameEngine.spawnEnemyManually();
                Toast.makeText(this, "生成敌人!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "GameEngine为空!", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置"暂停/继续"按钮点击事件
        btnPause.setOnClickListener(v -> {
            if (gameEngine != null) {
                // 切换游戏暂停状态
                togglePause();
            } else {
                Toast.makeText(this, "GameEngine为空!", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置弓箭塔选择按钮
        findViewById(R.id.btnArcherTower).setOnClickListener(v -> {
            // 设置游戏视图选中的塔类型为弓箭塔
            gameView.setSelectedTowerType(Tower.Type.ARCHER);
            Toast.makeText(this, "选中弓箭塔", Toast.LENGTH_SHORT).show();
        });

        // 设置炮塔选择按钮
        findViewById(R.id.btnCannonTower).setOnClickListener(v -> {
            // 设置游戏视图选中的塔类型为炮塔
            gameView.setSelectedTowerType(Tower.Type.CANNON);
            Toast.makeText(this, "选中炮塔", Toast.LENGTH_SHORT).show();
        });

        // 设置法师塔选择按钮
        findViewById(R.id.btnMageTower).setOnClickListener(v -> {
            // 设置游戏视图选中的塔类型为法师塔
            gameView.setSelectedTowerType(Tower.Type.MAGE);
            Toast.makeText(this, "选中法师塔", Toast.LENGTH_SHORT).show();
        });

        // 初始化按钮状态
        btnStart.setEnabled(true);
        btnStart.setText("出怪");
        btnPause.setEnabled(true);
        btnPause.setText("暂停");
    }

    /**
     * 设置游戏引擎并建立关联
     */
    private void setupGameEngine() {
        // 创建游戏引擎实例
        gameEngine = new GameEngine(this,currentLevelId);
        // 设置游戏状态更新监听器（当前Activity）
        gameEngine.setUpdateListener(this);
        // 将游戏引擎设置到游戏视图中
        gameView.setGameEngine(gameEngine);

        // 创建游戏引擎时传入关卡ID
        gameEngine = new GameEngine(this, currentLevelId);
        gameEngine.setUpdateListener(this);
        gameView.setGameEngine(gameEngine);

        // 可以在UI上显示当前关卡名称
        Toast.makeText(this, "当前关卡: " + currentLevelName, Toast.LENGTH_SHORT).show();
    }

    /**
     * 开始游戏 - 启动游戏循环并更新UI状态
     */
    private void startGame() {
        if (gameEngine == null) return;

        // 启动游戏引擎
        gameEngine.startGame();
        // 更新暂停按钮文本
        btnPause.setText(R.string.pause_button);  // 使用资源

        // 更新初始 UI 状态
        tvGold.setText(getString(R.string.gold_text, 100));    // 使用带参数的资源
        tvHealth.setText(getString(R.string.health_text, 100));// 使用带参数的资源
        tvWave.setText(getString(R.string.wave_text, 1));      // 使用带参数的资源

    //    Toast.makeText(this, R.string.level_selection, Toast.LENGTH_SHORT).show();
    }

    /**
     * 切换游戏暂停状态
     */
    private void togglePause() {
        if (gameEngine == null) return;

        if (gameEngine.isRunning()) {
            // 如果游戏正在运行，则暂停游戏
            gameEngine.pauseGame();
            btnPause.setText("继续");
            Toast.makeText(this, "游戏暂停", Toast.LENGTH_SHORT).show();
        } else {
            // 如果游戏已暂停，则继续游戏
            gameEngine.resumeGame();
            btnPause.setText("暂停");
            Toast.makeText(this, "游戏继续", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 游戏状态更新回调 - 当游戏引擎状态发生变化时调用
     * @param world 更新后的游戏世界状态
     */
    @Override
    public void onGameStateUpdated(World world) {
        // 在UI线程中更新界面
        runOnUiThread(() -> {
            // 强制游戏视图重绘，刷新游戏画面
            gameView.invalidate();

            // 更新游戏状态显示（金币、生命值、波次等）
            updateGameStats();
        });
    }

    /**
     * 更新游戏状态显示
     * 这里可以添加实际的金币、生命值、波次更新逻辑
     */
    private void updateGameStats() {
        // TODO: 添加实际的游戏状态更新逻辑
        // 例如：从游戏引擎获取当前金币数、生命值、波次等信息
        // tvGold.setText("金币: " + gameEngine.getGold());
        // tvHealth.setText("生命: " + gameEngine.getHealth());
        // tvWave.setText("波次: " + gameEngine.getWave());
    }

    /**
     * Activity恢复时的回调
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 如果游戏引擎存在且未运行，则恢复游戏
        if (gameEngine != null && !gameEngine.isRunning()) {
            gameEngine.resumeGame();
        }
        // 重新隐藏系统UI
        hideSystemUI();
    }

    /**
     * Activity暂停时的回调
     */
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停游戏
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
        // 停止游戏并释放资源
        if (gameEngine != null) {
            gameEngine.stopGame();
        }
    }
}