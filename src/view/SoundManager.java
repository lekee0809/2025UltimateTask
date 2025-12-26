package view;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * 音频管理器（单例）：统一管理背景音乐、音效的播放、暂停、音量调节
 */
public class SoundManager {
    // 单例实例
    private static SoundManager instance;
    // 背景音乐播放器（全局唯一）
    private MediaPlayer bgmPlayer;
    // 音效播放器缓存（射击、爆炸等，支持多音效同时播放）
    private Map<String, MediaPlayer> soundEffectPlayers;
    // 全局音量（0-1，默认1.0）
    private double globalVolume = 1.0;
    // 音频资源路径（放在resources/audio目录下）
    private static final String BGM_PATH = "sounds/background.wav"; // 背景音乐
    private static final String SHOOT_SOUND = "sounds/tank_fire.wav"; // 射击音效
    private static final String PLAY_SOUND="sounds/game.wav";//游戏音效
    private static final String TANK_MOVE="sounds/tank_move.wav";
    private static final String EXPLOSION_SOUND = "sounds/explosion.wav"; // 爆炸音效

    // 私有构造方法（单例）
    private SoundManager() {
        soundEffectPlayers = new HashMap<>();
        // 初始化背景音乐
        initBGM();
    }

    /**
     * 获取单例实例
     */
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * 初始化背景音乐（循环播放）
     */
    private void initBGM() {
        try {
            // 加载背景音乐资源（兼容jar包内路径）
            URI bgmUri = getClass().getClassLoader().getResource(BGM_PATH).toURI();
            Media bgmMedia = new Media(bgmUri.toString());
            bgmPlayer = new MediaPlayer(bgmMedia);
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE); // 无限循环
            bgmPlayer.setVolume(globalVolume); // 设置初始音量
        } catch (URISyntaxException e) {
            System.err.println("加载背景音乐失败：" + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("背景音乐文件不存在：" + BGM_PATH + "，请检查资源路径");
        }
    }

    /**
     * 播放背景音乐
     */
    public void playBGM() {
        if (bgmPlayer != null && bgmPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            bgmPlayer.play();
        }
    }

    /**
     * 暂停背景音乐
     */
    public void pauseBGM() {
        if (bgmPlayer != null && bgmPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            bgmPlayer.pause();
        }
    }

    /**
     * 播放短音效（射击/爆炸等，单次播放）
     * @param soundType 音效类型："shoot"/"explosion"
     */
    public void playSoundEffect(String soundType) {
        String soundPath = switch (soundType) {
            case "shoot" -> SHOOT_SOUND;
            case "explosion" -> EXPLOSION_SOUND;
            case "game"->PLAY_SOUND;
            case "move"->TANK_MOVE;
            default -> {
                System.err.println("未知音效类型：" + soundType);
                yield "";
            }
        };

        if (soundPath.isEmpty()) return;

        try {
            // 加载音效资源
            URI soundUri = getClass().getClassLoader().getResource(soundPath).toURI();
            Media soundMedia = new Media(soundUri.toString());
            MediaPlayer soundPlayer = new MediaPlayer(soundMedia);
            soundPlayer.setVolume(globalVolume);
            soundPlayer.play();

            // 音效播放完毕后释放资源（避免内存泄漏）
            soundPlayer.setOnEndOfMedia(() -> {
                soundPlayer.dispose();
                soundEffectPlayers.remove(soundType);
            });

            // 缓存音效播放器（便于后续调节音量）
            soundEffectPlayers.put(soundType, soundPlayer);
        } catch (URISyntaxException e) {
            System.err.println("加载音效失败：" + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("音效文件不存在：" + soundPath + "，请检查资源路径");
        }
    }

    /**
     * 设置全局音量（同步所有音频）
     * @param volume 音量值（0-1，0=静音，1=最大）
     */
    public void setGlobalVolume(double volume) {
        // 限制音量范围在0-1之间
        this.globalVolume = Math.max(0, Math.min(1, volume));

        // 更新背景音乐音量
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(this.globalVolume);
        }

        // 更新所有正在播放的音效音量
        for (MediaPlayer player : soundEffectPlayers.values()) {
            player.setVolume(this.globalVolume);
        }
    }

    /**
     * 获取当前全局音量
     */
    public double getGlobalVolume() {
        return globalVolume;
    }

    /**
     * 停止所有音频并释放资源
     */
    public void stopAllAudio() {
        // 停止背景音乐
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
        }
        // 停止所有音效
        for (MediaPlayer player : soundEffectPlayers.values()) {
            player.stop();
            player.dispose();
        }
        soundEffectPlayers.clear();
    }
}