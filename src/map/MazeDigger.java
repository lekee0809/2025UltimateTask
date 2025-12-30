package map;
import infra.GameConfig;

import java.util.*;

/**
 * 通用迷宫生成器
 * 支持：分辨率缩放 (1x1 或 2x2)、部分回路、绝对连通
 */
public class MazeDigger {

    // 逻辑网格 (可能比实际地图小)
    private int logicRows;
    private int logicCols;
    private boolean[][] visited;

    // 渲染结果
    private int[][] map;

    // 🔨 核心参数

    private int blockSize; // 1 = 细路(1格宽), 2 = 宽路(2格宽)
    private static final double LOOP_CHANCE = 0.05; // 5% 的几率打通死路形成回路

    private static final double STRAIGHT_BLOCK_CHANCE = 0.1; // 直道阻断概率
    private static final double INTERSECTION_BLOCK_CHANCE = 0.2; // 路口碉堡概率

    public MazeDigger() {
        // 默认随机：50% 概率生成宽路，50% 概率生成细路
        this.blockSize = Math.random() < 0.5 ? 2 : 1;
    }

    // 允许外部强制指定模式 (例如: new MazeDigger(2))
    public MazeDigger(int blockSize) {
        this.blockSize = blockSize;
    }

    public int[][] generate() {
        // 1. 计算逻辑网格大小
        // 如果是 2x2 模式，逻辑网格就是 30/2=15列, 22/2=11行
        this.logicRows = GameConfig.MAP_ROWS / blockSize;
        this.logicCols = GameConfig.MAP_COLS / blockSize;

        this.map = new int[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        this.visited = new boolean[logicRows][logicCols];

        // 2. 初始化：全填满石头
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            Arrays.fill(map[r], GameConfig.TILE_STONE);
        }

        // 3. 选一个起点 (逻辑坐标)
        // 必须是奇数 (1,1) 才能保证四周有墙
        // 注意：这里使用的是逻辑坐标系
        int startR = 1;
        int startC = 1;

        // 4. 开始递归挖掘 (DFS)
        dig(startR, startC);


        if(blockSize == 2){
            decorator(map);
        }

        // 5. 强制清理出生点 (物理坐标)
        // 无论迷宫怎么生成，确保左上角能站人
        clearSafeZone(1, 1); // 玩家
        clearSafeZone(1, GameConfig.MAP_COLS - 2); // 敌人

        System.out.println("Maze Generated! BlockSize: " + blockSize + "x" + blockSize);
        return map;
    }

    /**
     * 递归回溯挖掘算法
     * @param r 逻辑行
     * @param c 逻辑列
     */
    private void dig(int r, int c) {
        // 标记已访问
        visited[r][c] = true;

        // 🏗️ 渲染：在实际地图上挖坑
        carve(r, c);

        // 定义四个方向 (上, 下, 左, 右) - 步长为 2 (跨过墙壁)
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};

        // 随机打乱方向 (让迷宫扭曲的关键)
        List<int[]> dirList = Arrays.asList(dirs);
        Collections.shuffle(dirList);

