package com.example.towerdefense.ecs;

import java.util.*;

/**
 * 实体类 - ECS (Entity-Component-System) 架构的核心类
 *
 * 实体是游戏世界中所有对象的唯一标识和容器
 * 实体本身不包含逻辑，只作为组件的容器
 * 通过组合不同的组件来定义对象的行为和特性
 *
 * 设计理念：
 * - 实体 = 唯一ID + 组件集合
 * - 通过组件组合而非继承来定义对象特性
 * - 支持运行时的动态组件添加和移除
 */
public class Entity {
    /**
     * 实体唯一标识符 - 用于区分不同的实体
     * 使用final确保ID在实体生命周期内不变
     * 基于ID的equals和hashCode实现提供高效的实体比较
     */
    private final int id;

    /**
     * 组件映射 - 存储实体拥有的所有组件
     * Key: 组件类类型 (Class<? extends Component>)
     * Value: 组件实例 (Component)
     *
     * 设计选择：使用HashMap提供O(1)的组件访问性能
     * 每个组件类型只能有一个实例（符合ECS常见规范）
     */
    private final Map<Class<? extends Component>, Component> components;

    /**
     * 构造函数 - 创建具有指定ID的新实体
     * @param id 实体的唯一标识符
     * 通常由World类负责生成和管理实体ID
     */
    public Entity(int id) {
        this.id = id;
        this.components = new HashMap<>();
    }

    /**
     * 获取实体ID
     * @return 实体的唯一标识符
     */
    public int getId() {
        return id;
    }

    /**
     * 添加组件到实体
     * @param component 要添加的组件实例
     * @param <T> 组件类型，必须继承自Component接口
     *
     * 如果已存在同类型的组件，新的组件将覆盖旧的组件
     * 这允许在运行时动态修改实体的组件配置
     */
    public <T extends Component> void addComponent(T component) {
        components.put(component.getClass(), component);
    }

    /**
     * 获取指定类型的组件
     * @param componentClass 要获取的组件类型Class对象
     * @param <T> 组件类型
     * @return 组件实例，如果不存在则返回null
     *
     * 使用泛型确保类型安全，避免强制类型转换
     * 调用示例：Transform transform = entity.getComponent(Transform.class);
     */
    public <T extends Component> T getComponent(Class<T> componentClass) {
        return componentClass.cast(components.get(componentClass));
    }

    /**
     * 检查实体是否拥有指定类型的组件
     * @param componentClass 要检查的组件类型
     * @param <T> 组件类型
     * @return true如果实体拥有该类型的组件，否则false
     *
     * 系统使用此方法快速判断实体是否符合处理条件
     */
    public <T extends Component> boolean hasComponent(Class<T> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * 从实体中移除指定类型的组件
     * @param componentClass 要移除的组件类型
     * @param <T> 组件类型
     *
     * 移除后，实体将不再拥有该组件的行为和特性
     * 相关系统将停止处理该实体的对应逻辑
     */
    public <T extends Component> void removeComponent(Class<T> componentClass) {
        components.remove(componentClass);
    }

    /**
     * 获取实体拥有的所有组件类型
     * @return 组件类型集合
     *
     * 主要用于调试、序列化和系统匹配
     * World类使用此信息来优化实体到系统的分配
     */
    public Set<Class<? extends Component>> getComponentTypes() {
        return components.keySet();
    }

    /**
     * 实体相等性比较 - 基于ID比较
     * @param o 要比较的对象
     * @return true如果两个实体具有相同的ID
     *
     * 确保在集合中实体可以正确比较和去重
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    /**
     * 实体哈希码 - 基于ID生成
     * @return 实体的哈希码
     *
     * 与equals方法保持一致，确保实体在哈希集合中正确工作
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}