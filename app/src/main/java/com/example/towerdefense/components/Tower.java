package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

/**
 * 防御塔组件 - 定义防御塔的属性和战斗特性
 * 实现Component接口，作为ECS架构中防御塔实体的核心数据组件
 * 包含塔的类型、伤害、攻击范围、攻击速度等战斗属性
 */
public class Tower implements Component {
    /**
     * 防御塔类型枚举 - 定义游戏中不同类型的防御塔
     */
    public enum Type {
        ARCHER, // 弓箭塔：均衡的伤害和射程
        CANNON, // 加农炮：高伤害但射程较短
        MAGE    // 法师塔：中等伤害，长射程
    }

    // 公共字段 - 在ECS架构中通常直接访问以提高性能

    /**
     * 塔类型 - 决定塔的基础属性和外观
     */
    public Type type;

    /**
     * 伤害值 - 每次攻击对敌人造成的伤害量
     */
    public int damage;

    /**
     * 攻击范围 - 塔可以攻击敌人的最大距离（像素）
     */
    public float range;

    /**
     * 攻击速度 - 每秒攻击次数（attack per second）
     */
    public float attackSpeed;

    /**
     * 最后攻击时间 - 上次攻击的时间戳（毫秒）
     * 用于计算攻击冷却时间
     */
    public long lastAttackTime;

    /**
     * 构造函数 - 初始化防御塔属性
     * @param type 塔类型，决定基础属性模板
     * @param damage 伤害值，每次攻击造成的伤害
     * @param range 攻击范围，塔的有效攻击距离
     * @param attackSpeed 攻击速度，每秒攻击次数
     */
    public Tower(Type type, int damage, float range, float attackSpeed) {
        this.type = type;
        this.damage = damage;
        this.range = range;
        this.attackSpeed = attackSpeed;
        this.lastAttackTime = 0; // 初始化为0，表示可以立即攻击
    }

    // ========== Getter 方法 ==========

    /**
     * 获取塔类型
     * @return 塔的类型枚举
     */
    public Type getType() {
        return type;
    }

    /**
     * 获取伤害值
     * @return 塔的伤害值
     */
    public int getDamage() {
        return damage;
    }

    /**
     * 获取攻击范围
     * @return 塔的攻击范围（像素）
     */
    public float getRange() {
        return range;
    }

    /**
     * 获取攻击速度
     * @return 塔的攻击速度（每秒攻击次数）
     */
    public float getAttackSpeed() {
        return attackSpeed;
    }

    /**
     * 获取最后攻击时间
     * @return 上次攻击的时间戳（毫秒）
     */
    public long getLastAttackTime() {
        return lastAttackTime;
    }

    // ========== Setter 方法 ==========

    /**
     * 设置最后攻击时间
     * @param lastAttackTime 新的最后攻击时间戳
     * 在攻击后由AttackSystem调用，记录攻击时间
     */
    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    // ========== 业务逻辑方法 ==========

    /**
     * 检查塔是否可以攻击
     * @param currentTime 当前时间戳（毫秒）
     * @return true如果攻击冷却时间已过，可以发起攻击
     */
    public boolean canAttack(long currentTime) {
        // 计算攻击间隔（毫秒）= 1000毫秒 / 每秒攻击次数
        long attackInterval = (long)(1000 / attackSpeed);
        // 检查距离上次攻击是否已经过了足够的冷却时间
        return currentTime - lastAttackTime >= attackInterval;
    }
}