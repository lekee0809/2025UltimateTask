package map;
import java.util.*;

import infra.GameConfig;

/**
 * 野外大战场生成器 (Battlefield V3 - 丰满版)
 * * 优化重点：
 * 1. 解决空旷问题：增加小型掩体、散兵坑、废墟密度。
 * 2. 战术性增强：不再是大片空地，而是充满了可以躲避的障碍。
 */
public class BattlefieldMapGenerator {

    private int[][] map;
    private Random random = new Random();
    private static final int MAX_ATTEMPTS = 20;

    // 地形参数
    private static final int RIVER_WIDTH = 3;
    private static final int BRIDGE_INTERVAL = 15;

    public int[][] generate() {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            attempt++;

            // 1. 初始化：全是大平原
            map = new int[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
            for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
                Arrays.fill(map[r], GameConfig.TILE_EMPTY);
            }

            // 2. 生成主要地形
            generateContinuousRiver(); // 河流
            generateMountainRidges();  // 山脉

            // === 核心优化：填充空旷区域 ===
            generateRuinsClusters();   // 大型废墟 (数量增加)
            generateMiniBunkers();     // 新增：小型碉堡 (L型/U型掩体)
            generateScatteredDebris(); // 新增：零散掩体 (散兵坑)

            generateForests();         // 植被 (保持点缀)

            // 3. 清理出生点
            clearSafeZones();

            // 4. 连通性检查
            if (checkConnectivity()) {
                System.out.println("Battlefield V3 generated! (Attempt " + attempt + ")");
                return map;
            }
        }
        return map;
    }

    // ... (generateContinuousRiver, drawRiverSlice, buildBridge 保持不变，省略以节省空间) ...
    // 请直接保留原有的这三个河流相关方法代码
    private void generateContinuousRiver() {
        boolean isHorizontal = random.nextBoolean();
        if (isHorizontal) {
            int startY = random.nextInt(GameConfig.MAP_ROWS / 2) + (GameConfig.MAP_ROWS / 4);
            double phase = random.nextDouble() * Math.PI * 2;
            double frequency = 0.1 + random.nextDouble() * 0.1;
            for (int x = 0; x < GameConfig.MAP_COLS; x++) {
                int centerY = (int) (startY + Math.sin(x * frequency + phase) * 4);
                drawRiverSlice(centerY, x, true);
                if (x > 0 && x % BRIDGE_INTERVAL == 0 && x < GameConfig.MAP_COLS - 1) buildBridge(centerY, x, true);
            }
        } else {
            int startX = random.nextInt(GameConfig.MAP_COLS / 2) + (GameConfig.MAP_COLS / 4);
            double phase = random.nextDouble() * Math.PI * 2;
            double frequency = 0.1 + random.nextDouble() * 0.1;
            for (int y = 0; y < GameConfig.MAP_ROWS; y++) {
                int centerX = (int) (startX + Math.sin(y * frequency + phase) * 6);
                drawRiverSlice(y, centerX, false);
                if (y > 0 && y % BRIDGE_INTERVAL == 0 && y < GameConfig.MAP_ROWS - 1) buildBridge(y, centerX, false);
            }
        }
    }
    private void drawRiverSlice(int centerRow, int centerCol, boolean horizontal) {
        int halfWidth = RIVER_WIDTH / 2;
        if (horizontal) {
            for (int r = centerRow - halfWidth; r <= centerRow + halfWidth; r++) safeSet(r, centerCol, GameConfig.TILE_WATER);
        } else {
            for (int c = centerCol - halfWidth; c <= centerCol + halfWidth; c++) safeSet(centerRow, c, GameConfig.TILE_WATER);
        }
    }
    private void buildBridge(int r, int c, boolean horizontal) {
        if (horizontal) {
            for (int i = -RIVER_WIDTH; i <= RIVER_WIDTH; i++) for (int w = -1; w <= 1; w++) safeSet(r + i, c + w, GameConfig.TILE_EMPTY);
        } else {
            for (int i = -RIVER_WIDTH; i <= RIVER_WIDTH; i++) for (int w = -1; w <= 1; w++) safeSet(r + w, c + i, GameConfig.TILE_EMPTY);
        }
    }

    /**
     * 生成山脉 (数量略微增加，且更长)
     */
    private void generateMountainRidges() {
        int numRidges = 5 + random.nextInt(3); // 增加到 5-7 条

        for (int i = 0; i < numRidges; i++) {
            int r = random.nextInt(GameConfig.MAP_ROWS);
            int c = random.nextInt(GameConfig.MAP_COLS);
            int length = 8 + random.nextInt(10); // 更长
            int dr = random.nextInt(3) - 1;
            int dc = random.nextInt(3) - 1;
            if (dr == 0 && dc == 0) { dr = 1; dc = 0; }

            for (int step = 0; step < length; step++) {
                if (random.nextInt(10) < 3) r += random.nextInt(3) - 1;
                if (isValid(r, c) && !isNearWater(r, c)) {
                    map[r][c] = GameConfig.TILE_STONE;
                    // 30% 概率加厚山脉，更有立体感
                    if (random.nextInt(10) < 3) safeSet(r+1, c, GameConfig.TILE_STONE);
                }
                r += dr;
                c += dc;
            }
        }
    }

    /**
     * 生成废墟群 (数量增加，覆盖更广)
     */
    private void generateRuinsClusters() {
        int clusters = 12 + random.nextInt(6); // 增加到 12-18 个群落
        for (int i = 0; i < clusters; i++) {
            int r = random.nextInt(GameConfig.MAP_ROWS);
            int c = random.nextInt(GameConfig.MAP_COLS);
            int w = 2 + random.nextInt(4);
            int h = 2 + random.nextInt(4);

            for (int rr = r; rr < r + h; rr++) {
                for (int cc = c; cc < c + w; cc++) {
                    // 密度提高到 60%，让废墟更像废墟，而不是稀疏的几个点
                    if (isValid(rr, cc) && map[rr][cc] == GameConfig.TILE_EMPTY && !isNearWater(rr, cc)) {
                        if (random.nextInt(100) < 60) {
                            map[rr][cc] = GameConfig.TILE_BRICK;
                        }
                    }
                }
            }
        }
    }

    /**
     * ✅ [新增] 生成小型碉堡/掩体
     * 在空地上生成 L型、U型 或 小方块，提供战术掩护
     */
    private void generateMiniBunkers() {
        int bunkers = 8 + random.nextInt(5); // 生成 8-12 个小碉堡

        for (int i = 0; i < bunkers; i++) {
            int r = random.nextInt(GameConfig.MAP_ROWS - 2) + 1;
            int c = random.nextInt(GameConfig.MAP_COLS - 2) + 1;

            // 只有完全空旷的地方才生成碉堡
            if (isAreaEmpty(r, c, 3) && !isNearWater(r, c)) {
                int type = random.nextInt(3);

                // 类型 0: 横向掩体 (--)
                if (type == 0) {
                    map[r][c] = GameConfig.TILE_BRICK;
                    map[r][c+1] = GameConfig.TILE_BRICK;
                    map[r][c-1] = GameConfig.TILE_BRICK;
                }
                // 类型 1: 直角掩体 (L)
                else if (type == 1) {
                    map[r][c] = GameConfig.TILE_STONE; // 核心用铁
                    map[r+1][c] = GameConfig.TILE_BRICK;
                    map[r][c+1] = GameConfig.TILE_BRICK;
                }
                // 类型 2: 2x2 方块 (Box)
                else {
                    map[r][c] = GameConfig.TILE_BRICK;
                    map[r+1][c] = GameConfig.TILE_BRICK;
                    map[r][c+1] = GameConfig.TILE_BRICK;
                    map[r+1][c+1] = GameConfig.TILE_BRICK;
                }
            }
        }
    }

    /**
     * ✅ [新增] 散兵坑/零散掩体
     * 在地图上随机撒一些单块的石头或砖头，填补巨大的空白
     */
    private void generateScatteredDebris() {
        int debrisCount = 30 + random.nextInt(20); // 30-50 个零散点

        for (int i = 0; i < debrisCount; i++) {
            int r = random.nextInt(GameConfig.MAP_ROWS);
            int c = random.nextInt(GameConfig.MAP_COLS);

            if (isValid(r, c) && map[r][c] == GameConfig.TILE_EMPTY && !isNearWater(r, c)) {
                // 确保不要堵死路 (周围至少有2个空地)
                if (countEmptyNeighbors(r, c) >= 6) {
                    // 20% 是石头，80% 是砖头
                    map[r][c] = (random.nextInt(10) < 2) ? GameConfig.TILE_STONE : GameConfig.TILE_BRICK;
                }
            }
        }
    }

    /**
     * 植被 (保持较少)
     */
    private void generateForests() {
        int forests = 4;
        for (int i = 0; i < forests; i++) {
            int r = random.nextInt(GameConfig.MAP_ROWS);
            int c = random.nextInt(GameConfig.MAP_COLS);
            int radius = 2 + random.nextInt(2);
            for (int y = r - radius; y <= r + radius; y++) {
                for (int x = c - radius; x <= c + radius; x++) {
                    if ((y-r)*(y-r) + (x-c)*(x-c) <= radius*radius) {
                        if (isValid(y, x) && map[y][x] == GameConfig.TILE_EMPTY) {
                            if (random.nextInt(100) < 50) map[y][x] = GameConfig.TILE_GRASS;
                        }
                    }
                }
            }
        }
    }

    private void clearSafeZones() {
        clearArea(2, 2, 4); // 左上
        clearArea(GameConfig.MAP_ROWS - 3, GameConfig.MAP_COLS - 3, 4); // 右下
        clearArea(2, GameConfig.MAP_COLS - 3, 4); // 右上
    }

    private void clearArea(int r, int c, int size) {
        for (int i = -size/2; i <= size/2; i++) {
            for (int j = -size/2; j <= size/2; j++) {
                safeSet(r + i, c + j, GameConfig.TILE_EMPTY);
            }
        }
    }

    // 辅助：检查 3x3 区域是否全空 (用于放置碉堡)
    private boolean isAreaEmpty(int r, int c, int size) {
        for (int i = -size/2; i <= size/2; i++) {
            for (int j = -size/2; j <= size/2; j++) {
                if (!isValid(r+i, c+j) || map[r+i][c+j] != GameConfig.TILE_EMPTY) return false;
            }
        }
        return true;
    }

    // 辅助：计算周围空地数量
    private int countEmptyNeighbors(int r, int c) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (isValid(r+i, c+j) && map[r+i][c+j] == GameConfig.TILE_EMPTY) count++;
            }
        }
        return count;
    }

    private boolean isNearWater(int r, int c) {
        for(int i=-1; i<=1; i++) for(int j=-1; j<=1; j++)
            if(isValid(r+i, c+j) && map[r+i][c+j] == GameConfig.TILE_WATER) return true;
        return false;
    }

    private void safeSet(int r, int c, int type) {
        if (isValid(r, c)) map[r][c] = type;
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < GameConfig.MAP_ROWS && c >= 0 && c < GameConfig.MAP_COLS;
    }

    private boolean checkConnectivity() {
        // (保持原有的 BFS 逻辑不变，代码太长这里省略了，直接用你原来那个即可)
        // ... 请把原来的 checkConnectivity 代码贴在这里 ...

        // 为了方便直接运行，这里附上简版 BFS:
        boolean[][] visited = new boolean[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        Queue<int[]> queue = new LinkedList<>();
        int startR = -1, startC = -1, totalWalkable = 0;
        for(int r=0; r<GameConfig.MAP_ROWS; r++) {
            for(int c=0; c<GameConfig.MAP_COLS; c++) {
                if (map[r][c] != GameConfig.TILE_STONE && map[r][c] != GameConfig.TILE_WATER) {
                    totalWalkable++;
                    if(startR == -1) { startR = r; startC = c; }
                }
            }
        }
        if (startR == -1) return false;
        queue.add(new int[]{startR, startC});
        visited[startR][startC] = true;
        int visitedCount = 0;
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        while(!queue.isEmpty()) {
            int[] curr = queue.poll();
            visitedCount++;
            for(int[] d : dirs) {
                int nr = curr[0] + d[0];
                int nc = curr[1] + d[1];
                if(isValid(nr, nc) && !visited[nr][nc]) {
                    int type = map[nr][nc];
                    if (type != GameConfig.TILE_STONE && type != GameConfig.TILE_WATER) {
                        visited[nr][nc] = true;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return visitedCount >= totalWalkable * 0.95;
    }

    public int[][] getMap() {
        return map;
    }

    public void setMap(int[][] map) {
        this.map = map;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }
}