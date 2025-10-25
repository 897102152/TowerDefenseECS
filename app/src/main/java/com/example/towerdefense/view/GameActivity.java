package com.example.towerdefense.view;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog; // 添加这行导入
import com.example.towerdefense.GameEngine;
import com.example.towerdefense.R;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.ecs.World;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Projectile;
import android.os.Handler;
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
    // ========== 资源显示控件 ==========
    private TextView tvManpower;
    private TextView tvSupply;
    // ========== 建造模式控制 ==========
    private boolean isBuildMode = false;
    private LinearLayout buildMenuLayout;
    private AlertDialog pauseDialog;
    private boolean isGamePaused = false;

    private OnBackPressedCallback onBackPressedCallback;

    // 教程UI组件
    private LinearLayout tutorialOverlay;
    private TextView tutorialTitle;
    private TextView tutorialMessage;
    private TextView tutorialHint;

    /**
     * 初始化教程UI
     */
    private void initTutorialUI() {
        View includedLayout = findViewById(R.id.tutorialOverlay);
        if(includedLayout!=null) {
            tutorialOverlay = findViewById(R.id.tutorialOverlay);
            tutorialTitle = findViewById(R.id.tutorialTitle);
            tutorialMessage = findViewById(R.id.tutorialMessage);
            tutorialHint = findViewById(R.id.tutorialHint);
            System.out.println("GameActivity：教学布局id已传递");
        }
        System.out.println("GameActivity：教学初始化成功");
        // 设置点击监听器，用于教程推进
        if (tutorialOverlay != null) {
            System.out.println("GameActivity：教学点击监听器已设置");
            tutorialOverlay.setOnClickListener(v -> {
                if (gameEngine != null && gameEngine.isTutorialLevel()) {
                    gameEngine.advanceTutorial();
                    System.out.println("GameActivity：调用gameEngine.advanceTutorial方法");
                }
            });
        }
    }

    /**
     * 显示教程提示
     */
    private void showTutorialMessage(String title, String message, String hint) {
        System.out.println("GameActivity: showTutorialMessage被调用，title=" + title);
        runOnUiThread(() -> {
            System.out.println("GameActivity: showTutorialMessage的runOnUiThread中");
            if (tutorialOverlay != null) {
                System.out.println("GameActivity: tutorialOverlay不为null");
            } else {
                System.out.println("GameActivity: tutorialOverlay为null");
            }
            if (tutorialTitle != null && tutorialMessage != null && tutorialHint != null) {
                tutorialTitle.setText(title);
                tutorialMessage.setText(message);
                tutorialHint.setText(hint);
                tutorialOverlay.setVisibility(View.VISIBLE);
                System.out.println("GameActivity: 教程消息已设置并显示");
            } else {
                System.out.println("GameActivity: 教程UI组件有null值");
                System.out.println("tutorialTitle: " + tutorialTitle);
                System.out.println("tutorialMessage: " + tutorialMessage);
                System.out.println("tutorialHint: " + tutorialHint);
            }
        });
    }

    /**
     * 隐藏教程提示
     */
    private void hideTutorialMessage() {
        runOnUiThread(() -> {
            if (tutorialOverlay != null) {
                tutorialOverlay.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 教程步骤开始回调
     */
    @Override
    public void onTutorialStepStarted(GameEngine.TutorialState state, String message) {
        System.out.println("GameActivity：执行onTutorialStepStarted方法");
        runOnUiThread(() -> {
            System.out.println("GameActivity: 进入runOnUiThread，状态 = " + state);
            switch (state) {
                case WELCOME:
                    showTutorialMessage("欢迎进入教程关",
                            "游戏目标：建造防御塔阻止敌人到达终点,每个敌人到达终点会扣除生命值",
                            "点击屏幕继续");

                    break;

                case RESOURCE_EXPLANATION:
                    showTutorialMessage("资源系统",
                            "人力：用于建造防御塔;补给：通过击败敌人获得;当前资源显示在左上角",
                            "点击屏幕继续");
                    break;

                case BUILD_ARCHER_TOWER:
                    showTutorialMessage("建造防御塔",
                            "请按照引导建造三种防御塔:1. 点击右下角建造按钮; 2. 选择弓箭塔; 3. 在指定位置点击建造",
                            "请建造弓箭塔");
                    break;

                case BUILD_CANNON_TOWER:
                    showTutorialMessage("继续建造",
                            "很好！现在请建造炮塔,炮塔伤害高但攻击速度慢",
                            "请建造炮塔");
                    break;

                case BUILD_MAGE_TOWER:
                    showTutorialMessage("最后一种防御塔",
                            "现在请建造法师塔,法师塔射程最远",
                            "请建造法师塔");
                    break;

                case WAITING_FOR_ENEMIES:
                    showTutorialMessage("准备迎敌",
                            "所有防御塔已建造完成！,几秒后敌人将开始出现",
                            "请稍候...");
                    break;

                case COMPLETED:
                    showTutorialMessage("教程完成",
                            "敌人已经开始出现！使用你的防御塔击败它们",
                            "祝你好运！");
                    // 3秒后自动隐藏教程提示
                    new Handler().postDelayed(this::hideTutorialMessage, 3000);
                    System.out.println("GameActivity: 教程已隐藏");
                    break;
            }
        });
    }



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
        initPauseMenu();

        // 设置游戏引擎
        setupGameEngine();


        initTutorialUI();

        // 自动开始游戏
        startGame();

        // 延迟检查系统状态
        new Handler().postDelayed(() -> {
            if (gameEngine != null) {
                gameEngine.checkSystemStatus();
            }
        }, 1000);
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
        // ========== 初始化资源显示控件 ==========
        tvManpower = findViewById(R.id.tvManpower);
        tvSupply = findViewById(R.id.tvSupply);
        // 初始资源显示
        if (tvManpower != null && tvSupply != null) {
            // 初始值从资源管理器获取
            if (gameEngine != null) {
                int manpower = gameEngine.getResourceManager().getManpower();
                int supply = gameEngine.getResourceManager().getSupply();
                tvManpower.setText(String.valueOf(manpower));
                tvSupply.setText(String.valueOf(supply));
            } else {
                // 默认值
                tvManpower.setText("100");
                tvSupply.setText("50");
            }
        }
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
        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Toast.makeText(this, "设置按钮被点击", Toast.LENGTH_SHORT).show();
            showPauseMenu();
        });

        // 初始状态：建造模式关闭
        setBuildMode(false);
    }

    /**
     * 初始化暂停菜单
     */
    private void initPauseMenu() {
        // 创建暂停菜单对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.PauseMenuDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pause_menu, null);
        builder.setView(dialogView);

        // 设置对话框不可取消（必须通过按钮关闭）
        builder.setCancelable(false);

        pauseDialog = builder.create();

        // 设置对话框背景透明
        if (pauseDialog.getWindow() != null) {
            pauseDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 绑定按钮事件
        setupPauseMenuButtons(dialogView);
    }

    /**
     * 设置暂停菜单按钮事件
     */
    private void setupPauseMenuButtons(View dialogView) {
        // 回到游戏按钮
        dialogView.findViewById(R.id.btnResume).setOnClickListener(v -> {
            resumeGameFromPause();
        });

        // 重新开始按钮
        dialogView.findViewById(R.id.btnRestart).setOnClickListener(v -> {
            restartGame();
        });

        // 返回主菜单按钮
        dialogView.findViewById(R.id.btnMainMenu).setOnClickListener(v -> {
            returnToMainMenu();
        });
    }

    /**
     * 显示暂停菜单
     */
    private void showPauseMenu() {
        //Toast.makeText(this, "showPauseMenu方法被调用", Toast.LENGTH_SHORT).show();
        if (pauseDialog != null && !pauseDialog.isShowing()) {
            // 暂停游戏
            pauseGame();
            // 显示暂停菜单
            pauseDialog.show();
            isGamePaused = true;
            System.out.println("GameActivity: 游戏已暂停，显示暂停菜单");
        }
    }

    /**
     * 从暂停状态恢复游戏
     */
    private void resumeGameFromPause() {
        if (pauseDialog != null && pauseDialog.isShowing()) {
            pauseDialog.dismiss();
            resumeGame();
            isGamePaused = false;
            System.out.println("GameActivity: 游戏已恢复");
        }
    }

    /**
     * 暂停游戏
     */
    private void pauseGame() {
        if (gameEngine != null) {
            gameEngine.pauseGame();
        }
        // 如果建造模式开启，先关闭建造模式
        if (isBuildMode) {
            setBuildMode(false);
        }
    }

    /**
     * 恢复游戏
     */
    private void resumeGame() {
        if (gameEngine != null && !gameEngine.isRunning()) {
            gameEngine.resumeGame();
        }
    }

    /**
     * 重新开始游戏
     */
    private void restartGame() {
        if (pauseDialog != null && pauseDialog.isShowing()) {
            pauseDialog.dismiss();
        }

        // 重新创建游戏引擎
        setupGameEngine();
        // 开始游戏
        startGame();

        Toast.makeText(this, "游戏重新开始", Toast.LENGTH_SHORT).show();
        System.out.println("GameActivity: 游戏重新开始");
    }

    /**
     * 返回主菜单
     */
    private void returnToMainMenu() {
        if (pauseDialog != null && pauseDialog.isShowing()) {
            pauseDialog.dismiss();
        }

        // 停止游戏
        if (gameEngine != null) {
            gameEngine.stopGame();
        }

        // 结束当前Activity，返回主菜单
        finish();

        Toast.makeText(this, "返回主菜单", Toast.LENGTH_SHORT).show();
        System.out.println("GameActivity: 返回主菜单");
    }

    /**
     * Activity恢复时的回调

    @Override
    protected void onResume() {
        super.onResume();
        // 如果游戏不是因为暂停菜单而暂停，则恢复游戏
        if (!isGamePaused && gameEngine != null && !gameEngine.isRunning()) {
            gameEngine.resumeGame();
            System.out.println("GameActivity:游戏意外暂停，自动恢复进行");
        }
        hideSystemUI();
    }
     */
    /**
     * Activity暂停时的回调
     */
    @Override
    protected void onPause() {
        super.onPause();
        // 如果不是因为显示暂停菜单而暂停，则暂停游戏
        if (!isGamePaused && gameEngine != null) {
            gameEngine.pauseGame();
        }
    }

    /**
     * Activity销毁时的回调
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (pauseDialog != null && pauseDialog.isShowing()) {
            pauseDialog.dismiss();
        }
        if (gameEngine != null) {
            gameEngine.stopGame();
        }
    }

    /**
     * 处理返回键
     */
    private void setupBackPressedHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 如果暂停菜单正在显示，则关闭它
                if (pauseDialog != null && pauseDialog.isShowing()) {
                    resumeGameFromPause();
                } else {
                    // 否则显示暂停菜单
                    showPauseMenu();
                }
            }
        };

        // 将回调添加到 OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
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
        // 更新建造模式按钮的文本或状态
        updateBuildModeButton(buildMode);
    }

    /**
     * 更新建造模式按钮状态
     */
    private void updateBuildModeButton(boolean buildMode) {
        View buildButton = findViewById(R.id.btnBuildMode);
        if (buildButton instanceof android.widget.Button) {
            android.widget.Button button = (android.widget.Button) buildButton;
            if (buildMode) {
                button.setText("退出建造模式");
                button.setBackgroundColor(Color.RED);
            } else {
                button.setText("进入建造模式");
                button.setBackgroundColor(Color.GREEN);
            }
        }
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
     * 设置游戏引擎并建立关联
     */
    private void setupGameEngine() {
        gameEngine = new GameEngine(this, currentLevelId);
        gameEngine.setUpdateListener(this);
        gameView.setGameEngine(gameEngine);
        // 立即更新资源显示
        if (tvManpower != null && tvSupply != null) {
            int manpower = gameEngine.getResourceManager().getManpower();
            int supply = gameEngine.getResourceManager().getSupply();
            tvManpower.setText(String.valueOf(manpower));
            tvSupply.setText(String.valueOf(supply));
        }
        Toast.makeText(this, "当前关卡: " + currentLevelName, Toast.LENGTH_SHORT).show();
    }


    /**
     * 开始游戏
     */
    private void startGame() {
        if (gameEngine == null) return;
        System.out.println("GameActivity: 准备开始游戏");
        System.out.println("GameActivity: 是否是教程关卡 = " + gameEngine.isTutorialLevel());
        System.out.println("GameActivity: 当前教程状态 = " + gameEngine.getTutorialState());
        // 如果是教程关卡，延迟开始游戏循环
        if (gameEngine.isTutorialLevel()) {
            System.out.println("GameActivity: 教程关卡，延迟开始游戏循环");
            new Handler().postDelayed(() -> {
                System.out.println("GameActivity: 现在开始游戏循环");
                gameEngine.startGame();
            }, 2000); // 延迟2秒，确保教程UI完全初始化
        } else {
            System.out.println("GameActivity: 普通关卡，立即开始游戏循环");
            gameEngine.startGame();
        }
    }

    /**
     * 游戏状态更新回调
     */
    @Override
    public void onGameStateUpdated(World world) {
        runOnUiThread(() -> {
            gameView.invalidate();
            // 添加调试信息
            int enemyCount = 0;
            int towerCount = 0;
            int projectileCount = 0;

            for (Entity entity : world.getAllEntities()) {
                if (entity.hasComponent(Enemy.class)) enemyCount++;
                if (entity.hasComponent(Tower.class)) towerCount++;
                if (entity.hasComponent(Projectile.class)) projectileCount++;
            }

            System.out.println("调试 - 敌人: " + enemyCount +
                    ", 防御塔: " + towerCount +
                    ", 抛射体: " + projectileCount);
        });
    }

    /**
     * 资源更新回调 - 新增方法
     */
    /**
     * 资源更新回调 - 更新UI显示
     */
    @Override
    public void onResourcesUpdated(int manpower, int supply) {
        runOnUiThread(() -> {
            // 更新UI显示资源信息
            if (tvManpower != null) {
                tvManpower.setText(String.valueOf(manpower));
            }
            if (tvSupply != null) {
                tvSupply.setText(String.valueOf(supply));
            }

            System.out.println("GameActivity: 资源更新 - 人力:" + manpower + " 补给:" + supply);
        });
    }

    /**
     * 敌人被击败回调 - 新增方法
     */
    @Override
    public void onEnemyDefeated(Enemy enemy, int reward) {
        runOnUiThread(() -> {
            // 显示击败敌人的反馈
            String enemyName = "";
            switch (enemy.type) {
                case GOBLIN:
                    enemyName = "哥布林";
                    break;
                case ORC:
                    enemyName = "兽人";
                    break;
                case TROLL:
                    enemyName = "巨魔";
                    break;
            }

            String message = "击败" + enemyName + "! 获得补给: " + reward;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            System.out.println("GameActivity: " + message);
        });
    }
    }






