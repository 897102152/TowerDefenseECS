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
import com.example.towerdefense.components.Path;

import java.util.List;

public class GameView extends View {
    private GameEngine gameEngine;
    private Tower.Type selectedTowerType = Tower.Type.ARCHER;
    private Paint paint;

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
        canvas.drawColor(Color.DKGRAY);

        if (gameEngine == null) {
            paint.setColor(Color.RED);
            paint.setTextSize(40);
            canvas.drawText("GameEngine is null", 50, 100, paint);
            return;
        }

        World world = gameEngine.getWorld();
        if (world == null) {
            paint.setColor(Color.RED);
            paint.setTextSize(40);
            canvas.drawText("World is null", 50, 100, paint);
            return;
        }

        // 修复方法调用
        drawMap(canvas, world);
        drawAllEntities(canvas, world);
        drawUI(canvas);

        // 调试信息
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        int entityCount = world.getAllEntities().size();
        canvas.drawText("实体数量: " + entityCount, 10, getHeight() - 20, paint);
    }

    /**
     * 绘制游戏地图和路径
     */
    private void drawMap(Canvas canvas, World world) {
        // 从世界中获取所有Path实体
        List<Entity> pathEntities = world.getEntitiesWithComponent(Path.class);

        for (Entity pathEntity : pathEntities) {
            Path path = pathEntity.getComponent(Path.class);

            if (path != null && path.isVisible()) {
                drawSinglePath(canvas, path);
            }
        }
    }

    /**
     * 绘制单条路径
     */
    private void drawSinglePath(Canvas canvas, Path path) {
        // 将百分比坐标转换为实际屏幕坐标
        float[][] screenPoints = path.convertToScreenCoordinates(getWidth(), getHeight());

        if (screenPoints.length < 2) return;

        // 设置路径样式
        paint.setColor(path.getPathColor());
        paint.setStrokeWidth(path.getPathWidth());

        // 绘制路径线条
        for (int i = 0; i < screenPoints.length - 1; i++) {
            canvas.drawLine(
                    screenPoints[i][0], screenPoints[i][1],
                    screenPoints[i + 1][0], screenPoints[i + 1][1],
                    paint
            );
        }

        // 绘制路径点标记
        paint.setColor(Color.WHITE);
        for (float[] point : screenPoints) {
            canvas.drawCircle(point[0], point[1], 5f, paint);
        }

        // 绘制路径标签
        if (screenPoints.length > 0) {
            paint.setTextSize(15);
            canvas.drawText(
                    path.getTag().toString(),
                    screenPoints[0][0] + 10,
                    screenPoints[0][1] - 10,
                    paint
            );
        }
    }

    /**
     * 绘制所有游戏实体
     */
    private void drawAllEntities(Canvas canvas, World world) {
        List<Entity> entities = world.getAllEntities();

        for (Entity entity : entities) {
            Transform transform = entity.getComponent(Transform.class);
            if (transform == null) continue;

            if (entity.hasComponent(Enemy.class)) {
                drawEnemy(canvas, entity, transform);
            }
            else if (entity.hasComponent(Tower.class)) {
                drawTower(canvas, entity, transform);
            }
            else if (entity.hasComponent(Projectile.class)) {
                drawProjectile(canvas, entity, transform);
            }
        }
    }

    // drawEnemy, drawTower, drawProjectile 方法保持不变
    private void drawEnemy(Canvas canvas, Entity enemy, Transform transform) {
        Enemy enemyComp = enemy.getComponent(Enemy.class);
        Health health = enemy.getComponent(Health.class);

        if (enemyComp == null) return;

        switch (enemyComp.type) {
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

        canvas.drawCircle(transform.x, transform.y, 20f, paint);

        if (health != null) {
            float healthRatio = (float) health.current / health.max;

            paint.setColor(Color.DKGRAY);
            float barWidth = 40f;
            float barHeight = 5f;
            float barX = transform.x - barWidth / 2;
            float barY = transform.y - 30f;
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint);

            paint.setColor(Color.GREEN);
            canvas.drawRect(barX, barY, barX + barWidth * healthRatio, barY + barHeight, paint);
        }
    }

    private void drawTower(Canvas canvas, Entity tower, Transform transform) {
        Tower towerComp = tower.getComponent(Tower.class);
        if (towerComp == null) return;

        switch (towerComp.type) {
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

        float size = 25f;
        canvas.drawRect(
                transform.x - size, transform.y - size,
                transform.x + size, transform.y + size,
                paint
        );

        paint.setColor(Color.argb(30, 255, 255, 255));
        canvas.drawCircle(transform.x, transform.y, towerComp.range, paint);
    }

    private void drawProjectile(Canvas canvas, Entity projectile, Transform transform) {
        paint.setColor(Color.WHITE);
        canvas.drawCircle(transform.x, transform.y, 5f, paint);
    }

    /**
     * 绘制用户界面
     */
    private void drawUI(Canvas canvas) {
        paint.setColor(Color.WHITE);
        float textSize = Math.min(getWidth(), getHeight()) / 30f;
        paint.setTextSize(textSize);

        String towerText = "选中: " + selectedTowerType.name();
        canvas.drawText(towerText, 10, textSize + 5, paint);

        paint.setTextSize(textSize * 0.8f);
        canvas.drawText("点击放置塔", 10, textSize * 2 + 5, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameEngine != null) {
            float x = event.getX();
            float y = event.getY();
            gameEngine.placeTower(x, y, selectedTowerType);
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}