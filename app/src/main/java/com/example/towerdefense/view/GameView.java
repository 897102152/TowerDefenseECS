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

/**
 * 游戏视图类 - 负责游戏画面的渲染和触摸交互
 * 继承自Android的View类，实现自定义绘制和用户输入处理
 */
public class GameView extends View {
    // 游戏引擎引用，用于获取游戏状态数据
    private GameEngine gameEngine;
    // 当前选中的塔类型，默认为弓箭塔
    private Tower.Type selectedTowerType = Tower.Type.ARCHER;
    // 画笔对象，用于绘制各种图形和文本
    private Paint paint;

    /**
     * 游戏路径点 - 定义敌人的移动路径
     * 每个点包含[x, y]坐标，敌人会按照这个路径顺序移动
     */
    private float[][] pathPoints = {
            {100, 100}, {300, 100}, {300, 300}, {100, 300}, {100, 500}
    };

    /**
     * 构造函数 - 用于代码创建视图
     * @param context Android上下文
     */
    public GameView(Context context) {
        super(context);
        init();
    }

    /**
     * 构造函数 - 用于XML布局文件创建视图
     * @param context Android上下文
     * @param attrs XML属性集
     */
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 构造函数 - 用于XML布局文件创建视图（带默认样式）
     * @param context Android上下文
     * @param attrs XML属性集
     * @param defStyleAttr 默认样式属性
     */
    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化方法 - 设置视图的初始状态和属性
     */
    private void init() {
        // 创建并配置画笔
        paint = new Paint();
        paint.setAntiAlias(true); // 开启抗锯齿，使图形边缘更平滑

        // 设置视图背景颜色为深灰色
        setBackgroundColor(Color.DKGRAY);
    }

    /**
     * 设置游戏引擎引用
     * @param gameEngine 游戏引擎实例
     */
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    /**
     * 设置选中的塔类型
     * @param towerType 塔的类型
     */
    public void setSelectedTowerType(Tower.Type towerType) {
        this.selectedTowerType = towerType;
        // 触发重绘，更新选中塔类型的显示
        invalidate();
    }

    /**
     * 绘制方法 - 视图的核心绘制逻辑
     * 每帧都会被调用，负责渲染整个游戏画面
     * @param canvas 画布对象，用于绘制图形和文本
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制背景颜色
        canvas.drawColor(Color.DKGRAY);

        // 检查游戏引擎是否为空，如果为空显示错误信息
        if (gameEngine == null) {
            paint.setColor(Color.RED);
            paint.setTextSize(40);
            canvas.drawText("GameEngine is null", 50, 100, paint);
            return;
        }

        // 获取游戏世界实例
        World world = gameEngine.getWorld();
        // 检查世界是否为空，如果为空显示错误信息
        if (world == null) {
            paint.setColor(Color.RED);
            paint.setTextSize(40);
            canvas.drawText("World is null", 50, 100, paint);
            return;
        }

        // 正常绘制游戏内容
        drawMap(canvas);                    // 绘制地图和路径
        drawEntities(canvas, world);        // 绘制所有游戏实体
        drawHUD(canvas);                    // 绘制用户界面

        // 调试信息：显示当前实体数量
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        int entityCount = world.getAllEntities().size();
        canvas.drawText("实体数量: " + entityCount, 10, getHeight() - 20, paint);
    }

    /**
     * 绘制游戏地图和路径
     * @param canvas 画布对象
     */
    private void drawMap(Canvas canvas) {
        // 绘制路径线条
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(10f); // 设置路径线条宽度

        // 连接所有路径点形成路径
        for (int i = 0; i < pathPoints.length - 1; i++) {
            canvas.drawLine(
                    pathPoints[i][0], pathPoints[i][1],     // 起点坐标
                    pathPoints[i + 1][0], pathPoints[i + 1][1], // 终点坐标
                    paint
            );
        }

        // 绘制路径点标记
        paint.setColor(Color.WHITE);
        for (float[] point : pathPoints) {
            canvas.drawCircle(point[0], point[1], 5f, paint);
        }
    }

    /**
     * 绘制所有游戏实体
     * @param canvas 画布对象
     * @param world 游戏世界实例
     */
    private void drawEntities(Canvas canvas, World world) {
        // 获取世界中所有实体
        List<Entity> entities = world.getAllEntities();

        // 遍历所有实体并绘制
        for (Entity entity : entities) {
            // 获取实体的位置组件
            Transform transform = entity.getComponent(Transform.class);
            if (transform == null) continue; // 如果没有位置组件则跳过

            // 根据实体类型调用对应的绘制方法
            if (entity.hasComponent(Enemy.class)) {
                // 绘制敌人
                drawEnemy(canvas, entity, transform);
            }
            else if (entity.hasComponent(Tower.class)) {
                // 绘制防御塔
                drawTower(canvas, entity, transform);
            }
            else if (entity.hasComponent(Projectile.class)) {
                // 绘制弹道/子弹
                drawProjectile(canvas, entity, transform);
            }
        }
    }

