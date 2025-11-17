package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.ecs.World;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Projectile;
import com.example.towerdefense.components.Health;
import com.example.towerdefense.components.Path;
import java.util.List;
import com.example.towerdefense.GameEngine;

/**
 * 移动系统 - 处理游戏中所有实体的移动逻辑
 * 包括敌人沿着路径移动和弹道向目标移动
 * 继承自ECSSystem，处理所有具有Transform组件的实体
 */
public class MovementSystem extends ECSSystem {
    private GameEngine gameEngine;
    private float screenWidth;
    private float screenHeight;

    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    public void setScreenSize(float width, float height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public MovementSystem() {
        super(Transform.class);
    }

    @Override
    public void update(float deltaTime) {
        List<Entity> entities = getEntities();
        List<Entity> projectiles = new java.util.ArrayList<>();

        // 遍历所有实体，分离敌人和弹道的处理
        for (Entity entity : entities) {
            Transform transform = entity.getComponent(Transform.class);

            if (entity.hasComponent(Enemy.class)) {
                moveEnemy(entity, transform, deltaTime);
            }

            if (entity.hasComponent(Projectile.class)) {
                projectiles.add(entity);
            }
        }

        // 单独处理所有弹道
        if (!projectiles.isEmpty()) {
            updateProjectiles(deltaTime, projectiles);
        }
    }

    /**
     * 移动敌人 - 处理敌人沿着路径的移动，包含高地减速效果
     */
    private void moveEnemy(Entity enemy, Transform transform, float deltaTime) {
        Enemy enemyComp = enemy.getComponent(Enemy.class);
        Path enemyPath = getEnemyPath(enemyComp);

        if (enemyPath == null) {
            if (world != null) {
                world.removeEntity(enemy);
            }
            return;
        }

        // 只在有高地区域且高地由玩家控制时应用减速效果
        if (gameEngine != null && gameEngine.hasHighlandArea() && gameEngine.isHighlandControlled()) {
            // 检查敌人是否在高地区域内
            boolean wasInHighland = enemyComp.isInHighland;
            boolean isNowInHighland = gameEngine.isInHighlandArea(transform.x, transform.y);

            // 处理高地状态变化
            if (wasInHighland != isNowInHighland) {
                enemyComp.isInHighland = isNowInHighland;
                if (isNowInHighland) {
                    // 进入高地，减速
                    enemyComp.speed = enemyComp.originalSpeed * gameEngine.getHighlandSpeedMultiplier();
                    System.out.println("MovementSystem: 敌人进入高地，速度降至 " + enemyComp.speed);
                } else {
                    // 离开高地，恢复速度
                    enemyComp.speed = enemyComp.originalSpeed;
                    System.out.println("MovementSystem: 敌人离开高地，速度恢复至 " + enemyComp.speed);
                }
            }
        } else {
            // 如果高地失守或者没有高地区域，确保敌人速度正常
            if (enemyComp.isInHighland) {
                enemyComp.isInHighland = false;
                enemyComp.speed = enemyComp.originalSpeed;
            }
        }

        float[][] pathPoints = enemyPath.convertToScreenCoordinates(screenWidth, screenHeight);

        if (enemyComp.pathIndex < pathPoints.length) {
            float targetX = pathPoints[enemyComp.pathIndex][0];
            float targetY = pathPoints[enemyComp.pathIndex][1];

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
            // 敌人到达终点
            if (gameEngine != null) {
                gameEngine.onEnemyReachedEnd();
            }
            if (world != null) {
                world.removeEntity(enemy);
            }
        }
    }

    /**
     * 获取敌人对应的路径
     */
    private Path getEnemyPath(Enemy enemy) {
        if (world == null) {
            return null;
        }

        List<Entity> pathEntities = world.getEntitiesWithComponent(Path.class);
        for (Entity pathEntity : pathEntities) {
            Path path = pathEntity.getComponent(Path.class);
            if (path.getTag() == enemy.pathTag) {
                return path;
            }
        }
        return null;
    }

    /**
     * 更新所有弹道的移动
     */
    private void updateProjectiles(float deltaTime, List<Entity> projectiles) {
        for (Entity projectile : projectiles) {
            Transform transform = projectile.getComponent(Transform.class);
            Projectile projectileComp = projectile.getComponent(Projectile.class);

            if (transform == null || projectileComp == null) continue;

            if (projectileComp.isAreaDamage) {
                // 范围伤害弹道：飞向固定目标位置
                updateAreaDamageProjectile(projectile, transform, projectileComp, deltaTime);
            } else {
                // 追踪弹道：飞向移动目标
                updateTrackingProjectile(projectile, transform, projectileComp, deltaTime);
            }
        }
    }

    /**
     * 更新范围伤害弹道
     */
    private void updateAreaDamageProjectile(Entity projectile, Transform transform,
                                            Projectile projectileComp, float deltaTime) {
        float dx = projectileComp.targetX - transform.x;
        float dy = projectileComp.targetY - transform.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance < 5f) {
            applyAreaDamage(projectile, transform.x, transform.y,
                    projectileComp.damage, projectileComp.areaRadius);
            world.removeEntity(projectile);
        } else {
            float directionX = dx / distance;
            float directionY = dy / distance;
            transform.x += directionX * projectileComp.speed * deltaTime;
            transform.y += directionY * projectileComp.speed * deltaTime;
        }
    }

    /**
     * 更新追踪弹道
     */
    private void updateTrackingProjectile(Entity projectile, Transform transform,
                                          Projectile projectileComp, float deltaTime) {
        Entity target = projectileComp.target;
        if (target == null || !target.hasComponent(Transform.class)) {
            world.removeEntity(projectile);
            return;
        }

        Transform targetTransform = target.getComponent(Transform.class);
        float dx = targetTransform.x - transform.x;
        float dy = targetTransform.y - transform.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance < 10f) {
            applySingleTargetDamage(projectile, target, projectileComp.damage);
            world.removeEntity(projectile);
        } else {
            float directionX = dx / distance;
            float directionY = dy / distance;
            transform.x += directionX * projectileComp.speed * deltaTime;
            transform.y += directionY * projectileComp.speed * deltaTime;
        }
    }

    /**
     * 应用范围伤害
     */
    private void applyAreaDamage(Entity projectile, float centerX, float centerY,
                                 int damage, float radius) {
        List<Entity> enemies = world.getEntitiesWithComponent(Enemy.class);

        for (Entity enemy : enemies) {
            Transform enemyTransform = enemy.getComponent(Transform.class);
            if (enemyTransform == null) continue;

            float dx = enemyTransform.x - centerX;
            float dy = enemyTransform.y - centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= radius) {
                applyDamageToEnemy(enemy, damage);
            }
        }
    }

    /**
     * 应用单目标伤害
     */
    private void applySingleTargetDamage(Entity projectile, Entity target, int damage) {
        applyDamageToEnemy(target, damage);
    }

    /**
     * 对敌人应用伤害
     */
    private void applyDamageToEnemy(Entity enemy, int damage) {
        Health health = enemy.getComponent(Health.class);
        if (health != null) {
            health.current -= damage;

            if (health.current <= 0) {
                Enemy enemyComp = enemy.getComponent(Enemy.class);
                if (enemyComp != null && gameEngine != null) {
                    gameEngine.onEnemyDefeated(enemyComp);
                }
                world.removeEntity(enemy);
            }
        }
    }
}