import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 严谨版 BSP 地图生成器
 * 特点：走廊横平竖直，绝对干净（无杂物），且 100% 连通
 */
public class StrictBSPMapGenerator {

    private static final int MIN_NODE_SIZE = 8; // 分割单元尽量大一点，保证房间宽敞
    private static final int CORRIDOR_WIDTH = 2; // ✅ 走廊固定 2 格宽，坦克好走
    private int[][] map;

    public int[][] generate() {
        // 1. 初始化全为铁墙 (基底)
        map = new int[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            Arrays.fill(map[r], GameConfig.TILE_STONE);
        }

        // 2. BSP 递归分割
        Leaf root = new Leaf(1, 1, GameConfig.MAP_COLS - 2, GameConfig.MAP_ROWS - 2);
        List<Leaf> leaves = new ArrayList<>();
        leaves.add(root);

        boolean didSplit = true;
        while (didSplit) {
            didSplit = false;
            List<Leaf> nextLeaves = new ArrayList<>();
            for (Leaf l : leaves) {
                if (l.leftChild == null && l.rightChild == null) {
                    // 分割条件：足够大才分，避免太碎
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

        // 3. 生成房间 (只挖空，先不放装饰)
        root.createRooms(map);

        // 4. ⭐ 核心：生成规整走廊 (强行打通，覆盖一切)
        // 这一步在房间生成之后，确保走廊连通了所有区域
        root.createCorridors(map);

        // 5. 室内装修 (只在空地撒点草和砖，绝对不碰走廊)
        // 这里的逻辑稍微 tricky：我们需要知道哪里是走廊。
        // 由于上面 map 已经被填了 0，我们很难区分“房间的0”和“走廊的0”。
        // 简化策略：我们在 createRooms 里直接做完房间装饰。
        // 所以第 5 步其实融合在第 3 步里了。

        // 6. 出生点保护
        map[1][1] = GameConfig.TILE_EMPTY;
        map[1][2] = GameConfig.TILE_EMPTY;
        map[2][1] = GameConfig.TILE_EMPTY;

        return map;
    }

    // ================== BSP 内部节点类 ==================
    private static class Leaf {
        public int x, y, width, height;
        public Leaf leftChild, rightChild;
        public Rectangle room;

        public Leaf(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }

        public boolean split() {
            if (leftChild != null || rightChild != null) return false;

            // 决定分割方向：谁长切谁
            boolean splitH = (height > width); // 高度大，水平切(横切)
            if (width / (double)height >= 1.25) splitH = false; // 宽度大，竖直切
            else if (height / (double)width >= 1.25) splitH = true;

            int max = (splitH ? height : width) - MIN_NODE_SIZE;
            if (max <= MIN_NODE_SIZE) return false;

            // 随机切分点
            int splitPos = ThreadLocalRandom.current().nextInt(MIN_NODE_SIZE, max);

            if (splitH) { // 横切 -> 上下两个
                leftChild = new Leaf(x, y, width, splitPos);
                rightChild = new Leaf(x, y + splitPos, width, height - splitPos);
            } else { // 竖切 -> 左右两个
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
                // 这是一个叶子节点，生成房间
                // 为了规整，我们将房间设为矩形，且尽量充满区域，留 1 格墙壁即可
                int padding = 1;
                int roomW = Math.max(2, width - (padding * 2));
                int roomH = Math.max(2, height - (padding * 2));
                int roomX = x + (width - roomW) / 2;
                int roomY = y + (height - roomH) / 2;

                this.room = new Rectangle(roomX, roomY, roomW, roomH);

                // 挖空房间 并 装修
                for (int r = roomY; r < roomY + roomH; r++) {
                    for (int c = roomX; c < roomX + roomW; c++) {
                        // 1. 先挖空
                        map[r][c] = GameConfig.TILE_EMPTY;

                        // 2. 室内简单装饰 (绝不影响走廊，因为走廊还没画)
                        decorateRoomTile(map, r, c);
                    }
                }
            }
        }

        private void decorateRoomTile(int[][] map, int r, int c) {
            // 简单装饰逻辑：不碰边缘，内部随机放
            int rand = ThreadLocalRandom.current().nextInt(100);

            // 留出十字中心，方便坦克通行，不要堵死
            if (r == room.centerY() || c == room.centerX()) return;

            if (rand < 10) map[r][c] = GameConfig.TILE_BRICK; // 10% 砖
            else if (rand < 15) map[r][c] = GameConfig.TILE_GRASS; // 5% 草
            // 注意：房间里我没放水，防止房间被水隔断。水只适合做边界。
        }

        public Rectangle getRoom() {
            if (room != null) return room;
            // 递归获取子节点的房间中心
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

                // 连接左右孩子的房间
                Rectangle lRoom = leftChild.getRoom();
                Rectangle rRoom = rightChild.getRoom();

                if (lRoom != null && rRoom != null) {
                    // ✅ 强力笔刷：画走廊
                    drawStrictPath(map, lRoom.centerX(), lRoom.centerY(), rRoom.centerX(), rRoom.centerY());
                }
            }
        }

        // ✅ 核心方法：画规整走廊 (L型路径，覆盖一切)
        private void drawStrictPath(int[][] map, int x1, int y1, int x2, int y2) {
            int startX = Math.min(x1, x2);
            int endX = Math.max(x1, x2);
            int startY = Math.min(y1, y2);
            int endY = Math.max(y1, y2);

            // 策略：先画横线，再画竖线 (L型)
            // 1. 横线 (从 x1 到 x2，高度固定在 y1)
            for (int x = startX; x <= endX; x++) {
                drill(map, y1, x); // 使用 y1
            }

            // 2. 竖线 (从 y1 到 y2，X位置固定在 x2)
            for (int y = startY; y <= endY; y++) {
                drill(map, y, x2); // 使用 x2
            }
        }

        // 钻头：强制将该坐标设为空地，且宽度为 CORRIDOR_WIDTH
        private void drill(int[][] map, int r, int c) {
            for (int i = 0; i < CORRIDOR_WIDTH; i++) {
                for (int j = 0; j < CORRIDOR_WIDTH; j++) { // 如果想要正方形笔刷
                    int nr = r + i;
                    int nc = c + j; // 如果想让走廊稍微粗一点

                    if (nr > 0 && nr < GameConfig.MAP_ROWS - 1 && nc > 0 && nc < GameConfig.MAP_COLS - 1) {
                        // 🔥 霸道逻辑：不管这里原来是草、砖、还是铁墙，统统变成空地！
                        // 这就是你要的“不需要其他地块”
                        map[nr][nc] = GameConfig.TILE_EMPTY;
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
}