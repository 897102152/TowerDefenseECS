package com.example.towerdefense;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.towerdefense.ecs.World;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Path;
import com.example.towerdefense.systems.MovementSystem;
import com.example.towerdefense.systems.AttackSystem;
import com.example.towerdefense.systems.SpawnSystem;
import com.example.towerdefense.systems.LevelSystem;
import com.example.towerdefense.managers.ResourceManager;

import java.util.Random;
import java.util.List;

/**
 * 游戏引擎主类 - 采用ECS(Entity-Component-System)架构
 * 负责管理游戏循环、实体创建、系统更新和游戏状态
 */
public class GameEngine {
    // ========== 核心游戏组件 ==========
    private final World world;
    private final Handler gameHandler;
    private Runnable gameLoop;
    private boolean isRunning = false;
    private GameUpdateListener updateListener;
    private final Random random = new Random();

    // ========== 系统引用 ==========
    private SpawnSystem spawnSystem;
    private MovementSystem movementSystem;
    private LevelSystem levelSystem;

    // ========== 游戏状态字段 ==========
    private int screenWidth;
    private int screenHeight;
    private int currentLevelId;
    private int enemiesReachedEnd = 0;
    private int maxEnemiesAllowed = 20;
    private boolean isGameOver = false;
    private boolean isGameWon = false;

    // ========== 资源管理 ==========
    private final ResourceManager resourceManager;

    // ========== 上下文引用 ==========
    private final Context context;

    // ========== 教程系统 ==========
    public enum TutorialState {
        WELCOME,
        RESOURCE_EXPLANATION,
        DEPLOY_INFANTRY,
        DEPLOY_ANTI_TANK,
        DEPLOY_ARTILLERY,
        WAITING_FOR_ENEMIES,
        COMPLETED
    }

    private TutorialState tutorialState = TutorialState.WELCOME;
    private Handler tutorialHandler;
    private boolean isTutorialLevel;
    private int towersBuilt = 0;

    // 教程中断恢复机制
    private boolean tutorialInterrupted = false;
    private TutorialState interruptedState = null;
    private String interruptedMessage = "";
    // ========== 高地区域相关属性 ==========
    private float[] highlandArea = new float[]{0.25f, 0.35f, 0.55f, 0.9f}; // 左上x, 左上y, 右下x, 右下y
    private float[] highlandScreenRect = new float[4]; // 屏幕坐标下的高地矩形
    private float highlandSpeedMultiplier = 0.8f; // 高地内移速降至80%
    // 新增：高地争夺机制
    private int highlandEnemyThreshold = 5; // 高地失守的敌人数量阈值
    private int currentHighlandEnemies = 0; // 当前高地区域内的敌人数量
    private boolean isHighlandControlled = true; // 高地是否由玩家控制（初始为控制状态）
    private long lastHighlandCheckTime = 0; // 上次检查高地状态的时间
    private static final long HIGHLAND_CHECK_INTERVAL = 500; // 检查间隔（毫秒）

    // =====================================================================
    // 接口定义
    // =====================================================================

    public interface GameUpdateListener {
        void onGameStateUpdated(World world);
        void onResourcesUpdated(int manpower, int supply);
        void onEnemyDefeated(Enemy enemy, int reward);
        void onTutorialStepStarted(TutorialState state, String message);
        void onGameOver(); // 新增：游戏失败回调
        void onGameWon();
        // 新增：高地状态变化回调
        void onHighlandStatusChanged(boolean isControlled, int enemyCount);
        void onHighlandEnemyCountUpdated(int enemyCount);
    }

    // =====================================================================
    // 构造函数和初始化
    // =====================================================================

    /**
     * 构造函数 - 初始化游戏引擎
     * @param context Android上下文
     * @param levelId 关卡ID
     */
    public GameEngine(Context context, int levelId) {
        System.out.println("GameEngine: 创建新游戏引擎，关卡ID: " + levelId);
        this.context = context; // 保存Context引用
        world = new World();
        System.out.println("GameEngine: 创建新世界world");
        gameHandler = new Handler(Looper.getMainLooper());

        // 初始化资源管理器
        resourceManager = new ResourceManager(context);

        // 设置资源变化监听器
        resourceManager.setResourceChangeListener(new ResourceManager.ResourceChangeListener() {
            @Override
            public void onResourceChanged(int manpower, int supply) {
                if (updateListener != null) {
                    updateListener.onResourcesUpdated(manpower, supply);
                }
            }
        });

        // 使用整合的初始化方法
        initializeGame(levelId);
    }

    /**
     * 完整的游戏初始化
     */
    public void initializeGame(int levelId) {
        System.out.println("GameEngine: initializeGame()方法已调用");

        // 保存当前屏幕尺寸
        int savedWidth = screenWidth;
        int savedHeight = screenHeight;
        System.out.println("GameEngine: 保存屏幕尺寸: " + savedWidth + "x" + savedHeight);

        // 重置游戏状态
        resetGameState();
        System.out.println("GameEngine: 游戏状态已重置");

        // 恢复屏幕尺寸
        this.screenWidth = savedWidth;
        this.screenHeight = savedHeight;

        // 设置关卡相关
        setupLevel(levelId);
        System.out.println("GameEngine: 关卡代号"+ levelId +"已设置");

        // 初始化系统
        System.out.println("GameEngine: 开始初始化系统");
        initializeSystems();

        // 初始化教程（如果是教程关卡）
        if (isTutorialLevel) {
            initializeTutorial();
        }

        System.out.println("GameEngine: 完整游戏初始化完成");
    }

