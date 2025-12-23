
/**
 * 地图格子类型枚举
 *
 * 每一种 TileType 对应一种地图语义，
 * 决定：
 * 1. 坦克是否可通过
 * 2. 子弹是否可通过
 * 3. 子弹是否会发生反弹或销毁
 * 4. 是否提供隐身效果（草丛）
 */
public enum TileType {

    /** 空地 */
    EMPTY(
            GameConfig.TILE_EMPTY,
            true,   // tankPassable
            true,   // bulletPassable
            false,  // bulletReflect
            false   // providesCover
    ),

    /** 普通砖墙（子弹销毁） */
    BRICK(
            GameConfig.TILE_BRICK,
            false,
            false,
            false,
            false
    ),

    /** 石墙（子弹反弹） */
    STONE(
            GameConfig.TILE_STONE,
            false,
            false,
            true,
            false
    ),

    /** 水域（坦克不可过，子弹可过） */
    WATER(
            GameConfig.TILE_WATER,
            false,
            true,
            false,
            false
    ),

    /** 草丛（坦克可过，子弹可过，提供隐身效果） */
    GRASS(
            GameConfig.TILE_GRASS,
            true,   // tankPassable - 坦克可以通过
            true,   // bulletPassable - 子弹可以通过
            false,  // bulletReflect - 子弹不反弹
            true    // providesCover - 提供隐身效果
    );

    /** 与地图数组中使用的编号对应 */
    private final int code;

    /** 坦克是否可通过 */
    private final boolean tankPassable;

    /** 子弹是否可通过 */
    private final boolean bulletPassable;

    /** 子弹是否发生反弹 */
    private final boolean bulletReflect;

    /** 是否提供隐身效果（坦克进入后隐藏） */
    private final boolean hideTank;

    TileType(int code, boolean tankPassable, boolean bulletPassable, boolean bulletReflect, boolean hideTank) {
        this.code = code;
        this.tankPassable = tankPassable;
        this.bulletPassable = bulletPassable;
        this.bulletReflect = bulletReflect;
        this.hideTank = hideTank;
    }

    public int getCode() {
        return code;
    }

    public boolean isTankPassable() {
        return tankPassable;
    }

    public boolean isBulletPassable() {
        return bulletPassable;
    }

    public boolean isBulletReflect() {
        return bulletReflect;
    }

    public boolean isHideTank() {
        return hideTank;
    }

    /**
     * 根据地图编码获取 TileType
     */
    public static TileType fromCode(int code) {
        for (TileType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return EMPTY;
    }
}