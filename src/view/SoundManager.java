package view;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 音效管理类（单例模式）：支持 BGM 和 SFX 独立音量控制
 */
public class SoundManager {
    private static SoundManager instance;

    // 播放器
    private MediaPlayer backgroundPlayer; // 主菜单背景音
    private MediaPlayer gameMusicPlayer;  // 游戏内背景音 (game.wav)

    // 资源路径映射
    private Map<String, String> soundPathMap;

    // 音效缓存
    private Map<String, AudioClip> loadedEffects;

    // ========== 音量控制变量 ==========
    private double bgmVolume = 0.5; // 背景音乐音量
    private double sfxVolume = 0.5; // 音效音量

    // 私有构造
    private SoundManager() {
        initSoundMap();
        loadedEffects = new HashMap<>();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void initSoundMap() {
        soundPathMap = new HashMap<>();
        // 音乐
        soundPathMap.put("background", "sounds/background.wav");
        soundPathMap.put("game", "sounds/game.wav");
        // 音效
        soundPathMap.put("shoot", "sounds/tank_fire.wav");
        soundPathMap.put("explosion", "sounds/explosion.wav");
    }

    // ==================== 【关键修复】BGM 音量控制 ====================

    public double getBGMVolume() {
        return bgmVolume;
    }

    public void setBGMVolume(double volume) {
        this.bgmVolume = Math.max(0, Math.min(1, volume)); // 限制范围

        // 实时调整正在播放的音乐
        if (backgroundPlayer != null) {
            backgroundPlayer.setVolume(bgmVolume);
        }
        if (gameMusicPlayer != null) {
            gameMusicPlayer.setVolume(bgmVolume);
        }
    }

    // ==================== 【关键修复】SFX 音效音量控制 ====================

    public double getSFXVolume() {
        return sfxVolume;
    }

    public void setSFXVolume(double volume) {
        this.sfxVolume = Math.max(0, Math.min(1, volume)); // 限制范围
    }

    // ==================== 兼容旧代码的全局音量 (可选) ====================
    public void setGlobalVolume(double volume) {
        setBGMVolume(volume);
        setSFXVolume(volume);
    }
    public double getGlobalVolume() {
        return bgmVolume; // 简单返回其中一个
    }

    // ==================== 音效播放 ====================

    public void playSoundEffect(String name) {
        if (!soundPathMap.containsKey(name)) return;

        try {
            if (!loadedEffects.containsKey(name)) {
                String path = soundPathMap.get(name);
                URL url = getClass().getResource("/" + path);
                if (url == null) {
                    System.err.println("文件缺失: " + path);
                    return;
                }
                AudioClip clip = new AudioClip(url.toExternalForm());
                loadedEffects.put(name, clip);
            }

            AudioClip clip = loadedEffects.get(name);
            if (clip != null) {
                // 【关键】播放时使用 sfxVolume
                clip.play(sfxVolume);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 背景音乐控制 ====================

    public void playBackgroundMusic() {
        if (backgroundPlayer != null && backgroundPlayer.getStatus() == MediaPlayer.Status.PLAYING) return;
        stopGameMusic(); // 互斥

        try {
            if (backgroundPlayer == null) {
                String path = soundPathMap.get("background");
                URL url = getClass().getResource("/" + path);
                if (url != null) {
                    backgroundPlayer = new MediaPlayer(new Media(url.toExternalForm()));
                    backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                }
            }
            if (backgroundPlayer != null) {
                backgroundPlayer.setVolume(bgmVolume); // 【关键】使用 bgmVolume
                backgroundPlayer.play();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void stopBackgroundMusic() {
        if (backgroundPlayer != null) backgroundPlayer.stop();
    }

    public void pauseBGM() {
        if (backgroundPlayer != null && backgroundPlayer.getStatus() == MediaPlayer.Status.PLAYING) backgroundPlayer.pause();
    }

    public void playBGM() {
        if (backgroundPlayer != null && backgroundPlayer.getStatus() == MediaPlayer.Status.PAUSED) backgroundPlayer.play();
        else playBackgroundMusic();
    }

    // ==================== 游戏音乐控制 ====================

    public void playGameMusic() {
        if (gameMusicPlayer != null && gameMusicPlayer.getStatus() == MediaPlayer.Status.PLAYING) return;
        stopBackgroundMusic(); // 互斥

        try {
            if (gameMusicPlayer == null) {
                String path = soundPathMap.get("game");
                URL url = getClass().getResource("/" + path);
                if (url != null) {
                    gameMusicPlayer = new MediaPlayer(new Media(url.toExternalForm()));
                    gameMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                }
            }
            if (gameMusicPlayer != null) {
                gameMusicPlayer.setVolume(bgmVolume); // 【关键】使用 bgmVolume
                gameMusicPlayer.play();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void stopGameMusic() {
        if (gameMusicPlayer != null) gameMusicPlayer.stop();
    }

    public void pauseGameMusic() {
        if (gameMusicPlayer != null && gameMusicPlayer.getStatus() == MediaPlayer.Status.PLAYING) gameMusicPlayer.pause();
    }

    public void resumeGameMusic() {
        if (gameMusicPlayer != null) {
            if (gameMusicPlayer.getStatus() == MediaPlayer.Status.PAUSED) gameMusicPlayer.play();
            else if (gameMusicPlayer.getStatus() == MediaPlayer.Status.STOPPED) playGameMusic();
        } else {
            playGameMusic();
        }
    }
}