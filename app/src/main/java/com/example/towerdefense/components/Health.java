package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

public class Health implements Component {
    public int current;
    public int max;

    public Health(int max) {
        this.max = max;
        this.current = max;
    }

    // Getter methods
    public int getCurrent() {
        return current;
    }

    public int getMax() {
        return max;
    }

    // Setter methods
    public void setCurrent(int current) {
        this.current = current;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public boolean isAlive() {
        return current > 0;
    }

    public void takeDamage(int damage) {
        current = Math.max(0, current - damage);
    }
}