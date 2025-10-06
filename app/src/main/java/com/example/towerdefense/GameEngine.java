package com.example.towerdefense;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

// ECS 导入
import com.example.towerdefense.ecs.World;
import com.example.towerdefense.ecs.Entity;

// 组件导入
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Health;
import com.example.towerdefense.components.Enemy;

// 系统导入
import com.example.towerdefense.systems.MovementSystem;
import com.example.towerdefense.systems.AttackSystem;
import com.example.towerdefense.systems.SpawnSystem;

import java.util.Random;

public class GameEngine {
    private World world;
    private Handler gameHandler;
    private Runnable gameLoop;
    private boolean isRunning = false;
    private GameUpdateListener updateListener;
    private Random random = new Random();

    public interface GameUpdateListener {
        void onGameStateUpdated(World world);
    }

    public GameEngine(Context context) {
        world = new World();
        gameHandler = new Handler(Looper.getMainLooper());

        setupSystems();
        createInitialTowers();
    }

    private void setupSystems() {
        world.addSystem(new SpawnSystem());
        world.addSystem(new MovementSystem());
        world.addSystem(new AttackSystem());
    }

    // 添加手动生成敌人的方法
    public void spawnEnemyManually() {
        try {
            Entity enemy = world.createEntity();

            Enemy.Type[] types = Enemy.Type.values();
            Enemy.Type type = types[random.nextInt(types.length)];

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

            // 使用合适的起始位置
            enemy.addComponent(new Transform(100, 100));
            enemy.addComponent(new Health(health));
            enemy.addComponent(new Enemy(type, speed, reward));

            // 通知UI更新
            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 添加缺失的 createInitialTowers 方法
    private void createInitialTowers() {
        // 创建几个初始塔
        createTower(200, 200, Tower.Type.ARCHER);
        createTower(400, 300, Tower.Type.CANNON);
        createTower(600, 200, Tower.Type.MAGE);
    }

    // 确保 createTower 方法存在
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

    // 在 GameEngine 类中更新 startGame 方法
    public void startGame() {
        if (isRunning) return;

        isRunning = true;
        gameLoop = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    updateGame();
                    gameHandler.postDelayed(this, 16); // ~60 FPS
                }
            }
        };
        gameHandler.post(gameLoop);

        // 强制第一次更新
        updateGame();
    }

    // 确保 updateGame 方法正确触发重绘
    private void updateGame() {
        try {
            world.update(0.016f); // 假设 16ms 一帧

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

    // 公开的放置塔方法（用于 GameView 中的触摸事件）
    public void placeTower(float x, float y, Tower.Type type) {
        createTower(x, y, type);

        // 通知UI更新
        if (updateListener != null) {
            updateListener.onGameStateUpdated(world);
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
}