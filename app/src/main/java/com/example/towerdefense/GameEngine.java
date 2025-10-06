/**
 * 游戏引擎主类 - 采用ECS(Entity-Component-System)架构
 * 负责管理游戏循环、实体创建、系统更新和游戏状态
 */
package com.example.towerdefense;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

// ECS 架构相关导入
import com.example.towerdefense.ecs.World;
import com.example.towerdefense.ecs.Entity;

// 组件导入 - 定义实体的数据和属性
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Health;
import com.example.towerdefense.components.Enemy;

// 系统导入 - 处理游戏逻辑
import com.example.towerdefense.systems.MovementSystem;
import com.example.towerdefense.systems.AttackSystem;
import com.example.towerdefense.systems.SpawnSystem;

import java.util.Random;

/**
 * 游戏引擎主类 - 采用ECS(Entity-Component-System)架构
 * 负责管理游戏循环、实体创建、系统更新和游戏状态
 */
public class GameEngine {
    // ECS世界，管理所有实体和系统
    private final World world;
    // Android主线程处理器，用于游戏循环
    private final Handler gameHandler;
    // 游戏循环任务
    private Runnable gameLoop;
    // 游戏运行状态标志
    private boolean isRunning = false;
    // UI更新回调接口
    private GameUpdateListener updateListener;
    // 随机数生成器，用于敌人生成等
    private final Random random = new Random();

    /**
     * 游戏状态更新监听器接口
     * 用于通知UI层游戏状态发生变化需要重绘
     */
    public interface GameUpdateListener {
        void onGameStateUpdated(World world);
    }

    /**
     * 构造函数 - 初始化游戏引擎
     * @param context Android上下文
     */
    public GameEngine(Context context) {
        // 创建ECS世界实例
        world = new World();
        // 获取主线程的Handler，确保UI操作在主线程执行
        gameHandler = new Handler(Looper.getMainLooper());

        // 初始化游戏系统
        setupSystems();
        // 创建初始的防御塔
        createInitialTowers();
    }

    /**
     * 设置游戏系统 - 添加ECS系统中处理游戏逻辑的各个系统
     */
    private void setupSystems() {
        // 敌人生成系统 - 负责定期生成敌人
        world.addSystem(new SpawnSystem());
        // 移动系统 - 处理敌人的移动逻辑
        world.addSystem(new MovementSystem());
        // 攻击系统 - 处理塔的攻击逻辑和敌人的受伤逻辑
        world.addSystem(new AttackSystem());
    }

