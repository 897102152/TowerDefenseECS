package com.example.towerdefense.ecs;

import java.util.*;

public abstract class ECSSystem {
    protected World world;
    protected Set<Class<? extends Component>> requiredComponents;

    public ECSSystem(Class<? extends Component>... requiredComponents) {
        this.requiredComponents = new HashSet<>(Arrays.asList(requiredComponents));
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Set<Class<? extends Component>> getRequiredComponents() {
        return requiredComponents;
    }

    public abstract void update(float deltaTime);

    protected List<Entity> getEntities() {
        return world.getEntitiesForSystem(this);
    }
}