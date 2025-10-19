package com.example.towerdefense.ecs;

import java.util.*;

/**
 * ECS系统基类 - ECS (Entity-Component-System) 架构的核心抽象类
 */
public abstract class ECSSystem {
    protected World world;
    protected Set<Class<? extends Component>> requiredComponents;

    @SafeVarargs
    public ECSSystem(Class<? extends Component>... requiredComponents) {
        this.requiredComponents = new HashSet<>(Arrays.asList(requiredComponents));
    }

    public void setWorld(World world) {
        this.world = world;
        System.out.println("ECSSystem: " + this.getClass().getSimpleName() + " 的世界引用已设置");
    }

    // 添加公共方法来获取世界引用
    public World getWorld() {
        return world;
    }

    // 添加公共方法来检查世界引用是否设置
    public boolean isWorldSet() {
        return world != null;
    }

    public Set<Class<? extends Component>> getRequiredComponents() {
        return requiredComponents;
    }

    public abstract void update(float deltaTime);

    protected List<Entity> getEntities() {
        if (world == null) {
            String systemName = this.getClass().getSimpleName();
            System.err.println("ECSSystem: 严重错误！" + systemName + " 的 world 为 null，无法获取实体");
            return new ArrayList<>();
        }
        return world.getEntitiesForSystem(this);
    }
}