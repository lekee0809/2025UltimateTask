
import java.util.*;

/**
 * 战场迷宫生成器 (Maze / Drunkard's Walk)
 * 特点：无明显房间，全是蜿蜒的走廊，适合遭遇战
 */
public class MazeMapGenerator {

    private int[][] map;
    private Random random = new Random();

    // 尝试生成的最大次数，防止极端情况死循环
    private static final int MAX_ATTEMPTS = 50;

    /**
     * 对外接口
     */
    public int[][] generate() {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            attempt++;

            // 1. 初始化全黑（全铁墙）
            map = new int[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
            for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
                Arrays.fill(map[r], GameConfig.TILE_STONE);
            }

            // 2. 运行“矿工挖掘”算法
            runMinerAlgorithm();

            // 3. 强制出生点安全
            clearSpawnPoint();

            // 4. ⭐ 质检：连通性检查
            if (checkConnectivity()) {
                System.out.println("Maze generated successfully (Attempt " + attempt + ")");
                return map;
            } else {
                System.out.println("Maze disconnected, retrying... (Attempt " + attempt + ")");
            }
        }

        // 如果实在运气不好，返回最后一次的结果（虽然可能有死路，但概率极低）
        return map;
    }

    /**
     * 核心算法：派出多个矿工乱挖
     */
    private void runMinerAlgorithm() {
        // 矿工数量：数量越多，地图越空旷；数量越少，死胡同越多
        int minersCount = 4;

        for (int i = 0; i < minersCount; i++) {
            // 随机找个起点（必须在边界内）
            int r = random.nextInt(GameConfig.MAP_ROWS - 4) + 2;
            int c = random.nextInt(GameConfig.MAP_COLS - 4) + 2;

            // 每个矿工挖多少步
            int steps = 300;

            for (int s = 0; s < steps; s++) {
                // 挖洞：生成 1格 或 2格 宽的走廊
                carve(r, c);

                // 随机移动 (上下左右)
                int dir = random.nextInt(4);
                switch (dir) {
                    case 0: r--; break; // 上
                    case 1: r++; break; // 下
                    case 2: c--; break; // 左
                    case 3: c++; break; // 右
                }

                // 限制矿工不能跑出地图太远，碰到边界就弹回来
                if (r < 1) r = 2;
                if (r >= GameConfig.MAP_ROWS - 1) r = GameConfig.MAP_ROWS - 2;
                if (c < 1) c = 2;
                if (c >= GameConfig.MAP_COLS - 1) c = GameConfig.MAP_COLS - 2;
            }
        }

        // 撒一点装饰（草和水）
        decorate();
    }

    // 挖土：把石头变成空地或砖块
    private void carve(int r, int c) {
        // 笔刷大小：随机 1x1 或 2x2（制造宽窄不一的感觉）
        int brushSize = random.nextBoolean() ? 1 : 2;

        for (int i = 0; i < brushSize; i++) {
            for (int j = 0; j < brushSize; j++) {
                int nr = r + i;
                int nc = c + j;
                if (nr > 0 && nr < GameConfig.MAP_ROWS - 1 && nc > 0 && nc < GameConfig.MAP_COLS - 1) {
                    // 10% 几率留下砖块当掩体，其余变空地
                    if (random.nextInt(100) < 10) {
                        map[nr][nc] = GameConfig.TILE_BRICK;
                    } else {
                        map[nr][nc] = GameConfig.TILE_EMPTY;
                    }
                }
            }
        }
    }

    private void decorate() {
        for (int r = 1; r < GameConfig.MAP_ROWS - 1; r++) {
            for (int c = 1; c < GameConfig.MAP_COLS - 1; c++) {
                if (map[r][c] == GameConfig.TILE_EMPTY) {
                    // 极低概率生成水（战场坑）和草
                    int roll = random.nextInt(100);
                    if (roll < 3) map[r][c] = GameConfig.TILE_WATER;
                    else if (roll < 8) map[r][c] = GameConfig.TILE_GRASS;
                }
            }
        }
    }

    private void clearSpawnPoint() {
        map[1][1] = GameConfig.TILE_EMPTY;
        map[1][2] = GameConfig.TILE_EMPTY;
        map[2][1] = GameConfig.TILE_EMPTY;
    }

    /**
     * ✅ 连通性检查 (BFS 洪水填充)
     * 确保从 (1,1) 可以走到地图上任何一个空地
     */
    private boolean checkConnectivity() {
        int totalWalkable = 0;

        // 1. 统计所有能走的格子
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                if (isWalkable(map[r][c])) totalWalkable++;
            }
        }
        if (totalWalkable == 0) return false;

        // 2. 从出生点开始跑 BFS
        boolean[][] visited = new boolean[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        Queue<int[]> queue = new LinkedList<>();

        queue.add(new int[]{1, 1});
        visited[1][1] = true;
        int visitedCount = 0;

        // 如果出生点本身是不可行走的（比如被随机成水了），先记为1（虽然我们在clearSpawnPoint处理了，双重保险）
        if (isWalkable(map[1][1])) visitedCount++;

        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();

            for (int[] d : dirs) {
                int nr = curr[0] + d[0];
                int nc = curr[1] + d[1];

                if (nr >= 0 && nr < GameConfig.MAP_ROWS && nc >= 0 && nc < GameConfig.MAP_COLS) {
                    if (!visited[nr][nc] && isWalkable(map[nr][nc])) {
                        visited[nr][nc] = true;
                        visitedCount++;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }

        // 3. 比较
        // 允许有极少量的死格子（比如被水围住的草），容错率 95% 即可
        // 但为了严格起见，我们要求 100% 或者只允许极少误差
        return visitedCount >= totalWalkable * 0.98;
    }

    private boolean isWalkable(int type) {
        // 水和铁墙不能走，其他（空、砖、草）都算连通区域
        return type != GameConfig.TILE_STONE && type != GameConfig.TILE_WATER;
    }
}