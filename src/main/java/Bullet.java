
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Bullet extends Entity {
    private double speedx;
    private double speedy;

    private int direction;
    private int damage;
    public Boolean isEnemy;
    private int bounceCount = 0; // 记录反弹次数

    // 构造函数
    public Bullet(Boolean isEnemy, int damage, int direction, double speedx, double speedy, double x, double y, double width, double height) {
        super(x, y, width, height);
        this.isEnemy = isEnemy;
        this.damage = damage;
        this.direction = direction;
        this.speedx = speedx;
        this.speedy = speedy;
    }

    @Override
    public void update(Tile[][] map) {
        if (!alive) return;

        // ✅ 核心修改：采用分离轴逻辑
        // 1. 先尝试水平移动
        handleXMovement(map);

        // 2. 如果子弹还活着（没撞砖块），再尝试垂直移动
        if (alive) {
            handleYMovement(map);
        }

        // 3. 处理屏幕边缘反弹 (可选，视你的需求而定，也可以直接销毁)
        handleBoundaryBounce();
    }

    // --- X 轴移动逻辑 ---
    private void handleXMovement(Tile[][] map) {
        double nextX = x + speedx;

        // 我们检测子弹的【中心点】，这样判定更精准
        double justifyX = nextX + GameConfig.BULLET_RADIUS;
        double justifyY = y + GameConfig.BULLET_RADIUS;

        Tile tile = getTileAt(justifyX, justifyY, map);

        if (tile != null) {
            TileType type = tile.getType();

            // 1. 遇到空地/水/草 -> 通过
            if (type == TileType.EMPTY || type == TileType.WATER || type == TileType.GRASS) {
                x = nextX;
            }
            // 2. 遇到石头 -> 反弹
            else if (type == TileType.STONE) {
                speedx = -speedx; // 速度反转
                onBounce();       // 增加计数
                // 注意：这里不更新 x，相当于把子弹“挡”在了原地一帧，防止嵌入
            }
            // 3. 遇到砖块 -> 销毁
            else if (type == TileType.BRICK) {
                this.alive = false;          // 子弹死
                tile.setDestroyed(true);// 砖块碎 (这里假设你没有Tile的destroy方法，直接改Type)
            }
        } else {
            // 地图外，允许移动 (由边界检查处理)
            x = nextX;
        }
    }

    // --- Y 轴移动逻辑 ---
    private void handleYMovement(Tile[][] map) {
        double nextY = y + speedy;

        // 检测中心点
        double justifyX = x + GameConfig.BULLET_RADIUS;
        double justifyY = nextY + GameConfig.BULLET_RADIUS;

        Tile tile = getTileAt(justifyX, justifyY, map);

        if (tile != null) {
            TileType type = tile.getType();

            if (type == TileType.EMPTY || type == TileType.WATER || type == TileType.GRASS) {
                y = nextY;
            }
            else if (type == TileType.STONE) {
                speedy = -speedy; // Y轴反弹
                onBounce();
            }
            else if (type == TileType.BRICK) {
                this.alive = false;
                tile.setDestroyed(true);
            }
        } else {
            y = nextY;
        }
    }

    // 反弹计数处理
    private void onBounce() {
        bounceCount++;
        if (bounceCount > GameConfig.MAX_BULLET_BOUNCES) {
            alive = false;
        }
    }

    // 获取特定坐标下的 Tile
    private Tile getTileAt(double px, double py, Tile[][] map) {
        int col = (int) (px / GameConfig.GRID_SIZE);
        int row = (int) (py / GameConfig.GRID_SIZE);

        if (row >= 0 && row < GameConfig.MAP_ROWS && col >= 0 && col < GameConfig.MAP_COLS) {
            return map[row][col];
        }
        return null; // 超出地图范围
    }

    // 屏幕边缘反弹
    private void handleBoundaryBounce() {
        boolean bounced = false;

        // 左右边界
        if (x <= 0 || x >= GameConfig.SCREEN_WIDTH - width) {
            speedx = -speedx;
            x += speedx; // 把它推回来一点，防止粘在墙上
            bounced = true;
        }
        // 上下边界
        if (y <= 0 || y >= GameConfig.SCREEN_HEIGHT - height) {
            speedy = -speedy;
            y += speedy; // 把它推回来一点
            bounced = true;
        }

        if (bounced) {
            onBounce();
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(isEnemy ? Color.RED : Color.YELLOW);
        // 使用 Entity 的 width/height 绘制
        gc.fillOval(x, y, width, height);
    }
}