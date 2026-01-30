package map;
import infra.GameConfig;

/**
 * 地图子系统常量定义
 *
 * 说明：
 * 1. 所有地图尺寸、格点规则均来源于 GameConfig
 * 2. 本类不重新定义“事实”，只做地图层的语义封装
 *
 * 设计目的：
 * - 避免地图模块直接散落使用 GameConfig
 * - 为后期地图规则扩展提供统一入口
 */
public class MapConstants {
    /** 单个地图格子的像素尺寸 */
    public static final int TILE_SIZE = (int) GameConfig.GRID_SIZE;
    /** 地图行数 */
    public static final int MAP_ROWS = GameConfig.MAP_ROWS;
    /** 地图列数 */
    public static final int MAP_COLS = GameConfig.MAP_COLS;
    /** 地图总宽度（像素） */
    public static final int MAP_PIXEL_WIDTH = MAP_COLS * TILE_SIZE;
    /** 地图总高度（像素） */
    public static final int MAP_PIXEL_HEIGHT = MAP_ROWS * TILE_SIZE;
    /** 私有构造，禁止实例化 */
    private MapConstants() {}
}
