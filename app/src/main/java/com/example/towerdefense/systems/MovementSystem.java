package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Projectile;
import com.example.towerdefense.components.Health;

import java.util.List;

/**
 * 移动系统 - 处理游戏中所有实体的移动逻辑
 * 包括敌人沿着路径移动和弹道向目标移动
 * 继承自ECSSystem，处理所有具有Transform组件的实体
 */
public class MovementSystem extends ECSSystem {

    /**
     * 预设路径点 - 定义敌人从起点到终点的移动路径
     * 每个点包含[x, y]坐标，敌人按顺序经过这些点
     */
    private final float[][] path = {
            {100, 100}, {300, 100}, {300, 300}, {100, 300}, {100, 500}
    };

    /**
     * 构造函数 - 指定该系统处理的组件类型
     * 处理所有具有Transform组件的实体
     */
    public MovementSystem() {
        super(Transform.class);
    }

    /**
     * 更新方法 - 每帧调用，处理所有实体的移动逻辑
     * @param deltaTime 距离上一帧的时间间隔（秒），用于帧率无关的移动计算
     */
    @Override
    public void update(float deltaTime) {
        // 获取所有具有Transform组件的实体
        List<Entity> entities = getEntities();

        // 遍历所有实体，根据实体类型执行相应的移动逻辑
        for (Entity entity : entities) {
            Transform transform = entity.getComponent(Transform.class);

            // 处理敌人移动
            if (entity.hasComponent(Enemy.class)) {
                moveEnemy(entity, transform, deltaTime);
            }

            // 处理弹道移动
            if (entity.hasComponent(Projectile.class)) {
                moveProjectile(entity, transform, deltaTime);
            }
        }
    }

    /**
     * 移动敌人 - 处理敌人沿着预设路径的移动
     * @param enemy 敌人实体
     * @param transform 敌人的位置组件
     * @param deltaTime 时间增量
     */
    private void moveEnemy(Entity enemy, Transform transform, float deltaTime) {
        Enemy enemyComp = enemy.getComponent(Enemy.class);

        // 检查敌人是否还有路径点需要移动
        if (enemyComp.pathIndex < path.length) {
            // 获取当前目标路径点的坐标
            float targetX = path[enemyComp.pathIndex][0];
            float targetY = path[enemyComp.pathIndex][1];

            // 计算到目标点的方向向量和距离
            float dx = targetX - transform.x;
            float dy = targetY - transform.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // 如果距离目标点很近（小于5像素），则移动到下一个路径点
            if (distance < 5) {
                enemyComp.pathIndex++;
            } else {
                // 沿着方向向量移动敌人
                // 标准化方向向量并乘以速度和时间的乘积
                transform.x += (dx / distance) * enemyComp.speed * deltaTime;
                transform.y += (dy / distance) * enemyComp.speed * deltaTime;
            }
        } else {
            // 敌人已经到达路径终点，从世界中移除
            world.removeEntity(enemy);
        }
    }

    /**
     * 移动弹道 - 处理弹道向目标的移动和碰撞检测
     * @param projectile 弹道实体
     * @param transform 弹道的位置组件
     * @param deltaTime 时间增量
     */
    private void moveProjectile(Entity projectile, Transform transform, float deltaTime) {
        Projectile projComp = projectile.getComponent(Projectile.class);
        Entity target = projComp.target;

        // 检查目标是否仍然存在
        if (target != null && world.getAllEntities().contains(target)) {
            Transform targetTransform = target.getComponent(Transform.class);

            // 计算到目标的方向向量和距离
            float dx = targetTransform.x - transform.x;
            float dy = targetTransform.y - transform.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // 如果距离目标很近（小于10像素），视为命中
            if (distance < 10) {
                // 命中目标，处理伤害逻辑
                Health targetHealth = target.getComponent(Health.class);
                if (targetHealth != null) {
                    // 对目标造成伤害
                    targetHealth.takeDamage(projComp.damage);
                    // 如果目标死亡，从世界中移除
                    if (!targetHealth.isAlive()) {
                        world.removeEntity(target);
                    }
                }
                // 移除弹道实体
                world.removeEntity(projectile);
            } else {
                // 向目标移动弹道
                transform.x += (dx / distance) * projComp.speed * deltaTime;
                transform.y += (dy / distance) * projComp.speed * deltaTime;
            }
        } else {
            // 目标已不存在，移除弹道
            world.removeEntity(projectile);
        }
    }
}