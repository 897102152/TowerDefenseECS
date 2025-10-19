package com.example.towerdefense;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

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

import java.util.Random;

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

    // 新增：关卡系统
    private LevelSystem levelSystem;

    public interface GameUpdateListener {
        void onGameStateUpdated(World world);
    }

    /**
     * 构造函数 - 初始化游戏引擎
     * @param context Android上下文
     * @param levelId 关卡ID
     */
    public GameEngine(Context context, int levelId) {
        world = new World();
        gameHandler = new Handler(Looper.getMainLooper());

        // 初始化关卡系统
        setupLevelSystem(levelId);
        // 初始化游戏系统
        setupSystems();
    }

    /**
     * 设置关卡系统
     */
    private void setupLevelSystem(int levelId) {
        // 创建关卡系统
        levelSystem = new LevelSystem(levelId);
        // 初始化关卡（创建路径、初始塔等）
        levelSystem.initializeLevel(world);
    }

    /**
     * 设置游戏系统
     */
    private void setupSystems() {
        world.addSystem(new SpawnSystem());
        world.addSystem(new MovementSystem());
        world.addSystem(new AttackSystem());
        // 可以添加关卡系统到世界系统中，如果它需要每帧更新的话
        // world.addSystem(levelSystem);
    }

    /**
     * 手动生成敌人
     */
    public void spawnEnemyManually() {
        try {
            Entity enemy = world.createEntity();

            Enemy.Type[] types = Enemy.Type.values();
            Enemy.Type type = types[random.nextInt(types.length)];

            Path.PathTag[] pathTags = Path.PathTag.values();
            Path.PathTag pathTag = pathTags[random.nextInt(pathTags.length)];

            int health = 0;
            float speed = 0;
            int reward = 0;

            switch (type) {
                case GOBLIN:
                    health = 30;
                    speed = 50;
                    reward = 5;
                    break;
                case ORC:
                    health = 60;
                    speed = 30;
                    reward = 10;
                    break;
                case TROLL:
                    health = 100;
                    speed = 20;
                    reward = 20;
                    break;
            }

            enemy.addComponent(new Transform(100, 100));
            enemy.addComponent(new Health(health));
            enemy.addComponent(new Enemy(type, speed, reward));

            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 放置防御塔
     */
    public void placeTower(float x, float y, Tower.Type type) {
        createTower(x, y, type);

        if (updateListener != null) {
            updateListener.onGameStateUpdated(world);
        }
    }

    /**
     * 创建防御塔实体（现在作为内部方法，不暴露给外部）
     */
    private void createTower(float x, float y, Tower.Type type) {
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

        tower.addComponent(new Tower(type, damage, range, attackSpeed));
    }

    // 以下方法保持不变...
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

    public void stopGame() {
        isRunning = false;
        if (gameLoop != null) {
            gameHandler.removeCallbacks(gameLoop);
        }
    }

    public void pauseGame() {
        isRunning = false;
        if (gameLoop != null) {
            gameHandler.removeCallbacks(gameLoop);
        }
    }

    public void resumeGame() {
        if (!isRunning) {
            startGame();
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

        // 清理当前世界（可选，根据需求）
        // world.clear();

        // 切换关卡
        levelSystem.switchLevel(newLevelId);
        levelSystem.initializeLevel(world);

        // 重新开始游戏
        startGame();
    }
}