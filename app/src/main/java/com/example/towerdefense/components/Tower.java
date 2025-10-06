package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

public class Tower implements Component {
    public enum Type { ARCHER, CANNON, MAGE }

    public Type type;
    public int damage;
    public float range;
    public float attackSpeed; // attacks per second
    public long lastAttackTime;

    public Tower(Type type, int damage, float range, float attackSpeed) {
        this.type = type;
        this.damage = damage;
        this.range = range;
        this.attackSpeed = attackSpeed;
        this.lastAttackTime = 0;
    }

    // Getter methods
    public Type getType() {
        return type;
    }

    public int getDamage() {
        return damage;
    }

    public float getRange() {
        return range;
    }

    public float getAttackSpeed() {
        return attackSpeed;
    }

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    // Setter methods
    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    public boolean canAttack(long currentTime) {
        return currentTime - lastAttackTime >= (1000 / attackSpeed);
    }
}