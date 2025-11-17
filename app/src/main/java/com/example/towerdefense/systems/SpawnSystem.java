package com.example.towerdefense.systems;

import com.example.towerdefense.components.Path;
import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Health;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.ecs.World;

import java.util.List;

/**
 * 敌人生成系统 - 负责管理游戏波次和自动生成敌人
 * 控制敌人的生成节奏、波次难度递增和敌人类型随机化
 * 继承自ECSSystem，不需要特定组件，作为全局管理系统运行
 */
public class SpawnSystem extends ECSSystem {

    // 添加屏幕尺寸字段
    private int screenWidth = 1080; // 默认值
    private int screenHeight = 1920; // 默认值

    private boolean isReady = false;
    private boolean isActive = false;

    // 波次控制字段
    private int currentWaveIndex = 0;
    private int currentSpawnGroupIndex = 0;
    private int currentEnemyInGroupCount = 0;
    private float timeSinceLastSpawn = 0;
    private float timeSinceWaveStart = 0;
    private float timeSinceWaveCompleted = 0;
    private boolean isWaveActive = false;
    private boolean isWaitingForNextWave = false;
    private boolean allWavesCompleted = false;

    // 新增：为每个组维护独立的状态
    private static class SpawnGroupState {
        int currentEnemyCount = 0;
        float timeSinceLastSpawn = 0;
        boolean isCompleted = false;
    }

    private List<SpawnGroupState> currentWaveGroupStates = new java.util.ArrayList<>();

    // 添加LevelSystem引用
    private LevelSystem levelSystem;

    /**
     * 构造函数 - 这是一个全局管理系统，不需要特定组件
     * 负责整个游戏的敌人生成逻辑，不依赖于特定实体类型
     */
    public SpawnSystem() {
        super(); // 无必需组件，处理全局生成逻辑
    }

