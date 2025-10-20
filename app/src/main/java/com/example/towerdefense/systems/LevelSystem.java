package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.World;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Path;
import android.graphics.Color;

/**
 * 关卡管理系统 - 负责关卡初始化、路径创建、初始塔放置等
 */
public  class LevelSystem extends ECSSystem {
    private World world;
    private int currentLevelId;

    public LevelSystem(int levelId) {
        this.currentLevelId = levelId;
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
            case 1: // 训练关
                initializeTrainingLevel();
                break;
            case 2: // 正式关卡1
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
     * 初始化森林关卡

    private void initializeForestLevel() {
        createForestPaths();
        createForestTowers();
        // 森林关卡特有配置
    }
     */
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

    /**
     * 创建森林关卡的路径

    private void createForestPaths() {
        // 森林关卡的路径配置
        Entity pathA = world.createEntity();
        pathA.addComponent(new Path(
                Path.PathTag.PATH_A,
                new float[][]{
                        {0.1f, 0.2f},
                        {0.8f, 0.2f},
                        {0.8f, 0.6f},
                        {0.2f, 0.6f},
                        {0.2f, 0.8f}
                },
                Color.GREEN,
                10f
        ));

        // 可以添加更多森林关卡特有的路径
    }
     */
    /**
     * 创建初始防御塔 - 在游戏开始时放置几个默认的塔
     */


    /**
     * 创建森林关卡的初始防御塔

    private void createForestTowers() {
        // 森林关卡特有的塔布局
        createTower(150, 150, Tower.Type.ARCHER);
        createTower(500, 250, Tower.Type.MAGE);
        createTower(300, 400, Tower.Type.CANNON);
    }
     */
    /**
     * 创建防御塔实体
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

    /**
     * 获取当前关卡ID
     */
    public int getCurrentLevelId() {
        return currentLevelId;
    }

    /**
     * 切换关卡
     */
    public void switchLevel(int newLevelId) {
        this.currentLevelId = newLevelId;
        // 可以在这里添加关卡切换逻辑，如清理当前关卡、初始化新关卡等
    }
}