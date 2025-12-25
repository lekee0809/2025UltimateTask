
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MapGenerator V2 - "Battlefield Edition"
 * 特性：宽走廊、连续河流、自动连通性修复
 */
public class MapGenerator {

    private static final int MIN_NODE_SIZE = 8; // 节点稍微大一点
    private static final int CORRIDOR_WIDTH = 2; // ✅ 走廊宽度：2格
    private int[][] map;
    private Random random = new Random();

    public int[][] generateLevel() {
        // 1. 初始化全为铁墙
        map = new int[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            Arrays.fill(map[r], GameConfig.TILE_STONE);
        }

        // 2. BSP 分割与房间生成
        Leaf root = new Leaf(1, 1, GameConfig.MAP_COLS - 2, GameConfig.MAP_ROWS - 2);
        List<Leaf> leaves = new ArrayList<>();
        leaves.add(root);

        boolean didSplit = true;
        while (didSplit) {
            didSplit = false;
            List<Leaf> nextLeaves = new ArrayList<>();
            for (Leaf l : leaves) {
                if (l.leftChild == null && l.rightChild == null) {
                    // 只有当足够大时才分割
                    if (l.width > MIN_NODE_SIZE * 2 || l.height > MIN_NODE_SIZE * 2) {
                        if (l.split()) {
                            nextLeaves.add(l.leftChild);
                            nextLeaves.add(l.rightChild);
                            didSplit = true;
                        }
                    }
                }
            }
            leaves.addAll(nextLeaves);
        }

        // 3. 生成房间 (现在房间会很大，几乎填满节点)
        root.createRooms(map);

        // 4. 生成宽走廊
        root.createCorridors(map);

        // 5. 生成连续河流 (只会生成 1-2 条)
        createContinuousRivers();

        // 6. 装饰草地 (少量)
        decorateGrass();

        // 7. ✅ 核心算法：检查连通性并自动修补
        // 确保从 (1,1) 可以到达所有空地。如果被河截断，自动造桥。
        ensureConnectivity(1, 1);

        return map;
    }

    /**
     * 生成连续的河流 (随机游走算法)
     */
    private void createContinuousRivers() {
        int riverCount = random.nextInt(2) + 1; // 1 到 2 条河
        for (int i = 0; i < riverCount; i++) {
            // 随机选一个起点（左边或上边）
            int startX = random.nextInt(GameConfig.MAP_COLS - 4) + 2;
            int startY = random.nextInt(GameConfig.MAP_ROWS - 4) + 2;

            // 河流长度
            int length = 20 + random.nextInt(30);

            int currX = startX;
            int currY = startY;

            for (int step = 0; step < length; step++) {
                // 边界检查
                if (currX > 1 && currX < GameConfig.MAP_COLS - 2 &&
                        currY > 1 && currY < GameConfig.MAP_ROWS - 2) {

                    // 只有当这里本来是空地或者砖块时，才变成水
                    // (不要破坏铁墙边界)
                    if (map[currY][currX] != GameConfig.TILE_STONE) {
                        map[currY][currX] = GameConfig.TILE_WATER;

                        // 河流稍微宽一点点 (偶尔加粗)
                        if (random.nextBoolean()) {
                            if (map[currY][currX+1] != GameConfig.TILE_STONE)
                                map[currY][currX+1] = GameConfig.TILE_WATER;
                        }
                    }
                }

                // 随机游走：偏向一个方向移动，但也可能拐弯
                int dir = random.nextInt(4);
                // 0:右, 1:下, 2:左, 3:上
                // 增加惯性：让河流倾向于向右下流
                if (random.nextInt(100) < 40) currX++;
                else if (random.nextInt(100) < 40) currY++;
                else {
                    if (dir == 0) currX++;
                    else if (dir == 1) currY++;
                    else if (dir == 2) currX--;
                    else currY--;
                }
            }
        }
    }

    private void decorateGrass() {
        for (int r = 1; r < GameConfig.MAP_ROWS - 1; r++) {
            for (int c = 1; c < GameConfig.MAP_COLS - 1; c++) {
                if (map[r][c] == GameConfig.TILE_EMPTY) {
                    // 只有 5% 概率生成草 (之前是15%)
                    if (random.nextInt(100) < 5) {
                        map[r][c] = GameConfig.TILE_GRASS;
                        // 连带效应：草通常是成片的，周围也长一点
                        if (c + 1 < GameConfig.MAP_COLS - 1 && random.nextBoolean())
                            map[r][c+1] = GameConfig.TILE_GRASS;
                    }
                }
            }
        }
    }

