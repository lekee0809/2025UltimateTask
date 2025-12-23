
/**
 * 地图中的单个格子对象
 * Tile 是地图运行时的最小单位，
 * 负责保存：
 * 1. 自身位置
 * 2. 地形类型
 * 3. 状态信息（如是否被破坏）
 */
public class Tile {

    /** 行索引 */
    private final int row;

    /** 列索引 */
    private final int col;

    /** 地形类型 */
    private TileType type;

    /** 是否被破坏 */
    private boolean destroyed;

    public Tile(int row, int col, TileType type) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.destroyed = false;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public TileType getType() {
        return type;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * 标记该格子被破坏
     */
    public void destroy() {
        this.destroyed = true;
        this.type = TileType.EMPTY;
    }

    /**
     * 判断坦克是否可通过该格子
     */
    public boolean canTankPass() {
        return type.isTankPassable();
    }

    /**
     * 判断子弹是否可通过该格子
     */
    public boolean canBulletPass() {
        return type.isBulletPassable();
    }

    /**
     * 判断子弹是否会在该格子发生反弹
     */
    public boolean shouldBulletReflect() {
        return type.isBulletReflect();
    }
}
