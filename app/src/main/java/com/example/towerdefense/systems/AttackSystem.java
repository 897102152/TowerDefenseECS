package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Projectile;

import java.util.List;

public class AttackSystem extends ECSSystem {

    public AttackSystem() {
        super(Transform.class, Tower.class);
    }

    @Override
    public void update(float deltaTime) {
        List<Entity> towers = getEntities();

        // 修复：使用 System.currentTimeMillis() 而不是 ECSSystem.currentTimeMillis()
        long currentTime = System.currentTimeMillis();  // 改为 System.currentTimeMillis()

        // Get all enemies
        List<Entity> enemies = world.getAllEntities();
        enemies.removeIf(e -> !e.hasComponent(Enemy.class) || !e.hasComponent(Transform.class));

        for (Entity tower : towers) {
            Transform towerTransform = tower.getComponent(Transform.class);
            Tower towerComp = tower.getComponent(Tower.class);

            if (towerComp.canAttack(currentTime)) {
                Entity target = findTargetInRange(towerTransform, towerComp, enemies);
                if (target != null) {
                    createProjectile(tower, target, towerComp.damage);
                    towerComp.lastAttackTime = currentTime;
                }
            }
        }
    }

    private Entity findTargetInRange(Transform towerTransform, Tower tower, List<Entity> enemies) {
        for (Entity enemy : enemies) {
            Transform enemyTransform = enemy.getComponent(Transform.class);
            float distance = towerTransform.distanceTo(enemyTransform);
            if (distance <= tower.range) {
                return enemy;
            }
        }
        return null;
    }

    private void createProjectile(Entity tower, Entity target, int damage) {
        Transform towerTransform = tower.getComponent(Transform.class);

        Entity projectile = world.createEntity();
        projectile.addComponent(new Transform(towerTransform.x, towerTransform.y));
        projectile.addComponent(new Projectile(target, damage, 200f));
    }
}