package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Health;
import com.example.towerdefense.components.Enemy;

import java.util.Random;

/**
 * 敌人生成系统 - 负责管理游戏波次和自动生成敌人
 * 控制敌人的生成节奏、波次难度递增和敌人类型随机化
 * 继承自ECSSystem，不需要特定组件，作为全局管理系统运行
 */
public class SpawnSystem extends ECSSystem {
    // 随机数生成器，用于随机选择敌人类型
    private Random random = new Random();
    // 上次生成敌人的时间戳（毫秒）
    private long lastSpawnTime = 0;
    // 当前波次数
    private int waveNumber = 0;
    // 当前波次的敌人总数
    private int enemiesInWave = 0;
    // 当前波次已生成的敌人数
    private int enemiesSpawned = 0;

    /**
     * 构造函数 - 这是一个全局管理系统，不需要特定组件
     * 负责整个游戏的敌人生成逻辑，不依赖于特定实体类型
     */
    public SpawnSystem() {
        super(); // 无必需组件，处理全局生成逻辑
    }

    /**
     * 更新方法 - 每帧调用，管理敌人生成和波次控制
     * @param deltaTime 距离上一帧的时间间隔（秒）
     */
    @Override
    public void update(float deltaTime) {
        // 获取当前时间戳，用于生成间隔控制
        // 修复：使用 System.currentTimeMillis() 而不是 ECSSystem.currentTimeMillis()
        long currentTime = System.currentTimeMillis();

        // 检查当前波次是否已完成（所有敌人都已生成）
        if (enemiesSpawned >= enemiesInWave) {
            startNewWave(); // 开始新的波次
        }

        // 检查是否满足生成条件：距离上次生成超过2秒，且当前波次还有敌人要生成
        if (currentTime - lastSpawnTime > 2000 && enemiesSpawned < enemiesInWave) {
            spawnEnemy();      // 生成一个新敌人
            lastSpawnTime = currentTime; // 更新最后生成时间
            enemiesSpawned++;  // 增加已生成敌人数
        }
    }

    /**
     * 开始新波次 - 初始化新波次的参数
     * 每波敌人数量递增，增加游戏难度
     */
    private void startNewWave() {
        waveNumber++;           // 波次数增加
        enemiesInWave = 3 + waveNumber; // 基础3个敌人，每波增加1个
        enemiesSpawned = 0;     // 重置已生成敌人数
    }

    /**
     * 生成敌人 - 创建新的敌人实体并设置其属性
     * 随机选择敌人类型，并根据类型设置不同的属性
     */
    private void spawnEnemy() {
        // 创建新的敌人实体
        Entity enemy = world.createEntity();

        // 随机选择敌人类型
        Enemy.Type[] types = Enemy.Type.values();
        Enemy.Type type = types[random.nextInt(types.length)];

        // 根据敌人类型初始化属性
        int health = 0;   // 生命值
        float speed = 0;  // 移动速度
        int reward = 0;   // 击败奖励

        // 配置不同类型敌人的属性
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
        enemy.addComponent(new Transform(100, 100)); // 设置初始位置
        enemy.addComponent(new Health(health));       // 设置生命值
        enemy.addComponent(new Enemy(type, speed, reward)); // 设置敌人属性
    }
}