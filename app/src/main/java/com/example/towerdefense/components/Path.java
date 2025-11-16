package com.example.towerdefense.components;

import com.example.towerdefense.ecs.Component;

/**
 * 路径组件 - 定义游戏中的移动路径
 * 使用百分比坐标系统，便于适配不同屏幕尺寸
 */
public class Path implements Component {
    /**
     * 路径标签枚举 - 标识不同路径
     */
    public enum PathTag {
        PATH_A,
        PATH_B,
        PATH_C
    }

    // 公共字段
    public PathTag tag;                    // 路径标识
    public float[][] percentagePoints;     // 百分比坐标点数组 [0.0-1.0]
    public int pathColor;                  // 路径颜色
    public float pathWidth;                // 路径线条宽度
    public boolean isVisible;              // 是否可见

    /**
     * 构造函数 - 使用百分比坐标初始化路径
     * @param tag 路径标签
     * @param percentagePoints 百分比坐标点数组 [[x%, y%], ...]
     * @param pathColor 路径颜色
     * @param pathWidth 路径宽度
     */
    public Path(PathTag tag, float[][] percentagePoints, int pathColor, float pathWidth) {
        this.tag = tag;
        this.percentagePoints = percentagePoints;
        this.pathColor = pathColor;
        this.pathWidth = pathWidth;
        this.isVisible = true;
    }

    /**
     * 将百分比坐标转换为实际屏幕坐标
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @return 实际坐标点数组
     */
    public float[][] convertToScreenCoordinates(float screenWidth, float screenHeight) {
        System.out.println("Path: 转换坐标，屏幕尺寸=" + screenWidth + "x" + screenHeight);

        float[][] screenPoints = new float[percentagePoints.length][2];

        for (int i = 0; i < percentagePoints.length; i++) {
            screenPoints[i][0] = percentagePoints[i][0] * screenWidth;
            screenPoints[i][1] = percentagePoints[i][1] * screenHeight;

            System.out.println("Path: 点" + i + ": " + percentagePoints[i][0] + "," + percentagePoints[i][1] +
                    " -> " + screenPoints[i][0] + "," + screenPoints[i][1]);

        }

        return screenPoints;
    }

    // Getter 方法
    public PathTag getTag() { return tag; }
    public float[][] getPercentagePoints() { return percentagePoints; }
    public int getPathColor() { return pathColor; }
    public float getPathWidth() { return pathWidth; }
    public boolean isVisible() { return isVisible; }

    // Setter 方法
    public void setVisible(boolean visible) { this.isVisible = visible; }
    public void setPathColor(int color) { this.pathColor = color; }
}