package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

/**
 * 敌人组件 - 定义敌人的属性和行为特征
 * 实现Component接口，作为ECS架构中的数据组件
 * 包含敌人类型、移动速度、击败奖励和路径跟踪信息
 */
public class Enemy implements Component {
    /**
     * 敌人类型枚举 - 定义游戏中不同类型的敌人
     */
    public enum Type {
        GOBLIN, // 哥布林：快速但脆弱
        ORC,    // 兽人：均衡型敌人
        TROLL   // 巨魔：缓慢但强大
    }

    // 公共字段 - 在ECS架构中通常直接访问以提高性能
    public Type type;      // 敌人类型
    public float speed;    // 移动速度（像素/秒）
    public int reward;     // 击败后奖励的金币数
    public int pathIndex;  // 当前路径点索引，用于路径跟踪

    public Path.PathTag pathTag;
    /**
     * 构造函数 - 初始化敌人属性
     * @param type 敌人类型，决定基础属性
     * @param speed 移动速度，影响敌人在路径上的移动快慢
     * @param reward 击败奖励，玩家消灭敌人后获得的金币数
     */
    /**
    public Enemy(Type type, float speed, int reward) {
        this.type = type;
        this.speed = speed;
        this.reward = reward;
        this.pathIndex = 0; // 从第一个路径点开始移动
    }
     */
    public Enemy(Type type, float speed, int reward, Path.PathTag pathTag) {
        this.type = type;
        this.speed = speed;
        this.reward = reward;
        this.pathIndex = 0; // 从第一个路径点开始移动
        this.pathTag = pathTag;
    }
    // ========== Getter 方法 ==========
    // 提供封装访问，虽然字段是public，但getter提供了更好的API设计

    /**
     * 获取敌人类型
     * @return 敌人类型枚举值
     */
    public Type getType() {
        return type;
    }

    /**
     * 获取移动速度
     * @return 移动速度值（像素/秒）
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * 获取击败奖励
     * @return 奖励金币数量
     */
    public int getReward() {
        return reward;
    }

    /**
     * 获取当前路径索引
     * @return 当前目标路径点的索引
     */
    public int getPathIndex() {
        return pathIndex;
    }

    // ========== Setter 方法 ==========
    // 提供修改字段值的方法，支持数据封装和验证

    /**
     * 设置敌人类型
     * @param type 新的敌人类型
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * 设置移动速度
     * @param speed 新的移动速度值
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * 设置击败奖励
     * @param reward 新的奖励金币数量
     */
    public void setReward(int reward) {
        this.reward = reward;
    }

    /**
     * 设置路径索引
     * @param pathIndex 新的路径点索引
     */
    public void setPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
    }
}