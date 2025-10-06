package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

public class Enemy implements Component {
    public enum Type { GOBLIN, ORC, TROLL }

    public Type type;
    public float speed;
    public int reward;
    public int pathIndex;

    public Enemy(Type type, float speed, int reward) {
        this.type = type;
        this.speed = speed;
        this.reward = reward;
        this.pathIndex = 0;
    }

    // Getter methods
    public Type getType() {
        return type;
    }

    public float getSpeed() {
        return speed;
    }

    public int getReward() {
        return reward;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    // Setter methods
    public void setType(Type type) {
        this.type = type;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public void setPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
    }
}