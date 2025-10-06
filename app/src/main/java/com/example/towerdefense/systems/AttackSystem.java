package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Projectile;

import java.util.List;

/**
 * 攻击系统 - 处理防御塔的攻击逻辑
 * 负责检测敌人、发射弹道、处理攻击冷却等
 * 继承自ECSSystem，专门处理具有Transform和Tower组件的实体
 */
public class AttackSystem extends ECSSystem {

    /**
     * 构造函数 - 指定该系统处理的组件类型
     * 只处理同时拥有Transform和Tower组件的实体（即防御塔）
     */
    public AttackSystem() {
        super(Transform.class, Tower.class);
    }

    /**
     * 更新方法 - 每帧调用，处理所有防御塔的攻击逻辑
     * @param deltaTime 距离上一帧的时间间隔（秒）
     */
    @Override
    public void update(float deltaTime) {
        // 获取所有防御塔实体
        List<Entity> towers = getEntities();

        // 获取当前时间（毫秒），用于攻击冷却判断
        // 修复：使用 System.currentTimeMillis() 而不是 ECSSystem.currentTimeMillis()
        long currentTime = System.currentTimeMillis();

        // 获取所有敌人实体，并过滤出具有Transform和Enemy组件的有效敌人
        List<Entity> enemies = world.getAllEntities();
        enemies.removeIf(e -> !e.hasComponent(Enemy.class) || !e.hasComponent(Transform.class));

        // 遍历所有防御塔，检查是否可以攻击
        for (Entity tower : towers) {
            // 获取防御塔的位置和攻击属性组件
            Transform towerTransform = tower.getComponent(Transform.class);
            Tower towerComp = tower.getComponent(Tower.class);

            // 检查防御塔是否可以攻击（冷却时间已过）
            if (towerComp.canAttack(currentTime)) {
                // 在攻击范围内寻找目标敌人
                Entity target = findTargetInRange(towerTransform, towerComp, enemies);

                if (target != null) {
                    // 找到目标，创建弹道并攻击
                    createProjectile(tower, target, towerComp.damage);
                    // 更新最后攻击时间，开始冷却
                    towerComp.lastAttackTime = currentTime;
                }
            }
        }
    }

    /**
     * 在防御塔攻击范围内寻找目标敌人
     * @param towerTransform 防御塔的位置信息
     * @param tower 防御塔的攻击属性
     * @param enemies 所有有效的敌人实体列表
     * @return 攻击范围内的第一个敌人，如果没有则返回null
     */
    private Entity findTargetInRange(Transform towerTransform, Tower tower, List<Entity> enemies) {
        // 遍历所有敌人，检查是否在攻击范围内
        for (Entity enemy : enemies) {
            // 获取敌人的位置信息
            Transform enemyTransform = enemy.getComponent(Transform.class);

            // 计算防御塔与敌人之间的距离
            float distance = towerTransform.distanceTo(enemyTransform);

            // 如果敌人在攻击范围内，返回该敌人
            if (distance <= tower.range) {
                return enemy;
            }
        }
        // 没有找到在攻击范围内的敌人
        return null;
    }

    /**
     * 创建弹道实体 - 当防御塔攻击时调用
     * @param tower 发起攻击的防御塔实体
     * @param target 攻击目标敌人实体
     * @param damage 弹道造成的伤害值
     */
    private void createProjectile(Entity tower, Entity target, int damage) {
        // 获取防御塔的位置，作为弹道的起始位置
        Transform towerTransform = tower.getComponent(Transform.class);

        // 创建新的弹道实体
        Entity projectile = world.createEntity();

        // 添加位置组件，设置在防御塔的位置
        projectile.addComponent(new Transform(towerTransform.x, towerTransform.y));

        // 添加弹道组件，包含目标、伤害和移动速度
        projectile.addComponent(new Projectile(target, damage, 200f)); // 200f 是弹道移动速度
    }
}