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

public class GameActivity extends AppCompatActivity implements GameEngine.GameUpdateListener {

    private GameEngine gameEngine;
    private GameView gameView;
    private Button btnStart, btnPause;
    private TextView tvGold, tvHealth, tvWave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 修改全屏设置，避免黑边遮挡
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 隐藏状态栏和导航栏
        hideSystemUI();

        setContentView(R.layout.activity_game);

        initViews();
        setupGameEngine();

        // 自动开始游戏
        startGame();
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void initViews() {
        gameView = findViewById(R.id.gameView);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        tvGold = findViewById(R.id.tvGold);
        tvHealth = findViewById(R.id.tvHealth);
        tvWave = findViewById(R.id.tvWave);

        // 出怪按钮
        btnStart.setOnClickListener(v -> {
            if (gameEngine != null) {
                gameEngine.spawnEnemyManually();
                Toast.makeText(this, "生成敌人!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "GameEngine为空!", Toast.LENGTH_SHORT).show();
            }
        });

        // 暂停/继续按钮
        btnPause.setOnClickListener(v -> {
            if (gameEngine != null) {
                togglePause();
            } else {
                Toast.makeText(this, "GameEngine为空!", Toast.LENGTH_SHORT).show();
            }
        });

        // 塔选择按钮
        findViewById(R.id.btnArcherTower).setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.ARCHER);
            Toast.makeText(this, "选中弓箭塔", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnCannonTower).setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.CANNON);
            Toast.makeText(this, "选中炮塔", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnMageTower).setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.MAGE);
            Toast.makeText(this, "选中法师塔", Toast.LENGTH_SHORT).show();
        });

        // 初始状态
        btnStart.setEnabled(true);
        btnStart.setText("出怪");
        btnPause.setEnabled(true);
        btnPause.setText("暂停");
    }

    private void setupGameEngine() {
        gameEngine = new GameEngine(this);
        gameEngine.setUpdateListener(this);
        gameView.setGameEngine(gameEngine);
    }

    private void startGame() {
        if (gameEngine == null) return;

        gameEngine.startGame();
        btnPause.setText("暂停");

        // 更新初始 UI 状态
        tvGold.setText("金币: 100");
        tvHealth.setText("生命: 100");
        tvWave.setText("波次: 1");

        Toast.makeText(this, "游戏开始!", Toast.LENGTH_SHORT).show();
    }

    private void togglePause() {
        if (gameEngine == null) return;

        if (gameEngine.isRunning()) {
            gameEngine.pauseGame();
            btnPause.setText("继续");
            Toast.makeText(this, "游戏暂停", Toast.LENGTH_SHORT).show();
        } else {
            gameEngine.resumeGame();
            btnPause.setText("暂停");
            Toast.makeText(this, "游戏继续", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGameStateUpdated(World world) {
        // 更新 UI
        runOnUiThread(() -> {
            gameView.invalidate(); // 强制重绘

            // 更新游戏状态显示
            updateGameStats();
        });
    }

    private void updateGameStats() {
        // 这里可以添加实际的金币、生命值、波次更新逻辑
        // 暂时保持静态显示
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameEngine != null && !gameEngine.isRunning()) {
            gameEngine.resumeGame();
        }
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameEngine != null) {
            gameEngine.pauseGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameEngine != null) {
            gameEngine.stopGame();
        }
    }
}