package com.example.towerdefense.ecs;

import java.util.*;

public class Entity {
    private final int id;
    private final Map<Class<? extends Component>, Component> components;

    // 确保有这个带参数的构造函数
    public Entity(int id) {
        this.id = id;
        this.components = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public <T extends Component> void addComponent(T component) {
        components.put(component.getClass(), component);
    }

    public <T extends Component> T getComponent(Class<T> componentClass) {
        return componentClass.cast(components.get(componentClass));
    }

    public <T extends Component> boolean hasComponent(Class<T> componentClass) {
        return components.containsKey(componentClass);
    }

    public <T extends Component> void removeComponent(Class<T> componentClass) {
        components.remove(componentClass);
    }

    public Set<Class<? extends Component>> getComponentTypes() {
        return components.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}