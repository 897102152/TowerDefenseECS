package com.example.towerdefense.ecs;

import java.util.*;

/**
 * 世界类 - ECS (Entity-Component-System) 架构的核心容器和管理器
 *
 * World 是 ECS 架构的中央协调者，负责：
 * - 管理所有实体的生命周期（创建、销毁）
 * - 注册和管理所有系统
 * - 协调系统更新循环
 * - 提供实体查询和筛选功能
 *
 * 设计模式：管理器模式 (Manager Pattern)
 * 目的：集中管理游戏中的所有实体和系统，提供统一的访问接口
 */
public class World {
    /**
     * 实体列表 - 存储世界中所有的活动实体
     * 使用ArrayList提供快速的顺序访问和随机访问
     * 实体在创建时添加，在销毁时移除
     */
    private List<Entity> entities;

    /**
     * 系统列表 - 存储世界中所有注册的游戏系统
     * 使用ArrayList保持系统的添加顺序，这会影响系统更新顺序
     * 系统按照添加顺序依次更新
     */
    private List<ECSSystem> systems;

    /**
     * 下一个实体ID - 用于生成实体的唯一标识符
     * 采用自增策略，确保每个实体都有唯一的ID
     * 从0开始，每次创建新实体时递增
     */
    private int nextEntityId;

    /**
     * 构造函数 - 初始化空的世界
     * 创建空的实体列表和系统列表
     * 重置实体ID计数器
     */
    public World() {
        this.entities = new ArrayList<>();
        this.systems = new ArrayList<>();
        this.nextEntityId = 0;
    }

    /**
     * 创建新实体 - 生成具有唯一ID的空实体
     * @return 新创建的实体实例
     *
     * 流程：
     * 1. 使用当前nextEntityId创建实体
     * 2. 递增nextEntityId为下一个实体准备
     * 3. 将实体添加到实体列表中
     * 4. 返回新创建的实体供外部配置组件
     */
    public Entity createEntity() {
        Entity entity = new Entity(nextEntityId++);
        entities.add(entity);
        return entity;
    }

    /**
     * 移除实体 - 从世界中删除指定实体
     * @param entity 要移除的实体
     *
     * 注意：这会从实体列表中移除实体引用
     * 但没有显式清理组件的资源，依赖GC自动回收
     * 实体被移除后，所有系统将不再处理该实体
     */
    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    /**
     * 添加系统 - 注册新的游戏系统到世界中
     * @param system 要添加的系统实例
     *
     * 流程：
     * 1. 设置系统的世界引用（建立双向连接）
     * 2. 将系统添加到系统列表中
     * 3. 系统将在每帧更新时被调用
     */
    public void addSystem(ECSSystem system) {
        // 设置系统的世界引用
        system.setWorld(this);
        systems.add(system);
        System.out.println("World: 添加系统 " + system.getClass().getSimpleName() +
                " (world=" + (system.world != null) + ")");
    }
    /**
     * 更新世界 - 执行一帧的游戏逻辑
     * @param deltaTime 距离上一帧的时间间隔（秒）
     *
     * 这是游戏循环的核心方法，每帧调用一次
     * 按照系统注册顺序依次更新所有系统
     * 系统更新顺序可能影响游戏逻辑，需要谨慎设计
     */
    public void update(float deltaTime) {
        System.out.println("World: 更新开始，系统数量=" + systems.size() + ", 实体数量=" + entities.size());

        for (ECSSystem system : systems) {
            // 使用公共方法检查
            if (!system.isWorldSet()) {
                System.err.println("World: 严重错误！系统 " + system.getClass().getSimpleName() + " 的 world 为 null");
                system.setWorld(this); // 立即修复
            }

            System.out.println("World: 更新系统 " + system.getClass().getSimpleName() + " (world=" + system.isWorldSet() + ")");
            system.update(deltaTime);
        }

        System.out.println("World: 更新结束");
    }

    /**
     * 获取系统对应的实体列表 - 筛选拥有系统所需所有组件的实体
     * @param system 请求实体的系统
     * @return 符合系统组件要求的实体列表
     *
     * 这是ECS架构的关键方法，实现实体到系统的自动分配
     * 性能考虑：每次调用都会实时筛选，适合实体数量不多的场景
     * 对于大型游戏，可以考虑使用缓存优化
     */
    public List<Entity> getEntitiesForSystem(ECSSystem system) {
        List<Entity> result = new ArrayList<>();
        Set<Class<? extends Component>> required = system.getRequiredComponents();

        // 遍历所有实体，检查是否符合系统的组件要求
        for (Entity entity : entities) {
            boolean hasAllComponents = true;
            for (Class<? extends Component> componentClass : required) {
                if (!entity.hasComponent(componentClass)) {
                    hasAllComponents = false;
                    break;
                }
            }
            if (hasAllComponents) {
                result.add(entity);
            }
        }

        return result;
    }

    /**
     * 获取所有实体 - 返回世界中所有实体的副本
     * @return 所有实体的列表副本
     *
     * 返回副本是为了防止外部代码直接修改内部实体列表
     * 使用场景：调试、序列化、特殊系统需要访问所有实体
     * 注意：返回的是实体引用的副本，实体本身是原始引用
     */
    public List<Entity> getAllEntities() {
        return new ArrayList<>(entities);
    }
    /**
     * 获取所有包含指定组件的实体
     * @param componentClass 组件类
     * @return 包含该组件的实体列表
     */
    public List<Entity> getEntitiesWithComponent(Class<? extends Component> componentClass) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.hasComponent(componentClass)) {
                result.add(entity);
            }
        }
        return result;
    }
}