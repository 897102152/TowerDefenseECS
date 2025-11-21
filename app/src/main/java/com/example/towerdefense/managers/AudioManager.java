package com.example.towerdefense.managers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import com.example.towerdefense.R; // R 类用于访问资源

/**
 * AudioManager - 负责统一管理背景音乐(BGM)和音效(SFX)。
 * BGM 使用 MediaPlayer，SFX 使用 SoundPool。
 */
public class AudioManager {
    private Context context;
    private MediaPlayer bgmPlayer;
    private SoundPool soundPool;
    private boolean isBgmEnabled = true;
    private boolean isSfxEnabled = true;

    // 音效ID映射，用于 SoundPool 播放
    private int sfxClick, sfxBuild, sfxShootArrow, sfxShootCannon, sfxAirRaid;
    private int sfxExplosion, sfxVictory, sfxDefeat; // 新增的音效ID

    public AudioManager(Context context) {
        this.context = context.getApplicationContext();
        initSoundPool();
    }

    private void initSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attributes)
                .build();

        // 预加载所有音效，使用 R.raw.文件名 引用
        try {
            sfxClick = soundPool.load(context, R.raw.sfx_click, 1);
            sfxBuild = soundPool.load(context, R.raw.sfx_build, 1);
            sfxShootArrow = soundPool.load(context, R.raw.sfx_shoot_arrow, 1);
            sfxShootCannon = soundPool.load(context, R.raw.sfx_shoot_cannon, 1);
            sfxAirRaid = soundPool.load(context, R.raw.sfx_air_raid, 1);
            sfxExplosion = soundPool.load(context, R.raw.sfx_explosion, 1); // 新增加载
            sfxVictory = soundPool.load(context, R.raw.sfx_victory, 1);     // 新增加载
            sfxDefeat = soundPool.load(context, R.raw.sfx_defeat, 1);       // 新增加载
        } catch (Exception e) {
            System.err.println("AudioManager: 警告！部分音效加载失败，请检查资源文件是否存在: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== BGM 控制 (MediaPlayer) ==========
    public void playBgm() {
        if (!isBgmEnabled) return;
        stopBgm();
        try {
            bgmPlayer = MediaPlayer.create(context, R.raw.bgm_main);
            if (bgmPlayer != null) {
                bgmPlayer.setLooping(true);
                bgmPlayer.setVolume(0.5f, 0.5f);
                bgmPlayer.start();
            }
        } catch (Exception e) { System.err.println("AudioManager: BGM播放失败"); }
    }

    public void stopBgm() {
        if (bgmPlayer != null) {
            if (bgmPlayer.isPlaying()) { bgmPlayer.stop(); }
            bgmPlayer.release();
            bgmPlayer = null;
        }
    }

    public void pauseBgm() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) bgmPlayer.pause();
    }

    public void resumeBgm() {
        if (bgmPlayer != null && !bgmPlayer.isPlaying() && isBgmEnabled) bgmPlayer.start();
    }

    // ========== SFX 播放方法 (SoundPool) ==========
    public void playClick() { playSound(sfxClick, 1.0f); }
    public void playBuild() { playSound(sfxBuild, 1.0f); }
    public void playShootArrow() { playSound(sfxShootArrow, 0.6f); }
    public void playShootCannon() { playSound(sfxShootCannon, 1.0f); }
    public void playAirRaid() { playSound(sfxAirRaid, 1.0f); }
    public void playExplosion() { playSound(sfxExplosion, 0.8f); }
    public void playVictory() {
        stopBgm();
        playSound(sfxVictory, 1.0f);
    }
    public void playDefeat() {
        stopBgm();
        playSound(sfxDefeat, 1.0f);
    }

// ========== [新增] 缺失的音效方法 ==========
    /**
     * 播放击中音效（抛射体击中敌人）
     */
    public void playHitSound() {
        System.out.println("🎯 AudioManager: 播放击中音效");
        // 可以使用现有的爆炸音效，或者创建一个新的
        playSound(sfxExplosion, 0.6f); // 暂时使用爆炸音效
    }

    /**
     * 播放空袭音效
     */
    public void playAirStrike() {
        System.out.println("✈️ AudioManager: 播放空袭音效");
        playSound(sfxAirRaid, 1.0f); // 使用空袭警报音效
    }

    /**
     * 播放建造音效（别名方法，与 playBuild() 功能相同）
     */
    public void playBuildSound() {
        System.out.println("🔊 AudioManager: 播放建造音效（通过playBuildSound）");
        playBuild(); // 直接调用现有的 playBuild 方法
    }

    private void playSound(int soundId, float volume) {
        if (isSfxEnabled && soundId != 0) {
            soundPool.play(soundId, volume, volume, 1, 0, 1f);
        }
    }

    // ========== 释放资源 ==========
    public void release() {
        stopBgm();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}