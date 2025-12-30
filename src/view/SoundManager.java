package view;

import infra.GameConfig;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 音效管理器
 * 单例模式，统一管理背景音乐和游戏音效的播放、暂停、音量控制
 * 新增兼容方法：playSoundEffect/playGameMusic/playBackgroundMusic 等
 */
public class SoundManager {
    // 单例实例
    private static SoundManager instance;

    // 背景音乐播放器（主菜单）
    private MediaPlayer bgmPlayer;
    // 游戏音乐播放器（游戏内）
    private MediaPlayer gameMusicPlayer;
    // 音效播放器缓存（避免重复创建）
    private Map<String, MediaPlayer> sfxPlayers;

    // 音量配置（默认值）
    private double bgmVolume = 0.7;   // 背景音乐音量 0-1
    private double sfxVolume = 0.8;   // 音效音量 0-1
    private double gameMusicVolume = 0.6; // 游戏音乐音量

    // 暂停状态标记
    private boolean isBgmPaused = false;
    private boolean isGameMusicPaused = false;

    /**
     * 私有构造方法（单例模式）
     */
    private SoundManager() {
        sfxPlayers = new HashMap<>();
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

    // ====================== 主菜单背景音乐相关 ======================
    /**
     * 播放主菜单背景音乐（兼容 playBackgroundMusic 调用）
     */
    public void playBackgroundMusic() {
        playBGM("bgm_menu.mp3"); // 默认主菜单音乐
    }

    /**
     * 播放背景音乐（循环播放）
     * @param bgmFileName 背景音乐文件名（如 "bgm_main.mp3"）
     */
    public void playBGM(String bgmFileName) {
        // 停止原有背景音乐
        if (bgmPlayer != null) {
            bgmPlayer.stop();
        }

        // 加载新的背景音乐
        String bgmPath = GameConfig.SOUND_PATH + bgmFileName;
        URL bgmUrl = getClass().getResource(bgmPath);
        if (bgmUrl != null) {
            Media bgmMedia = new Media(bgmUrl.toExternalForm());
            bgmPlayer = new MediaPlayer(bgmMedia);
            bgmPlayer.setVolume(bgmVolume);       // 设置音量
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE); // 无限循环
            bgmPlayer.play();                     // 开始播放
            isBgmPaused = false;
        } else {
            System.err.println("背景音乐文件未找到: " + bgmPath);
        }
    }

    /**
     * 重载：播放默认背景音乐（如果已初始化过BGM，直接恢复播放）
     */
    public void playBGM() {
        if (bgmPlayer != null) {
            if (isBgmPaused) {
                bgmPlayer.play();
                isBgmPaused = false;
            }
        } else {
            // 自动播放默认主菜单音乐
            playBackgroundMusic();
        }
    }

    /**
     * 暂停背景音乐
     */
    public void pauseBGM() {
        if (bgmPlayer != null && bgmPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            bgmPlayer.pause();
            isBgmPaused = true;
        }
    }

