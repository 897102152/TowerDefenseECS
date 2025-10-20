package com.example.towerdefense.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

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
    // 网格系统相关属性
    private boolean showGrid = true; // 是否显示网格
    private float gridSizePercentage = 0.08f; // 网格大小为屏幕宽度的8%
    private int gridSize; // 实际网格大小（像素），根据屏幕尺寸计算
    private Paint gridPaint;
    private boolean isBuildMode = true; // 是否处于建造模式

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
        // 初始化网格画笔
        gridPaint = new Paint();
        gridPaint.setColor(Color.argb(80, 255, 255, 255)); // 半透明白色
        gridPaint.setStrokeWidth(1f);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        System.out.println("GameView: onSizeChanged - " + w + "x" + h);
        if (gameEngine != null) {
            gameEngine.setScreenSize(w, h);
            System.out.println("GameView: 屏幕尺寸已传递给GameEngine");
        } else {
            System.err.println("GameView: 错误！gameEngine为null，无法设置屏幕尺寸");
        }
        // 当视图尺寸变化时，重新计算网格大小
        calculateGridSize();

        System.out.println("GameView: 屏幕尺寸变化 " + w + "x" + h);
        System.out.println("GameView: 网格大小 " + gridSize + "px");
    }
    /**
     * 根据屏幕尺寸计算网格大小
     */
    private void calculateGridSize() {
        int screenWidth = getWidth();
        int screenHeight = getHeight();

        if (screenWidth > 0 && screenHeight > 0) {
            // 使用屏幕宽度的百分比作为网格大小
            gridSize = (int) (screenWidth * gridSizePercentage);

            // 确保网格大小在合理范围内
            gridSize = Math.max(30, Math.min(gridSize, 100));
        } else {
            // 默认网格大小
            gridSize = 60;
        }
    }
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    public void setSelectedTowerType(Tower.Type towerType) {
        this.selectedTowerType = towerType;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
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
        // 绘制网格（在建造模式下）
        if (showGrid && isBuildMode) {
            drawGrid(canvas);
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
        // 添加网格信息显示
        canvas.drawText("网格: " + gridSize + "px", 10, getHeight() - 50, paint);
    }
    /**
     * 绘制网格
     */
    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        // 绘制垂直线
        for (int x = 0; x <= width; x += gridSize) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }

        // 绘制水平线
        for (int y = 0; y <= height; y += gridSize) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        // 绘制网格交叉点（可选）
        gridPaint.setColor(Color.argb(120, 255, 255, 255));
        for (int x = gridSize; x < width; x += gridSize) {
            for (int y = gridSize; y < height; y += gridSize) {
                canvas.drawCircle(x, y, 2f, gridPaint);
            }
        }
        gridPaint.setColor(Color.argb(80, 255, 255, 255)); // 恢复颜色
    }
    /**
     * 将屏幕坐标转换为网格坐标
     */
    private GridPosition convertToGridPosition(float screenX, float screenY) {
        int gridX = (int) (screenX / gridSize);
        int gridY = (int) (screenY / gridSize);
        return new GridPosition(gridX, gridY);
    }

    /**
     * 将网格坐标转换为屏幕坐标（网格中心点）
     */
    private ScreenPosition convertToScreenPosition(int gridX, int gridY) {
        float screenX = gridX * gridSize + gridSize / 2f;
        float screenY = gridY * gridSize + gridSize / 2f;
        return new ScreenPosition(screenX, screenY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameEngine != null) {
            float x = event.getX();
            float y = event.getY();

            // 添加建造模式检查
            if (!isBuildMode) {
                System.out.println("GameView: 建造模式未开启，无法放置防御塔");
                // 可以在这里添加提示，比如显示Toast或者改变UI状态
                showBuildModeRequiredMessage();
                performClick();
                return true;
            }

            if (isBuildMode) {
                // 建造模式：将点击位置吸附到网格
                GridPosition gridPos = convertToGridPosition(x, y);
                ScreenPosition screenPos = convertToScreenPosition(gridPos.x, gridPos.y);

                gameEngine.placeTower(screenPos.x, screenPos.y, selectedTowerType);
                System.out.println("放置塔在网格位置: (" + gridPos.x + ", " + gridPos.y + ")");
            } else {
                // 非建造模式：直接使用原始坐标
                gameEngine.placeTower(x, y, selectedTowerType);
            }

            invalidate();
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }
    /**
     * 显示需要建造模式的提示信息
     */
    private void showBuildModeRequiredMessage() {
        // 这里可以添加UI提示，比如：
        // - 显示一个短暂的Toast消息
        // - 在游戏界面上显示提示文本
        // - 改变UI元素的颜色或状态

        // 示例：如果要在GameView上绘制提示，可以设置一个标志并在onDraw中处理
        // 这里我们先简单地在控制台输出
        System.out.println("GameView: 请先开启建造模式来放置防御塔");

        // 如果需要更明显的用户反馈，可以在这里添加：
        // Toast.makeText(getContext(), "请先开启建造模式", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理可访问性点击事件
     */
    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    // 网格位置辅助类
    private static class GridPosition {
        final int x;
        final int y;

        GridPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // 屏幕位置辅助类
    private static class ScreenPosition {
        final float x;
        final float y;

        ScreenPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    // 网格系统公共方法
    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        invalidate();
    }

    public void setGridSizePercentage(float percentage) {
        this.gridSizePercentage = Math.max(0.05f, Math.min(percentage, 0.2f)); // 限制在5%-20%之间
        calculateGridSize();
        invalidate();
    }

    public void setBuildMode(boolean buildMode) {
        this.isBuildMode = buildMode;
        invalidate();
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
        paint.setTextSize(15);
        canvas.drawText(
                path.getTag().toString(),
                screenPoints[0][0] + 10,
                screenPoints[0][1] - 10,
                paint
        );
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
                paint.setColor(Color.BLUE);
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


        // 添加建造模式状态显示
        String buildModeText = "建造模式: " + (isBuildMode ? "开启" : "关闭");
        paint.setColor(isBuildMode ? Color.GREEN : Color.RED);
        canvas.drawText(buildModeText, 10, textSize * 2 + 5, paint);

        // 如果建造模式关闭，显示提示信息
        if (!isBuildMode) {
            paint.setColor(Color.YELLOW);
            paint.setTextSize(textSize * 0.6f);
            canvas.drawText("点击建造模式按钮来放置防御塔", 10, textSize * 3 + 5, paint);
        }

        paint.setTextSize(textSize * 0.8f);
        canvas.drawText("点击放置塔", 10, textSize * 2 + 5, paint);
    }


}