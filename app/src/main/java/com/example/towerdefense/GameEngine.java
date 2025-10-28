package com.example.towerdefense;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.towerdefense.ecs.World;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Health;
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
    private final World world;
    private final Handler gameHandler;
    private Runnable gameLoop;
    private boolean isRunning = false;
    private GameUpdateListener updateListener;
    private final Random random = new Random();

    // 系统引用
    private SpawnSystem spawnSystem;
    private MovementSystem movementSystem;
    private LevelSystem levelSystem;

    // 屏幕尺寸字段
    private int screenWidth;
    private int screenHeight;
    private int currentLevelId;

    // 新增：资源管理器
    private final ResourceManager resourceManager;

    public interface GameUpdateListener {
        void onGameStateUpdated(World world);
        void onResourcesUpdated(int manpower, int supply);
        void onEnemyDefeated(Enemy enemy, int reward);
        void onTutorialStepStarted(TutorialState state, String message);
        void onGameOver(); // 新增：游戏失败回调
    }

    public enum TutorialState {
        WELCOME,
        RESOURCE_EXPLANATION,
        BUILD_ARCHER_TOWER,
        BUILD_CANNON_TOWER,
        BUILD_MAGE_TOWER,
        WAITING_FOR_ENEMIES,
        COMPLETED
    }

    // 添加教程相关字段
    private TutorialState tutorialState = TutorialState.WELCOME;
    private Handler tutorialHandler;
    private boolean isTutorialLevel;
    private int towersBuilt = 0;

    /**
     * 教程中断恢复机制
     */
    private boolean tutorialInterrupted = false;
    private TutorialState interruptedState = null;
    private String interruptedMessage = "";

    // 游戏状态字段
    private int enemiesReachedEnd = 0;
    private int maxEnemiesAllowed = 20;
    private boolean isGameOver = false;

    /**
     * 构造函数 - 初始化游戏引擎
     * @param context Android上下文
     * @param levelId 关卡ID
     */
    public GameEngine(Context context, int levelId) {
        System.out.println("GameEngine: 创建新游戏引擎，关卡ID: " + levelId);
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
        System.out.println("GameEngine: 重置教程状态变量:\nGameEngine: tutorialState = WELCOME\nGameEngine: towersBuilt = 0\nGameEngine: tutorialInterrupted = false;\nGameEngine: interruptedState = null\nGameEngine: interruptedMessage = ");
        if (spawnSystem != null) {
            spawnSystem.reset();
            System.out.println("GameEngine: spawnSystem已重置");

            // 如果不是教程关卡，立即激活生成
            if (!isTutorialLevel) {
                spawnSystem.startSpawning();
                System.out.println("GameEngine: spawnSystem已重置，自动生成敌人");
            }
            else {
                spawnSystem.setActive(false); // 教程关卡初始不激活
                System.out.println("GameEngine: 教程关卡，spawnSystem初始不激活");
            }
        }
    }

    /**
     * 设置关卡
     */
    private void setupLevel(int levelId) {
        System.out.println("GameEngine: 设置关卡 " + levelId);

        this.currentLevelId = levelId;
        this.isTutorialLevel = (levelId == 0);

        // 根据关卡设置最大允许通过的敌人数量
        if (levelId == 0) { // 教学关
            maxEnemiesAllowed = 20;
        } else {
            maxEnemiesAllowed = 10; // 其他关卡默认值
        }

        // 初始化关卡系统
        setupLevelSystem(levelId);
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
        // 如果是教程关卡，确保生成系统不立即开始
        if (isTutorialLevel) {
            spawnSystem.setActive(false);
            System.out.println("GameEngine: 教程关卡，SpawnSystem设置为非激活状态");
        } else {
            spawnSystem.setActive(true);
            System.out.println("GameEngine: 普通关卡，SpawnSystem设置为激活状态");
        }
        System.out.println("GameEngine: 系统初始化完成");
    }

    /**
     * 初始化教程
     */
    private void initializeTutorial() {
        System.out.println("GameEngine: 初始化教程");

        tutorialHandler = new Handler(Looper.getMainLooper());
        tutorialState = TutorialState.WELCOME;
        towersBuilt = 0;
        tutorialInterrupted = false;

        // 延迟显示第一个教程提示
        tutorialHandler.postDelayed(() -> {
            System.out.println("GameEngine: 显示第一个教程提示");
            if (updateListener != null) {
                updateListener.onTutorialStepStarted(tutorialState, "点击屏幕继续");
            }
        }, 1000);
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
     * 设置屏幕尺寸 - 传递给所有需要的系统
     */
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        System.out.println("GameEngine: 屏幕尺寸设置为 " + width + "x" + height);

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

    /**
     * 放置防御塔
     */
    public void placeTower(float x, float y, Tower.Type type) {
        // 根据塔类型获取消耗
        int manpowerCost = 0;
        int supplyCost = 0;

        switch (type) {
            case ARCHER:
                manpowerCost = 10;
                supplyCost = 5;
                break;
            case CANNON:
                manpowerCost = 20;
                supplyCost = 15;
                break;
            case MAGE:
                manpowerCost = 15;
                supplyCost = 10;
                break;
        }
        if (isTutorialLevel) {
            checkTutorialBuildProgress(type);
        }
        // 检查资源是否足够
        if (resourceManager.canConsume(manpowerCost, supplyCost)) {
            // 消耗资源
            resourceManager.consumeManpower(manpowerCost);
            resourceManager.consumeSupply(supplyCost);

            // 创建防御塔
            createTower(x, y, type, manpowerCost, supplyCost);
            System.out.println("GameEngine: 放置防御塔 " + type + "，消耗人力:" + manpowerCost + "，补给:" + supplyCost);

            // 通知UI更新
            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }
        } else {
            System.out.println("GameEngine: 资源不足，无法放置防御塔 " + type);
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
        float attackSpeed = 0;

        switch (type) {
            case ARCHER:
                damage = 10;
                range = 150;
                attackSpeed = 1.0f;
                break;
            case CANNON:
                damage = 25;
                range = 120;
                attackSpeed = 0.5f;
                break;
            case MAGE:
                damage = 15;
                range = 180;
                attackSpeed = 0.8f;
                break;
        }

        tower.addComponent(new Tower(type, damage, range, attackSpeed, manpowerCost, supplyCost));
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

    /**
     * 推进教程到下一步
     */
    public void advanceTutorial() {
        if (!isTutorialLevel) return;

        switch (tutorialState) {
            case WELCOME:
                tutorialState = TutorialState.RESOURCE_EXPLANATION;
                showTutorialMessage("资源系统说明", "人力用于建造防御塔，补给通过击败敌人获得。点击继续");
                break;

            case RESOURCE_EXPLANATION:
                tutorialState = TutorialState.BUILD_ARCHER_TOWER;
                showTutorialMessage("建造弓箭塔", "请点击建造按钮，选择弓箭塔并在指定位置建造");
                break;

            case BUILD_ARCHER_TOWER:
                // 等待玩家建造弓箭塔，这里不自动推进
                break;

            case BUILD_CANNON_TOWER:
                // 等待玩家建造炮塔
                break;

            case BUILD_MAGE_TOWER:
                // 等待玩家建造法师塔
                break;

            case WAITING_FOR_ENEMIES:
                // 等待敌人生成
                break;

            case COMPLETED:
                // 教程完成
                break;
        }
    }

    /**
     * 显示教程消息
     */
    private void showTutorialMessage(String title, String message) {
        if (updateListener != null) {
            updateListener.onTutorialStepStarted(tutorialState, message);
        }
    }

    /**
     * 检查教程建造任务完成情况
     */
    public void checkTutorialBuildProgress(Tower.Type towerType) {
        if (!isTutorialLevel) return;

        switch (tutorialState) {
            case BUILD_ARCHER_TOWER:
                if (towerType == Tower.Type.ARCHER) {
                    towersBuilt++;
                    tutorialState = TutorialState.BUILD_CANNON_TOWER;
                    showTutorialMessage("建造炮塔", "很好！现在请建造一个炮塔");
                } else {
                    // 如果建造了错误的塔类型，中断教程
                    interruptTutorial();
                    showTutorialMessage("建造错误", "请建造弓箭塔而不是" + getTowerTypeName(towerType));
                }
                break;

            case BUILD_CANNON_TOWER:
                if (towerType == Tower.Type.CANNON) {
                    towersBuilt++;
                    tutorialState = TutorialState.BUILD_MAGE_TOWER;
                    showTutorialMessage("建造法师塔", "不错！最后请建造一个法师塔");
                } else {
                    interruptTutorial();
                    showTutorialMessage("建造错误", "请建造炮塔而不是" + getTowerTypeName(towerType));
                }
                break;

            case BUILD_MAGE_TOWER:
                if (towerType == Tower.Type.MAGE) {
                    towersBuilt++;
                    tutorialState = TutorialState.WAITING_FOR_ENEMIES;
                    showTutorialMessage("准备迎敌", "所有防御塔已建造完成！几秒后敌人将开始出现");

                    // 延迟生成敌人
                    tutorialHandler.postDelayed(() -> {
                        spawnSystem.startSpawning();
                        tutorialState = TutorialState.COMPLETED;
                        if (updateListener != null) {
                            updateListener.onTutorialStepStarted(tutorialState, "教程完成！敌人开始出现");
                        }
                    }, 1000);
                } else {
                    interruptTutorial();
                    showTutorialMessage("建造错误", "请建造法师塔而不是" + getTowerTypeName(towerType));
                }
                break;
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
     * 获取当前教程步骤的消息
     */
    private String getCurrentTutorialMessage() {
        switch (tutorialState) {
            case WELCOME:
                return "欢迎进入教程关，游戏目标：建造防御塔阻止敌人到达终点";
            case RESOURCE_EXPLANATION:
                return "资源系统：人力用于建造防御塔，补给通过击败敌人获得";
            case BUILD_ARCHER_TOWER:
                return "请按照引导建造三种防御塔：1. 点击建造按钮 2. 选择弓箭塔 3. 在指定位置点击建造";
            case BUILD_CANNON_TOWER:
                return "很好！现在请建造炮塔，炮塔伤害高但攻击速度慢";
            case BUILD_MAGE_TOWER:
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
            case ARCHER: return "弓箭塔";
            case CANNON: return "炮塔";
            case MAGE: return "法师塔";
            default: return "未知类型";
        }
    }

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
            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    /**
     * 暂停游戏
     */
    public void pauseGame() {
        isRunning = false;
        if (gameLoop != null) {
            gameHandler.removeCallbacks(gameLoop);
        }
        System.out.println("GameEngine: 游戏已暂停");
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

    // Getter方法
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
}