        for (int[] d : dirList) {
            int nextR = r + d[0];
            int nextC = c + d[1];

            // 1. 越界检查 (逻辑坐标系)
            if (nextR > 0 && nextR < logicRows - 1 && nextC > 0 && nextC < logicCols - 1) {

                // 2. 如果没去过 -> 正常打通
                if (!visited[nextR][nextC]) {
                    // 打通中间的墙
                    int wallR = r + d[0] / 2;
                    int wallC = c + d[1] / 2;
                    carve(wallR, wallC);

                    // 递归进入下一格
                    dig(nextR, nextC);
                }
                // 3. ⭐ 关键：如果去过了 -> 只有 5% 概率打通 (形成回路！)
                else if (Math.random() < LOOP_CHANCE) {
                    // 打通中间的墙，但**不要**递归进去 (否则会死循环)
                    int wallR = r + d[0] / 2;
                    int wallC = c + d[1] / 2;

                    // 只有当这堵墙还没被打通时才打通 (防止重复)
                    if (!isCarved(wallR, wallC)) {
                        carve(wallR, wallC);
                    }
                }
            }
        }
    }

    private void decorator(int[][] map) {
        // 遍历所有逻辑节点 (避开最外层边缘，防止越界)
        for (int r = 1; r < logicRows - 1; r++) {
            for (int c = 1; c < logicCols - 1; c++) {

                // 如果这个逻辑格子本身是实心墙，跳过
                if (!isSpotWalkable(r, c)) continue;

                // 分析连接情况：上下左右是否有路连通？
                boolean u = isPathConnected(r, c, -1, 0);
                boolean d = isPathConnected(r, c, 1, 0);
                boolean l = isPathConnected(r, c, 0, -1);
                boolean right = isPathConnected(r, c, 0, 1);

                int connections = (u?1:0) + (d?1:0) + (l?1:0) + (right?1:0);

                // === 场景 A: 直线路段 (2个连接，且相对) ===
                if (connections == 2) {
                    // 竖向直道 (|) -> 生成水平阻断 (H_BAR)
                    if (u && d) {
                        if (Math.random() < STRAIGHT_BLOCK_CHANCE) {
                            placePattern(r, c, "H_BAR");
                        }
                    }
                    // 横向直道 (-) -> 生成垂直阻断 (V_BAR)
                    else if (l && right) {
                        if (Math.random() < STRAIGHT_BLOCK_CHANCE) {
                            placePattern(r, c, "V_BAR");
                        }
                    }
                }

                // === 场景 B: 路口 (T型 或 十字) -> 生成碉堡 ===
                else if (connections >= 3) {
                    if (Math.random() < INTERSECTION_BLOCK_CHANCE) {
                        placePattern(r, c, "FULL");
                    }
                }
            }
        }
    }

    /**
     * 辅助：放置特定形状的砖块
     */
    private void placePattern(int logicR, int logicC, String type) {
        int startY = logicR * blockSize;
        int startX = logicC * blockSize;

        if (type.equals("FULL")) {
            // 2x2 全填满 (碉堡) - 堵死路口，强迫绕路或开炮
            for (int i = 0; i < blockSize; i++)
                for (int j = 0; j < blockSize; j++)
                    map[startY + i][startX + j] = GameConfig.TILE_BRICK;
        }
        else if (type.equals("H_BAR")) {
            // 2x1 横条 (阻断竖向路)
            // 在 2x2 的区域里，只填满上面一行 (y)，留出下面一行 (y+1)
            // 这样形成 1格厚 的横向掩体
            for (int j = 0; j < blockSize; j++) {
                map[startY][startX + j] = GameConfig.TILE_BRICK;
            }
        }
        else if (type.equals("V_BAR")) {
            // 1x2 竖条 (阻断横向路)
            // 在 2x2 的区域里，只填满左边一列 (x)，留出右边一列 (x+1)
            for (int i = 0; i < blockSize; i++) {
                map[startY + i][startX] = GameConfig.TILE_BRICK;
            }
        }
    }

    /**
     * 辅助：检查逻辑坐标 (r,c) 和 (r+dr, c+dc) 之间是否被打通了
     * 原理：检查它们中间那堵墙的物理像素是不是 STONE
     */
    private boolean isPathConnected(int r, int c, int dr, int dc) {
        int checkR = r + dr;
        int checkC = c + dc;

        // 1. 目标越界或不可走，肯定不通
        if (!isSpotWalkable(checkR, checkC)) return false;

        // 2. 检查中间的墙壁像素位置
        // 如果 block=2:
        // 向下(dr=1): 墙在 [r*2 + 2][c*2]
        // 向右(dc=1): 墙在 [r*2][c*2 + 2]
        int wallPixelR = (r * blockSize) + (dr == 1 ? blockSize : (dr == -1 ? -1 : 0));
        if (dr == 0) wallPixelR = r * blockSize; // 如果是横向检查，y坐标不变(取左上角)

        int wallPixelC = (c * blockSize) + (dc == 1 ? blockSize : (dc == -1 ? -1 : 0));
        if (dc == 0) wallPixelC = c * blockSize; // 如果是纵向检查，x坐标不变

        // 修正逻辑：采样点必须在地图内
        if (wallPixelR >= 0 && wallPixelR < GameConfig.MAP_ROWS && wallPixelC >= 0 && wallPixelC < GameConfig.MAP_COLS) {
            // 只要采样点不是石头，就说明墙被打通了
            return map[wallPixelR][wallPixelC] != GameConfig.TILE_STONE;
        }
        return false;
    }

    // 辅助：判断逻辑格是否在范围内且已被挖空
    private boolean isSpotWalkable(int r, int c) {
        // 边界检查
        if (r <= 0 || r >= logicRows - 1 || c <= 0 || c >= logicCols - 1) return false;

        // 检查实际物理像素是否为空 (取左上角采样)
        return map[r * blockSize][c * blockSize] != GameConfig.TILE_STONE;
    }

    /**
     * 雕刻刀：将逻辑坐标 (r, c) 映射到 物理地图 并挖空
     */
    private void carve(int logicR, int logicC) {
        // 转换逻辑坐标 -> 物理坐标
        int startY = logicR * blockSize;
        int startX = logicC * blockSize;

        // 根据 blockSize 填充区域
        for (int i = 0; i < blockSize; i++) {
            for (int j = 0; j < blockSize; j++) {
                int y = startY + i;
                int x = startX + j;

                // 安全检查
                if (y < GameConfig.MAP_ROWS && x < GameConfig.MAP_COLS) {
                    map[y][x] = GameConfig.TILE_EMPTY;
                }
            }
        }
    }

    // 辅助：检查物理地图上这个位置是不是已经被挖过了
    private boolean isCarved(int logicR, int logicC) {
        int y = logicR * blockSize;
        int x = logicC * blockSize;
        return map[y][x] != GameConfig.TILE_STONE;
    }



    // 清理出生点 (确保 3x3 区域无墙)
    private void clearSafeZone(int r, int c) {
        for(int i=-1; i<=1; i++) {
            for(int j=-1; j<=1; j++) {
                int nr = r + i;
                int nc = c + j;
                if(nr > 0 && nr < GameConfig.MAP_ROWS-1 && nc > 0 && nc < GameConfig.MAP_COLS-1) {
                    map[nr][nc] = GameConfig.TILE_EMPTY;
                }
            }
        }
    }

    public int getLogicRows() {
        return logicRows;
    }

    public void setLogicRows(int logicRows) {
        this.logicRows = logicRows;
    }

    public int getLogicCols() {
        return logicCols;
    }

    public void setLogicCols(int logicCols) {
        this.logicCols = logicCols;
    }

    public boolean[][] getVisited() {
        return visited;
    }

    public void setVisited(boolean[][] visited) {
        this.visited = visited;
    }

    public int[][] getMap() {
        return map;
    }

    public void setMap(int[][] map) {
        this.map = map;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }
}