package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;
import com.example.towerdefense.ecs.Entity;

public class Projectile implements Component {
    public Entity target;
    public int damage;
    public float speed;

    public Projectile(Entity target, int damage, float speed) {
        this.target = target;
        this.damage = damage;
        this.speed = speed;
    }

    // Getter methods
    public Entity getTarget() {
        return target;
    }

    public int getDamage() {
        return damage;
    }

    public float getSpeed() {
        return speed;
    }

    // Setter methods
    public void setTarget(Entity target) {
        this.target = target;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}