    /**
     * ✅ 洪水填充算法检查连通性
     * 如果发现有不可达的区域，就把阻碍的水填成土
     */
    private void ensureConnectivity(int startX, int startY) {
        // 1. 找出所有非墙的可行走区域 (包括水，因为水是我们要检查的阻断源)
        // 实际上我们要检查的是：玩家(只能走空地/草/砖)能否到达所有空地

        boolean[][] visited = new boolean[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        Queue<Point> queue = new LinkedList<>();

        // 强制起点安全
        map[startY][startX] = GameConfig.TILE_EMPTY;
        queue.add(new Point(startX, startY));
        visited[startY][startX] = true;

        // BFS 遍历所有能走到的地方
        while (!queue.isEmpty()) {
            Point p = queue.poll();

            // 四个方向
            int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];

                // 越界检查
                if (nx < 0 || nx >= GameConfig.MAP_COLS || ny < 0 || ny >= GameConfig.MAP_ROWS) continue;

                // 如果已经访问过，跳过
                if (visited[ny][nx]) continue;

                // 如果是铁墙，绝对过不去
                if (map[ny][nx] == GameConfig.TILE_STONE) continue;

                // 如果是水，它是阻断源，暂时不能通过（除非我们正在造桥）
                // 我们这里的 BFS 是模拟坦克的移动，所以遇水则停
                if (map[ny][nx] == GameConfig.TILE_WATER) {
                    // 记录一下这块水是边界，但坦克过不去
                    continue;
                }

                // 是路，加入队列
                visited[ny][nx] = true;
                queue.add(new Point(nx, ny));
            }
        }

