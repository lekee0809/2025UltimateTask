package map;

import infra.GameConfig; // ✅ 核心修改：统一使用 GameConfig，不再使用 MapConstants
import model.Tile;
import model.TileType;

/**
 * 地图模型类（兼容 闯关模式 + 无尽模式）
 */
public class MapModel {

    public static final int LEVEL_1 = 1;
    public static final int LEVEL_2 = 2;
    public static final int LEVEL_3 = 3;

    // ✅ 修改点：统一使用 GameConfig 的尺寸，确保和工厂生成的地图大小一致
    private final int rows = GameConfig.MAP_ROWS;
    private final int cols = GameConfig.MAP_COLS;

    /** 地图格子 */
    private final Tile[][] tiles;

    /** 是否为闯关模式（影响草丛隐身） */
    private boolean campaignMode = true;

    // ==========================================
    // 构造函数 (兼容两种模式)
    // ==========================================

    /** * 构造函数 1：闯关模式专用
     * 读取 LevelData 静态数据
     */
    public MapModel(int level) {
        tiles = new Tile[rows][cols];
        loadLevel(level); // ✅ 保留你原有的加载逻辑
    }

    /** * 构造函数 2：无尽模式专用
     * 接收 MapFactory 生成的动态数据
     */
    public MapModel(int[][] data) {
        tiles = new Tile[rows][cols];
        // 直接使用传入的 data 填充
        initFromData(data);
    }

    // ==========================================
    // 关卡加载逻辑 (保持不变)
    // ==========================================

    /** 从 LevelData 加载 (闯关模式) */
    private void loadLevel(int level) {
        int[][] data;
        switch (level) {
            case 2:
                // 请确保 LevelData 类存在且可访问
                data = LevelData.LEVEL_2;
                break;
            case 3:
                data = LevelData.LEVEL_3;
                break;
            default:
                data = LevelData.LEVEL_1;
        }
        initFromData(data);
    }

    /** 通用填充逻辑 (提取出来避免重复) */
    private void initFromData(int[][] data) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                TileType type = TileType.EMPTY;

                // 安全检查
                if (data != null && r < data.length && c < data[r].length) {
                    type = TileType.fromCode(data[r][c]);
                }
                tiles[r][c] = new Tile(r, c, type);
            }
        }
    }

    // ==========================================
    // Tile 获取 (修正像素计算)
    // ==========================================

    public Tile getTile(int row, int col) {
        if (row < 0 || col < 0 || row >= rows || col >= cols) {
            return null;
        }
        return tiles[row][col];
    }

    public Tile getTileByPixel(double x, double y) {
        // ✅ 修改点：必须除以 GameConfig.GRID_SIZE (40.0)
        // 如果用 MapConstants.TILE_SIZE (可能是34.0)，坐标判断会错乱
        int col = (int) (x / GameConfig.GRID_SIZE);
        int row = (int) (y / GameConfig.GRID_SIZE);
        return getTile(row, col);
    }

    // ==========================================
    // 坦克与子弹规则 (保持不变)
    // ==========================================

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

    public boolean handleBullet(double x, double y) {
        Tile tile = getTileByPixel(x, y);
        if (tile == null) return true;

        if (tile.canBulletPass()) return false;
        if (tile.shouldBulletReflect()) return false;

        if (tile.getType() == TileType.BRICK && !tile.isDestroyed()) {
            tile.destroy();
            return true;
        }
        return true;
    }

    // ==========================================
    // 控制接口
    // ==========================================

    public void reset(int level) {
        loadLevel(level);
    }

    public void setCampaignMode(boolean campaignMode) {
        this.campaignMode = campaignMode;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public Tile[][] getTiles() { return tiles; }
}