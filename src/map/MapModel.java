package map;
import model.Tile;
import model.TileType;
/**
 * 地图模型类（纯数据 + 规则）
 *
 * 职责：
 * 1. 维护 Tile 二维数组
 * 2. 根据像素坐标 / 格子坐标提供地形判断
 * 3. 处理子弹与地形的交互规则
 * 4. 提供草丛隐身判定
 *
 * 不负责：
 * - 渲染
 * - 坦克 / 子弹实体
 * - 道具系统（后续再加）
 */
public class MapModel {

    /** 地图行列 */
    private final int rows = MapConstants.MAP_ROWS;
    private final int cols = MapConstants.MAP_COLS;

    /** 地图格子 */
    private final Tile[][] tiles;

    /** 是否为闯关模式（影响草丛隐身） */
    private boolean campaignMode = true;

    public MapModel(int level) {
        tiles = new Tile[rows][cols];
        loadLevel(level);
    }

    /* ===================== 关卡加载 ===================== */

    private void loadLevel(int level) {
        int[][] data;

        switch (level) {
            case 2:
                data = LevelData.LEVEL_2;
                break;
            case 3:
                data = LevelData.LEVEL_3;
                break;
            default:
                data = LevelData.LEVEL_1;
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                TileType type = TileType.EMPTY;

                if (r < data.length && c < data[r].length) {
                    type = TileType.fromCode(data[r][c]);
                }

                tiles[r][c] = new Tile(r, c, type);
            }
        }
    }

    /* ===================== Tile 获取 ===================== */

    public Tile getTile(int row, int col) {
        if (row < 0 || col < 0 || row >= rows || col >= cols) {
            return null;
        }
        return tiles[row][col];
    }

    public Tile getTileByPixel(double x, double y) {
        int col = (int) (x / MapConstants.TILE_SIZE);
        int row = (int) (y / MapConstants.TILE_SIZE);
        return getTile(row, col);
    }

    /* ===================== 坦克规则 ===================== */

    public boolean canTankMove(double x, double y) {
        Tile tile = getTileByPixel(x, y);
        if (tile == null) return false;

        return tile.canTankPass();
    }

    public boolean isTankHidden(double x, double y) {
        if (!campaignMode) return false;

        Tile tile = getTileByPixel(x, y);
        return tile != null && tile.getType().isHideTank();
    }

    /* ===================== 子弹规则 ===================== */

    /**
     * @return true  子弹应销毁
     *         false 子弹继续飞行（或反弹）
     */
    public boolean handleBullet(double x, double y) {
        Tile tile = getTileByPixel(x, y);
        if (tile == null) return true;

        // 子弹可穿透
        if (tile.canBulletPass()) {
            return false;
        }

        // 子弹反弹（具体反弹方向由 Bullet 决定）
        if (tile.shouldBulletReflect()) {
            return false;
        }

        // 可破坏砖墙
        if (tile.getType() == TileType.BRICK && !tile.isDestroyed()) {
            tile.destroy();
            return true;
        }

        // 其他情况：子弹消失
        return true;
    }

    /* ===================== 控制接口 ===================== */

    public void setCampaignMode(boolean campaignMode) {
        this.campaignMode = campaignMode;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Tile[][] getTiles() {
        return tiles;
    }
}
