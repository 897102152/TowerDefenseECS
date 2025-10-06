package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

/**
 * 变换组件 - 表示实体在游戏世界中的位置信息
 * 实现Component接口，是ECS架构中最基础且最常用的组件
 * 几乎所有可见的实体都需要此组件来定义其位置
 */
public class Transform implements Component {
    /**
     * X坐标 - 实体在游戏世界中的水平位置
     * 使用float类型提供足够的位置精度
     */
    public float x;

    /**
     * Y坐标 - 实体在游戏世界中的垂直位置
     * 使用float类型提供足够的位置精度
     */
    public float y;

    /**
     * 构造函数 - 初始化变换组件的位置
     * @param x 初始X坐标
     * @param y 初始Y坐标
     */
    public Transform(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // ========== Getter 方法 ==========

    /**
     * 获取X坐标
     * @return 当前的X坐标值
     */
    public float getX() {
        return x;
    }

    /**
     * 获取Y坐标
     * @return 当前的Y坐标值
     */
    public float getY() {
        return y;
    }

    // ========== Setter 方法 ==========

    /**
     * 设置X坐标
     * @param x 新的X坐标值
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * 设置Y坐标
     * @param y 新的Y坐标值
     */
    public void setY(float y) {
        this.y = y;
    }

    // ========== 业务逻辑方法 ==========

    /**
     * 计算到另一个变换组件的距离
     * 使用欧几里得距离公式：√(Δx² + Δy²)
     * @param other 另一个变换组件
     * @return 两个位置之间的直线距离
     */
    public float distanceTo(Transform other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}