    public void startSpawning() {
        this.isActive = true;
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
    public void setWorld(World world) {
        super.setWorld(world);
        System.out.println("SpawnSystem: 世界引用已设置，world=" + (world != null ? "有效" : "null"));
    }

    /**
     * 设置LevelSystem引用
     */
    public void setLevelSystem(LevelSystem levelSystem) {
        this.levelSystem = levelSystem;
        System.out.println("SpawnSystem: LevelSystem引用已设置");
    }

    @Override
    public void update(float deltaTime) {
        // 如果系统未就绪，直接返回
        if (!isReady) {
            System.out.println("SpawnSystem: 系统未就绪，等待屏幕尺寸设置");
            return;
        }
        if (!isActive) {
            System.out.println("SpawnSystem: 系统未激活，当前状态: isActive=" + isActive);
            return;
        }
        if (allWavesCompleted) {
            return;
        }

        timeSinceLastSpawn += deltaTime;

        // 检查是否在等待下一波开始
        if (isWaitingForNextWave) {
            timeSinceWaveCompleted += deltaTime;
            LevelSystem.LevelWaveConfig waveConfig = levelSystem.getCurrentWaveConfig();
            if (timeSinceWaveCompleted >= waveConfig.delayBetweenWaves) {
                isWaitingForNextWave = false;
                startNextWave();
            }
            return;
        }

        // 如果没有激活的波次，检查是否开始新波次
        if (!isWaveActive) {
            startNextWave();
            return;
        }

        // 执行当前波次的生成逻辑
        executeCurrentWave(deltaTime);
    }

    /**
     * 开始下一波敌人
     */
    private void startNextWave() {
        LevelSystem.LevelWaveConfig waveConfig = levelSystem.getCurrentWaveConfig();
        if (waveConfig == null || currentWaveIndex >= waveConfig.waves.size()) {
            // 所有波次完成
            allWavesCompleted = true;
            System.out.println("SpawnSystem: 所有波次已完成！");
            return;
        }

        List<LevelSystem.WaveConfig> currentWave = waveConfig.waves.get(currentWaveIndex);
        if (currentWave.isEmpty()) {
            currentWaveIndex++;
            return;
        }

        // 初始化每个组的状态
        currentWaveGroupStates.clear();
        for (int i = 0; i < currentWave.size(); i++) {
            currentWaveGroupStates.add(new SpawnGroupState());
        }

        isWaveActive = true;
        isWaitingForNextWave = false;
        timeSinceWaveStart = 0;
        timeSinceWaveCompleted = 0;

        System.out.println("SpawnSystem: 开始第 " + (currentWaveIndex + 1) + " 波敌人，包含 " +
                currentWave.size() + " 个生成组");
    }

    /**
     * 执行当前波次的生成逻辑 - 优化后的版本
     */
    private void executeCurrentWave(float deltaTime) {
        LevelSystem.LevelWaveConfig waveConfig = levelSystem.getCurrentWaveConfig();
        List<LevelSystem.WaveConfig> currentWave = waveConfig.waves.get(currentWaveIndex);

        timeSinceWaveStart += deltaTime;

        boolean allGroupsCompleted = true;

        // 遍历所有组，独立更新每个组的生成状态
        for (int i = 0; i < currentWave.size(); i++) {
            LevelSystem.WaveConfig group = currentWave.get(i);
            SpawnGroupState groupState = currentWaveGroupStates.get(i);

            if (groupState.isCompleted) {
                continue; // 这个组已经完成
            }

            allGroupsCompleted = false; // 至少有一个组未完成

            groupState.timeSinceLastSpawn += deltaTime;

            // 检查是否应该生成敌人
            if (groupState.currentEnemyCount < group.count &&
                    groupState.timeSinceLastSpawn >= group.delayBetweenSpawns) {

                spawnEnemy(group.enemyType, group.pathTag);
                groupState.currentEnemyCount++;
                groupState.timeSinceLastSpawn = 0;

                System.out.println("SpawnSystem: 生成 " + group.enemyType + " 在路径 " +
                        group.pathTag + " (" + groupState.currentEnemyCount + "/" + group.count + ")");

                // 检查这个组是否完成
                if (groupState.currentEnemyCount >= group.count) {
                    groupState.isCompleted = true;
                }
            }
        }

        // 如果所有组都完成了，结束当前波次
        if (allGroupsCompleted) {
            completeCurrentWave();
        }
    }

    /**
     * 生成指定类型和路径的敌人
     */
    private void spawnEnemy(Enemy.Type enemyType, Path.PathTag pathTag) {
        Entity enemy = world.createEntity();

        // 配置敌人属性
        int health = 0;
        float speed = 0;
        int reward = 0;

        switch (enemyType) {
            case Vehicle:
                health = 30;
                speed = 100;
                reward = 5;
                break;
            case Infantry:
                health = 60;
                speed = 60;
                reward = 10;
                break;
            case Armour:
                health = 100;
                speed = 40;
                reward = 20;
                break;
        }

        float[] startPosition = getPathStartPosition(pathTag);
        enemy.addComponent(new Transform(startPosition[0], startPosition[1]));
        enemy.addComponent(new Health(health));
        enemy.addComponent(new Enemy(enemyType, speed, reward, pathTag));

        System.out.println("SpawnSystem: 生成 " + enemyType + " 敌人，路径=" + pathTag);
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

    /**
     * 完成当前波次
     */
    private void completeCurrentWave() {
        isWaveActive = false;

        LevelSystem.LevelWaveConfig waveConfig = levelSystem.getCurrentWaveConfig();

        // 检查是否还有下一波
        if (currentWaveIndex + 1 < waveConfig.waves.size()) {
            // 还有下一波，进入等待状态
            isWaitingForNextWave = true;
            timeSinceWaveCompleted = 0;
            System.out.println("SpawnSystem: 第 " + (currentWaveIndex + 1) + " 波完成，等待 " +
                    waveConfig.delayBetweenWaves + " 秒后开始下一波");
        } else {
            // 所有波次完成
            allWavesCompleted = true;
            System.out.println("SpawnSystem: 所有波次已完成！");
        }

        currentWaveIndex++;
    }

    /**
     * 重置系统状态
     */
    public void reset() {
        this.currentWaveIndex = 0;
        this.currentSpawnGroupIndex = 0;
        this.currentEnemyInGroupCount = 0;
        this.timeSinceLastSpawn = 0;
        this.timeSinceWaveStart = 0;
        this.timeSinceWaveCompleted = 0;
        this.isWaveActive = false;
        this.isWaitingForNextWave = false;
        this.allWavesCompleted = false;
        this.isActive = false;
        this.isReady = false;
        this.currentWaveGroupStates.clear();

        System.out.println("SpawnSystem: 波次系统已完全重置");
    }

    /**
     * 设置激活状态
     */
    public void setActive(boolean active) {
        this.isActive = active;
        System.out.println("SpawnSystem: 激活状态设置为 " + active);
    }

    /**
     * 检查是否所有波次都已完成
     */
    public boolean areAllWavesCompleted() {
        return allWavesCompleted;
    }

    /**
     * 获取当前波次信息
     */
    public String getCurrentWaveInfo() {
        if (allWavesCompleted) {
            return "所有波次已完成";
        }

        LevelSystem.LevelWaveConfig waveConfig = levelSystem.getCurrentWaveConfig();
        if (waveConfig == null) return "无波次配置";

        int totalWaves = waveConfig.waves.size();
        return "波次: " + (currentWaveIndex + 1) + "/" + totalWaves;
    }
    /**
     * 获取系统激活状态
     */
    public boolean isActive() {
        return isActive;
    }
}