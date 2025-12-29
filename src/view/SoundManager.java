package view;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 音效管理类（单例模式）：修复背景音异常 + 音效按需加载
 */
public class SoundManager {
    private static SoundManager instance;
    private MediaPlayer backgroundPlayer; // 背景音播放器（全局唯一）
    private MediaPlayer gameMusicPlayer;  // 游戏内音乐播放器（game.wav，全局唯一）
    private Map<String, String> soundMap; // 音效路径映射

    // 私有构造（单例）
    private SoundManager() {
        initSoundMap();
    }

    // 获取单例
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * 初始化音效路径（按需配置，避免未知类型报错）
     */
    private void initSoundMap() {
        soundMap = new HashMap<>();
        // 背景音（你的背景音路径，替换为实际路径）
        soundMap.put("background", "sounds/background.wav");
        //游戏音乐
        soundMap.put("game", "sounds/game.wav");
        // 有效音效（仅配置存在的文件，避免报错）
        soundMap.put("shoot", "sounds/tank_fire.wav");
        soundMap.put("explosion", "sounds/explosion.wav");
        // 重生音效（若有文件再添加，暂时注释避免报错）
        // soundMap.put("rebirth", "sounds/rebirth.wav");
    }

    /**
     * 播放背景音（全局唯一，重复调用不重新播放）
     */
    public void playBackgroundMusic() {
        // 若背景音已在播放，直接返回
        if (backgroundPlayer != null && backgroundPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }

        // 加载背景音（改用 getClass().getResource，兼容性更强）
        String bgPath = soundMap.get("background");
        // 相对路径补充前缀 "/"，确保从 resources 根目录查找
        URL bgUrl = getClass().getResource("/" + bgPath);
        if (bgUrl == null) {
            System.err.println("背景音文件不存在：" + bgPath + "，请检查资源路径");
            return;
        }

        // 创建播放器并循环播放
        Media bgMedia = new Media(bgUrl.toExternalForm());
        backgroundPlayer = new MediaPlayer(bgMedia);
        backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE); // 无限循环
        backgroundPlayer.play();
    }

    /**
     * 停止背景音（仅在退出时调用）
     */
    public void stopBackgroundMusic() {
        if (backgroundPlayer != null && backgroundPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            backgroundPlayer.stop();
        }
    }

    /**
     * 播放普通音效（按需播放，不存在则提示不报错）
     */
    public void playSoundEffect(String soundType) {
        // 校验音效类型是否存在
        if (!soundMap.containsKey(soundType)) {
            System.err.println("未知音效类型：" + soundType);
            return;
        }

        // 加载音效文件（统一改用 getClass().getResource，保持一致性）
        String soundPath = soundMap.get(soundType);
        URL soundUrl = getClass().getResource("/" + soundPath);
        if (soundUrl == null) {
            System.err.println("音效文件不存在：" + soundPath + "，请检查资源路径");
            return;
        }

        // 播放音效（单次播放，不循环）
        Media soundMedia = new Media(soundUrl.toExternalForm());
        MediaPlayer soundPlayer = new MediaPlayer(soundMedia);
        soundPlayer.play();

        // 播放完毕后释放资源
        soundPlayer.setOnEndOfMedia(soundPlayer::dispose);
    }

    // 暂停背景音乐
    public void pauseBGM() {
        if (backgroundPlayer != null && backgroundPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            backgroundPlayer.pause();
        }
    }

    // 播放/恢复背景音乐（修复：补充播放器未初始化的兜底逻辑）
    public void playBGM() {
        // 兜底：若播放器未初始化，先调用 playBackgroundMusic() 初始化并播放
        if (backgroundPlayer == null) {
            playBackgroundMusic();
            return;
        }

        // 原有逻辑：恢复暂停状态，或重置后播放
        if (backgroundPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
            backgroundPlayer.play();
        } else if (backgroundPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            backgroundPlayer.stop(); // 重置播放状态
            backgroundPlayer.play();
        }
    }
    // ==================== 新增：游戏内音乐（game.wav）相关方法 ====================
    /**
     * 播放游戏内音乐（game.wav，无限循环）
     */
    public void playGameMusic() {
        // 若游戏音乐已在播放，直接返回
        if (gameMusicPlayer != null && gameMusicPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }

        // 加载游戏音乐 game.wav
        String gamePath = soundMap.get("game");
        URL gameUrl = getClass().getResource("/" + gamePath);
        if (gameUrl == null) {
            System.err.println("游戏音乐文件不存在：" + gamePath + "，请检查资源路径");
            return;
        }

        // 停止主菜单背景音（避免音频冲突）
        stopBackgroundMusic();

        // 创建游戏音乐播放器并无限循环
        Media gameMedia = new Media(gameUrl.toExternalForm());
        gameMusicPlayer = new MediaPlayer(gameMedia);
        gameMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE); // 游戏音乐无限循环播放
        gameMusicPlayer.play();
    }

    /**
     * 暂停游戏内音乐（game.wav）
     */
    public void pauseGameMusic() {
        if (gameMusicPlayer != null && gameMusicPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            gameMusicPlayer.pause();
        }
    }

    /**
     * 恢复游戏内音乐（game.wav）
     */
    public void resumeGameMusic() {
        if (gameMusicPlayer == null) {
            playGameMusic(); // 若未初始化，直接播放
            return;
        }

        if (gameMusicPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
            gameMusicPlayer.play();
        } else if (gameMusicPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            gameMusicPlayer.stop();
            gameMusicPlayer.play();
        }
    }

    /**
     * 停止游戏内音乐（game.wav）
     */
    public void stopGameMusic() {
        if (gameMusicPlayer != null && gameMusicPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
            gameMusicPlayer.stop();
        }
    }

}