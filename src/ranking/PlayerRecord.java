package ranking;

import java.text.SimpleDateFormat;
import java.util.Date;

// 玩家记录模型（含完成时间、游戏模式，无需玩家名称）
public class PlayerRecord {
    private int score; // 分数
    private int playTime; // 游玩时长（秒）
    private long finishTimeStamp; // 游戏完成时间戳（唯一区分记录）
    private String finishTimeStr; // 格式化时间（便于展示：yyyy-MM-dd HH:mm:ss）
    private GameMode gameMode; // 游戏模式

    // 游戏模式枚举（固定3种模式，避免手动输入出错）
    public enum GameMode {
        SINGLE_CHALLENGE("单人闯关"),
        DOUBLE_BATTLE("双人对战"),
        ENDLESS_MODE("无尽模式");

        private final String modeName;

        GameMode(String modeName) {
            this.modeName = modeName;
        }

        public String getModeName() {
            return modeName;
        }

        // 根据模式名称获取枚举（用于界面选择）
        public static GameMode getByModeName(String modeName) {
            for (GameMode mode : values()) {
                if (mode.modeName.equals(modeName)) {
                    return mode;
                }
            }
            return SINGLE_CHALLENGE; // 默认单人闯关
        }
    }

    // 构造方法：自动生成完成时间（无需手动传入）
    public PlayerRecord(int score, int playTime, GameMode gameMode) {
        this.score = score;
        this.playTime = playTime;
        this.gameMode = gameMode;
        // 记录时间戳（唯一标识）
        this.finishTimeStamp = System.currentTimeMillis();
        // 格式化时间（便于展示和阅读）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.finishTimeStr = sdf.format(new Date(finishTimeStamp));
    }

    // Getter方法
    public int getScore() {
        return score;
    }

    public int getPlayTime() {
        return playTime;
    }

    public String getFinishTimeStr() {
        return finishTimeStr;
    }

    public long getFinishTimeStamp() {
        return finishTimeStamp;
    }

    public GameMode getGameMode() {
        return gameMode;
    }
}