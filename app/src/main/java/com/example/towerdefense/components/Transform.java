package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

public class Transform implements Component {
    public float x;
    public float y;

    public Transform(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // Getter methods
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    // Setter methods (optional)
    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float distanceTo(Transform other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}