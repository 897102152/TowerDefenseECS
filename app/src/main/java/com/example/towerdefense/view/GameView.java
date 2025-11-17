package com.example.towerdefense.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.example.towerdefense.GameEngine;
import com.example.towerdefense.R;
import com.example.towerdefense.components.Enemy;
import com.example.towerdefense.components.Health;
import com.example.towerdefense.components.Path;
import com.example.towerdefense.components.Projectile;
import com.example.towerdefense.components.Tower;
import com.example.towerdefense.components.Transform;
import com.example.towerdefense.ecs.Entity;
import com.example.towerdefense.ecs.World;

import java.util.List;

public class GameView extends View {
    // ========== 核心游戏组件 ==========
    private GameEngine gameEngine;

    // ========== 网格系统相关属性 ==========
    private boolean showGrid = true;
    private float gridSizePercentage = 0.08f;
    private int gridSize;
    private boolean isBuildMode = true;

    // ========== 塔选择和移除模式 ==========
    private Tower.Type selectedTowerType = Tower.Type.ARCHER;
    private boolean isRemoveMode = false;

    // ========== 绘制工具 ==========
    private Paint paint;
    private Paint gridPaint;
    private Paint highlightPaint;
    private Paint removeModePaint;

    // ========== 路径检测和高亮相关字段 ==========
    private GridPosition highlightedGrid = null;
    private boolean showHighlight = false;
    private Handler handler;

    // ========== 消息监听器 ==========
    public interface GameViewListener {
        void showGameMessage(String title, String message, String hint, boolean autoHide);
    }
    private GameViewListener gameViewListener;

    // ========== 矢量图资源 ==========
    private Drawable goblinDrawable;
    private Drawable orcDrawable;
    private Drawable trollDrawable;
    private Drawable archerTowerDrawable;
    private Drawable cannonTowerDrawable;
    private Drawable mageTowerDrawable;

    // ========== 图标尺寸控制 ==========
    private int enemyIconSize = 60;
    private int towerIconSize;
    // ========== 背景相关属性 ==========
    private Drawable backgroundDrawable;
    private boolean showBackground = false;
    private int currentLevelId = -1;
    // =====================================================================
    // 构造函数和初始化
    // =====================================================================

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

        // 初始化画笔
        gridPaint = new Paint();
        gridPaint.setColor(Color.argb(80, 255, 255, 255));
        gridPaint.setStrokeWidth(1f);

        highlightPaint = new Paint();
        highlightPaint.setColor(Color.argb(150, 255, 0, 0));
        highlightPaint.setStyle(Paint.Style.FILL);

        removeModePaint = new Paint();
        removeModePaint.setColor(Color.argb(150, 255, 165, 0));
        removeModePaint.setStyle(Paint.Style.FILL);

