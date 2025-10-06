package com.example.towerdefense.ecs;

import java.util.*;

public class World {
    private List<Entity> entities;
    private List<ECSSystem> systems;  // 改为 ECSSystem
    private int nextEntityId;

    public World() {
        this.entities = new ArrayList<>();
        this.systems = new ArrayList<>();
        this.nextEntityId = 0;
    }

    public Entity createEntity() {
        Entity entity = new Entity(nextEntityId++);
        entities.add(entity);
        return entity;
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public void addSystem(ECSSystem system) {  // 改为 ECSSystem
        system.setWorld(this);
        systems.add(system);
    }

    public void update(float deltaTime) {
        for (ECSSystem system : systems) {  // 改为 ECSSystem
            system.update(deltaTime);
        }
    }

    public List<Entity> getEntitiesForSystem(ECSSystem system) {  // 改为 ECSSystem
        List<Entity> result = new ArrayList<>();
        Set<Class<? extends Component>> required = system.getRequiredComponents();

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

    public List<Entity> getAllEntities() {
        return new ArrayList<>(entities);
    }
}