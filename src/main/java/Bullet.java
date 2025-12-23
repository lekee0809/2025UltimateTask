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

    @Override // 现在签名一致了，报错会消失
    public void update(Tile[][] map) {
        if (!alive) return;

        // 执行你之前写的逻辑
        handleXMovement(map);
        handleYMovement(map);
        handleBoundaryBounce();
    }

    private void handleXMovement(Tile[][] map) {
        double nextX = x + speedx;
        // 使用目标位置的中心点进行探测
        Tile tile = getTileAt(nextX + GameConfig.BULLET_RADIUS, y + GameConfig.BULLET_RADIUS, map);

        if (tile != null && !tile.isDestroyed()) {
            if (tile.shouldBulletReflect()) {
                speedx = -speedx; // 撞到石墙：X轴反弹
                onBounce();
            } else if (!tile.canBulletPass()) {
                // 撞到砖墙：子弹碎，砖块毁
                tile.destroy();
                this.alive = false;
            } else {
                x = nextX; // 空地、水、草丛：正常通过
            }
        } else {
            x = nextX; // 超出地图边界或无瓦片时（理论上由边界逻辑处理）
        }
    }

    private void handleYMovement(Tile[][] map) {
        if (!alive) return; // 如果 X 轴撞砖块碎了，就不处理 Y 了

        double nextY = y + speedy;
        Tile tile = getTileAt(x + GameConfig.BULLET_RADIUS, nextY + GameConfig.BULLET_RADIUS, map);

        if (tile != null && !tile.isDestroyed()) {
            if (tile.shouldBulletReflect()) {
                speedy = -speedy; // 撞到石墙：Y轴反弹
                onBounce();
            } else if (!tile.canBulletPass()) {
                tile.destroy();
                this.alive = false;
            } else {
                y = nextY;
            }
        } else {
            y = nextY;
        }
    }

    private void onBounce() {
        bounceCount++;
        if (bounceCount > GameConfig.MAX_BULLET_BOUNCES) {
            alive = false;
        }
    }

    // 辅助方法：通过物理坐标直接获取 Tile 对象
    private Tile getTileAt(double px, double py, Tile[][] map) {
        int col = (int) (px / GameConfig.GRID_SIZE);
        int row = (int) (py / GameConfig.GRID_SIZE);

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