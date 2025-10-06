package com.example.towerdefense.systems;

import com.example.towerdefense.ecs.ECSSystem;  // 改为 ECSSystem
import com.example.towerdefense.components.Transform;

public class RenderingSystem extends ECSSystem {  // 改为 ECSSystem

    public RenderingSystem() {
        super(Transform.class);
    }

    @Override
    public void update(float deltaTime) {
        // 在 Android 中，渲染由 GameView 处理
        // 这个系统现在只用于逻辑，不进行实际渲染
    }
}