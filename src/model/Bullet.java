package model;

import infra.*;
import javafx.scene.canvas.GraphicsContext;
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

    @Override
    public void update(Tile[][] map) {
        if (!alive) return;

        // 1. 记录移动前的旧位置（用于反弹时判断撞击面）
        double oldX = x;
        double oldY = y;

        // 2. 根据速度更新位置 (速度已经在发射时根据 GameConfig.BULLET_SPEED 设置)
        x += speedx;
        y += speedy;

        // 3. 计算当前的网格索引 (使用你定义的 GRID_SIZE: 40.0)
        int col = (int) (x / GameConfig.GRID_SIZE);
        int row = (int) (y / GameConfig.GRID_SIZE);

        // 4. 地图边界检查
        if (row < 0 || row >= GameConfig.MAP_ROWS || col < 0 || col >= GameConfig.MAP_COLS) {
            this.alive = false;
            return;
        }

        // 5. 获取当前格子并处理逻辑
        Tile currentTile = map[row][col];

        if (!currentTile.getType().isBulletPassable()) {
            // --- 逻辑 A：遇到石墙 (STONE) - 处理反弹 ---
            if (currentTile.getType().isBulletReflect()) {
                handleBounce(oldX, oldY, col, row);
                bounceCount++;

                // 达到最大反弹次数后销毁 (使用 GameConfig.MAX_BULLET_BOUNCES: 3)
                if (bounceCount > GameConfig.MAX_BULLET_BOUNCES) {
                    this.alive = false;
                }
            }
            // --- 逻辑 B：遇到砖墙 (BRICK) - 销毁子弹并破坏墙体 ---
            else {
                this.alive = false;
                currentTile.setDestroyed(true); // 砖墙消失逻辑
            }
        }
    }

    private void handleBounce(double oldX, double oldY, int targetCol, int targetRow) {
        // 计算旧坐标所在的网格
        int oldCol = (int) (oldX / GameConfig.GRID_SIZE);
        int oldRow = (int) (oldY / GameConfig.GRID_SIZE);

        // 左右碰撞：如果旧坐标的列与目标列不同
        if (oldCol != targetCol) {
            speedx = -speedx; // X轴速度反转
            x = oldX;         // 坐标回正，防止子弹“嵌”进墙里
        }

        // 上下碰撞：如果旧坐标的行与目标行不同
        if (oldRow != targetRow) {
            speedy = -speedy; // Y轴速度反转
            y = oldY;         // 坐标回正
        }
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