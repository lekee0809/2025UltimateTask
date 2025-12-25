import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 子弹类 - 由 Lekee 指挥官统一定制
 * 特性：支持分轴碰撞检测、3次物理反弹、砖墙破坏
 */
public class Bullet extends Entity {
    private double speedx;    // X轴分速度
    private double speedy;    // Y轴分速度
    private double direction; // 子弹朝向角度（double 精度）
    private int damage;       // 伤害值
    public boolean isEnemy;   // 敌我标志
    private int bounceCount = 0; // 反弹计数器

    public Bullet(boolean isEnemy, int damage, double direction, double speedx, double speedy,
                  double x, double y, double width, double height) {
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

        // 记录旧位置用于回退处理
        double oldX = x;
        double oldY = y;

        // --- 分轴检测逻辑 ---
        // 1. 处理 X 轴移动
        handleXMovement(map);

        // 2. 处理 Y 轴移动
        handleYMovement(map);

        // 3. 处理屏幕边界反弹
        handleBoundaryBounce();
    }

    /**
     * X 轴物理逻辑：移动 + 碰撞检测
     */
    private void handleXMovement(Tile[][] map) {
        x += speedx;
        // 探测子弹中心点所在的格子
        Tile tile = getTileAt(x + GameConfig.BULLET_RADIUS, y + GameConfig.BULLET_RADIUS, map);

        if (tile != null && !tile.getType().isBulletPassable()) {
            if (tile.getType().isBulletReflect()) {
                // 撞击石墙：反弹
                speedx = -speedx;
                x = (speedx > 0) ? x + 2 : x - 2; // 微量位移防止卡墙
                onBounce();
            } else {
                // 撞击砖墙：摧毁
                tile.setDestroyed(true);
                this.alive = false;
            }
        }
    }

    /**
     * Y 轴物理逻辑：移动 + 碰撞检测
     */
    private void handleYMovement(Tile[][] map) {
        if (!alive) return;
        y += speedy;
        Tile tile = getTileAt(x + GameConfig.BULLET_RADIUS, y + GameConfig.BULLET_RADIUS, map);

        if (tile != null && !tile.getType().isBulletPassable()) {
            if (tile.getType().isBulletReflect()) {
                // 撞击石墙：反弹
                speedy = -speedy;
                y = (speedy > 0) ? y + 2 : y - 2;
                onBounce();
            } else {
                // 撞击砖墙：摧毁
                tile.setDestroyed(true);
                this.alive = false;
            }
        }
    }

    /**
     * 统一处理反弹逻辑（计数 + 视觉角度同步）
     */
    private void onBounce() {
        bounceCount++;
        // 实时同步子弹的视觉旋转角度
        this.direction = Math.toDegrees(Math.atan2(speedy, speedx)) + 90;

        if (bounceCount > GameConfig.MAX_BULLET_BOUNCES) {
            this.alive = false;
        }
    }

    /**
     * 屏幕边界检测
     */
    private void handleBoundaryBounce() {
        boolean bounced = false;
        if (x <= 0 || x >= GameConfig.SCREEN_WIDTH - width) {
            speedx = -speedx;
            bounced = true;
        }
        if (y <= 0 || y >= GameConfig.SCREEN_HEIGHT - height) {
            speedy = -speedy;
            bounced = true;
        }

        if (bounced) onBounce();
    }

    /**
     * 工具方法：坐标转格点
     */
    private Tile getTileAt(double px, double py, Tile[][] map) {
        int col = (int) (px / GameConfig.GRID_SIZE);
        int row = (int) (py / GameConfig.GRID_SIZE);

        if (row >= 0 && row < GameConfig.MAP_ROWS && col >= 0 && col < GameConfig.MAP_COLS) {
            return map[row][col];
        }
        return null;
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (!alive) return;

        gc.save();
        // 1. 同样平移到中心
        gc.translate(x + width / 2, y + height / 2);
        // 2. 旋转到子弹的飞行角度
        gc.rotate(direction);

        // 3. 绘制“曳光效果” (不需要贴图也能有高级感)
        // 我们不画圆，画一个长条形的矩形，看起来更有破空感
        double bulletLength = width * 1.5; // 让子弹稍长一点
        double bulletWidth = width;

        // 核心：亮黄色/红色
        gc.setFill(isEnemy ? Color.RED : Color.GOLD);
        gc.fillRoundRect(-bulletWidth / 2, -bulletLength / 2, bulletWidth, bulletLength, 5, 5);

        // 增加一个外发光光晕 (模拟 CS 里的曳光弹)
        gc.setGlobalAlpha(0.4);
        gc.setFill(isEnemy ? Color.ORANGERED : Color.YELLOW);
        gc.fillOval(-bulletWidth, -bulletLength / 2, bulletWidth * 2, bulletLength);

        gc.restore();
    }
}