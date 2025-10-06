package com.example.towerdefense.ecs;

/**
 * 组件标记接口 - ECS (Entity-Component-System) 架构的核心接口
 *
 * 这是一个标记接口 (Marker Interface)，不包含任何方法声明
 * 用于标识所有组件类，为ECS框架提供类型安全的基础
 *
 * 在ECS架构中：
 * - Entity（实体）: 游戏对象的唯一标识，是组件的容器
 * - Component（组件）: 纯数据类，存储实体的各种属性和状态
 * - System（系统）: 处理具有特定组件组合的实体的逻辑
 *
 * 设计模式：标记接口模式 (Marker Interface Pattern)
 * 目的：在编译时和运行时提供类型检查，确保只有正确的组件类型被使用
 */
public interface Component {
    // 标记接口 - 没有任何方法声明
    // 所有组件类都必须实现此接口
    // 例如：Transform、Tower、Enemy、Health等都实现此接口
}