    /**
     * 重置游戏状态
     */
    private void resetGameState() {
        System.out.println("GameEngine: 开始重置游戏状态");

        // 停止当前游戏
        stopGame();

        // 重置状态变量
        enemiesReachedEnd = 0;
        isGameOver = false;
        isRunning = false;
        isGameWon = false;
        System.out.println("GameEngine: 重置状态变量:\nGameEngine: enemiesReachedEnd = 0\nGameEngine: isGameOver = false\nGameEngine: isRunning = false");

        // 清除所有实体
        world.clearEntities();
        System.out.println("GameEngine: 所有实体已清除");

        // 重置资源管理器
        resourceManager.resetResources();
        System.out.println("GameEngine: 资源管理器已重置");

        // 重置教程状态
        tutorialState = TutorialState.WELCOME;
        towersBuilt = 0;
        tutorialInterrupted = false;
        interruptedState = null;
        interruptedMessage = "";

        // 清除教程Handler的延迟任务
        if (tutorialHandler != null) {
            tutorialHandler.removeCallbacksAndMessages(null);
        }

        System.out.println("GameEngine: 重置教程状态变量:\nGameEngine: tutorialState = WELCOME\nGameEngine: towersBuilt = 0\nGameEngine: tutorialInterrupted = false;\nGameEngine: interruptedState = null\nGameEngine: interruptedMessage = ");

        if (spawnSystem != null) {
            spawnSystem.reset();
            System.out.println("GameEngine: spawnSystem已重置");
        }
    }

    private void setupLevel(int levelId) {
        System.out.println("GameEngine: 设置关卡 " + levelId);

        this.currentLevelId = levelId;
        this.isTutorialLevel = (levelId == 0);

        // 根据关卡设置最大允许通过的敌人数量
        if (levelId == 0) { // 教学关
            maxEnemiesAllowed = 20;
            highlandArea = null; // 教学关没有高地
        } else if (levelId == 1) { // 第一关
            maxEnemiesAllowed = 10;
            // 只在第一关设置高地区域
            highlandArea = new float[]{0.25f, 0.35f, 0.55f, 0.9f}; // 左上x, 左上y, 右下x, 右下y
            // 重置高地状态
            currentHighlandEnemies = 0;
            isHighlandControlled = true;
            lastHighlandCheckTime = 0;
            System.out.println("GameEngine: 第一关 - 启用高地区域争夺机制");
        } else {
            maxEnemiesAllowed = 10; // 其他关卡默认值
            highlandArea = null; // 其他关卡没有高地
        }

        // 初始化关卡系统
        setupLevelSystem(levelId);
    }

    /**
     * 设置关卡系统
     */
    private void setupLevelSystem(int levelId) {
        // 创建关卡系统
        levelSystem = new LevelSystem(levelId);
        // 初始化关卡（创建路径、初始塔等）
        levelSystem.initializeLevel(world);
        // 确保路径实体已经创建
        System.out.println("GameEngine: 关卡路径初始化完成，路径数量: " +
                world.getEntitiesWithComponent(Path.class).size());
    }

    /**
     * 初始化系统
     */
    private void initializeSystems() {
        System.out.println("GameEngine: ----------------初始化系统--------------");
        // 清除现有系统
        world.clearSystems();
        // 创建系统实例
        spawnSystem = new SpawnSystem();
        movementSystem = new MovementSystem();
        AttackSystem attackSystem = new AttackSystem();
        // 设置系统依赖
        attackSystem.setResourceManager(resourceManager);
        attackSystem.setGameEngine(this);
        spawnSystem.setLevelSystem(levelSystem);
        movementSystem.setGameEngine(this);
        // 添加到世界
        world.addSystem(spawnSystem);
        world.addSystem(movementSystem);
        world.addSystem(attackSystem);
        // 关键修复：立即设置屏幕尺寸给新创建的系统
        if (screenWidth > 0 && screenHeight > 0) {
            System.out.println("GameEngine: 立即设置新系统的屏幕尺寸: " + screenWidth + "x" + screenHeight);
            spawnSystem.setScreenSize(screenWidth, screenHeight);
            movementSystem.setScreenSize(screenWidth, screenHeight);
        } else {
            System.out.println("GameEngine: 警告！屏幕尺寸未设置，spawnSystem将无法就绪");
        }
        // 重置SpawnSystem状态
        spawnSystem.reset();

        // 所有关卡初始都不激活生成，等待手动激活
        spawnSystem.setActive(false);
        System.out.println("GameEngine: 所有关卡初始都不激活敌人生成，等待手动开始");

        System.out.println("GameEngine: 系统初始化完成");
    }
    // =====================================================================
    // 游戏循环控制
    // =====================================================================

