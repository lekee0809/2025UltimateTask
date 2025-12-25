import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 纯走廊 BSP 生成器 (No Rooms, Just Corridors)
 * 逻辑：全图实心，仅在分割区域的中心点之间挖掘通道。
 */
public class CorridorOnlyGenerator {

    // 分割得越细，路网越密。
    // 设为 6 或 8 能保证没有大片空旷，全是紧凑的巷道。
    private static final int MIN_NODE_SIZE = 6;

    // 走廊宽度：2格 (坦克好走，不拥挤)
    private static final int CORRIDOR_WIDTH = 2;

    private int[][] map;

    public int[][] generate() {
        // 1. 【核心】开局全图填满铁墙 (STONE)
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
                    // 只要大于最小尺寸，就继续切，切得越碎越好
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

        // 3. 【绝对不生成房间】
        // 直接跳过 createRooms 步骤！
        // 我们只做一件事：把分割出来的区域连起来。
        root.createCorridors(map);

        // 4. (可选) 打通一些死胡同形成回路
        // 因为 BSP 树天然生成的是“树状图”（没有环），为了游戏性，
        // 我们随机打通几个墙壁，让玩家能绕圈子。
        makeRandomLoops();

        // 5. 出生点保护
        map[1][1] = GameConfig.TILE_EMPTY;
        map[1][2] = GameConfig.TILE_EMPTY;
        map[2][1] = GameConfig.TILE_EMPTY;

        return map;
    }

    /**
     * 在地图上随机打几个洞，形成回路
     */
    private void makeRandomLoops() {
        int loops = 5; // 打通 5 个位置
        for(int i=0; i<loops; i++) {
            int r = ThreadLocalRandom.current().nextInt(2, GameConfig.MAP_ROWS - 2);
            int c = ThreadLocalRandom.current().nextInt(2, GameConfig.MAP_COLS - 2);
            // 只要不是边缘，就暴力打通一个 2x2 的口子
            drill(map, r, c);
        }
    }

    // 钻头：挖空地
    private void drill(int[][] map, int r, int c) {
        for (int i = 0; i < CORRIDOR_WIDTH; i++) {
            for (int j = 0; j < CORRIDOR_WIDTH; j++) {
                int nr = r + i;
                int nc = c + j;
                if (nr > 0 && nr < GameConfig.MAP_ROWS - 1 && nc > 0 && nc < GameConfig.MAP_COLS - 1) {
                    map[nr][nc] = GameConfig.TILE_EMPTY;
                }
            }
        }
    }

    // ================== BSP 内部节点类 ==================
    private static class Leaf {
        public int x, y, width, height;
        public Leaf leftChild, rightChild;

        // 注意：这里删除了 Rectangle room 属性，因为根本没有房间！

        public Leaf(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }

        // 获取该区域的几何中心点 (用于走廊连接的目标点)
        public int getCenterX() { return x + width / 2; }
        public int getCenterY() { return y + height / 2; }

        public boolean split() {
            if (leftChild != null || rightChild != null) return false;

            // 优先切长边
            boolean splitH = (height > width);
            if (width / (double)height >= 1.25) splitH = false;
            else if (height / (double)width >= 1.25) splitH = true;

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

        public void createCorridors(int[][] map) {
            if (leftChild != null && rightChild != null) {
                // 1. 先递归到底层
                leftChild.createCorridors(map);
                rightChild.createCorridors(map);

                // 2. 连接左右子区域的中心点
                // 这里的“中心点”是虚拟的，我们直接从中心点开始挖
                int x1 = leftChild.getCenterX();
                int y1 = leftChild.getCenterY();
                int x2 = rightChild.getCenterX();
                int y2 = rightChild.getCenterY();

                drawPath(map, x1, y1, x2, y2);
            }
        }

        // 规整的 L 型走廊挖掘
        private void drawPath(int[][] map, int x1, int y1, int x2, int y2) {
            int startX = Math.min(x1, x2);
            int endX = Math.max(x1, x2);
            int startY = Math.min(y1, y2);
            int endY = Math.max(y1, y2);

            // 先横着挖
            for (int x = startX; x <= endX; x++) {
                // 钻头逻辑在外部类定义
                drillStatic(map, y1, x);
            }

            // 再竖着挖 (注意拐点位置)
            for (int y = startY; y <= endY; y++) {
                drillStatic(map, y, x2);
            }
        }

        // 静态辅助钻头 (为了让内部类能调用)
        private void drillStatic(int[][] map, int r, int c) {
            for (int i = 0; i < CORRIDOR_WIDTH; i++) {
                for (int j = 0; j < CORRIDOR_WIDTH; j++) {
                    int nr = r + i;
                    int nc = c + j;
                    if (nr > 0 && nr < GameConfig.MAP_ROWS - 1 && nc > 0 && nc < GameConfig.MAP_COLS - 1) {
                        map[nr][nc] = GameConfig.TILE_EMPTY;
                    }
                }
            }
        }
    }
}