        // 加载矢量图资源
        loadVectorDrawables();
        loadTowerVectorDrawables();
    }

    // =====================================================================
    // 视图生命周期和尺寸处理
    // =====================================================================

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 计算网格大小
        calculateGridSize();

        // 设置防御塔图标大小等于网格大小
        towerIconSize = gridSize;
        System.out.println("GameView: 防御塔图标大小设置为网格大小: " + towerIconSize + "px");

        // 根据屏幕尺寸调整敌人图标大小
        enemyIconSize = Math.min(w, h) / 15;
        enemyIconSize = Math.max(40, Math.min(enemyIconSize, 80));

        // 重新设置Drawable边界
        updateDrawableBounds();

        // 更新背景图尺寸
        if (showBackground && backgroundDrawable != null) {
            backgroundDrawable.setBounds(0, 0, w, h);
            System.out.println("GameView: 背景图尺寸更新为: " + w + "x" + h);
        }
        // 加载防御塔矢量图
        loadTowerVectorDrawables();

        System.out.println("GameView: 屏幕尺寸变化 " + w + "x" + h);
        System.out.println("GameView: 网格大小 " + gridSize + "px");
        System.out.println("GameView: 敌人图标大小 " + enemyIconSize + "px");
        System.out.println("GameView: 防御塔图标大小 " + towerIconSize + "px");

        if (gameEngine != null) {
            gameEngine.setScreenSize(w, h);
            System.out.println("GameView: 屏幕尺寸已传递给GameEngine");
        } else {
            System.err.println("GameView: gameEngine为null，无法设置屏幕尺寸");
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.GRAY);
        // 先绘制背景
        if (showBackground && backgroundDrawable != null) {
            backgroundDrawable.draw(canvas);
        } else {
            // 如果没有背景，使用默认背景色
            canvas.drawColor(Color.DKGRAY);
        }
        if (gameEngine == null) {
            drawErrorText(canvas, "GameEngine is null");
            return;
        }

        World world = gameEngine.getWorld();
        if (world == null) {
            drawErrorText(canvas, "World is null");
            return;
        }

        // 绘制游戏元素
        if (showGrid && isBuildMode) {
            drawGrid(canvas);
        }

        drawMap(canvas, world);
        drawAllEntities(canvas, world);
        drawUI(canvas);

        // 调试信息
        drawDebugInfo(canvas, world);
    }

    // =====================================================================
    // 矢量图资源管理
    // =====================================================================

    /**
     * 加载敌人矢量图资源
     */
    private void loadVectorDrawables() {
        System.out.println("GameView: 开始加载敌人矢量图");

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                goblinDrawable = getContext().getDrawable(R.drawable.enemy_reconnaissance);
                orcDrawable = getContext().getDrawable(R.drawable.enemy_infantry);
                trollDrawable = getContext().getDrawable(R.drawable.enemy_armour);
            } else {
                goblinDrawable = VectorDrawableCompat.create(getResources(), R.drawable.enemy_reconnaissance, getContext().getTheme());
                orcDrawable = VectorDrawableCompat.create(getResources(), R.drawable.enemy_infantry, getContext().getTheme());
                trollDrawable = VectorDrawableCompat.create(getResources(), R.drawable.enemy_armour, getContext().getTheme());
            }

            System.out.println("GameView: 敌人矢量图加载完成");
        } catch (Exception e) {
            System.err.println("GameView: 加载敌人矢量图失败: " + e.getMessage());
        }
    }

    /**
     * 加载防御塔矢量图资源
     */
    private void loadTowerVectorDrawables() {
        System.out.println("GameView: 开始加载防御塔矢量图");

        try {
            archerTowerDrawable = getContext().getDrawable(R.drawable.tower_infantry);
            cannonTowerDrawable = getContext().getDrawable(R.drawable.tower_anti_tank);
            mageTowerDrawable = getContext().getDrawable(R.drawable.tower_artillery);

            System.out.println("GameView: 防御塔矢量图加载完成");
        } catch (Exception e) {
            System.err.println("GameView: 加载防御塔矢量图失败: " + e.getMessage());
        }
    }

    /**
     * 更新Drawable边界
     */
    private void updateDrawableBounds() {
        if (goblinDrawable != null) goblinDrawable.setBounds(0, 0, enemyIconSize, enemyIconSize);
        if (orcDrawable != null) orcDrawable.setBounds(0, 0, enemyIconSize, enemyIconSize);
        if (trollDrawable != null) trollDrawable.setBounds(0, 0, enemyIconSize, enemyIconSize);
    }

    // =====================================================================
    // 网格系统相关方法
    // =====================================================================

    /**
     * 根据屏幕尺寸计算网格大小
     */
    private void calculateGridSize() {
        int screenWidth = getWidth();
        int screenHeight = getHeight();

        if (screenWidth > 0 && screenHeight > 0) {
            gridSize = (int) (screenWidth * gridSizePercentage);
            gridSize = Math.max(30, Math.min(gridSize, 100));
            System.out.println("GameView: 计算网格大小: " + gridSize + "px");
        } else {
            gridSize = 60;
            System.out.println("GameView: 使用默认网格大小: " + gridSize + "px");
        }
    }

    /**
     * 绘制网格
     */
    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        // 绘制网格线
        for (int x = 0; x <= width; x += gridSize) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
        for (int y = 0; y <= height; y += gridSize) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        // 绘制高亮网格
        if (showHighlight && highlightedGrid != null) {
            int gridX = highlightedGrid.x * gridSize;
            int gridY = highlightedGrid.y * gridSize;

            canvas.drawRect(gridX, gridY, gridX + gridSize, gridY + gridSize,
                    isRemoveMode ? removeModePaint : highlightPaint);
        }
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

    // =====================================================================
    // 触摸事件处理
    // =====================================================================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameEngine != null) {
            float x = event.getX();
            float y = event.getY();

            if (!isBuildMode) {
                System.out.println("GameView: 建造模式未开启，无法进行操作");
                showBuildModeRequiredMessage();
                performClick();
                return true;
            }

            if (isRemoveMode) {
                System.out.println("GameView: 移除模式下点击位置 (" + x + ", " + y + ")");
                gameEngine.removeTower(x, y);
            } else if (selectedTowerType != null) {
                GridPosition gridPos = convertToGridPosition(x, y);
                ScreenPosition screenPos = convertToScreenPosition(gridPos.x, gridPos.y);

                // 使用统一的防御塔放置方法
                boolean placed = gameEngine.placeTowerWithValidation(screenPos.x, screenPos.y, selectedTowerType);

                if (placed) {
                    System.out.println("放置塔在网格位置: (" + gridPos.x + ", " + gridPos.y + ")");
                    // 放置成功后显示成功消息
                    //showTowerPlacedMessage(selectedTowerType);
                } else {
                    System.out.println("GameView: 无法在指定位置放置防御塔");
                    highlightGrid(gridPos);
                    // 不再调用showPathRestrictionMessage，因为GameEngine已经处理了错误消息
                }
            } else {
                System.out.println("GameView: 建造模式下未选择塔类型");
                showNoTowerSelectedMessage();
            }

            invalidate();
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * 高亮显示指定的网格
     */
    private void highlightGrid(GridPosition gridPos) {
        this.highlightedGrid = gridPos;
        this.showHighlight = true;
        invalidate();

        new Handler().postDelayed(() -> {
            showHighlight = false;
            invalidate();
        }, 1000);
    }

    // =====================================================================
    // 绘制方法
    // =====================================================================

    /**
     * 绘制游戏地图和路径
     */
    private void drawMap(Canvas canvas, World world) {
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
        float[][] screenPoints = path.convertToScreenCoordinates(getWidth(), getHeight());
        if (screenPoints.length < 2) return;

        paint.setColor(path.getPathColor());
        paint.setStrokeWidth(path.getPathWidth());

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
        canvas.drawText(path.getTag().toString(), screenPoints[0][0] + 10, screenPoints[0][1] - 10, paint);
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
            } else if (entity.hasComponent(Tower.class)) {
                drawTower(canvas, entity, transform);
            } else if (entity.hasComponent(Projectile.class)) {
                drawProjectile(canvas, entity, transform);
            }
        }
    }

    /**
     * 绘制敌人 - 使用矢量图
     */
    private void drawEnemy(Canvas canvas, Entity enemy, Transform transform) {
        Enemy enemyComp = enemy.getComponent(Enemy.class);
        Health health = enemy.getComponent(Health.class);

        if (enemyComp == null) return;

        Drawable enemyDrawable = getEnemyDrawable(enemyComp.type);

        if (enemyDrawable != null) {
            canvas.save();
            canvas.translate(transform.x - enemyIconSize / 2f, transform.y - enemyIconSize / 2f);
            enemyDrawable.draw(canvas);
            canvas.restore();
        } else {
            System.err.println("GameView: 敌人矢量图为null，类型: " + enemyComp.type);
        }

        // 绘制血条
        if (health != null) {
            drawHealthBar(canvas, transform, health, enemyIconSize / 2f + 10f);
        }
    }

    /**
     * 绘制防御塔 - 使用矢量图，保持原始比例
     */
    private void drawTower(Canvas canvas, Entity tower, Transform transform) {
        Tower towerComp = tower.getComponent(Tower.class);
        if (towerComp == null) return;
        try {
            Drawable towerDrawable = getTowerDrawable(towerComp.type);
            if (towerDrawable != null) {
                // 获取矢量图固有尺寸和宽高比
                float aspectRatio = getDrawableAspectRatio(towerDrawable);

                // 计算绘制尺寸：宽度等于网格大小，高度按比例缩放
                int drawWidth = towerIconSize;
                int drawHeight = (int) (towerIconSize / aspectRatio);

                // 计算绘制位置（使防御塔位于网格中心）
                int left = (int) (transform.x - drawWidth / 2f);
                int top = (int) (transform.y - drawHeight / 2f);

                // 设置Drawable边界并绘制
                towerDrawable.setBounds(left, top, left + drawWidth, top + drawHeight);
                towerDrawable.draw(canvas);
            } else {
                drawFallbackTower(canvas, transform, towerComp.type);
            }
        } catch(Exception e){
            System.err.println("GameView: 绘制防御塔时发生异常: " + e.getMessage());
            // 使用备用绘制方案
            drawFallbackTower(canvas, transform, towerComp.type);
        }

        // 绘制攻击范围
        if (towerComp != null) {
            if (towerComp.type == Tower.Type.MAGE) {
                // 法师塔：只绘制圆环区域（外圈减去内圈）
                paint.setColor(Color.argb(50, 255, 255, 255)); // 蓝色圆环

                // 使用Path绘制圆环
                android.graphics.Path path = new android.graphics.Path();
                path.setFillType(android.graphics.Path.FillType.EVEN_ODD);

                // 外圈圆形
                path.addCircle(transform.x, transform.y, towerComp.range, android.graphics.Path.Direction.CW);
                // 内圈圆形（会被从外圈中减去）
                path.addCircle(transform.x, transform.y, towerComp.innerRange, android.graphics.Path.Direction.CCW);

                canvas.drawPath(path, paint);

            } else {
                // 其他塔：绘制圆形范围
                paint.setColor(Color.argb(50, 255, 255, 255));
                canvas.drawCircle(transform.x, transform.y, towerComp.range, paint);
            }
        }
    }

    // 添加备用绘制方法
    private void drawFallbackTower(Canvas canvas, Transform transform, Tower.Type type) {
        // 简单的几何图形绘制
        paint.setStyle(Paint.Style.FILL);
        switch (type) {
            case ARCHER: paint.setColor(Color.GREEN); break;
            case CANNON: paint.setColor(Color.RED); break;
            case MAGE: paint.setColor(Color.BLUE); break;
            default: paint.setColor(Color.GRAY); break;
        }
        canvas.drawCircle(transform.x, transform.y, towerIconSize / 3f, paint);
    }

    /**
     * 绘制抛射体
     */
    private void drawProjectile(Canvas canvas, Entity projectile, Transform transform) {
        Projectile projectileComp = projectile.getComponent(Projectile.class);

        if (projectileComp != null) {
            // 根据速度设置不同颜色（可选，便于区分）
            if (projectileComp.speed > 220f) { // 弓箭塔速度 > 200*1.1
                paint.setColor(Color.GREEN); // 绿色 - 弓箭塔
            } else if (projectileComp.speed < 100f) { // 炮塔速度 < 200*0.55
                paint.setColor(Color.RED); // 红色 - 炮塔
            } else {
                paint.setColor(Color.BLUE); // 蓝色 - 法师塔
            }
        } else {
            paint.setColor(Color.WHITE); // 默认白色
        }

        canvas.drawCircle(transform.x, transform.y, 5f, paint);
    }

    /**
     * 绘制用户界面
     */
    private void drawUI(Canvas canvas) {
        paint.setColor(Color.WHITE);
        float textSize = Math.min(getWidth(), getHeight()) / 30f;
        paint.setTextSize(textSize);

        // 显示当前模式状态
        String modeText = getModeText();
        paint.setColor(getModeColor());
        canvas.drawText(modeText, 10, textSize + 5, paint);

        // 绘制敌人计数器
        drawEnemyCounter(canvas, textSize);

        // 绘制建造模式状态
        drawBuildModeStatus(canvas, textSize);
    }

    /**
     * 绘制调试信息
     */
    private void drawDebugInfo(Canvas canvas, World world) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        int entityCount = world.getAllEntities().size();
        canvas.drawText("实体数量: " + entityCount, 10, getHeight() - 20, paint);
        canvas.drawText("网格: " + gridSize + "px", 10, getHeight() - 50, paint);
    }

    // =====================================================================
    // 辅助绘制方法
    // =====================================================================

    /**
     * 绘制错误文本
     */
    private void drawErrorText(Canvas canvas, String text) {
        paint.setColor(Color.RED);
        paint.setTextSize(40);
        canvas.drawText(text, 50, 100, paint);
    }

    /**
     * 绘制血条
     */
    private void drawHealthBar(Canvas canvas, Transform transform, Health health, float yOffset) {
        float healthRatio = (float) health.current / health.max;

        paint.setColor(Color.DKGRAY);
        float barWidth = 40f;
        float barHeight = 5f;
        float barX = transform.x - barWidth / 2;
        float barY = transform.y - yOffset;

        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint);

        paint.setColor(Color.GREEN);
        canvas.drawRect(barX, barY, barX + barWidth * healthRatio, barY + barHeight, paint);
    }

    /**
     * 绘制敌人计数器
     */
    private void drawEnemyCounter(Canvas canvas, float textSize) {
        if (gameEngine != null) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(textSize * 0.8f);
            String enemyCounter = "敌人通过: " + gameEngine.getEnemiesReachedEnd() + "/" + gameEngine.getMaxEnemiesAllowed();

            // 根据数量改变颜色
            if (gameEngine.getEnemiesReachedEnd() >= gameEngine.getMaxEnemiesAllowed() * 0.8) {
                paint.setColor(Color.RED);
            } else if (gameEngine.getEnemiesReachedEnd() >= gameEngine.getMaxEnemiesAllowed() * 0.5) {
                paint.setColor(Color.YELLOW);
            }

            float textWidth = paint.measureText(enemyCounter);
            canvas.drawText(enemyCounter, getWidth() - textWidth - 10, textSize + 5, paint);
        }
    }

    /**
     * 绘制建造模式状态
     */
    private void drawBuildModeStatus(Canvas canvas, float textSize) {
        String buildModeText = "建造模式: " + (isBuildMode ? "开启" : "关闭");
        paint.setColor(isBuildMode ? Color.GREEN : Color.RED);
        canvas.drawText(buildModeText, 10, textSize * 2 + 5, paint);

        if (!isBuildMode) {
            paint.setColor(Color.YELLOW);
            paint.setTextSize(textSize * 0.6f);
            canvas.drawText("点击建造模式按钮来放置防御塔", 10, textSize * 3 + 5, paint);
        }
    }

    // =====================================================================
    // 工具方法
    // =====================================================================

    /**
     * 获取敌人Drawable
     */
    private Drawable getEnemyDrawable(Enemy.Type type) {
        switch (type) {
            case GOBLIN: return goblinDrawable;
            case ORC: return orcDrawable;
            case TROLL: return trollDrawable;
            default: return null;
        }
    }

    /**
     * 获取防御塔Drawable
     */
    private Drawable getTowerDrawable(Tower.Type type) {
        switch (type) {
            case ARCHER: return archerTowerDrawable;
            case CANNON: return cannonTowerDrawable;
            case MAGE: return mageTowerDrawable;
            default: return null;
        }
    }

    /**
     * 获取Drawable的宽高比（宽度/高度）
     */
    private float getDrawableAspectRatio(Drawable drawable) {
        if (drawable == null) return 1.0f;

        try {
            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();

            if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
                return 1.0f;
            }

            return (float) intrinsicWidth / intrinsicHeight;
        } catch (Exception e) {
            System.err.println("GameView: 获取Drawable宽高比时发生异常: " + e.getMessage());
            return 1.0f;
        }
    }

    /**
     * 获取模式文本
     */
    private String getModeText() {
        if (isRemoveMode) {
            return "移除模式：点击防御塔移除";
        } else if (selectedTowerType != null) {
            return "建造模式 - 选中: " + selectedTowerType.name();
        } else {
            return "建造模式 - 请选择塔类型";
        }
    }

    /**
     * 获取模式颜色
     */
    private int getModeColor() {
        if (isRemoveMode) {
            return Color.RED;
        } else if (selectedTowerType != null) {
            return Color.GREEN;
        } else {
            return Color.YELLOW;
        }
    }

    // =====================================================================
    // 消息显示方法
    // =====================================================================

    private void showBuildModeRequiredMessage() {
        System.out.println("GameView: 请先开启建造模式来放置防御塔");
        if (gameViewListener != null) {
            gameViewListener.showGameMessage("建造提示", "请先开启建造模式", "点击右下角建造按钮开启建造模式", true);
        }
    }

    private void showNoTowerSelectedMessage() {
        if (gameViewListener != null) {
            gameViewListener.showGameMessage("建造提示", "请先选择要建造的防御塔类型", "点击建造菜单中的塔图标", true);
        }
    }

    /**
     * 显示防御塔放置成功消息
     */
    private void showTowerPlacedMessage(Tower.Type towerType) {
        if (gameViewListener != null) {
            String towerName = "";
            switch (towerType) {
                case ARCHER: towerName = "弓箭塔"; break;
                case CANNON: towerName = "炮塔"; break;
                case MAGE: towerName = "法师塔"; break;
            }
            gameViewListener.showGameMessage("建造成功", towerName + "已放置", "继续建造或退出建造模式", true);
        }
    }

    // =====================================================================
    // 公共方法
    // =====================================================================

    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    /**
     * 设置选中塔类型
     */
    public void setSelectedTowerType(Tower.Type towerType) {
        this.selectedTowerType = towerType;
        this.isRemoveMode = false;

        // 通知Activity更新按钮高亮状态
        if (gameViewListener != null) {
            // 这里可以通过回调通知Activity哪个按钮应该被高亮
            // 但由于我们已经在Activity中处理了按钮点击，所以这里不需要额外处理
        }
        invalidate();
    }

    /**
     * 设置移除模式
     */
    public void setRemoveMode(boolean removeMode) {
        this.isRemoveMode = removeMode;
        if (removeMode) {
            this.selectedTowerType = null;
        }
        invalidate();
    }

    public boolean isRemoveMode() {
        return isRemoveMode;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        invalidate();
    }

    public void setGridSizePercentage(float percentage) {
        this.gridSizePercentage = Math.max(0.05f, Math.min(percentage, 0.2f));
        calculateGridSize();
        invalidate();
    }

    public void setBuildMode(boolean buildMode) {
        this.isBuildMode = buildMode;
        if (!buildMode) {
            this.isRemoveMode = false;
        }
        invalidate();
    }

    public void setGameViewListener(GameViewListener listener) {
        this.gameViewListener = listener;
    }

    /**
     * 设置当前关卡ID并加载对应的背景
     */
    public void setLevelId(int levelId) {
        this.currentLevelId = levelId;
        loadBackgroundDrawable(levelId);
    }

    /**
     * 加载背景矢量图
     */
    private void loadBackgroundDrawable(int levelId) {
        System.out.println("GameView: 开始加载关卡 " + levelId + " 的背景图");

        showBackground = true;
        try {
            int drawableId;
            switch (levelId) {
                case 0: // 教学关
                    drawableId = R.drawable.map0;
                    break;
                case 1: // 第一关
                    drawableId = R.drawable.map1;
                    break;
                default:
                    drawableId = R.drawable.map0; // 默认
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                backgroundDrawable = getContext().getDrawable(drawableId);
            } else {
                backgroundDrawable = VectorDrawableCompat.create(getResources(), drawableId, getContext().getTheme());
            }

            if (backgroundDrawable != null) {
                // 设置背景图边界为整个视图大小
                backgroundDrawable.setBounds(0, 0, getWidth(), getHeight());
                System.out.println("GameView: 背景图加载成功，尺寸: " + getWidth() + "x" + getHeight());
            } else {
                System.err.println("GameView: 背景图加载失败，drawable为null");
                showBackground = false;
            }
        } catch (Exception e) {
            System.err.println("GameView: 加载背景图失败: " + e.getMessage());
            showBackground = false;
        }

        invalidate();
    }

    // =====================================================================
    // 内部类
    // =====================================================================

    private static class GridPosition {
        final int x;
        final int y;

        GridPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class ScreenPosition {
        final float x;
        final float y;

        ScreenPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}