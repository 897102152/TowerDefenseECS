package com.example.towerdefense.systems;

import com.example.towerdefense.components.Path;
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
    private final Random random = new Random();
    // 上次生成敌人的时间戳（毫秒）
    private long lastSpawnTime = 0;
    // 当前波次数
    private int waveNumber = 0;
    // 当前波次的敌人总数
    private int enemiesInWave = 0;
    // 当前波次已生成的敌人数
    private int enemiesSpawned = 0;
    // 添加屏幕尺寸字段
    private int screenWidth = 1080; // 默认值
    private int screenHeight = 1920; // 默认值

    private boolean isReady = false;
    /**
     * 构造函数 - 这是一个全局管理系统，不需要特定组件
     * 负责整个游戏的敌人生成逻辑，不依赖于特定实体类型
     */
    public SpawnSystem() {
        super(); // 无必需组件，处理全局生成逻辑
    }
    /**
     * 设置屏幕尺寸 - 从 GameView 获取
     */
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.isReady = true; // 屏幕尺寸设置后即可开始生成
        System.out.println("SpawnSystem: 屏幕尺寸设置为 " + width + "x" + height + "，准备生成敌人");
    }

    @Override
    public void update(float deltaTime) {
        // 如果系统未就绪，直接返回
        if (!isReady) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        // 获取当前时间戳，用于生成间隔控制
        // 修复：使用 System.currentTimeMillis() 而不是 ECSSystem.currentTimeMillis()

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
        Path.PathTag pathTag= Path.PathTag.PATH_A;

        // 配置不同类型敌人的属性
        switch (type) {
            case GOBLIN:
                // 哥布林：低血量、高速度、低奖励 - 快速但脆弱
                health = 30;
                speed = 100;
                reward = 5;
               //pathTag = Path.PathTag.PATH_A;
                break;
            case ORC:
                // 兽人：中等血量、中等速度、中等奖励 - 均衡型
                health = 60;
                speed = 60;
                reward = 10;
                pathTag = Path.PathTag.PATH_B;
                break;
            case TROLL:
                // 巨魔：高血量、低速度、高奖励 - 缓慢但强大
                health = 100;
                speed = 40;
                reward = 20;
                //pathTag = Path.PathTag.PATH_A;
                break;
        }

        // 使用存储的屏幕尺寸获取路径起点
        float[] startPosition = getPathStartPosition(pathTag);
        // 为敌人实体添加必要的组件
        enemy.addComponent(new Transform(startPosition[0], startPosition[1])); // 使用路径起点
        enemy.addComponent(new Health(health));       // 设置生命值
        enemy.addComponent(new Enemy(type, speed, reward, pathTag)); // 设置敌人属性
    }
    /**
     * 根据路径标签获取路径的起点位置
     */
    private float[] getPathStartPosition(Path.PathTag pathTag) {
        // 从世界中找到对应的路径
        for (Entity entity : world.getEntitiesWithComponent(Path.class)) {
            Path path = entity.getComponent(Path.class);
            if (path.getTag() == pathTag) {
                // 使用路径组件的转换方法，确保一致性
                float[][] screenPoints = path.convertToScreenCoordinates(screenWidth, screenHeight);
                if (screenPoints.length > 0) {
                    float startX = screenPoints[0][0];
                    float startY = screenPoints[0][1];
                    System.out.println("SpawnSystem: 路径 " + pathTag + " 起点: (" + startX + ", " + startY + ")");
                    return new float[]{startX, startY};
                }
            }
        }

        // 如果找不到路径，返回默认位置并记录警告
        System.err.println("SpawnSystem: 警告！找不到路径 " + pathTag + "，使用默认起点");
        return new float[]{100, 100};
    }
    }
