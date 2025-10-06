package com.example.towerdefense.ecs;

import java.util.*;

/**
 * ECS系统基类 - ECS (Entity-Component-System) 架构的核心抽象类
 *
 * 所有游戏系统都应该继承此类，定义系统需要处理的组件类型和更新逻辑
 * 采用模板方法模式，为具体系统提供统一的框架和工具方法
 *
 * 在ECS架构中：
 * - 系统负责处理具有特定组件组合的实体的逻辑
 * - 系统通过requiredComponents定义其关注的组件类型
 * - 世界(World)自动将符合条件的实体提供给系统处理
 */
public abstract class ECSSystem {
    /**
     * 世界引用 - 系统所属的游戏世界实例
     * 用于访问实体和其他系统
     */
    protected World world;

    /**
     * 必需组件集合 - 定义该系统需要处理的实体必须拥有的组件类型
     * 使用Set确保组件类型唯一性，避免重复
     */
    protected Set<Class<? extends Component>> requiredComponents;

    /**
     * 构造函数 - 初始化系统所需的组件类型
     * @param requiredComponents 可变参数，指定系统处理实体必须拥有的组件类型
     * 如果为空数组，表示系统处理所有实体（全局系统）
     *
     * 示例：
     * - new MovementSystem(Transform.class) 处理所有有位置的实体
     * - new AttackSystem(Transform.class, Tower.class) 处理所有防御塔
     * - new SpawnSystem() 全局生成系统，不需要特定组件
     */
    @SafeVarargs
    public ECSSystem(Class<? extends Component>... requiredComponents) {
        // 将可变参数转换为Set，便于快速查找和去重
        this.requiredComponents = new HashSet<>(Arrays.asList(requiredComponents));
    }

    /**
     * 设置世界引用 - 在系统被添加到世界时调用
     * @param world 游戏世界实例
     * 由World类在addSystem时自动调用，建立系统与世界的双向引用
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * 获取必需组件集合
     * @return 系统需要的组件类型集合
     * 世界使用此信息来筛选适合该系统处理的实体
     */
    public Set<Class<? extends Component>> getRequiredComponents() {
        return requiredComponents;
    }

    /**
     * 抽象更新方法 - 每帧被调用的核心逻辑
     * @param deltaTime 距离上一帧的时间间隔（秒）
     * 子类必须实现此方法，包含该系统每帧需要执行的逻辑
     *
     * 使用示例：
     * - MovementSystem: 移动所有具有Transform组件的实体
     * - AttackSystem: 处理所有防御塔的攻击逻辑
     * - SpawnSystem: 生成新的敌人
     */
    public abstract void update(float deltaTime);

    /**
     * 获取符合条件的实体列表
     * @return 拥有该系统所需所有组件的实体列表
     * 保护方法，仅供子类在update方法中调用
     *
     * 性能优化：World会缓存实体列表，避免每帧重新筛选
     */
    protected List<Entity> getEntities() {
        return world.getEntitiesForSystem(this);
    }
}