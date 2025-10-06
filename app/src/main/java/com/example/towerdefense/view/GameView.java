package com.example.towerdefense.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.towerdefense.GameEngine;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.ecs.World;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.components.Health;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Projectile;

import java.util.List;

public class GameView extends View {
    private GameEngine gameEngine;
    private Tower.Type selectedTowerType = Tower.Type.ARCHER;
    private Paint paint;

    // 游戏路径点
    private float[][] pathPoints = {
            {100, 100}, {300, 100}, {300, 300}, {100, 300}, {100, 500}
    };

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        setBackgroundColor(Color.DKGRAY);
    }

    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    public void setSelectedTowerType(Tower.Type towerType) {
        this.selectedTowerType = towerType;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制背景
        canvas.drawColor(Color.DKGRAY);

        if (gameEngine == null) {
            // 显示调试信息：GameEngine 为空
            paint.setColor(Color.RED);
            paint.setTextSize(40);
            canvas.drawText("GameEngine is null", 50, 100, paint);
            return;
        }

        World world = gameEngine.getWorld();
        if (world == null) {
            // 显示调试信息：World 为空
            paint.setColor(Color.RED);
            paint.setTextSize(40);
            canvas.drawText("World is null", 50, 100, paint);
            return;
        }

        // 正常绘制游戏内容
        drawMap(canvas);
        drawEntities(canvas, world);
        drawHUD(canvas);

        // 调试信息：显示实体数量
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        int entityCount = world.getAllEntities().size();
        canvas.drawText("实体数量: " + entityCount, 10, getHeight() - 20, paint);
    }

    private void drawMap(Canvas canvas) {
        // 绘制路径
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(10f);
        for (int i = 0; i < pathPoints.length - 1; i++) {
            canvas.drawLine(
                    pathPoints[i][0], pathPoints[i][1],
                    pathPoints[i + 1][0], pathPoints[i + 1][1],
                    paint
            );
        }

        // 绘制路径点
        paint.setColor(Color.WHITE);
        for (float[] point : pathPoints) {
            canvas.drawCircle(point[0], point[1], 5f, paint);
        }
    }

    private void drawEntities(Canvas canvas, World world) {
        List<Entity> entities = world.getAllEntities();

        for (Entity entity : entities) {
            Transform transform = entity.getComponent(Transform.class);
            if (transform == null) continue;

            // 绘制敌人
            if (entity.hasComponent(Enemy.class)) {
                drawEnemy(canvas, entity, transform);
            }
            // 绘制塔
            else if (entity.hasComponent(Tower.class)) {
                drawTower(canvas, entity, transform);
            }
            // 绘制弹道
            else if (entity.hasComponent(Projectile.class)) {
                drawProjectile(canvas, entity, transform);
            }
        }
    }

    private void drawEnemy(Canvas canvas, Entity enemy, Transform transform) {
        Enemy enemyComp = enemy.getComponent(Enemy.class);
        Health health = enemy.getComponent(Health.class);

        if (enemyComp == null) return;

        // 设置颜色基于敌人类型
        switch (enemyComp.type) { // 使用字段而不是getter，因为Enemy类中字段是public
            case GOBLIN:
                paint.setColor(Color.GREEN);
                break;
            case ORC:
                paint.setColor(Color.RED);
                break;
            case TROLL:
                paint.setColor(Color.BLUE);
                break;
        }

        // 绘制敌人身体
        canvas.drawCircle(transform.x, transform.y, 20f, paint); // 使用字段而不是getter

        // 绘制血条
        if (health != null) {
            float healthRatio = (float) health.current / health.max; // 使用字段而不是getter

            // 血条背景
            paint.setColor(Color.DKGRAY);
            float barWidth = 40f;
            float barHeight = 5f;
            float barX = transform.x - barWidth / 2; // 使用字段而不是getter
            float barY = transform.y - 30f; // 使用字段而不是getter
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint);

            // 当前血量
            paint.setColor(Color.GREEN);
            canvas.drawRect(barX, barY, barX + barWidth * healthRatio, barY + barHeight, paint);
        }
    }

    private void drawTower(Canvas canvas, Entity tower, Transform transform) {
        Tower towerComp = tower.getComponent(Tower.class);

        if (towerComp == null) return;

        // 设置颜色基于塔类型
        switch (towerComp.type) { // 使用字段而不是getter
            case ARCHER:
                paint.setColor(Color.YELLOW);
                break;
            case CANNON:
                paint.setColor(Color.DKGRAY);
                break;
            case MAGE:
                paint.setColor(Color.MAGENTA);
                break;
        }

        // 绘制塔身
        float size = 25f;
        canvas.drawRect(
                transform.x - size, transform.y - size, // 使用字段而不是getter
                transform.x + size, transform.y + size, // 使用字段而不是getter
                paint
        );

        // 绘制攻击范围（调试用）
        paint.setColor(Color.argb(30, 255, 255, 255));
        canvas.drawCircle(transform.x, transform.y, towerComp.range, paint); // 使用字段而不是getter
    }

    private void drawProjectile(Canvas canvas, Entity projectile, Transform transform) {
        paint.setColor(Color.WHITE);
        canvas.drawCircle(transform.x, transform.y, 5f, paint); // 使用字段而不是getter
    }

    private void drawHUD(Canvas canvas) {
        paint.setColor(Color.WHITE);

        // 根据屏幕尺寸动态调整文字大小
        float textSize = Math.min(getWidth(), getHeight()) / 30f;
        paint.setTextSize(textSize);

        // 绘制选中的塔类型指示器
        String towerText = "选中: " + selectedTowerType.name();
        canvas.drawText(towerText, 10, textSize + 5, paint);

        // 绘制简单的游戏说明
        paint.setTextSize(textSize * 0.8f);
        canvas.drawText("点击放置塔", 10, textSize * 2 + 5, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameEngine != null) {
            float x = event.getX();
            float y = event.getY();

            // 放置塔
            gameEngine.placeTower(x, y, selectedTowerType);
            invalidate();

            return true;
        }
        return super.onTouchEvent(event);
    }
}