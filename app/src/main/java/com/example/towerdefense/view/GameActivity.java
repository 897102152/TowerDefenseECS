package com.example.towerdefense.view;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
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

    // ========== 核心游戏组件 ==========
    private GameEngine gameEngine;
    private GameView gameView;
    private int currentLevelId;
    private String currentLevelName;

    // ========== UI控件 ==========
    private TextView tvManpower;
    private TextView tvSupply;
    private LinearLayout buildMenuLayout;
    private LinearLayout tutorialOverlay;
    private TextView tutorialTitle;
    private TextView tutorialMessage;
    private TextView tutorialHint;
    private Button btnStartGame;

    // ========== 状态控制 ==========
    private boolean isBuildMode = false;
    private boolean isGamePaused = false;
    private boolean isGameOver = false;
    private boolean isShowingTutorial = false;
    private boolean isShowingMessage = false;
    private boolean isGameStarted = false;
    private GameEngine.TutorialState currentTutorialState = null;
    private String currentTutorialMessage = "";
    // ========== 消息系统 ==========
    private Handler messageHandler = new Handler();
    private long lastMessageTime = 0;
    private static final long MESSAGE_COOLDOWN = 1000; // 消息冷却时间1秒

    // ========== 对话框 ==========
    private AlertDialog pauseDialog;
    private OnBackPressedCallback onBackPressedCallback;
    // ========== 建造模式按钮高亮状态 ==========
    private View currentSelectedButton = null;
    private int defaultButtonColor = Color.GRAY;
    private int selectedButtonColor = Color.BLUE;

    // =====================================================================
    // Activity生命周期方法
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取关卡信息
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

        // 使用整合的初始化方法
        initializeGame();

        // 设置返回键处理
        setupBackPressedHandler();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 如果不是因为显示暂停菜单而暂停，则暂停游戏
        if (!isGamePaused && gameEngine != null) {
            gameEngine.pauseGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 恢复教程显示（如果有中断的教程）
        if (gameEngine != null && gameEngine.isTutorialLevel()) {
            gameEngine.resumeTutorialDisplay();
        }
    }

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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
    //================level Highland相关回调=======================================
    /**
     * 高地状态变化回调
     */
    @Override
    public void onHighlandStatusChanged(boolean isControlled, int enemyCount) {
        runOnUiThread(() -> {
            String status = isControlled ? "已控制" : "已失守";
            String message = "高地区域" + status + "！当前敌人数量: " + enemyCount;

            // 只在第一关显示高地状态消息
            if (currentLevelId == 1) {
                displayGameMessage("高地状态", message, "敌人数量决定高地控制权", true);
            }

            System.out.println("GameActivity: " + message);
        });
    }

    /**
     * 高地敌人数量更新回调
     */
    @Override
    public void onHighlandEnemyCountUpdated(int enemyCount) {
        // 不需要特殊处理，UI会在下次绘制时更新
        System.out.println("GameActivity: 高地区域敌人数量更新: " + enemyCount);
    }
    /**
     * 敌人被击败回调 - 添加伤害类型信息
     */
    @Override
    public void onEnemyDefeated(Enemy enemy, int reward) {
        // 可以在这里添加伤害类型的提示信息
        String damageInfo = getDamageTypeInfo(enemy.type);
        System.out.println("GameActivity: 敌人被击败 - " + enemy.type + "，奖励: " + reward + "，伤害特性: " + damageInfo);

        // 如果是教程关卡，可以显示伤害类型说明
        if (gameEngine != null && gameEngine.isTutorialLevel()) {
            displayGameMessage("伤害类型",
                    "击败" + getEnemyTypeName(enemy.type) + "，" + damageInfo,
                    "不同敌人对不同类型的防御塔有不同抗性", true);
        }
    }

    /**
     * 获取敌人伤害类型信息
     */
    private String getDamageTypeInfo(Enemy.Type enemyType) {
        switch (enemyType) {
            case GOBLIN:
                return "受到所有类型伤害+25%";
            case ORC:
                return "受到标准伤害";
            case TROLL:
                return "对弓箭伤害-50%，对炮击和魔法伤害+25%";
            default:
                return "未知伤害类型";
        }
    }

    /**
     * 获取敌人类型名称
     */
    private String getEnemyTypeName(Enemy.Type enemyType) {
        switch (enemyType) {
            case GOBLIN: return "哥布林";
            case ORC: return "兽人";
            case TROLL: return "巨魔";
            default: return "未知敌人";
        }
    }
    // =====================================================================
    // 游戏初始化相关方法
    // =====================================================================

    /**
     * 完整的游戏初始化
     */
    private void initializeGame() {
        System.out.println("GameActivity: initializeGame()开始游戏初始化进程");

        // 重置游戏开始状态
        isGameStarted = (currentLevelId == 0); // 教程关卡自动开始

        // 重置UI状态
        resetUIState();
        System.out.println("GameActivity: initializeGame()重置UI状态");

        // 初始化UI组件
        initializeUI();
        System.out.println("GameActivity: initializeGame()重新初始化UI");

        // 设置游戏引擎
        setupGameEngine();
        System.out.println("GameActivity: initializeGame()设置游戏引擎GameEngine");

        // 确保屏幕尺寸被设置
        if (gameView != null && gameEngine != null) {
            int width = gameView.getWidth();
            int height = gameView.getHeight();
            if (width > 0 && height > 0) {
                gameEngine.setScreenSize(width, height);
                System.out.println("GameActivity: 设置游戏引擎屏幕尺寸: " + width + "x" + height);
            } else {
                // 如果GameView还没有测量完成，延迟设置
                gameView.post(() -> {
                    int w = gameView.getWidth();
                    int h = gameView.getHeight();
                    if (w > 0 && h > 0 && gameEngine != null) {
                        gameEngine.setScreenSize(w, h);
                        System.out.println("GameActivity: 延迟设置游戏引擎屏幕尺寸: " + w + "x" + h);
                    }
                });
            }
        }

        // 开始游戏
        startGame();
        System.out.println("GameActivity: 完整游戏进程初始化完成");
    }

    /**
     * 重置UI状态
     */
    private void resetUIState() {
        System.out.println("GameActivity: 重置UI状态");

        isGameOver = false;
        isShowingTutorial = false;
        isShowingMessage = false;
        isGameStarted = (currentLevelId == 0); // 重置游戏开始状态
        currentTutorialState = null;
        currentTutorialMessage = "";

        // 清除所有消息和延迟任务
        messageHandler.removeCallbacksAndMessages(null);
        lastMessageTime = 0;

        // 隐藏教程消息
        if (tutorialOverlay != null) {
            tutorialOverlay.setVisibility(View.GONE);
        }

        // 清除按钮选中状态
        clearButtonSelection();

        // 重置建造模式
        isBuildMode = false;
        if (gameView != null) {
            gameView.setBuildMode(false);
            gameView.setShowGrid(false);
        }
        if (buildMenuLayout != null) {
            buildMenuLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        System.out.println("GameActivity: 初始化UI组件");

        // 初始化视图
        initViews();

        // 初始化暂停菜单
        initPauseMenu();

        // 初始化教程UI（如果是教程关卡）
        initTutorialUI();

        // 更新资源显示
        updateResourceDisplay();
    }

    /**
     * 初始化所有视图组件和事件监听器
     */
    private void initViews() {
        // 查找并绑定视图组件
        gameView = findViewById(R.id.gameView);

        // 设置 GameView 监听器 - 修复无限递归问题
        if (gameView != null) {
            gameView.setGameViewListener(new GameView.GameViewListener() {
                @Override
                public void showGameMessage(String title, String message, String hint, boolean autoHide) {
                    // 只在教程关卡显示消息
                    if (gameEngine != null && gameEngine.isTutorialLevel()) {
                        GameActivity.this.displayGameMessage(title, message, hint, autoHide);
                    }
                }
            });
        }

        buildMenuLayout = findViewById(R.id.buildMenuLayout);

        // ========== 初始化资源显示控件 ==========
        tvManpower = findViewById(R.id.tvManpower);
        tvSupply = findViewById(R.id.tvSupply);

        // 初始资源显示
        updateResourceDisplay();

        // ========== 初始化开始按钮 ==========
        btnStartGame = findViewById(R.id.btnStartGame);
        if (btnStartGame != null) {
            btnStartGame.setOnClickListener(v -> {
                startGameWave();
            });

            // 只在非教程关卡显示开始按钮
            if (currentLevelId != 0) {
                btnStartGame.setVisibility(View.VISIBLE);
                System.out.println("GameActivity: 非教程关卡，显示开始按钮");
            } else {
                btnStartGame.setVisibility(View.GONE);
            }
        }

        // ========== 建造模式主按钮 ==========
        findViewById(R.id.btnBuildMode).setOnClickListener(v -> {
            toggleBuildMode();
        });

        // ========== 建造菜单中的塔选择按钮 ==========
        // 弓箭塔选择按钮
        View btnArcherTower = findViewById(R.id.btnArcherTower);
        btnArcherTower.setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.ARCHER);
            setButtonSelected(btnArcherTower);
        });

        // 炮塔选择按钮
        View btnCannonTower = findViewById(R.id.btnCannonTower);
        btnCannonTower.setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.CANNON);
            setButtonSelected(btnCannonTower);
        });

        // 法师塔选择按钮
        View btnMageTower = findViewById(R.id.btnMageTower);
        btnMageTower.setOnClickListener(v -> {
            gameView.setSelectedTowerType(Tower.Type.MAGE);
            setButtonSelected(btnMageTower);
        });

        // 移除按钮 - 新增移除功能
        View btnBuildRemove = findViewById(R.id.btnBuildRemove);
        btnBuildRemove.setOnClickListener(v -> {
            if (gameView.isRemoveMode()) {
                // 如果已经在移除模式，则退出移除模式
                gameView.setRemoveMode(false);
                clearButtonSelection();
                // 只在教程关卡显示消息
                if (gameEngine != null && gameEngine.isTutorialLevel()) {
                    displayGameMessage("移除模式", "退出移除模式", "现在可以建造防御塔", true);
                }
            } else {
                // 进入移除模式
                gameView.setRemoveMode(true);
                setButtonSelected(btnBuildRemove);
                // 只在教程关卡显示消息
                if (gameEngine != null && gameEngine.isTutorialLevel()) {
                    displayGameMessage("移除模式", "移除模式开启", "点击防御塔可移除", true);
                }
            }
        });

        // 设置按钮
        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            showPauseMenu();
        });

        // 初始状态：建造模式关闭
        setBuildMode(false);
    }

    /**
     * 设置游戏引擎并建立关联
     */
    private void setupGameEngine() {
        System.out.println("GameActivity: 开始初始化游戏引擎GameEngine");

        // 如果已有游戏引擎，先停止它
        if (gameEngine != null) {
            gameEngine.stopGame();
        }

        // 创建新的游戏引擎实例
        gameEngine = new GameEngine(this, currentLevelId);
        gameEngine.setUpdateListener(this);
        gameView.setGameEngine(gameEngine);
        gameView.setLevelId(currentLevelId);
        System.out.println("GameActivity: 已创建新的GameEngine实例");

        // 只在非教程关卡显示开始消息
        if (!gameEngine.isTutorialLevel()) {
            // 不显示任何消息
        }
    }

    // =====================================================================
    // 游戏控制方法
    // =====================================================================

    /**
     * 开始游戏
     */
    private void startGame() {
        if (gameEngine == null) return;
        System.out.println("GameActivity: 系统准备就绪，准备开始游戏");
        System.out.println("GameActivity: 是否是教程关卡 = " + gameEngine.isTutorialLevel());
        System.out.println("GameActivity: 当前教程状态 = " + gameEngine.getTutorialState());

        // 如果是教程关卡，延迟开始游戏循环
        if (gameEngine.isTutorialLevel()) {
            System.out.println("GameActivity: 教程关卡，延迟开始游戏循环");
            new Handler().postDelayed(() -> {
                System.out.println("GameActivity: 现在开始游戏循环");
                gameEngine.startGame();
                // 教程关卡不显示额外消息，由教程系统控制
            }, 1000);
        } else {
            System.out.println("GameActivity: 普通关卡，立即开始游戏循环");
            // 普通关卡开始游戏循环，但敌人生成系统保持关闭
            gameEngine.startGame();
            System.out.println("GameActivity: 普通关卡，敌人生成系统初始为关闭状态");
        }
    }
    /**
     * 开始游戏波次
     */
    private void startGameWave() {
        if (gameEngine != null && !isGameStarted) {
            isGameStarted = true;
            btnStartGame.setVisibility(View.GONE);

            // 启用敌人生成
            gameEngine.enableEnemySpawning();

            System.out.println("GameActivity: 游戏波次开始 - 敌人生成已启用");
        }
    }

    /**
     * 重新开始游戏
     */
    private void restartGame() {
        System.out.println("GameActivity: 正在重置游戏进程");

        if (pauseDialog != null && pauseDialog.isShowing()) {
            pauseDialog.dismiss();
        }

        // 保存当前屏幕尺寸
        int currentWidth = gameView.getWidth();
        int currentHeight = gameView.getHeight();
        System.out.println("GameActivity: 当前屏幕尺寸: " + currentWidth + "x" + currentHeight);

        // 重置UI状态
        resetUIState();

        // 重新初始化UI组件
        initializeUI();

        // 设置游戏引擎
        setupGameEngine();

        // 确保屏幕尺寸被重新设置
        if (gameEngine != null && currentWidth > 0 && currentHeight > 0) {
            gameEngine.setScreenSize(currentWidth, currentHeight);
            System.out.println("GameActivity: 重新设置游戏引擎屏幕尺寸: " + currentWidth + "x" + currentHeight);
        } else {
            System.out.println("GameActivity: 无法设置屏幕尺寸，gameEngine=" + (gameEngine != null) +
                    ", width=" + currentWidth + ", height=" + currentHeight);
        }

        // 如果是教程关卡，确保教程系统正确初始化
        if (gameEngine != null && gameEngine.isTutorialLevel()) {
            System.out.println("GameActivity: 教程关卡重新开始，当前教程状态: " + gameEngine.getTutorialState());

            // 延迟触发第一个教程提示，确保系统完全初始化
            new Handler().postDelayed(() -> {
                if (gameEngine != null) {
                    System.out.println("GameActivity: 延迟触发第一个教程提示");
                    // 直接调用我们自己的 onTutorialStepStarted 方法
                    onTutorialStepStarted(GameEngine.TutorialState.WELCOME, "点击屏幕继续");
                }
            }, 500);
        }

        // 确保游戏开始
        startGame();
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

    // =====================================================================
    // 建造模式相关方法
    // =====================================================================
    /**
     * 设置按钮选中状态
     */
    private void setButtonSelected(View button) {
        // 清除之前选中的按钮状态
        clearButtonSelection();

        // 设置新按钮为选中状态
        currentSelectedButton = button;
        button.setBackgroundColor(selectedButtonColor);

        // 如果是移除按钮，需要特殊处理
        if (button.getId() == R.id.btnBuildRemove) {
            // 移除模式已经在上层处理中设置
        } else {
            // 确保退出移除模式
            gameView.setRemoveMode(false);
        }
    }

    /**
     * 清除按钮选中状态
     */
    private void clearButtonSelection() {
        if (currentSelectedButton != null) {
            currentSelectedButton.setBackgroundColor(defaultButtonColor);
            currentSelectedButton = null;
        }

        // 重置所有建造按钮的默认颜色
        View btnArcherTower = findViewById(R.id.btnArcherTower);
        View btnCannonTower = findViewById(R.id.btnCannonTower);
        View btnMageTower = findViewById(R.id.btnMageTower);
        View btnBuildRemove = findViewById(R.id.btnBuildRemove);

        if (btnArcherTower != null) btnArcherTower.setBackgroundColor(defaultButtonColor);
        if (btnCannonTower != null) btnCannonTower.setBackgroundColor(defaultButtonColor);
        if (btnMageTower != null) btnMageTower.setBackgroundColor(defaultButtonColor);
        if (btnBuildRemove != null) btnBuildRemove.setBackgroundColor(defaultButtonColor);
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

        // 如果关闭建造模式，清除所有按钮选中状态
        if (!buildMode) {
            clearButtonSelection();
            gameView.setRemoveMode(false);
        } else {
            // 如果开启建造模式，默认选中第一个按钮（弓箭塔）
            View btnArcherTower = findViewById(R.id.btnArcherTower);
            if (btnArcherTower != null) {
                setButtonSelected(btnArcherTower);
                gameView.setSelectedTowerType(Tower.Type.ARCHER);
            }
        }
    }

    /**
     * 切换建造模式
     */
    private void toggleBuildMode() {
        isBuildMode = !isBuildMode;
        setBuildMode(isBuildMode);

        // 只在教程关卡显示消息
        if (gameEngine != null && gameEngine.isTutorialLevel()) {
            String message = isBuildMode ? "建造模式开启" : "建造模式关闭";
            displayGameMessage("建造模式", message, isBuildMode ? "现在可以放置防御塔" : "建造功能已禁用", true);
        }
    }
    // =====================================================================
    // 暂停菜单相关方法
    // =====================================================================

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
            System.out.println("GameActivity监听器: 已点击回到游戏按钮");
            resumeGameFromPause();
        });

        // 重新开始按钮
        dialogView.findViewById(R.id.btnRestart).setOnClickListener(v -> {
            System.out.println("GameActivity监听器: 已点击重新开始按钮");
            pauseDialog.dismiss();
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
    }

    // =====================================================================
    // 教程系统相关方法（完全独立）
    // =====================================================================
    /**
     * 初始化教程UI
     */
    private void initTutorialUI() {
        View includedLayout = findViewById(R.id.tutorialOverlay);
        if(includedLayout != null) {
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
                System.out.println("GameActivity: 教程提示框被点击");
                System.out.println("GameActivity: isShowingTutorial=" + isShowingTutorial + ", isShowingMessage=" + isShowingMessage);

                // 确保游戏引擎已经初始化并且是教程关卡
                if (gameEngine != null && gameEngine.isTutorialLevel()) {
                    if (isShowingTutorial) {
                        // 只有在显示教程消息时才推进教程
                        System.out.println("GameActivity: 调用 advanceTutorial");
                        gameEngine.advanceTutorial();
                    } else if (isShowingMessage) {
                        // 如果是普通消息，点击隐藏
                        System.out.println("GameActivity: 隐藏普通消息");
                        hideGameMessage();
                    }
                } else {
                    System.out.println("GameActivity: 条件不满足 - gameEngine=" + (gameEngine != null) +
                            ", isTutorialLevel=" + (gameEngine != null && gameEngine.isTutorialLevel()));
                }
            });
        }
    }
    /**
     * 显示教程提示 - 只由教程系统调用
     */
    private void showTutorialMessage(String title, String message, String hint) {
        runOnUiThread(() -> {
            System.out.println("GameActivity: showTutorialMessage - " + title);

            isShowingTutorial = true;
            isShowingMessage = false; // 确保普通消息标志为false

            if (tutorialTitle != null && tutorialMessage != null && tutorialHint != null) {
                tutorialTitle.setText(title);
                tutorialMessage.setText(message);
                tutorialHint.setText(hint);
                tutorialOverlay.setVisibility(View.VISIBLE);

                // 确保教程提示框可以接收点击事件
                tutorialOverlay.setClickable(true);
                tutorialOverlay.setFocusable(true);

                System.out.println("GameActivity: 教程消息已显示 - " + title);
            } else {
                System.err.println("GameActivity: 教程UI组件为null");
            }
        });
    }

    /**
     * 隐藏教程消息 - 只由教程系统调用
     */
    private void hideTutorialMessage() {
        runOnUiThread(() -> {
            isShowingTutorial = false;
            if (tutorialOverlay != null) {
                tutorialOverlay.setVisibility(View.GONE);
            }
        });
    }

    // =====================================================================
    // 统一的消息显示系统（使用教程提示框）
    // =====================================================================

    /**
     * 显示游戏消息 - 统一使用教程提示框显示
     * 这是公开接口，供外部调用
     */
    void showGameMessage(String title, String message, String hint, boolean autoHide) {
        // 只在教程关卡显示消息
        if (gameEngine != null && gameEngine.isTutorialLevel()) {
            displayGameMessage(title, message, hint, autoHide);
        }
    }

    /**
     * 内部实现的消息显示方法 - 使用教程提示框显示所有消息
     */
    private void displayGameMessage(String title, String message, String hint, boolean autoHide) {
        runOnUiThread(() -> {
            // 如果正在显示教程消息，不显示普通消息
            if (isShowingTutorial) {
                System.out.println("GameActivity: 正在显示教程消息，忽略普通消息: " + title);
                return;
            }

            // 防止消息过快重复显示
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMessageTime < MESSAGE_COOLDOWN) {
                System.out.println("GameActivity: 消息显示过快，忽略");
                return;
            }
            lastMessageTime = currentTime;

            // 如果正在显示消息，先隐藏当前消息
            if (isShowingMessage) {
                hideGameMessageImmediately();
            }

            isShowingMessage = true;

            // 使用教程提示框显示消息
            showMessageAsTutorialOverlay(title, message, hint, autoHide);

            // 如果设置了自动隐藏，延迟隐藏消息
            if (autoHide) {
                messageHandler.postDelayed(() -> {
                    hideGameMessage();
                }, 3000);
            }
        });
    }

    /**
     * 使用教程提示框显示消息
     */
    private void showMessageAsTutorialOverlay(String title, String message, String hint, boolean autoHide) {
        runOnUiThread(() -> {
            if (tutorialTitle != null && tutorialMessage != null && tutorialHint != null) {
                tutorialTitle.setText(title);
                tutorialMessage.setText(message);
                tutorialHint.setText(hint);
                tutorialOverlay.setVisibility(View.VISIBLE);

                // 设置点击行为
                tutorialOverlay.setOnClickListener(v -> {
                    if (autoHide) {
                        hideGameMessage();
                    }
                    // 对于非自动隐藏的消息，点击不做特殊处理
                });

                System.out.println("GameActivity: 显示消息 - " + title + ": " + message);
            }
        });
    }

    /**
     * 隐藏游戏消息
     */
    private void hideGameMessage() {
        runOnUiThread(() -> {
            isShowingMessage = false;
            if (tutorialOverlay != null) {
                tutorialOverlay.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 立即隐藏游戏消息
     */
    private void hideGameMessageImmediately() {
        runOnUiThread(() -> {
            isShowingMessage = false;
            // 移除所有待处理的消息隐藏任务
            messageHandler.removeCallbacksAndMessages(null);
            if (tutorialOverlay != null) {
                tutorialOverlay.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 清除所有消息状态
     */
    private void clearAllMessages() {
        runOnUiThread(() -> {
            isShowingMessage = false;
            lastMessageTime = 0;
            messageHandler.removeCallbacksAndMessages(null);

            // 隐藏教程消息
            if (isShowingTutorial) {
                hideTutorialMessage();
            }
        });
    }

    // =====================================================================
    // UI更新和资源管理
    // =====================================================================

    /**
     * 更新资源显示
     */
    private void updateResourceDisplay() {
        if (tvManpower != null && tvSupply != null && gameEngine != null) {
            int manpower = gameEngine.getResourceManager().getManpower();
            int supply = gameEngine.getResourceManager().getSupply();
            tvManpower.setText(String.valueOf(manpower));
            tvSupply.setText(String.valueOf(supply));
        }
    }

    // =====================================================================
    // 游戏结束处理
    // =====================================================================

    /**
     * 显示游戏失败菜单
     */
    private void showGameOverMenu() {
        runOnUiThread(() -> {
            // 创建游戏失败对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.PauseMenuDialogTheme);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_pause_menu, null);
            builder.setView(dialogView);
            builder.setCancelable(false);

            AlertDialog gameOverDialog = builder.create();

            // 设置对话框背景透明
            if (gameOverDialog.getWindow() != null) {
                gameOverDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // 修改对话框标题和按钮
            TextView title = dialogView.findViewById(R.id.dialogTitle);
            if (title != null) {
                title.setText("游戏失败");
            }

            // 隐藏"回到游戏"按钮，因为游戏已经失败
            View btnResume = dialogView.findViewById(R.id.btnResume);
            if (btnResume != null) {
                btnResume.setVisibility(View.GONE);
            }

            // 设置按钮事件
            setupGameOverMenuButtons(dialogView, gameOverDialog);

            // 显示对话框
            gameOverDialog.show();
        });
    }

    /**
     * 设置游戏失败菜单按钮事件
     */
    private void setupGameOverMenuButtons(View dialogView, AlertDialog dialog) {
        // 重新开始按钮
        dialogView.findViewById(R.id.btnRestart).setOnClickListener(v -> {
            dialog.dismiss();
            restartGame();
        });

        // 返回主菜单按钮
        dialogView.findViewById(R.id.btnMainMenu).setOnClickListener(v -> {
            dialog.dismiss();
            returnToMainMenu();
        });
    }

    // =====================================================================
    // 系统UI控制
    // =====================================================================

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

    // =====================================================================
    // GameEngine.GameUpdateListener 接口实现
    // =====================================================================

    /**
     * 游戏状态更新回调
     */
    @Override
    public void onGameStateUpdated(World world) {
        runOnUiThread(() -> {
            gameView.invalidate();

            // 更新资源显示
            updateResourceDisplay();

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
     * 教程步骤开始回调
     */
    @Override
    public void onTutorialStepStarted(GameEngine.TutorialState state, String message) {
        System.out.println("GameActivity: onTutorialStepStarted 被调用，状态=" + state + ", 消息=" + message);

        runOnUiThread(() -> {
            currentTutorialState = state;

            switch (state) {
                case WELCOME:
                    showTutorialMessage("欢迎进入教程关",
                            "游戏目标：建造防御塔阻止敌人到达终点\n每个敌人到达终点会扣除生命值",
                            "点击屏幕继续");
                    break;

                case RESOURCE_EXPLANATION:
                    showTutorialMessage("资源系统",
                            "人力：用于建造防御塔\n补给：通过击败敌人获得\n当前资源显示在左上角",
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
                    // 教程完成时不显示消息
                    System.out.println("GameActivity: 教程完成，隐藏教程消息");
                    hideTutorialMessage();
                    break;

                default:
                    System.out.println("GameActivity: 未知教程状态: " + state);
                    break;
            }
        });
    }
    /**
     * 游戏失败回调
     */
    @Override
    public void onGameOver() {
        runOnUiThread(() -> {
            isGameOver = true;
            System.out.println("GameActivity: 游戏失败回调触发");

            // 不显示失败消息，直接显示失败菜单
            showGameOverMenu();
        });
    }

    @Override
    public void onGameWon() {
        showGameWinMenu();
    }

    /**
     * 显示游戏胜利菜单
     */
    private void showGameWinMenu() {
        runOnUiThread(() -> {
            // 创建游戏胜利对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.PauseMenuDialogTheme);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_pause_menu, null);
            builder.setView(dialogView);
            builder.setCancelable(false);

            AlertDialog gameWinDialog = builder.create();

            if (gameWinDialog.getWindow() != null) {
                gameWinDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // 修改对话框标题
            TextView title = dialogView.findViewById(R.id.dialogTitle);
            if (title != null) {
                title.setText("游戏胜利");
            }

            // 隐藏"回到游戏"按钮
            View btnResume = dialogView.findViewById(R.id.btnResume);
            if (btnResume != null) {
                btnResume.setVisibility(View.GONE);
            }

            // 设置按钮事件
            setupGameWinMenuButtons(dialogView, gameWinDialog);

            gameWinDialog.show();
        });
    }

    /**
     * 设置游戏胜利菜单按钮事件
     */
    private void setupGameWinMenuButtons(View dialogView, AlertDialog dialog) {
        // 重新开始按钮
        dialogView.findViewById(R.id.btnRestart).setOnClickListener(v -> {
            dialog.dismiss();
            restartGame();
        });

        // 返回主菜单按钮
        dialogView.findViewById(R.id.btnMainMenu).setOnClickListener(v -> {
            dialog.dismiss();
            returnToMainMenu();
        });
    }

}