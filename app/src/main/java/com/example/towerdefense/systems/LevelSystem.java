package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.World;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Path;
import com.example.towerdefense.components.Enemy;
import android.graphics.Color;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关卡管理系统 - 负责关卡初始化、路径创建、初始塔放置等
 */
public class LevelSystem extends ECSSystem {
    private World world;
    private int currentLevelId;

    public LevelSystem(int levelId) {
        this.currentLevelId = levelId;
        initializeWaveConfigs(); // 在构造函数中初始化波次配置
    }

    @Override
    public void update(float deltaTime) {
        // 关卡系统可能不需要每帧更新，主要用于初始化
    }

    /**
     * 初始化关卡
     */
    public void initializeLevel(World world) {
        this.world = world;

        // 根据关卡ID初始化不同的关卡配置
        switch (currentLevelId) {
            case 0: // 训练关
                initializeTrainingLevel();
                break;
            case 1: // 正式关卡1
                initializeTrainingLevel();
                //initializeForestLevel();
                break;
            // 可以添加更多关卡...
            default:
                initializeTrainingLevel(); // 默认训练关
        }
    }

    /**
     * 初始化训练关卡
     */
    private void initializeTrainingLevel() {
        createPaths_training();
        // 训练关特有配置：更多金币、无限生命等
    }

    /**
     * 创建路径实体 - 教学关
     */
    private void createPaths_training() {
        // 创建路径A - 较长的路径
        Entity pathA = world.createEntity();
        pathA.addComponent(new Path(
                Path.PathTag.PATH_A,
                new float[][]{
                        {0.3f, 0.3f},
                        {0.7f, 0.3f},
                        {0.7f, 0.5f},
                        {0.9f, 0.5f},
                },
                Color.GRAY,
                10f
        ));

        // 创建路径B - 较短的路径
        Entity pathB = world.createEntity();
        pathB.addComponent(new Path(
                Path.PathTag.PATH_B,
                new float[][]{
                        {0.3f, 0.7f},
                        {0.7f, 0.7f},
                        {0.7f, 0.5f},
                        {0.9f, 0.5f}
                },
                Color.rgb(100, 100, 255),
                10f
        ));
    }

    public static class WaveConfig {
        public Enemy.Type enemyType;
        public Path.PathTag pathTag;
        public int count;
        public float delayBetweenSpawns; // 同一波次内敌人生成间隔

        public WaveConfig(Enemy.Type enemyType, Path.PathTag pathTag, int count, float delayBetweenSpawns) {
            this.enemyType = enemyType;
            this.pathTag = pathTag;
            this.count = count;
            this.delayBetweenSpawns = delayBetweenSpawns;
        }
    }

    /**
     * 关卡波次配置类
     */
    public static class LevelWaveConfig {
        public List<List<WaveConfig>> waves; // 外层List是波次，内层List是该波次的敌人配置
        public float delayBetweenWaves; // 波次间延迟

        public LevelWaveConfig(List<List<WaveConfig>> waves, float delayBetweenWaves) {
            this.waves = waves;
            this.delayBetweenWaves = delayBetweenWaves;
        }
    }

    // 添加波次配置存储
    private LevelWaveConfig currentWaveConfig;
    private Map<Integer, LevelWaveConfig> levelWaveConfigs = new HashMap<>();

    /**
     * 初始化波次配置
     */
    private void initializeWaveConfigs() {
        levelWaveConfigs.clear();

        // 教学关波次配置
        List<List<WaveConfig>> trainingWaves = new ArrayList<>();

        // 第一波
        List<WaveConfig> wave1 = Arrays.asList(
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_A, 3, 2.0f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_B, 3, 2.0f)
        );
        //这里有一点问题，在游戏中这样写会导致路径A的敌人生成结束后再生成路径B的敌人
        // 第二波
        List<WaveConfig> wave2 = Arrays.asList(
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_A, 3, 2.0f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_B, 3, 2.0f)
        );

        trainingWaves.add(wave1);
        trainingWaves.add(wave2);

        levelWaveConfigs.put(0, new LevelWaveConfig(trainingWaves, 10.0f)); // 波次间延迟3秒
    }

    /**
     * 获取当前关卡的波次配置
     */
    public LevelWaveConfig getCurrentWaveConfig() {
        return levelWaveConfigs.get(currentLevelId);
    }

    /**
     * 获取总波次数
     */
    public int getTotalWaves() {
        LevelWaveConfig config = getCurrentWaveConfig();
        return config != null ? config.waves.size() : 0;
    }

    /**
     * 切换关卡
     */
    public void switchLevel(int newLevelId) {
        this.currentLevelId = newLevelId;
        // 可以在这里添加关卡切换逻辑，如清理当前关卡、初始化新关卡等
    }
}