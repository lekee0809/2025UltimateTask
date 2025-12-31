package ranking;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// 玩家记录模型（含完成时间、游戏模式，无需玩家名称，完全兼容BaseGameScene写入逻辑）
public class PlayerRecord implements Comparable<PlayerRecord> {
    private int score; // 分数（对应BaseGameScene最终得分）
    private int playTime; // 游玩时长（秒，对应BaseGameScene存活时间）
    private long finishTimeStamp; // 游戏完成时间戳（唯一区分记录）
    private String finishTimeStr; // 格式化时间（yyyy-MM-dd HH:mm:ss，对应BaseGameScene游戏时间）
    private GameMode gameMode; // 游戏模式（拓展属性，兼容原有逻辑）
    // 新增：胜负状态（可选，完全匹配BaseGameScene的胜负记录）
    private boolean isWin;
    // 新增：拾取道具数量（可选，完全匹配BaseGameScene的道具记录）
    private int itemCount;

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

        // 根据模式名称获取枚举（优化空值判断和空格兼容，避免空指针）
        public static GameMode getByModeName(String modeName) {
            if (modeName == null || modeName.trim().isEmpty()) {
                return SINGLE_CHALLENGE; // 默认单人闯关
            }
            for (GameMode mode : values()) {
                if (mode.modeName.equals(modeName.trim())) {
                    return mode;
                }
            }
            return SINGLE_CHALLENGE; // 匹配失败返回默认值
        }
    }

    // 构造方法1：完整参数构造（完全匹配BaseGameScene的所有记录维度，推荐使用）
    public PlayerRecord(int score, int playTime, GameMode gameMode, boolean isWin, int itemCount) {
        // 合法性校验：避免无效数据
        this.score = Math.max(0, score);
        this.playTime = Math.max(0, playTime);
        this.gameMode = gameMode == null ? GameMode.SINGLE_CHALLENGE : gameMode;
        this.isWin = isWin;
        this.itemCount = Math.max(0, itemCount);
        // 自动生成时间戳和格式化时间
        this.finishTimeStamp = System.currentTimeMillis();
        this.finishTimeStr = formatTimeStamp(this.finishTimeStamp);
    }

    // 构造方法2：支持手动传入时间戳（用于读取历史记录，拓展性）
    public PlayerRecord(int score, int playTime, GameMode gameMode, boolean isWin, int itemCount, long finishTimeStamp) {
        this.score = Math.max(0, score);
        this.playTime = Math.max(0, playTime);
        this.gameMode = gameMode == null ? GameMode.SINGLE_CHALLENGE : gameMode;
        this.isWin = isWin;
        this.itemCount = Math.max(0, itemCount);
        this.finishTimeStamp = finishTimeStamp > 0 ? finishTimeStamp : System.currentTimeMillis();
        this.finishTimeStr = formatTimeStamp(this.finishTimeStamp);
    }

    // 构造方法3：兼容原有简化使用（保留无胜负/道具参数的构造）
    public PlayerRecord(int score, int playTime, GameMode gameMode) {
        this(score, playTime, gameMode, false, 0); // 默认未胜利、道具数0
    }

    // 私有工具方法：格式化时间戳（解决线程安全问题）
    private String formatTimeStamp(long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return sdf.format(new Date(timeStamp));
    }

    // 核心：生成兼容BaseGameScene原有格式的字符串（直接用于原有writeRecordToFile方法）
    public String toCompatibleRecordString() {
        // 格式和BaseGameScene的writeGameFinalRecord完全一致，无缝兼容
        return String.format("游戏时间：%s, 胜负状态：%s, 最终得分：%d, 存活时间：%d秒, 游戏模式：%s, 拾取道具数：%d",
                this.finishTimeStr,
                this.isWin ? "胜利" : "失败",
                this.score,
                this.playTime,
                this.gameMode.getModeName(),
                this.itemCount
        );
    }

    // 实现Comparable接口：默认按分数降序、同分按时间戳降序（排行榜排序专用）
    @Override
    public int compareTo(PlayerRecord other) {
        if (other == null) {
            return -1;
        }
        // 先按分数降序
        int scoreCompare = Integer.compare(other.getScore(), this.getScore());
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        // 分数相同按时间戳降序（最新记录在前）
        return Long.compare(other.getFinishTimeStamp(), this.getFinishTimeStamp());
    }

    // 重写toString：便于日志打印和对象写入
    @Override
    public String toString() {
        return this.toCompatibleRecordString(); // 直接复用兼容格式，统一输出
    }

    // ======== 所有Getter方法（含新增的胜负、道具数） ========
    public int getScore() {
        return score;
    }

    public int getPlayTime() {
        return playTime;
    }

    public long getFinishTimeStamp() {
        return finishTimeStamp;
    }

    public String getFinishTimeStr() {
        return finishTimeStr;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public boolean isWin() {
        return isWin;
    }

    public int getItemCount() {
        return itemCount;
    }

    // ======== 可选Setter方法（带合法性校验） ========
    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public void setPlayTime(int playTime) {
        this.playTime = Math.max(0, playTime);
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode == null ? GameMode.SINGLE_CHALLENGE : gameMode;
    }

    public void setWin(boolean win) {
        isWin = win;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = Math.max(0, itemCount);
    }
}