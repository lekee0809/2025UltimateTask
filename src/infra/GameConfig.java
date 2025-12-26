package infra;

/**
 * 坦克大战全局配置文件
 * 由指挥官 Lekee 统一定义，确保全队逻辑对齐
 */
public class GameConfig {

    // === 1. 分辨率与场地设置 ===
    // 采用 1200x900 方案，相比 800x600 拥有更广的视野和战术纵深
    public static final double SCREEN_WIDTH = 1200.0;
    public static final double SCREEN_HEIGHT = 900.0;

    // === 2. 格点地图规范 ===
    // 墙壁厚度维持 40px，兼顾巷战的紧凑感与碰撞安全性
    public static final double GRID_SIZE = 40.0;
    // 自动计算格点行列：30列 x 22.5行 (取22行，剩余20px留给UI边距)
    public static final int MAP_COLS = 30;
    public static final int MAP_ROWS = 22;

    // === 3. 坦克参数 (组员 A & B 参考) ===
    // 坦克大小设为 34px，确保在 40px 的窄路中旋转时有缓冲余地
    public static final double TANK_SIZE = 34.0;
    public static final double TANK_SPEED = 3.0;
    public static final double TANK_ROTATION_SPEED = 4.0; // 360度旋转灵敏度

    // === 4. 子弹参数 (你负责的核心模块) ===
    public static final double BULLET_RADIUS = 3.0;
    // 分辨率调大后速度提升至 8.0，保证手感不迟钝
    public static final double BULLET_SPEED = 8.0;
    // 满足你要求的 3 次反弹逻辑
    public static final int MAX_BULLET_BOUNCES = 3;

    // === 5. 资源路径 (UI 组参考) ===
    // 统一资源根路径，彻底解决你遇到的 NullPointerException 问题
    public static final String IMAGE_PATH = "/images/";
    public static final String SOUND_PATH = "/sounds/";

    // === 6. 地形编号定义 (地图组参考) ===
    public static final int TILE_EMPTY = 0; // 空地
    public static final int TILE_BRICK = 1; // 普通砖墙
    public static final int TILE_STONE = 2; // 硬石头（子弹在此反弹）
    public static final int TILE_WATER = 3; // 水（坦克不可过，子弹可过）
    public static final int TILE_GRASS = 4; //草地（坦克可过，隐身，子弹可过）


    // === 7. 游戏循环核心设置 (Game Loop) ===
    // 类似于 CSGO 的 64-tick 或 128-tick，这里我们锁定逻辑帧率为 60
    public static final int TARGET_FPS = 60;

    // 每一帧逻辑应该消耗的纳秒数 (1秒 = 1,000,000,000 纳秒)
    // 1_000_000_000 / 60 ≈ 16,666,666 ns
    public static final long TIME_PER_FRAME = 1_000_000_000L / TARGET_FPS;
    // 在 GameConfig 类中添加：
    public static final int PLAYER_HEALTH = 100;
    public static final int PLAYER_FIRE_COOLDOWN = 50; // 0.05秒一发
    public static final int PLAYER_BULLET_DAMAGE = 10000;

    // === Enemy Stats (数值体系) ===

    // 1. 重型坦克 (HEAVY): 笨重但致命，像个移动堡垒
    public static final double HEAVY_SPEED_MULTIPLIER = 0.6;
    public static final double HEAVY_ROTATION_MULTIPLIER = 0.5;
    public static final int HEAVY_HEALTH = 200;
    public static final int HEAVY_FIRE_COOLDOWN = 1500; // 射速慢
    public static final int HEAVY_BULLET_DAMAGE = 50;   // 一炮半血
    public static final double HEAVY_BULLET_SPEED_MULTIPLIER = 0.8;
    public static final int HEAVY_SCORE_VALUE = 500;

    // 2. 侦查坦克 (FAST): 脆皮但极快，类似 CS 里的拉枪位
    public static final double FAST_SPEED_MULTIPLIER = 1.5;
    public static final double FAST_ROTATION_MULTIPLIER = 1.8;
    public static final int FAST_HEALTH = 50;
    public static final int FAST_FIRE_COOLDOWN = 400;  // 泼水射速
    public static final int FAST_BULLET_DAMAGE = 10;
    public static final double FAST_BULLET_SPEED_MULTIPLIER = 1.4;
    public static final int FAST_SCORE_VALUE = 200;

    // 3. 普通坦克 (NORMAL): 中规中矩
    public static final double NORMAL_SPEED_MULTIPLIER = 1.0;
    public static final double NORMAL_ROTATION_MULTIPLIER = 1.0;
    public static final int NORMAL_HEALTH = 100;
    public static final int NORMAL_FIRE_COOLDOWN = 800;
    public static final int NORMAL_BULLET_DAMAGE = 20;
    public static final int NORMAL_SCORE_VALUE = 100;
}