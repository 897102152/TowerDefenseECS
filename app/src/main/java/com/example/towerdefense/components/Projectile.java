package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;
import com.example.towerdefense.ecs.Entity;

/**
 * 弹道组件 - 表示游戏中发射的弹道实体（箭矢、炮弹、魔法飞弹等）
 * 实现Component接口，用于追踪弹道的目标、伤害和移动特性
 * 在攻击系统中创建，在移动系统中更新，在碰撞时造成伤害
 */
public class Projectile implements Component {
    /**
     * 目标实体 - 弹道追踪的敌人目标
     * 当目标被销毁或不存在时，弹道也会被清除
     */
    public Entity target;

    /**
     * 目标位置 - 对于范围伤害的弹道，记录攻击的目标位置
     */
    public float targetX, targetY;

    /**
     * 伤害值 - 弹道命中目标时造成的伤害量
     * 这个值通常从发射的防御塔组件中传递过来
     */
    public int damage;

    /**
     * 移动速度 - 弹道每帧移动的速度（像素/秒）
     * 影响弹道飞向目标的快慢，不同类型的弹道可以有不同速度
     */
    public float speed;

    /**
     * 是否是范围伤害
     */
    public boolean isAreaDamage = false;

    /**
     * 范围伤害半径
     */
    public float areaRadius = 0;

    /**
     * 发射该弹道的防御塔类型
     */
    public Tower.Type towerType;

    /**
     * 构造函数 - 初始化弹道组件（追踪目标）
     * @param target 弹道要追踪的目标敌人实体
     * @param damage 命中时造成的伤害值
     * @param speed 弹道的移动速度
     * @param towerType 发射弹道的防御塔类型
     */
    public Projectile(Entity target, int damage, float speed, Tower.Type towerType) {
        this.target = target;
        this.damage = damage;
        this.speed = speed;
        this.towerType = towerType;

        // 记录目标当前位置
        if (target != null && target.hasComponent(Transform.class)) {
            Transform targetTransform = target.getComponent(Transform.class);
            this.targetX = targetTransform.x;
            this.targetY = targetTransform.y;
        }
    }

    /**
     * 构造函数 - 初始化弹道组件（范围伤害，不追踪）
     * @param targetX 目标位置X
     * @param targetY 目标位置Y
     * @param damage 命中时造成的伤害值
     * @param speed 弹道的移动速度
     * @param isAreaDamage 是否是范围伤害
     * @param areaRadius 范围伤害半径
     * @param towerType 发射弹道的防御塔类型
     */
    public Projectile(float targetX, float targetY, int damage, float speed, boolean isAreaDamage, float areaRadius, Tower.Type towerType) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.damage = damage;
        this.speed = speed;
        this.isAreaDamage = isAreaDamage;
        this.areaRadius = areaRadius;
        this.towerType = towerType;
    }

    // ========== Getter 方法 ==========

    /**
     * 获取目标实体
     * @return 弹道当前追踪的目标敌人
     */
    public Entity getTarget() {
        return target;
    }

    /**
     * 获取伤害值
     * @return 弹道命中时造成的伤害量
     */
    public int getDamage() {
        return damage;
    }

    /**
     * 获取移动速度
     * @return 弹道的移动速度（像素/秒）
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * 获取是否是范围伤害
     * @return 是否是范围伤害
     */
    public boolean isAreaDamage() {
        return isAreaDamage;
    }

    /**
     * 获取范围伤害半径
     * @return 范围伤害半径
     */
    public float getAreaRadius() {
        return areaRadius;
    }

    /**
     * 获取发射弹道的防御塔类型
     * @return 防御塔类型
     */
    public Tower.Type getTowerType() {
        return towerType;
    }

    // ========== Setter 方法 ==========

    /**
     * 设置目标实体
     * @param target 新的目标敌人实体
     * 可用于实现弹道重定向或目标切换
     */
    public void setTarget(Entity target) {
        this.target = target;
    }

    /**
     * 设置伤害值
     * @param damage 新的伤害值
     * 可用于实现伤害强化或减益效果
     */
    public void setDamage(int damage) {
        this.damage = damage;
    }

    /**
     * 设置移动速度
     * @param speed 新的移动速度
     * 可用于实现加速或减速效果
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * 设置范围伤害
     * @param areaDamage 是否是范围伤害
     * @param radius 范围伤害半径
     */
    public void setAreaDamage(boolean areaDamage, float radius) {
        this.isAreaDamage = areaDamage;
        this.areaRadius = radius;
    }

    /**
     * 设置防御塔类型
     * @param towerType 防御塔类型
     */
    public void setTowerType(Tower.Type towerType) {
        this.towerType = towerType;
    }
}