        // 2. 检查是否有“孤岛”
        // 遍历全图，如果发现某个地方不是墙也不是水，但是 visited 是 false，说明去不了
        for (int r = 1; r < GameConfig.MAP_ROWS - 1; r++) {
            for (int c = 1; c < GameConfig.MAP_COLS - 1; c++) {
                int tile = map[r][c];
                // 如果这是一个应该能到达的地方（空地/砖/草），但没访问到
                if ((tile == GameConfig.TILE_EMPTY || tile == GameConfig.TILE_BRICK || tile == GameConfig.TILE_GRASS)
                        && !visited[r][c]) {

                    // 发现孤岛！执行“架桥手术”
                    // 简单策略：向四周寻找最近的 visited 点，把中间的水填平
                    buildBridgeTo(c, r, visited);
                }
            }
        }
    }

    /**
     * 暴力修路：从孤岛向某个方向钻孔，直到连通
     */
    private void buildBridgeTo(int targetX, int targetY, boolean[][] visited) {
        int x = targetX;
        int y = targetY;

        // 简单的向左上角（或者出生点方向）挖掘，直到遇到已探索区域
        // 这里简化为：向 (1,1) 方向直线挖掘
        while (x > 1 && y > 1) {
            if (visited[y][x]) return; // 挖通了！

            // 如果遇到水，填成土
            if (map[y][x] == GameConfig.TILE_WATER) {
                map[y][x] = GameConfig.TILE_EMPTY;
            }
            // 如果遇到石头，打穿
            else if (map[y][x] == GameConfig.TILE_STONE) {
                map[y][x] = GameConfig.TILE_BRICK; // 变成可破坏的墙
            }

            // 标记为已联通，避免重复计算
            visited[y][x] = true;

            x--;
            y--;
        }
    }

    // ================== BSP 内部类 ==================
    private static class Leaf {
        public int x, y, width, height;
        public Leaf leftChild, rightChild;
        public Rectangle room;

        public Leaf(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean split() {
            if (leftChild != null || rightChild != null) return false;
            boolean splitH = ThreadLocalRandom.current().nextBoolean();
            if (width > height && width / (double) height >= 1.25) splitH = false;
            else if (height > width && height / (double) width >= 1.25) splitH = true;

            int max = (splitH ? height : width) - MIN_NODE_SIZE;
            if (max <= MIN_NODE_SIZE) return false;

            int splitPos = ThreadLocalRandom.current().nextInt(MIN_NODE_SIZE, max);

            if (splitH) {
                leftChild = new Leaf(x, y, width, splitPos);
                rightChild = new Leaf(x, y + splitPos, width, height - splitPos);
            } else {
                leftChild = new Leaf(x, y, splitPos, height);
                rightChild = new Leaf(x + splitPos, y, width - splitPos, height);
            }
            return true;
        }

        public void createRooms(int[][] map) {
            if (leftChild != null || rightChild != null) {
                if (leftChild != null) leftChild.createRooms(map);
                if (rightChild != null) rightChild.createRooms(map);
            } else {
                // 1. 确定房间范围
                int padding = 0;
                int roomW = width - (padding * 2);
                int roomH = height - (padding * 2);
                int roomX = x + padding;
                int roomY = y + padding;

                this.room = new Rectangle(roomX, roomY, roomW, roomH);

                // 2. 挖空房间并进行“室内装修”
                for (int r = roomY; r < roomY + roomH; r++) {
                    for (int c = roomX; c < roomX + roomW; c++) {
                        if(r > 0 && r < GameConfig.MAP_ROWS - 1 && c > 0 && c < GameConfig.MAP_COLS - 1) {
                            // 先默认挖空
                            map[r][c] = GameConfig.TILE_EMPTY;

                            // === 🧱 新增：撒砖头逻辑 ===
                            // 不在边缘(保留走位)，也不在房间正中心(防止出生点卡死或路堵死)
                            boolean isEdge = (r == roomY || r == roomY + roomH - 1 || c == roomX || c == roomX + roomW - 1);
                            boolean isCenter = (Math.abs(r - room.centerY()) < 2 && Math.abs(c - room.centerX()) < 2);

                            if (!isEdge && !isCenter) {
                                // 15% 概率放砖头作为掩体
                                if (ThreadLocalRandom.current().nextInt(100) < 15) {
                                    map[r][c] = GameConfig.TILE_BRICK;
                                }
                                // 2% 概率放个铁墩子(加强掩体)
                                else if (ThreadLocalRandom.current().nextInt(100) < 2) {
                                    map[r][c] = GameConfig.TILE_STONE;
                                }
                            }
                        }
                    }
                }
            }
        }

        public Rectangle getRoom() {
            if (room != null) return room;
            Leaf l = null;
            if (leftChild != null && rightChild != null) {
                l = ThreadLocalRandom.current().nextBoolean() ? leftChild : rightChild;
            } else if (leftChild != null) l = leftChild;
            else if (rightChild != null) l = rightChild;
            return (l != null) ? l.getRoom() : null;
        }

        public void createCorridors(int[][] map) {
            if (leftChild != null && rightChild != null) {
                leftChild.createCorridors(map);
                rightChild.createCorridors(map);
                Rectangle lRoom = leftChild.getRoom();
                Rectangle rRoom = rightChild.getRoom();
                if (lRoom != null && rRoom != null) {
                    drawWidePath(map, lRoom.centerX(), lRoom.centerY(), rRoom.centerX(), rRoom.centerY());
                }
            }
        }

        // ✅ 宽走廊绘制算法
        private void drawWidePath(int[][] map, int x1, int y1, int x2, int y2) {
            int startX = Math.min(x1, x2);
            int endX = Math.max(x1, x2);
            int startY = Math.min(y1, y2);
            int endY = Math.max(y1, y2);

            // 水平
            for (int x = startX; x <= endX; x++) {
                carve(map, y1, x);
            }
            // 垂直
            for (int y = startY; y <= endY; y++) {
                carve(map, y, x2);
            }
        }

        // 雕刻函数：支持宽笔刷
        // 雕刻函数：支持宽笔刷
        private void carve(int[][] map, int r, int c) {
            for (int i = 0; i < CORRIDOR_WIDTH; i++) {
                for (int j = 0; j < CORRIDOR_WIDTH; j++) {
                    int nr = r + i;
                    int nc = c + j;
                    if (nr > 0 && nr < GameConfig.MAP_ROWS - 1 && nc > 0 && nc < GameConfig.MAP_COLS - 1) {
                        // 如果原来是实心铁墙，我们在打洞
                        if (map[nr][nc] == GameConfig.TILE_STONE) {
                            // === 🧱 修改：提升走廊路障概率 ===
                            // 30% 概率变成砖墙 (之前是 20%)，增加巷战复杂度
                            if (ThreadLocalRandom.current().nextInt(100) < 30) {
                                map[nr][nc] = GameConfig.TILE_BRICK;
                            } else {
                                map[nr][nc] = GameConfig.TILE_EMPTY;
                            }
                        } else {
                            // 如果本来就是空地或者砖头(房间内部)，保持原样，不要把房间里的砖头铲平了！
                            // 除非它是水（修桥逻辑在外面处理），或者我们想强制打通
                            // 这里改为：只有遇到水才填平，遇到房间里的砖头(BRICK)则保留，遇到空地保留
                            if (map[nr][nc] != GameConfig.TILE_BRICK) {
                                map[nr][nc] = GameConfig.TILE_EMPTY;
                            }
                        }
                    }
                }
            }
        }
    }

    private static class Rectangle {
        int x, y, w, h;
        Rectangle(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        int centerX() { return x + w / 2; }
        int centerY() { return y + h / 2; }
    }

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }
}