    /**
     * 绘制敌人实体
     * @param canvas 画布对象
     * @param enemy 敌人实体
     * @param transform 位置组件
     */
    private void drawEnemy(Canvas canvas, Entity enemy, Transform transform) {
        // 获取敌人组件和生命值组件
        Enemy enemyComp = enemy.getComponent(Enemy.class);
        Health health = enemy.getComponent(Health.class);

        if (enemyComp == null) return;

        // 根据敌人类型设置不同颜色
        switch (enemyComp.type) {
            case GOBLIN:    // 哥布林 - 绿色
                paint.setColor(Color.GREEN);
                break;
            case ORC:       // 兽人 - 红色
                paint.setColor(Color.RED);
                break;
            case TROLL:     // 巨魔 - 蓝色
                paint.setColor(Color.BLUE);
                break;
        }

        // 绘制敌人身体（圆形）
        canvas.drawCircle(transform.x, transform.y, 20f, paint);

        // 绘制血条（如果敌人有生命值组件）
        if (health != null) {
            // 计算生命值比例
            float healthRatio = (float) health.current / health.max;

            // 绘制血条背景（灰色）
            paint.setColor(Color.DKGRAY);
            float barWidth = 40f;
            float barHeight = 5f;
            float barX = transform.x - barWidth / 2;
            float barY = transform.y - 30f;
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint);

            // 绘制当前血量（绿色，长度根据生命值比例）
            paint.setColor(Color.GREEN);
            canvas.drawRect(barX, barY, barX + barWidth * healthRatio, barY + barHeight, paint);
        }
    }

    /**
     * 绘制防御塔实体
     * @param canvas 画布对象
     * @param tower 防御塔实体
     * @param transform 位置组件
     */
    private void drawTower(Canvas canvas, Entity tower, Transform transform) {
        // 获取防御塔组件
        Tower towerComp = tower.getComponent(Tower.class);

        if (towerComp == null) return;

        // 根据塔类型设置不同颜色
        switch (towerComp.type) {
            case ARCHER:    // 弓箭塔 - 黄色
                paint.setColor(Color.YELLOW);
                break;
            case CANNON:    // 加农炮 - 深灰色
                paint.setColor(Color.DKGRAY);
                break;
            case MAGE:      // 法师塔 - 洋红色
                paint.setColor(Color.MAGENTA);
                break;
        }

        // 绘制塔身（正方形）
        float size = 25f;
        canvas.drawRect(
                transform.x - size, transform.y - size, // 左上角
                transform.x + size, transform.y + size, // 右下角
                paint
        );

        // 绘制攻击范围（半透明圆形，用于调试）
        paint.setColor(Color.argb(30, 255, 255, 255)); // 半透明白色
        canvas.drawCircle(transform.x, transform.y, towerComp.range, paint);
    }

    /**
     * 绘制弹道/子弹实体
     * @param canvas 画布对象
     * @param projectile 弹道实体
     * @param transform 位置组件
     */
    private void drawProjectile(Canvas canvas, Entity projectile, Transform transform) {
        // 绘制子弹（白色小圆点）
        paint.setColor(Color.WHITE);
        canvas.drawCircle(transform.x, transform.y, 5f, paint);
    }

    /**
     * 绘制用户界面（HUD）
     * @param canvas 画布对象
     */
    private void drawHUD(Canvas canvas) {
        paint.setColor(Color.WHITE);

        // 根据屏幕尺寸动态调整文字大小
        float textSize = Math.min(getWidth(), getHeight()) / 30f;
        paint.setTextSize(textSize);

        // 绘制当前选中的塔类型
        String towerText = "选中: " + selectedTowerType.name();
        canvas.drawText(towerText, 10, textSize + 5, paint);

        // 绘制游戏操作说明
        paint.setTextSize(textSize * 0.8f);
        canvas.drawText("点击放置塔", 10, textSize * 2 + 5, paint);
    }

    /**
     * 触摸事件处理 - 处理用户的触摸输入
     * @param event 触摸事件
     * @return 是否已处理该事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 只处理按下事件
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameEngine != null) {
            // 获取触摸坐标
            float x = event.getX();
            float y = event.getY();

            // 在触摸位置放置选中的塔
            gameEngine.placeTower(x, y, selectedTowerType);

            // 触发重绘，显示新放置的塔
            invalidate();

            return true; // 事件已处理
        }
        return super.onTouchEvent(event);
    }
}