    /**
     * 手动生成敌人 - 用于测试或特定触发条件
     * 在随机位置生成随机类型的敌人
     */
    public void spawnEnemyManually() {
        try {
            // 创建新的敌人实体
            Entity enemy = world.createEntity();

            // 随机选择敌人类型
            Enemy.Type[] types = Enemy.Type.values();
            Enemy.Type type = types[random.nextInt(types.length)];

            // 根据敌人类型设置属性
            int health = 0;
            float speed = 0;
            int reward = 0;

            switch (type) {
                case GOBLIN:
                    // 哥布林：低血量、高速度、低奖励 - 快速但脆弱
                    health = 30;
                    speed = 50;
                    reward = 5;
                    break;
                case ORC:
                    // 兽人：中等血量、中等速度、中等奖励 - 均衡型
                    health = 60;
                    speed = 30;
                    reward = 10;
                    break;
                case TROLL:
                    // 巨魔：高血量、低速度、高奖励 - 缓慢但强大
                    health = 100;
                    speed = 20;
                    reward = 20;
                    break;
            }

            // 为敌人实体添加必要的组件
            // Transform组件：定义敌人在游戏世界中的位置
            enemy.addComponent(new Transform(100, 100));
            // Health组件：定义敌人的生命值
            enemy.addComponent(new Health(health));
            // Enemy组件：定义敌人的类型、移动速度和击败奖励
            enemy.addComponent(new Enemy(type, speed, reward));

            // 通知UI层游戏状态已更新，需要重绘
            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }
        } catch (Exception e) {
            // 打印异常信息，便于调试
            e.printStackTrace();
        }
    }

    /**
     * 创建初始防御塔 - 在游戏开始时放置几个默认的塔
     */
    private void createInitialTowers() {
        // 在指定位置创建不同类型的防御塔
        createTower(200, 200, Tower.Type.ARCHER);  // 弓箭手塔
        createTower(400, 300, Tower.Type.CANNON);  // 加农炮塔
        createTower(600, 200, Tower.Type.MAGE);    // 法师塔
    }

    /**
     * 创建防御塔实体
     * @param x 塔的X坐标
     * @param y 塔的Y坐标
     * @param type 塔的类型
     */
    private void createTower(float x, float y, Tower.Type type) {
        // 创建新的塔实体
        Entity tower = world.createEntity();
        // 添加位置组件
        tower.addComponent(new Transform(x, y));

        // 根据塔类型设置属性
        int damage = 0;
        float range = 0;
        float attackSpeed = 0;

        switch (type) {
            case ARCHER:
                // 弓箭手：均衡的伤害、射程和攻速
                damage = 10;
                range = 150;
                attackSpeed = 1.0f;
                break;
            case CANNON:
                // 加农炮：高伤害但射程较短、攻速较慢
                damage = 25;
                range = 120;
                attackSpeed = 0.5f;
                break;
            case MAGE:
                // 法师：中等伤害、长射程、较快攻速
                damage = 15;
                range = 180;
                attackSpeed = 0.8f;
                break;
        }

        // 添加塔组件，包含塔的所有战斗属性
        tower.addComponent(new Tower(type, damage, range, attackSpeed));
    }

    /**
     * 开始游戏 - 启动游戏循环
     * 如果游戏已经在运行，则不做任何操作
     */
    public void startGame() {
        if (isRunning) return;

        // 设置运行状态标志
        isRunning = true;
        // 创建游戏循环任务
        gameLoop = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // 更新游戏状态
                    updateGame();
                    // 16ms后再次执行，实现约60FPS的游戏循环
                    gameHandler.postDelayed(this, 16);
                }
            }
        };
        // 启动游戏循环
        gameHandler.post(gameLoop);

        // 强制进行第一次更新，确保游戏立即开始
        updateGame();
    }

    /**
     * 更新游戏状态 - 游戏循环的核心方法
     * 每帧调用一次，更新所有游戏系统
     */
    private void updateGame() {
        try {
            // 更新所有ECS系统，传入时间增量(16ms = 0.016秒)
            world.update(0.016f);

            // 通知UI层游戏状态已更新，需要重绘
            if (updateListener != null) {
                updateListener.onGameStateUpdated(world);
            }
        } catch (Exception e) {
            // 打印异常信息，便于调试
            e.printStackTrace();
        }
    }

    /**
     * 停止游戏 - 完全停止游戏循环
     * 清除所有回调，游戏状态重置
     */
    public void stopGame() {
        isRunning = false;
        if (gameLoop != null) {
            gameHandler.removeCallbacks(gameLoop);
        }
    }

    /**
     * 暂停游戏 - 暂停游戏循环但保持游戏状态
     * 可以后续通过resumeGame恢复
     */
    public void pauseGame() {
        isRunning = false;
        if (gameLoop != null) {
            gameHandler.removeCallbacks(gameLoop);
        }
    }

    /**
     * 恢复游戏 - 从暂停状态恢复游戏
     * 如果游戏没有运行，则重新启动游戏循环
     */
    public void resumeGame() {
        if (!isRunning) {
            startGame();
        }
    }

    /**
     * 放置防御塔 - 公开方法，用于在指定位置放置指定类型的塔
     * 通常由GameView中的触摸事件调用
     * @param x 放置位置的X坐标
     * @param y 放置位置的Y坐标
     * @param type 塔的类型
     */
    public void placeTower(float x, float y, Tower.Type type) {
        // 创建新的防御塔
        createTower(x, y, type);

        // 通知UI层游戏状态已更新，需要重绘
        if (updateListener != null) {
            updateListener.onGameStateUpdated(world);
        }
    }

    /**
     * 获取ECS世界实例
     * @return 当前游戏的世界对象
     */
    public World getWorld() {
        return world;
    }

    /**
     * 设置游戏状态更新监听器
     * @param listener 实现GameUpdateListener接口的监听器
     */
    public void setUpdateListener(GameUpdateListener listener) {
        this.updateListener = listener;
    }

    /**
     * 检查游戏是否正在运行
     * @return true如果游戏正在运行，false否则
     */
    public boolean isRunning() {
        return isRunning;
    }
}