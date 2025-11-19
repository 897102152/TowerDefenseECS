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
                initializeLevel1();
                break;
            case 2: // 正式关卡2
                initializeLevel2();
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
        createTrainingLevelPaths();
        // 训练关特有配置：更多金币、无限生命等
    }

    /**
     * 初始化第一关
     */
    private void initializeLevel1() {
        createLevel1Paths();
        // 第一关特有配置
    }

    /**
     * 初始化第二关
     */
    private void initializeLevel2() {
        createLevel2Paths();
        // 第二关特有配置
    }
    /**
     * 创建路径实体 - 教学关
     */
    private void createTrainingLevelPaths() {
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

    /**
     * 创建路径实体 - 第一关
     */
    private void createLevel1Paths() {
        // 创建路径A
        Entity pathA = world.createEntity();
        pathA.addComponent(new Path(
                Path.PathTag.PATH_A,
                new float[][]{
                        {0.05f, 0.15f},
                        {0.6f, 0.15f},
                        {0.6f, 0.25f},
                        {0.95f, 0.25f}
                },
                Color.GRAY,
                10f
        ));

        // 创建路径B
        Entity pathB = world.createEntity();
        pathB.addComponent(new Path(
                Path.PathTag.PATH_B,
                new float[][]{
                        {0.05f, 0.5f},
                        {0.2f, 0.5f},
                        {0.2f, 0.6f},
                        {0.3f, 0.6f},
                        {0.3f, 0.4f},
                        {0.4f, 0.4f},
                        {0.4f, 0.5f},
                        {0.6f, 0.5f},
                        {0.6f, 0.6f},
                        {0.95f, 0.6f},
                        {0.95f, 0.25f}
                },
                Color.rgb(100, 100, 255),
                10f
        ));

        // 创建路径C
        Entity pathC = world.createEntity();
        pathC.addComponent(new Path(
                Path.PathTag.PATH_C,
                new float[][]{
                        {0.05f, 0.85f},
                        {0.4f, 0.85f},
                        {0.4f, 0.75f},
                        {0.5f, 0.75f},
                        {0.5f, 0.85f},
                        {0.7f, 0.85f},
                        {0.7f, 0.6f},
                        {0.95f, 0.6f},
                        {0.95f, 0.25f}
                },
                Color.rgb(255, 100, 100),
                10f
        ));
    }

        /**
         * 创建路径实体 - 第二关
         */
        private void createLevel2Paths() {
            // 创建路径A
            Entity pathA = world.createEntity();
            pathA.addComponent(new Path(
                    Path.PathTag.PATH_A,
                    new float[][]{
                            {0.05f, 0.09f},
                            {0.39f, 0.09f},
                            {0.39f, 0.45f}
                    },
                    Color.GRAY,
                    10f
            ));

            // 创建路径B
            Entity pathB = world.createEntity();
            pathB.addComponent(new Path(
                    Path.PathTag.PATH_B,
                    new float[][]{
                            {0.05f, 0.87f},
                            {0.3f, 0.87f},
                            {0.3f, 0.45f},
                            {0.39f, 0.45f}
                    },
                    Color.rgb(100, 100, 255),
                    10f
            ));

            // 创建路径C
            Entity pathC = world.createEntity();
            pathC.addComponent(new Path(
                    Path.PathTag.PATH_C,
                    new float[][]{
                            {0.95f, 0.65f},
                            {0.39f, 0.65f},
                            {0.39f, 0.45f}
                    },
                    Color.rgb(255, 100, 100),
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

        // ==========================================教学关波次配置=============================================
        List<List<WaveConfig>> trainingWaves = new ArrayList<>();

        // 第一波 - 同时生成
        List<WaveConfig> wave1 = Arrays.asList(
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_A, 3, 2.0f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_B, 3, 2.0f)
        );

        // 第二波 - 同时生成
        List<WaveConfig> wave2 = Arrays.asList(
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_A, 3, 2.0f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_B, 3, 2.0f)
        );

        trainingWaves.add(wave1);
        trainingWaves.add(wave2);

        levelWaveConfigs.put(0, new LevelWaveConfig(trainingWaves, 10.0f));

        // =========================================第一关波次配置================================================
        List<List<WaveConfig>> level1Waves = new ArrayList<>();

        // 第一波 - 三条路径同时生成敌人
        List<WaveConfig> level1Wave1 = Arrays.asList(
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_A, 5, 1.5f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_B, 5, 1.5f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_C, 5, 1.5f)
        );

        // 第二波 - 不同类型敌人在不同路径
        List<WaveConfig> level1Wave2 = Arrays.asList(
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_A, 6, 2.0f),
                new WaveConfig(Enemy.Type.ORC, Path.PathTag.PATH_B, 6, 3.0f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_C, 6, 2.0f)
        );

        // 第三波 - Boss波
        List<WaveConfig> level1Wave3 = Arrays.asList(
                new WaveConfig(Enemy.Type.TROLL, Path.PathTag.PATH_A, 3, 1f),
                new WaveConfig(Enemy.Type.ORC, Path.PathTag.PATH_B, 6, 1.5f),
                new WaveConfig(Enemy.Type.ORC, Path.PathTag.PATH_C, 6, 1.5f)
        );

        level1Waves.add(level1Wave1);
        level1Waves.add(level1Wave2);
        level1Waves.add(level1Wave3);

        levelWaveConfigs.put(1, new LevelWaveConfig(trainingWaves, 10.0f));

        // =========================================第一关波次配置================================================
        List<List<WaveConfig>> level2Waves = new ArrayList<>();

        // 第一波 - 三条路径同时生成敌人
        List<WaveConfig> level2Wave1 = Arrays.asList(
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_A, 5, 1.5f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_B, 5, 1.5f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_C, 5, 1.5f)
        );

        // 第二波 - 不同类型敌人在不同路径
        List<WaveConfig> level2Wave2 = Arrays.asList(
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_A, 6, 2.0f),
                new WaveConfig(Enemy.Type.ORC, Path.PathTag.PATH_B, 6, 3.0f),
                new WaveConfig(Enemy.Type.GOBLIN, Path.PathTag.PATH_C, 6, 2.0f)
        );

        // 第三波 - Boss波
        List<WaveConfig> level2Wave3 = Arrays.asList(
                new WaveConfig(Enemy.Type.TROLL, Path.PathTag.PATH_A, 3, 1f),
                new WaveConfig(Enemy.Type.ORC, Path.PathTag.PATH_B, 6, 1.5f),
                new WaveConfig(Enemy.Type.ORC, Path.PathTag.PATH_C, 6, 1.5f)
        );

        level2Waves.add(level2Wave1);
        level2Waves.add(level2Wave2);
        level2Waves.add(level2Wave3);

        levelWaveConfigs.put(2, new LevelWaveConfig(level1Waves, 15.0f));
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