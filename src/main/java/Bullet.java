import javafx.scene.canvas.GraphicsContext;

import javafx.scene.paint.Color;

public class Bullet extends Entity { // 建议类名大写
    private double speedx; // 每一帧 X 轴的位移量
    private double speedy; // 每一帧 Y 轴的位移量

    private int direction; // 可以用来表示子弹图片的旋转角度
    private int damage;
    public Boolean isEnemy;
    private int bounceCount = 0; // 记录反弹次数

    public Bullet(Boolean isEnemy, int damage, int direction, double speedx, double speedy, double x, double y, double width, double height) {
        super(x, y, width, height);
        this.isEnemy = isEnemy;
        this.damage = damage;
        this.direction = direction;
        this.speedx = speedx;
        this.speedy = speedy;
    }

    public void update(Tile[][] map) {
        if (!alive) return;

        // 分轴检测：先处理 X
        handleXMovement(map);
        // 后处理 Y
        handleYMovement(map);

        handleBoundaryBounce();
    }

    private void handleXMovement(Tile[][] map) {
        double nextX = x + speedx;
        Tile tile = getTileAt(nextX, y, map);

        if (tile != null) {
            if (tile.shouldBulletReflect()) {
                speedx = -speedx; // 反弹
                onBounce();
            } else if (!tile.canBulletPass()) {
                // 如果不能穿过且不反弹，那就是砖块
                tile.destroy();
                this.alive = false; // 子弹碎了
            } else {
                x = nextX; // 水或草丛，穿过
            }
        }
    }

// Y 轴逻辑同上，只需换成 nextY 和 speedy...

    private Tile getTileAt(double tx, double ty, Tile[][] map) {
        int col = (int) ((tx + GameConfig.BULLET_RADIUS) / GameConfig.GRID_SIZE);
        int row = (int) ((ty + GameConfig.BULLET_RADIUS) / GameConfig.GRID_SIZE);

        if (row >= 0 && row < GameConfig.MAP_ROWS && col >= 0 && col < GameConfig.MAP_COLS) {
            return map[row][col];
        }
        return null;
    }

    private void handleBoundaryBounce() {
        boolean bounced = false;

        // 左右边界
        if (x <= 0 || x >= GameConfig.SCREEN_WIDTH - GameConfig.BULLET_RADIUS * 2) {
            speedx = -speedx; // X轴速度取反
            bounced = true;
        }
        // 上下边界
        if (y <= 0 || y >= GameConfig.SCREEN_HEIGHT - GameConfig.BULLET_RADIUS * 2) {
            speedy = -speedy; // Y轴速度取反
            bounced = true;
        }

        if (bounced) {
            bounceCount++;
            if (bounceCount > GameConfig.MAX_BULLET_BOUNCES) {
                alive = false; // 超过反弹次数销毁
            }
        }
    }
    @Override
    public void draw(GraphicsContext gc) {
        // 根据敌我显示不同颜色
        gc.setFill(isEnemy ? Color.RED : Color.YELLOW);
        // 使用 Config 中定义的半径
        gc.fillOval(x, y, GameConfig.BULLET_RADIUS * 2, GameConfig.BULLET_RADIUS * 2);
    }

}