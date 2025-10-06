package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Projectile;
import com.example.towerdefense.components.Health;

import java.util.List;

public class MovementSystem extends ECSSystem {

    private final float[][] path = {
            {100, 100}, {300, 100}, {300, 300}, {100, 300}, {100, 500}
    };

    public MovementSystem() {
        super(Transform.class);
    }

    @Override
    public void update(float deltaTime) {
        List<Entity> entities = getEntities();

        for (Entity entity : entities) {
            Transform transform = entity.getComponent(Transform.class);

            if (entity.hasComponent(Enemy.class)) {
                moveEnemy(entity, transform, deltaTime);
            }

            if (entity.hasComponent(Projectile.class)) {
                moveProjectile(entity, transform, deltaTime);
            }
        }
    }

    private void moveEnemy(Entity enemy, Transform transform, float deltaTime) {
        Enemy enemyComp = enemy.getComponent(Enemy.class);

        if (enemyComp.pathIndex < path.length) {
            float targetX = path[enemyComp.pathIndex][0];
            float targetY = path[enemyComp.pathIndex][1];

            float dx = targetX - transform.x;
            float dy = targetY - transform.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < 5) {
                enemyComp.pathIndex++;
            } else {
                transform.x += (dx / distance) * enemyComp.speed * deltaTime;
                transform.y += (dy / distance) * enemyComp.speed * deltaTime;
            }
        } else {
            // Enemy reached the end
            world.removeEntity(enemy);
        }
    }

    private void moveProjectile(Entity projectile, Transform transform, float deltaTime) {
        Projectile projComp = projectile.getComponent(Projectile.class);
        Entity target = projComp.target;

        if (target != null && world.getAllEntities().contains(target)) {
            Transform targetTransform = target.getComponent(Transform.class);

            float dx = targetTransform.x - transform.x;
            float dy = targetTransform.y - transform.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < 10) {
                // Hit target
                Health targetHealth = target.getComponent(Health.class);
                if (targetHealth != null) {
                    targetHealth.takeDamage(projComp.damage);
                    if (!targetHealth.isAlive()) {
                        world.removeEntity(target);
                    }
                }
                world.removeEntity(projectile);
            } else {
                transform.x += (dx / distance) * projComp.speed * deltaTime;
                transform.y += (dy / distance) * projComp.speed * deltaTime;
            }
        } else {
            world.removeEntity(projectile);
        }
    }
}