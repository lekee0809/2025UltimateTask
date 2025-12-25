
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

    // 在 GameConfig 类中添加：
    public static final int PLAYER_HEALTH = 100;
    public static final int PLAYER_FIRE_COOLDOWN = 500; // 0.5秒一发
    public static final int PLAYER_BULLET_DAMAGE = 20;
}