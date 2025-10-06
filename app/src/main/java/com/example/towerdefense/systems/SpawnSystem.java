package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Health;
import com.example.towerdefense.components.Enemy;

import java.util.Random;

public class SpawnSystem extends ECSSystem {
    private Random random = new Random();
    private long lastSpawnTime = 0;
    private int waveNumber = 0;
    private int enemiesInWave = 0;
    private int enemiesSpawned = 0;

    public SpawnSystem() {
        super(); // No required components
    }

    @Override
    public void update(float deltaTime) {
        // 修复：使用 System.currentTimeMillis() 而不是 ECSSystem.currentTimeMillis()
        long currentTime = System.currentTimeMillis();  // 改为 System.currentTimeMillis()

        if (enemiesSpawned >= enemiesInWave) {
            startNewWave();
        }

        if (currentTime - lastSpawnTime > 2000 && enemiesSpawned < enemiesInWave) {
            spawnEnemy();
            lastSpawnTime = currentTime;
            enemiesSpawned++;
        }
    }

    private void startNewWave() {
        waveNumber++;
        enemiesInWave = 3 + waveNumber;
        enemiesSpawned = 0;
    }

    private void spawnEnemy() {
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

        enemy.addComponent(new Transform(100, 100));
        enemy.addComponent(new Health(health));
        enemy.addComponent(new Enemy(type, speed, reward));
    }
}