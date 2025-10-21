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
import java.util.List; // 确保有这行导入
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
        void onResourcesUpdated(int manpower, int supply); // 新增：资源更新回调
        void onEnemyDefeated(Enemy enemy, int reward);     // 新增：敌人被击败回调
    }
    /**
     * 构造函数 - 初始化游戏引擎
     * @param context Android上下文
     * @param levelId 关卡ID
     */
    public GameEngine(Context context, int levelId) {
        world = new World();
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
        // 初始化系统
        setupSystems();
        // 初始化关卡系统
        setupLevelSystem(levelId);
    }

    /**
     * 设置游戏系统
     */
    private void setupSystems() {
        System.out.println("GameEngine: 开始设置系统...");

        // 创建系统实例
        spawnSystem = new SpawnSystem();
        movementSystem = new MovementSystem();
        AttackSystem attackSystem = new AttackSystem();

        // 设置系统依赖
        attackSystem.setResourceManager(resourceManager);
        attackSystem.setGameEngine(this);
        movementSystem.setGameEngine(this); // 设置MovementSystem的GameEngine引用

        System.out.println("GameEngine: 系统创建完成");

        // 添加到世界（这会自动调用基类的 setWorld 方法）
        world.addSystem(spawnSystem);
        world.addSystem(movementSystem);
        world.addSystem(attackSystem);

        System.out.println("GameEngine: 系统已添加到世界");

        // 使用公共方法验证世界引用
        System.out.println("=== 系统世界引用验证 ===");
        System.out.println("GameEngine: SpawnSystem.world = " + spawnSystem.isWorldSet());
        System.out.println("GameEngine: MovementSystem.world = " + movementSystem.isWorldSet());
        System.out.println("GameEngine: AttackSystem.world = " + attackSystem.isWorldSet());
        System.out.println("GameEngine: 系统设置完成");
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
        System.out.println("GameEngine: 路径初始化完成，路径数量: " +
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
        }else {
            System.out.println("GameEngine: spawnSystem为null！");
        }
        if (movementSystem != null) {
            System.out.println("GameEngine: 设置MovementSystem的屏幕尺寸");
            movementSystem.setScreenSize(width, height);
        }else {
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
     * 手动生成敌人
     */
    public void spawnEnemyManually() {
        try {
            Entity enemy = world.createEntity();

            Enemy.Type[] types = Enemy.Type.values();
            Enemy.Type type = types[random.nextInt(types.length)];

            // 删除随机选择 pathTag 的代码，因为后面会根据敌人类型设置
            // Path.PathTag[] pathTags = Path.PathTag.values();
            // Path.PathTag pathTag = pathTags[random.nextInt(pathTags.length)];

            int health = 0;
            float speed = 0;
            int reward = 0;
            Path.PathTag pathTag = Path.PathTag.PATH_A; // 默认值

            switch (type) {
                case GOBLIN:
                    health = 30;
                    speed = 50;
                    reward = 5;
                //    pathTag = Path.PathTag.PATH_A;
                    break;
                case ORC:
                    health = 60;
                    speed = 30;
                    reward = 10;
                    pathTag = Path.PathTag.PATH_B;
                    break;
                case TROLL:
                    health = 100;
                    speed = 20;
                    reward = 20;
                //    pathTag = Path.PathTag.PATH_A;
                    break;
            }

            // 获取路径起点作为敌人的初始位置
            float[] startPosition = getPathStartPosition(pathTag);

            // 使用路径起点而不是固定坐标
            enemy.addComponent(new Transform(startPosition[0], startPosition[1]));
            enemy.addComponent(new Health(health));
            enemy.addComponent(new Enemy(type, speed, reward, pathTag));

            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据路径标签获取路径的起点位置
     */
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
            // 可以通过回调通知UI显示资源不足提示
        }
    }

    /**
     * 创建防御塔实体（现在作为内部方法，不暴露给外部）
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

        tower.addComponent(new Tower(type, damage, range, attackSpeed, manpowerCost,supplyCost));
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
     * 获取资源管理器
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
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
     * 新增：获取关卡系统
     */
    public LevelSystem getLevelSystem() {
        return levelSystem;
    }

    /**
     * 新增：切换关卡
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
     * 重新开始游戏
     */
    public void restartGame() {
        stopGame();

        // 清除所有实体
        world.clearEntities();

        // 重新初始化系统
        setupSystems();
        setupLevelSystem(currentLevelId);
        resourceManager.resetResources();
        System.out.println("GameEngine: 游戏已重新开始");
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
}