    /**
     * 停止背景音乐（彻底停止，恢复需重新play）
     */
    public void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            isBgmPaused = false;
        }
    }

    // ====================== 游戏内音乐相关（新增） ======================
    /**
     * 播放游戏内背景音乐（如双人模式/闯关模式）
     */
    public void playGameMusic() {
        // 停止原有游戏音乐
        if (gameMusicPlayer != null) {
            gameMusicPlayer.stop();
        }

        // 加载游戏音乐（默认 game_bgm.mp3）
        String gameMusicPath = GameConfig.SOUND_PATH + "game_bgm.mp3";
        URL gameMusicUrl = getClass().getResource(gameMusicPath);
        if (gameMusicUrl != null) {
            Media gameMedia = new Media(gameMusicUrl.toExternalForm());
            gameMusicPlayer = new MediaPlayer(gameMedia);
            gameMusicPlayer.setVolume(gameMusicVolume);
            gameMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            gameMusicPlayer.play();
            isGameMusicPaused = false;
        } else {
            System.err.println("游戏音乐文件未找到: " + gameMusicPath);
        }
    }

    /**
     * 暂停游戏内音乐
     */
    public void pauseGameMusic() {
        if (gameMusicPlayer != null && gameMusicPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            gameMusicPlayer.pause();
            isGameMusicPaused = true;
        }
    }

    /**
     * 恢复游戏内音乐
     */
    public void resumeGameMusic() {
        if (gameMusicPlayer != null && isGameMusicPaused) {
            gameMusicPlayer.play();
            isGameMusicPaused = false;
        }
    }

    /**
     * 停止游戏内音乐
     */
    public void stopGameMusic() {
        if (gameMusicPlayer != null) {
            gameMusicPlayer.stop();
            isGameMusicPaused = false;
        }
    }

    // ====================== 音效相关（兼容 playSoundEffect 调用） ======================
    /**
     * 播放游戏音效（兼容 playSoundEffect 调用）
     * @param sfxName 音效名（如 "shoot" -> 对应 shoot.wav）
     */
    public void playSoundEffect(String sfxName) {
        playSFX(sfxName + ".wav"); // 自动补全后缀
    }

    /**
     * 播放游戏音效（一次性播放，如开枪、爆炸）
     * @param sfxFileName 音效文件名（如 "fire.wav"）
     */
    public void playSFX(String sfxFileName) {
        // 检查缓存中是否已有该音效播放器
        MediaPlayer sfxPlayer = sfxPlayers.get(sfxFileName);
        String sfxPath = GameConfig.SOUND_PATH + sfxFileName;
        URL sfxUrl = getClass().getResource(sfxPath);

        if (sfxUrl == null) {
            System.err.println("音效文件未找到: " + sfxPath);
            return;
        }

        // 没有缓存则创建新的播放器
        if (sfxPlayer == null) {
            Media sfxMedia = new Media(sfxUrl.toExternalForm());
            sfxPlayer = new MediaPlayer(sfxMedia);
            sfxPlayers.put(sfxFileName, sfxPlayer);
        }

        // 设置音量并播放（重置播放位置，避免重复播放时卡顿）
        sfxPlayer.setVolume(sfxVolume);
        sfxPlayer.seek(sfxPlayer.getStartTime());
        sfxPlayer.play();
    }

    /**
     * 停止指定音效
     */
    public void stopSFX(String sfxFileName) {
        MediaPlayer sfxPlayer = sfxPlayers.get(sfxFileName);
        if (sfxPlayer != null) {
            sfxPlayer.stop();
        }
    }

    /**
     * 停止所有音效
     */
    public void stopAllSFX() {
        for (MediaPlayer player : sfxPlayers.values()) {
            player.stop();
        }
    }

    // ====================== 音量控制 ======================
    /**
     * 获取背景音乐音量
     */
    public double getBGMVolume() {
        return bgmVolume;
    }

    /**
     * 设置背景音乐音量（实时生效）
     */
    public void setBGMVolume(double volume) {
        // 限制音量范围 0-1
        this.bgmVolume = Math.max(0, Math.min(1, volume));
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(this.bgmVolume);
        }
    }

    /**
     * 获取音效音量
     */
    public double getSFXVolume() {
        return sfxVolume;
    }

    /**
     * 设置音效音量（后续播放的音效生效）
     */
    public void setSFXVolume(double volume) {
        // 限制音量范围 0-1
        this.sfxVolume = Math.max(0, Math.min(1, volume));
        // 实时更新已有音效播放器的音量
        for (MediaPlayer player : sfxPlayers.values()) {
            player.setVolume(this.sfxVolume);
        }
    }

    /**
     * 获取游戏音乐音量
     */
    public double getGameMusicVolume() {
        return gameMusicVolume;
    }

    /**
     * 设置游戏音乐音量
     */
    public void setGameMusicVolume(double volume) {
        this.gameMusicVolume = Math.max(0, Math.min(1, volume));
        if (gameMusicPlayer != null) {
            gameMusicPlayer.setVolume(this.gameMusicVolume);
        }
    }

    /**
     * 静音/取消静音所有声音
     */
    public void toggleMute() {
        if (bgmVolume > 0 || sfxVolume > 0 || gameMusicVolume > 0) {
            // 静音
            setBGMVolume(0);
            setSFXVolume(0);
            setGameMusicVolume(0);
        } else {
            // 恢复原有音量
            setBGMVolume(0.7);
            setSFXVolume(0.8);
            setGameMusicVolume(0.6);
        }
    }

    /**
     * 释放资源（游戏退出时调用）
     */
    public void release() {
        stopBGM();
        stopGameMusic();
        stopAllSFX();
        if (bgmPlayer != null) {
            bgmPlayer.dispose();
        }
        if (gameMusicPlayer != null) {
            gameMusicPlayer.dispose();
        }
        for (MediaPlayer player : sfxPlayers.values()) {
            player.dispose();
        }
        sfxPlayers.clear();
    }
}