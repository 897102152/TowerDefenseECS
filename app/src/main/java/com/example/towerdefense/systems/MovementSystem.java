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


// 添加 setGameEngine 方法
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
        System.out.println("MovementSystem: GameEngine引用已设置");
    }
    /**
     * 预设路径点 - 定义敌人从起点到终点的移动路径
     * 每个点包含[x, y]坐标，敌人按顺序经过这些点
     * 设置屏幕尺寸
     */
    public void setScreenSize(float width, float height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    /**
     * 构造函数 - 指定该系统处理的组件类型
     * 处理所有具有Transform组件的实体
     */
    public MovementSystem() {
        super(Transform.class);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        System.out.println("MovementSystem: 世界引用已设置，world=" + (world != null ? "有效" : "null"));
    }

    /**
     * 更新方法 - 每帧调用，处理所有实体的移动逻辑
     *
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
    /**
     * 移动敌人 - 处理敌人沿着路径的移动
     */
    private void moveEnemy(Entity enemy, Transform transform, float deltaTime) {
        Enemy enemyComp = enemy.getComponent(Enemy.class);

        // 获取敌人对应的路径
        Path enemyPath = getEnemyPath(enemyComp);
        if (enemyPath == null) {
            // 如果没有找到路径，移除敌人
            if (world != null) {
                world.removeEntity(enemy);
            }
            return;
        }

        // 将百分比路径点转换为屏幕坐标
        float[][] pathPoints = enemyPath.convertToScreenCoordinates(screenWidth, screenHeight);
        System.out.println("MovementSystem: 敌人 " + enemyComp.type + " 路径 " + enemyComp.pathTag +
                " 有 " + pathPoints.length + " 个点，当前位置: (" + transform.x + ", " + transform.y + ")");
        // 检查敌人是否还有路径点需要移动
        if (enemyComp.pathIndex < pathPoints.length) {
            // 获取当前目标路径点的坐标
            float targetX = pathPoints[enemyComp.pathIndex][0];
            float targetY = pathPoints[enemyComp.pathIndex][1];

            // 计算到目标点的方向向量和距离
            float dx = targetX - transform.x;
            float dy = targetY - transform.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // 如果距离目标点很近（小于5像素），则移动到下一个路径点
            if (distance < 5) {
                enemyComp.pathIndex++;
                System.out.println("MovementSystem: 敌人 " + enemyComp.type + " 到达路径点 " + enemyComp.pathIndex);
            } else {
                // 沿着方向向量移动敌人
                transform.x += (dx / distance) * enemyComp.speed * deltaTime;
                transform.y += (dy / distance) * enemyComp.speed * deltaTime;
            }
        } else {
            // 敌人已经到达路径终点，从世界中移除
            if (world != null) {
                world.removeEntity(enemy);
            }
        }
    }

    /**
     * 获取敌人对应的路径 - 直接从World中查找
     */
    private Path getEnemyPath(Enemy enemy) {
        if (world == null) {
            System.err.println("MovementSystem: world为null");
            return null;
        }

        // 获取所有路径实体
        List<Entity> pathEntities = world.getEntitiesWithComponent(Path.class);
        System.out.println("MovementSystem: 查找路径 " + enemy.pathTag + "，当前世界中有 " + pathEntities.size() + " 条路径");
        for (Entity pathEntity : pathEntities) {
            Path path = pathEntity.getComponent(Path.class);
            System.out.println("MovementSystem: 检查路径 " + path.getTag());
            // 根据敌人的路径标签找到对应的路径
            if (path.getTag() == enemy.pathTag) {
                System.out.println("MovementSystem: 找到匹配的路径 " + path.getTag());
                return path;
            }
        }
        System.err.println("MovementSystem: 错误！没有找到路径 " + enemy.pathTag);
        return null; // 没有找到对应的路径
    }

    /**
     * 移动弹道 - 处理弹道向目标的移动和碰撞检测
     *
     * @param projectile 弹道实体
     * @param transform  弹道的位置组件
     * @param deltaTime  时间增量
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

                    // 如果目标死亡，处理奖励
                    if (!targetHealth.isAlive()) {
                        Enemy enemyComp = target.getComponent(Enemy.class);
                        if (enemyComp != null) {
                            // 通过GameEngine发放奖励
                            GameEngine gameEngine = getGameEngine();
                            if (gameEngine != null) {
                                gameEngine.onEnemyDefeated(enemyComp);
                            }
                        }
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

    // 添加获取GameEngine的方法
    private GameEngine getGameEngine() {
        // 通过世界引用获取GameEngine
        // 这需要在World类中添加相应的方法，或者通过其他方式获取
        // 临时解决方案：在MovementSystem中添加GameEngine引用
        return this.gameEngine;
    }
}