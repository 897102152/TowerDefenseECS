package com.example.towerdefense.managers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 资源管理器 - 负责管理游戏中的资源（人力和补给）
 */
public class ResourceManager {
    private static final String PREFS_NAME = "TowerDefenseResources";
    private static final String KEY_MANPOWER = "manpower";
    private static final String KEY_SUPPLY = "supply";

    private SharedPreferences prefs;
    private int manpower;
    private int supply;

    // 资源变化监听器
    public interface ResourceChangeListener {
        void onResourceChanged(int manpower, int supply);
    }
    private ResourceChangeListener listener;

    public ResourceManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // 强制重置为默认值，不读取保存的数据
        resetResources();
    }

    /**
     * 加载资源
     */
    private void loadResources() {
        manpower = 100;
        supply = 50;
        System.out.println("ResourceManager: 加载默认资源 - 人力:" + manpower + " 补给:" + supply);
        manpower = prefs.getInt(KEY_MANPOWER, 100); // 默认100人力
        supply = prefs.getInt(KEY_SUPPLY, 50);      // 默认50补给
    }

    /**
     * 保存资源
     */
    private void saveResources() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_MANPOWER, manpower);
        editor.putInt(KEY_SUPPLY, supply);
        editor.apply();

        // 通知资源变化
        if (listener != null) {
            listener.onResourceChanged(manpower, supply);
        }
    }

    /**
     * 获取当前人力
     */
    public int getManpower() {
        return manpower;
    }

    /**
     * 获取当前补给
     */
    public int getSupply() {
        return supply;
    }

    /**
     * 增加人力
     */
    public void addManpower(int amount) {
        manpower += amount;
        saveResources();
    }

    /**
     * 增加补给
     */
    public void addSupply(int amount) {
        supply += amount;
        saveResources();
    }

    /**
     * 消耗人力
     */
    public boolean consumeManpower(int amount) {
        if (manpower >= amount) {
            manpower -= amount;
            saveResources();
            return true;
        }
        return false;
    }

    /**
     * 消耗补给
     */
    public boolean consumeSupply(int amount) {
        if (supply >= amount) {
            supply -= amount;
            saveResources();
            return true;
        }
        return false;
    }

    /**
     * 检查是否可以消耗资源
     */
    public boolean canConsume(int manpowerCost, int supplyCost) {
        return manpower >= manpowerCost && supply >= supplyCost;
    }

    /**
     * 设置资源变化监听器
     */
    public void setResourceChangeListener(ResourceChangeListener listener) {
        this.listener = listener;
    }

    /**
     * 重置资源（开始新游戏时使用）
     */
    public void resetResources() {
        manpower = 100;
        supply = 50;
        saveResources();
    }
}