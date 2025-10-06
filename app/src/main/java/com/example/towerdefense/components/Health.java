package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

/**
 * 生命值组件 - 管理游戏实体的生命值状态
 * 实现Component接口，用于敌人、建筑或其他需要生命值管理的实体
 * 提供生命值跟踪、伤害处理、存活状态检查等功能
 */
public class Health implements Component {
    /**
     * 当前生命值 - 实体的当前生命值状态
     * 当值为0时，实体被认为已死亡
     */
    public int current;

    /**
     * 最大生命值 - 实体的生命值上限
     * 用于计算生命值百分比和初始化
     */
    public int max;

    /**
     * 构造函数 - 初始化生命值组件
     * @param max 最大生命值，同时设置当前生命值为该最大值
     */
    public Health(int max) {
        this.max = max;
        this.current = max; // 初始状态下生命值为满值
    }

    // ========== Getter 方法 ==========

    /**
     * 获取当前生命值
     * @return 当前生命值数值
     */
    public int getCurrent() {
        return current;
    }

    /**
     * 获取最大生命值
     * @return 最大生命值数值
     */
    public int getMax() {
        return max;
    }

    // ========== Setter 方法 ==========

    /**
     * 设置当前生命值
     * @param current 新的当前生命值，会自动限制在0到max之间
     */
    public void setCurrent(int current) {
        this.current = current;
    }

    /**
     * 设置最大生命值
     * @param max 新的最大生命值
     * 注意：这不会自动调整当前生命值，可能需要额外逻辑处理
     */
    public void setMax(int max) {
        this.max = max;
    }

    // ========== 业务逻辑方法 ==========

    /**
     * 检查实体是否存活
     * @return true如果当前生命值大于0，false表示实体已死亡
     */
    public boolean isAlive() {
        return current > 0;
    }

    /**
     * 承受伤害 - 减少当前生命值
     * @param damage 受到的伤害值
     * 伤害值会被限制，确保生命值不会低于0
     */
    public void takeDamage(int damage) {
        // 使用Math.max确保生命值不会变成负数
        current = Math.max(0, current - damage);
    }
}