    public void startGame() {
        if (isRunning) return;

        isRunning = true;
        gameLoop = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    updateGame();
                    gameHandler.postDelayed(this, 16);
                }
            }
        };
        gameHandler.post(gameLoop);
        updateGame();
    }

    private void updateGame() {
        try {
            world.update(0.016f);

            // 检查高地状态（每500毫秒检查一次）
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastHighlandCheckTime >= HIGHLAND_CHECK_INTERVAL) {
                updateHighlandStatus();
                lastHighlandCheckTime = currentTime;
            }

            // 检查胜利条件
            checkWinCondition();
            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 暂停游戏
     */
    public void pauseGame() {
        isRunning = false;
        if (gameLoop != null) {
            gameHandler.removeCallbacks(gameLoop);
        }
        System.out.println("GameEngine: 游戏已暂停");

        // 中断教程显示
        if (isTutorialLevel) {
            interruptTutorial();
        }
    }

    /**
     * 恢复游戏
     */
    public void resumeGame() {
        if (!isRunning) {
            startGame();
        }
        System.out.println("GameEngine: 游戏已恢复");
    }

    /**
     * 停止游戏
     */
    public void stopGame() {
        isRunning = false;
        if (gameLoop != null) {
            gameHandler.removeCallbacks(gameLoop);
        }
        System.out.println("GameEngine: 游戏已停止");
    }

    /**
     * 检查游戏胜利条件
     */
    private void checkWinCondition() {
        if (isGameOver || isGameWon) return;

        // 条件1: 所有波次都已完成
        boolean allWavesCompleted = spawnSystem.areAllWavesCompleted();

        // 条件2: 场上没有存活的敌人
        boolean noEnemiesRemaining = world.getEntitiesWithComponent(Enemy.class).isEmpty();

        if (allWavesCompleted && noEnemiesRemaining) {
            isGameWon = true;
            System.out.println("GameEngine: 游戏胜利！所有敌人都被消灭");

            // 暂停游戏
            pauseGame();

            // 通知游戏胜利
            if (updateListener != null) {
                updateListener.onGameWon();
            }
        }
    }

    // 添加获取胜利状态的方法
    public boolean isGameWon() {
        return isGameWon;
    }

    /**
     * 根据位置查找防御塔
     */
    private Entity findTowerAtPosition(float x, float y) {
        float clickRadius = 50f; // 点击检测半径

        for (Entity entity : world.getEntitiesWithComponent(Tower.class)) {
            Transform transform = entity.getComponent(Transform.class);
            if (transform != null) {
                float distance = (float) Math.sqrt(
                        Math.pow(transform.x - x, 2) + Math.pow(transform.y - y, 2)
                );

                if (distance <= clickRadius) {
                    System.out.println("GameEngine: 找到防御塔，距离: " + distance);
                    return entity;
                }
            }
        }
        return null;
    }

    /**
     * 通过实体ID移除防御塔
     */
    public void removeTowerById(int entityId) {
        Entity towerToRemove = world.getEntityById(entityId);
        if (towerToRemove != null && towerToRemove.hasComponent(Tower.class)) {
            Tower towerComp = towerToRemove.getComponent(Tower.class);
            if (towerComp != null) {
                // 返还人力（不返还补给）
                resourceManager.addManpower(towerComp.manpowerCost);
                System.out.println("GameEngine: 通过ID移除防御塔 " + towerComp.type + "，返还人力:" + towerComp.manpowerCost);

                // 从世界中移除实体
                world.removeEntity(towerToRemove);

                // 通知UI更新
                if (updateListener != null) {
                    updateListener.onGameStateUpdated(world);
                    updateListener.onResourcesUpdated(
                            resourceManager.getManpower(),
                            resourceManager.getSupply()
                    );
                }
            }
        }
    }

    // =====================================================================
    // 敌人事件处理
    // =====================================================================
    /**
     * 启用敌人生成 - 供开始按钮调用
     */
    public void enableEnemySpawning() {
        if (spawnSystem != null && !spawnSystem.isActive()) {
            spawnSystem.setActive(true);
            spawnSystem.startSpawning();
            System.out.println("GameEngine: 敌人生成已启用并开始生成");
        }
    }

    /**
     * 检查敌人生成是否激活
     */
    public boolean isEnemySpawningEnabled() {
        return spawnSystem != null && spawnSystem.isActive();
    }
    /**
     * 敌人被击败时调用（由AttackSystem调用）
     */
    public void onEnemyDefeated(Enemy enemy) {
        if (!enemy.rewardGiven) {
            // 发放补给奖励
            resourceManager.addSupply(enemy.reward);
            enemy.rewardGiven = true;

            System.out.println("GameEngine: 击败敌人 " + enemy.type + "，获得补给:" + enemy.reward);

            // 通知监听器
            if (updateListener != null) {
                updateListener.onEnemyDefeated(enemy, enemy.reward);
            }
        }
    }

    /**
     * 敌人到达终点时调用
     */
    public void onEnemyReachedEnd() {
        if (isGameOver) return;

        enemiesReachedEnd++;
        System.out.println("GameEngine: 敌人到达终点，当前计数: " + enemiesReachedEnd + "/" + maxEnemiesAllowed);

        // 检查是否游戏失败
        if (enemiesReachedEnd >= maxEnemiesAllowed) {
            isGameOver = true;
            System.out.println("GameEngine: 游戏失败！到达终点的敌人数量超过限制");

            // 暂停游戏
            pauseGame();

            // 通知游戏失败
            if (updateListener != null) {
                updateListener.onGameOver();
            }
        }
    }

    // =====================================================================
    // 教程系统
    // =====================================================================

    /**
     * 初始化教程
     */
    private void initializeTutorial() {
        System.out.println("GameEngine: 初始化教程");

        tutorialHandler = new Handler(Looper.getMainLooper());
        tutorialState = TutorialState.WELCOME;
        towersBuilt = 0;
        tutorialInterrupted = false;

        // 延迟显示第一个教程提示，确保UI已初始化
        tutorialHandler.postDelayed(() -> {
            System.out.println("GameEngine: 延迟显示第一个教程提示");
            if (updateListener != null) {
                updateListener.onTutorialStepStarted(tutorialState, "点击屏幕继续");
            }
        }, 500);
    }
    /**
     * 推进教程到下一步
     */
    public void advanceTutorial() {
        if (!isTutorialLevel) return;

        System.out.println("GameEngine: advanceTutorial 被调用，当前状态: " + tutorialState);

        switch (tutorialState) {
            case WELCOME:
                tutorialState = TutorialState.RESOURCE_EXPLANATION;
                System.out.println("GameEngine: 教程状态推进到 RESOURCE_EXPLANATION");
                // 通过监听器更新UI
                if (updateListener != null) {
                    updateListener.onTutorialStepStarted(tutorialState, "资源系统说明");
                }
                break;

            case RESOURCE_EXPLANATION:
                tutorialState = TutorialState.DEPLOY_INFANTRY;
                System.out.println("GameEngine: 教程状态推进到 BUILD_ARCHER_TOWER");
                if (updateListener != null) {
                    updateListener.onTutorialStepStarted(tutorialState, "请建造弓箭塔");
                }
                break;

            case DEPLOY_INFANTRY:
                // 等待玩家建造弓箭塔，这里不自动推进
                System.out.println("GameEngine: BUILD_ARCHER_TOWER 状态，等待玩家建造");
                break;

            case DEPLOY_ANTI_TANK:
                // 等待玩家建造炮塔
                System.out.println("GameEngine: BUILD_CANNON_TOWER 状态，等待玩家建造");
                break;

            case DEPLOY_ARTILLERY:
                // 等待玩家建造法师塔
                System.out.println("GameEngine: BUILD_MAGE_TOWER 状态，等待玩家建造");
                break;

            case WAITING_FOR_ENEMIES:
                // 等待敌人生成
                System.out.println("GameEngine: WAITING_FOR_ENEMIES 状态");
                break;

            case COMPLETED:
                // 教程完成
                System.out.println("GameEngine: 教程已完成");
                break;
        }

        System.out.println("GameEngine: advanceTutorial 完成，新状态: " + tutorialState);
    }

    /**
     * 检查教程建造任务完成情况
     */
    public void checkTutorialBuildProgress(Tower.Type towerType) {
        if (!isTutorialLevel) return;
        System.out.println(" GameEngine: 当前正在建造防御塔type："+ towerType);

        switch (tutorialState) {
            case DEPLOY_INFANTRY:
                if (towerType == Tower.Type.Infantry) {
                    towersBuilt++;
                    tutorialState = TutorialState.DEPLOY_ANTI_TANK;
                    // 直接推进教程，不通过消息系统
                    if (updateListener != null) {
                        updateListener.onTutorialStepStarted(tutorialState, "很好！现在请建造一个炮塔");
                    }
                } else {
                    // 建造错误时显示错误提示，并阻止放置
                    String errorMessage = "教程错误：请建造弓箭塔而不是" + getTowerTypeName(towerType);
                    System.out.println("GameEngine: " + errorMessage);
                    if (updateListener != null) {
                        updateListener.onTutorialStepStarted(tutorialState, errorMessage);
                    }
                    // 阻止放置，返回false
                    throw new TutorialBuildException(errorMessage);
                }
                break;

            case DEPLOY_ANTI_TANK:
                if (towerType == Tower.Type.Anti_tank) {
                    towersBuilt++;
                    tutorialState = TutorialState.DEPLOY_ARTILLERY;
                    if (updateListener != null) {
                        updateListener.onTutorialStepStarted(tutorialState, "不错！最后请建造一个法师塔");
                    }
                } else {
                    String errorMessage = "教程错误：请建造炮塔而不是" + getTowerTypeName(towerType);
                    System.out.println("GameEngine: " + errorMessage);
                    if (updateListener != null) {
                        updateListener.onTutorialStepStarted(tutorialState, errorMessage);
                    }
                    throw new TutorialBuildException(errorMessage);
                }
                break;

            case DEPLOY_ARTILLERY:
                if (towerType == Tower.Type.Artillery) {
                    towersBuilt++;
                    tutorialState = TutorialState.WAITING_FOR_ENEMIES;
                    if (updateListener != null) {
                        updateListener.onTutorialStepStarted(tutorialState, "所有防御塔已建造完成！几秒后敌人将开始出现");
                    }

                    // 延迟生成敌人
                    tutorialHandler.postDelayed(() -> {
                        enableEnemySpawning(); // 使用统一的方法启用敌人生成
                        tutorialState = TutorialState.COMPLETED;
                        if (updateListener != null) {
                            updateListener.onTutorialStepStarted(tutorialState, "教程完成！敌人开始出现");
                        }
                    }, 2000);
                } else {
                    String errorMessage = "教程错误：请建造法师塔而不是" + getTowerTypeName(towerType);
                    System.out.println("GameEngine: " + errorMessage);
                    if (updateListener != null) {
                        updateListener.onTutorialStepStarted(tutorialState, errorMessage);
                    }
                    throw new TutorialBuildException(errorMessage);
                }
                break;
        }
    }

    /**
     * 教程建造异常类 - 用于阻止错误的防御塔放置
     */
    private static class TutorialBuildException extends RuntimeException {
        public TutorialBuildException(String message) {
            super(message);
        }
    }

    /**
     * 获取当前教程步骤要求的防御塔类型
     */
    private Tower.Type getRequiredTowerTypeForTutorial() {
        switch (tutorialState) {
            case DEPLOY_INFANTRY:
                return Tower.Type.Infantry;
            case DEPLOY_ANTI_TANK:
                return Tower.Type.Anti_tank;
            case DEPLOY_ARTILLERY:
                return Tower.Type.Artillery;
            default:
                return null; // 其他状态不要求特定类型
        }
    }

    /**
     * 中断教程
     */
    public void interruptTutorial() {
        if (!isTutorialLevel) return;

        tutorialInterrupted = true;
        interruptedState = tutorialState;
        interruptedMessage = getCurrentTutorialMessage();

        System.out.println("GameEngine: 教程被中断，保存状态: " + interruptedState);
    }

    /**
     * 恢复教程显示
     */
    public void resumeTutorialDisplay() {
        if (!isTutorialLevel || !tutorialInterrupted) return;

        tutorialInterrupted = false;
        System.out.println("GameEngine: 恢复教程显示，状态: " + interruptedState);

        // 通知Activity重新显示教程提示
        if (updateListener != null && interruptedState != null) {
            updateListener.onTutorialStepStarted(interruptedState, interruptedMessage);
        }
    }

    /**
     * 显示教程消息
     */
    private void showTutorialMessage(String title, String message) {
        if (updateListener != null && !tutorialInterrupted) {
            updateListener.onTutorialStepStarted(tutorialState, message);
        }
    }

    /**
     * 获取当前教程步骤的消息
     */
    private String getCurrentTutorialMessage() {
        switch (tutorialState) {
            case WELCOME:
                return "欢迎进入教程关，游戏目标：建造防御塔阻止敌人到达终点";
            case RESOURCE_EXPLANATION:
                return "资源系统：人力用于建造防御塔，补给通过击败敌人获得";
            case DEPLOY_INFANTRY:
                return "请按照引导建造三种防御塔：1. 点击建造按钮 2. 选择弓箭塔 3. 在指定位置点击建造";
            case DEPLOY_ANTI_TANK:
                return "很好！现在请建造炮塔，炮塔伤害高但攻击速度慢";
            case DEPLOY_ARTILLERY:
                return "现在请建造法师塔，法师塔射程最远";
            case WAITING_FOR_ENEMIES:
                return "所有防御塔已建造完成！几秒后敌人将开始出现";
            case COMPLETED:
                return "教程完成！敌人已经开始出现";
            default:
                return "请继续教程";
        }
    }

    /**
     * 获取塔类型名称
     */
    private String getTowerTypeName(Tower.Type type) {
        switch (type) {
            case Infantry: return "弓箭塔";
            case Anti_tank: return "炮塔";
            case Artillery: return "法师塔";
            default: return "未知类型";
        }
    }

    // =====================================================================
    // 屏幕尺寸和路径管理
    // =====================================================================

    /**
     * 设置屏幕尺寸 - 传递给所有需要的系统
     */
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        System.out.println("GameEngine: 屏幕尺寸设置为 " + width + "x" + height);

        // 只在有高地区域的关卡计算屏幕坐标
        if (highlandArea != null) {
            calculateHighlandScreenRect();
        }

        // 验证路径转换
        validatePathCoordinates();

        // 传递屏幕尺寸给各个系统
        if (spawnSystem != null) {
            System.out.println("GameEngine: 设置SpawnSystem的屏幕尺寸");
            spawnSystem.setScreenSize(width, height);
        } else {
            System.out.println("GameEngine: spawnSystem为null！");
        }
        if (movementSystem != null) {
            System.out.println("GameEngine: 设置MovementSystem的屏幕尺寸");
            movementSystem.setScreenSize(width, height);
        } else {
            System.out.println("GameEngine: MovementSystem为null！");
        }
    }


    /**
     * 验证路径坐标转换
     */
    private void validatePathCoordinates() {
        List<Entity> paths = world.getEntitiesWithComponent(Path.class);
        System.out.println("GameEngine: 验证 " + paths.size() + " 条路径");

        for (Entity pathEntity : paths) {
            Path path = pathEntity.getComponent(Path.class);
            float[][] screenPoints = path.convertToScreenCoordinates(screenWidth, screenHeight);
            System.out.println("GameEngine: 路径 " + path.getTag() + " 起点: (" +
                    screenPoints[0][0] + ", " + screenPoints[0][1] + ")");
        }
    }

    /**
     * 根据路径标签获取路径的起点位置
     */
    private float[] getPathStartPosition(Path.PathTag pathTag) {
        // 从世界中找到对应的路径
        for (Entity entity : world.getEntitiesWithComponent(Path.class)) {
            Path path = entity.getComponent(Path.class);
            if (path.getTag() == pathTag) {
                // 使用路径组件的转换方法
                float[][] screenPoints = path.convertToScreenCoordinates(screenWidth, screenHeight);
                if (screenPoints.length > 0) {
                    float startX = screenPoints[0][0];
                    float startY = screenPoints[0][1];
                    System.out.println("GameEngine: 路径 " + pathTag + " 起点: (" + startX + ", " + startY + ")");
                    return new float[]{startX, startY};
                }
            }
        }

        System.err.println("GameEngine: 警告！找不到路径 " + pathTag + "，使用默认起点");
        return new float[]{100, 100};
    }
    //=====================level01特有，高地区域检查===================================
    /**
     * 计算高地区域的屏幕坐标
     */
    private void calculateHighlandScreenRect() {
        if (highlandArea == null) {
            highlandScreenRect = null;
            return;
        }

        highlandScreenRect = new float[4];
        highlandScreenRect[0] = screenWidth * highlandArea[0];  // 左上x
        highlandScreenRect[1] = screenHeight * highlandArea[1]; // 左上y
        highlandScreenRect[2] = screenWidth * highlandArea[2];  // 右下x
        highlandScreenRect[3] = screenHeight * highlandArea[3]; // 右下y

        System.out.println("GameEngine: 高地区域屏幕坐标 - 左上(" + highlandScreenRect[0] + "," + highlandScreenRect[1] +
                "), 右下(" + highlandScreenRect[2] + "," + highlandScreenRect[3] + ")");
    }

    /**
     * 检查位置是否在高地区域内
     */
    public boolean isInHighlandArea(float x, float y) {
        // 如果没有高地区域，直接返回false
        if (highlandArea == null || highlandScreenRect == null) {
            return false;
        }

        return x >= highlandScreenRect[0] && x <= highlandScreenRect[2] &&
                y >= highlandScreenRect[1] && y <= highlandScreenRect[3];
    }

    /**
     * 获取高地减速倍率
     */
    public float getHighlandSpeedMultiplier() {
        return highlandSpeedMultiplier;
    }

    /**
     * 获取高地区域屏幕坐标（用于绘制）
     */
    public float[] getHighlandScreenRect() {
        return highlandScreenRect;
    }

    /**
     * 检查当前关卡是否有高地区域
     */
    public boolean hasHighlandArea() {
        return highlandArea != null;
    }
    /**
     * 更新高地区域状态
     */
    private void updateHighlandStatus() {
        if (!hasHighlandArea()) return;

        // 统计高地区域内的敌人数量
        int previousCount = currentHighlandEnemies;
        currentHighlandEnemies = countEnemiesInHighland();

        // 检查是否需要更新高地控制状态
        boolean previousControlState = isHighlandControlled;

        if (currentHighlandEnemies > highlandEnemyThreshold) {
            // 敌人数量超过阈值，高地失守
            if (isHighlandControlled) {
                isHighlandControlled = false;
                System.out.println("GameEngine: 高地失守！敌人数量: " + currentHighlandEnemies);

                // 通知UI更新
                if (updateListener != null) {
                    updateListener.onHighlandStatusChanged(false, currentHighlandEnemies);
                }
            }
        } else {
            // 敌人数量低于阈值，高地恢复控制
            if (!isHighlandControlled) {
                isHighlandControlled = true;
                System.out.println("GameEngine: 高地重新控制！敌人数量: " + currentHighlandEnemies);

                // 通知UI更新
                if (updateListener != null) {
                    updateListener.onHighlandStatusChanged(true, currentHighlandEnemies);
                }
            }
        }

        // 如果敌人数量变化但控制状态未变，也通知UI更新显示
        if (previousCount != currentHighlandEnemies && previousControlState == isHighlandControlled) {
            if (updateListener != null) {
                updateListener.onHighlandEnemyCountUpdated(currentHighlandEnemies);
            }
        }
    }
    /**
     * 统计高地区域内的敌人数量
     */
    private int countEnemiesInHighland() {
        if (!hasHighlandArea()) return 0;

        int count = 0;
        List<Entity> enemies = world.getEntitiesWithComponent(Enemy.class);

        for (Entity enemy : enemies) {
            Transform transform = enemy.getComponent(Transform.class);
            if (transform != null && isInHighlandArea(transform.x, transform.y)) {
                count++;
            }
        }

        return count;
    }

    // =====================================================================
    // 系统状态检查和调试
    // =====================================================================

    /**
     * 检查所有系统状态
     */
    public void checkSystemStatus() {
        System.out.println("=== 系统状态检查 ===");
        System.out.println("GameEngine: 屏幕尺寸=" + screenWidth + "x" + screenHeight);
        System.out.println("GameEngine: 世界实体数=" + world.getAllEntities().size());

        // 检查路径
        List<Entity> paths = world.getEntitiesWithComponent(Path.class);
        System.out.println("GameEngine: 路径数量=" + paths.size());
        for (Entity path : paths) {
            Path pathComp = path.getComponent(Path.class);
            System.out.println("  - " + pathComp.getTag() + ": " + pathComp.getPercentagePoints().length + "个点");
        }

        // 检查敌人
        List<Entity> enemies = world.getEntitiesWithComponent(Enemy.class);
        System.out.println("GameEngine: 敌人数量=" + enemies.size());
        for (Entity enemy : enemies) {
            Enemy enemyComp = enemy.getComponent(Enemy.class);
            Transform transform = enemy.getComponent(Transform.class);
            System.out.println("  - " + enemyComp.type + " 位置=(" + transform.x + "," + transform.y +
                    ") 路径=" + enemyComp.pathTag + " 索引=" + enemyComp.pathIndex);
        }

        System.out.println("=== 检查完成 ===");
    }

    // =====================================================================
    // 防御塔位置检查
    // =====================================================================

    /**
     * 检查指定位置是否可以放置防御塔
     */
    public boolean canPlaceTower(float x, float y) {
        if (world == null) return false;

        // 检查是否在路径上
        return !isPositionOnPath(x, y);
    }

    /**
     * 检查指定位置是否在路径上
     */
    private boolean isPositionOnPath(float x, float y) {
        List<Entity> pathEntities = world.getEntitiesWithComponent(Path.class);

        for (Entity pathEntity : pathEntities) {
            Path path = pathEntity.getComponent(Path.class);
            if (path != null && path.isVisible()) {
                float[][] screenPoints = path.convertToScreenCoordinates(screenWidth, screenHeight);
                float pathWidth = path.getPathWidth();

                for (int i = 0; i < screenPoints.length - 1; i++) {
                    if (isPointNearLine(x, y,
                            screenPoints[i][0], screenPoints[i][1],
                            screenPoints[i + 1][0], screenPoints[i + 1][1],
                            pathWidth + 20)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查点是否靠近线段
     */
    private boolean isPointNearLine(float px, float py, float x1, float y1, float x2, float y2, float threshold) {
        float A = px - x1;
        float B = py - y1;
        float C = x2 - x1;
        float D = y2 - y1;

        float dot = A * C + B * D;
        float len_sq = C * C + D * D;
        float param = -1;

        if (len_sq != 0) {
            param = dot / len_sq;
        }

        float xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        float dx = px - xx;
        float dy = py - yy;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        return distance <= threshold;
    }

    // =====================================================================
    // 防御塔管理
    // =====================================================================

    /**
     * 统一的防御塔放置方法 - 包含资源判定和位置判定 - 每次放置防御塔调用这个方法
     */
    public boolean placeTowerWithValidation(float x, float y, Tower.Type type) {
        try {
            // 1. 教程类型检查（如果是教程关卡）
            if (isTutorialLevel) {
                Tower.Type requiredType = getRequiredTowerTypeForTutorial();
                if (requiredType != null && type != requiredType) {
                    // 显示错误消息，阻止放置
                    String errorMessage = "教程错误：请建造" + getTowerTypeName(requiredType) + "而不是" + getTowerTypeName(type);
                    System.out.println("GameEngine: " + errorMessage);

                    // 通过教程系统显示错误消息
                    if (updateListener != null) {
                        updateListener.onTutorialStepStarted(tutorialState, errorMessage);
                    }
                    return false;
                }
            }

            // 2. 位置判定
            if (!canPlaceTower(x, y)) {
                System.out.println("GameEngine: 位置不可用，不能在路径上放置防御塔");
                // 只在教程关卡显示路径限制消息
                if (isTutorialLevel && updateListener != null) {
                    updateListener.onTutorialStepStarted(tutorialState, "建造限制：不能在敌人路线上部署防御塔");
                }
                return false;
            }

            // 3. 资源判定
            int manpowerCost = 0;
            int supplyCost = 0;

            switch (type) {
                case Infantry:
                    manpowerCost = 10;
                    supplyCost = 5;
                    break;
                case Anti_tank:
                    manpowerCost = 20;
                    supplyCost = 15;
                    break;
                case Artillery:
                    manpowerCost = 15;
                    supplyCost = 10;
                    break;
            }

            if (!resourceManager.canConsume(manpowerCost, supplyCost)) {
                System.out.println("GameEngine: 资源不足，无法放置防御塔 " + type);
                // 只在教程关卡显示资源不足消息
                if (isTutorialLevel && updateListener != null) {
                    updateListener.onTutorialStepStarted(tutorialState,
                            "资源不足：需要人力 " + manpowerCost + " 和补给 " + supplyCost +
                                    "\n当前：人力 " + resourceManager.getManpower() + " 补给 " + resourceManager.getSupply());
                }
                return false;
            }

            // 所有条件都满足，放置防御塔
            createTower(x, y, type, manpowerCost, supplyCost);

            // 消耗资源
            resourceManager.consumeManpower(manpowerCost);
            resourceManager.consumeSupply(supplyCost);

            System.out.println("GameEngine: 放置防御塔 " + type + "，消耗人力:" + manpowerCost + "，补给:" + supplyCost);

            // 通知UI更新
            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }

            // 教程进度检查
            if (isTutorialLevel) {
                checkTutorialBuildProgress(type);
            }

            return true;
        } catch (TutorialBuildException e) {
            // 教程建造错误，阻止放置但不显示额外消息（已经在checkTutorialBuildProgress中显示）
            System.out.println("GameEngine: 教程建造错误，阻止防御塔放置: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建防御塔实体
     */
    private void createTower(float x, float y, Tower.Type type, int manpowerCost, int supplyCost) {
        Entity tower = world.createEntity();
        tower.addComponent(new Transform(x, y));

        int damage = 0;
        float range = 0;
        float innerRange = 0; // 新增：法师塔的内圈范围
        float attackSpeed = 0;

        // 使用与GameView完全一致的方式计算网格大小
        int gridSize = 60; // 默认值
        if (screenWidth > 0) {
            gridSize = (int) (screenWidth * 0.08f);
            gridSize = Math.max(30, Math.min(gridSize, 100));
        }

        System.out.println("GameEngine: 计算网格大小: " + gridSize + "px (屏幕宽度: " + screenWidth + "px)");

        switch (type) {
            case Infantry:
                // 弓箭塔：2格半径，5x5格子的内切圆
                damage = 10;
                range = 4 * gridSize; // 2格半径
                attackSpeed = 1.0f;//攻击速度
                System.out.println("GameEngine: 弓箭塔攻击范围: " + range + "px (2格半径)");
                break;
            case Anti_tank:
                // 炮塔：1格半径，3x3格子的内切圆
                damage = 25;
                range = 2 * gridSize; // 1格半径
                attackSpeed = 0.5f;
                System.out.println("GameEngine: 炮塔攻击范围: " + range + "px (1格半径)");
                break;
            case Artillery:
                // 法师塔：圆环攻击范围，内圈1.5格，外圈3格
                damage = 50;
                innerRange = 3f * gridSize; // 内圈半径（1.5格）
                range = 6 * gridSize; // 外圈半径（3格）
                attackSpeed = 0.1f;
                System.out.println("GameEngine: 法师塔攻击范围: 内圈" + innerRange + "px, 外圈" + range + "px");
                break;
        }

        // 创建防御塔组件
        Tower towerComponent = new Tower(type, damage, range, attackSpeed, manpowerCost, supplyCost, innerRange);
        tower.addComponent(towerComponent);
    }

    /**
     * 移除防御塔并返还人力（不返还补给）
     */
    public void removeTower(float x, float y) {
        System.out.println("GameEngine: 尝试移除位置 (" + x + ", " + y + ") 的防御塔");

        // 查找点击位置附近的防御塔
        Entity towerToRemove = findTowerAtPosition(x, y);

        if (towerToRemove != null) {
            Tower towerComp = towerToRemove.getComponent(Tower.class);
            if (towerComp != null) {
                // 返还人力（不返还补给）
                resourceManager.addManpower(towerComp.manpowerCost);
                System.out.println("GameEngine: 移除防御塔 " + towerComp.type + "，返还人力:" + towerComp.manpowerCost);

                // 从世界中移除实体
                world.removeEntity(towerToRemove);

                // 通知UI更新
                if (updateListener != null) {
                    updateListener.onGameStateUpdated(world);
                    updateListener.onResourcesUpdated(
                            resourceManager.getManpower(),
                            resourceManager.getSupply()
                    );
                }

                // 只在教程关卡显示移除成功的消息
                if (isTutorialLevel) {
                    gameHandler.post(() -> {
                        if (updateListener != null) {
                            updateListener.onTutorialStepStarted(tutorialState,
                                    "移除防御塔 " + getTowerTypeName(towerComp.type) + "，返还人力: " + towerComp.manpowerCost);
                        }
                    });
                }
                return;
            }
        }

        // 如果没有找到防御塔，只在教程关卡显示提示
        System.out.println("GameEngine: 未找到可移除的防御塔");
        if (isTutorialLevel) {
            gameHandler.post(() -> {
                if (updateListener != null) {
                    updateListener.onTutorialStepStarted(tutorialState, "该位置没有防御塔");
                }
            });
        }
    }

    // =====================================================================
    // Getter和Setter方法
    // =====================================================================

    public World getWorld() {
        return world;
    }

    public void setUpdateListener(GameUpdateListener listener) {
        this.updateListener = listener;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public Entity getPathEntity(Path.PathTag pathTag) {
        return null;
    }

    /**
     * 获取敌人生成系统
     */
    public SpawnSystem getSpawnSystem() {
        return spawnSystem;
    }

    /**
     * 获取关卡系统
     */
    public LevelSystem getLevelSystem() {
        return levelSystem;
    }

    /**
     * 切换关卡
     */
    public void switchLevel(int newLevelId) {
        // 暂停游戏
        pauseGame();

        // 切换关卡
        levelSystem.switchLevel(newLevelId);
        levelSystem.initializeLevel(world);

        // 重新开始游戏
        startGame();
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public TutorialState getTutorialState() {
        return tutorialState;
    }

    public boolean isTutorialLevel() {
        return isTutorialLevel;
    }

    public int getEnemiesReachedEnd() {
        return enemiesReachedEnd;
    }

    public int getMaxEnemiesAllowed() {
        return maxEnemiesAllowed;
    }

    public boolean isGameOver() {
        return isGameOver;
    }
    // 添加高地状态相关的getter方法：
    /**
     * 获取高地控制状态
     */
    public boolean isHighlandControlled() {
        return isHighlandControlled;
    }

    /**
     * 获取当前高地区域内的敌人数量
     */
    public int getHighlandEnemyCount() {
        return currentHighlandEnemies;
    }

    /**
     * 获取高地敌人数量阈值
     */
    public int getHighlandEnemyThreshold() {
        return highlandEnemyThreshold;
    }
}