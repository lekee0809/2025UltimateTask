package model;

import infra.GameConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 子弹实体类
 * 负责处理子弹的飞行轨迹、与地图元素的碰撞检测以及物理反弹逻辑。
 */
public class Bullet extends Entity {
    // 速度分量：控制子弹每帧在水平和垂直方向移动的像素数
    private double speedx;
    private double speedy;

    private int direction; // 子弹朝向（0-360度）
    public int damage;    // 伤害值
    public Boolean isEnemy; // true=敌方子弹, false=我方子弹
    private int bounceCount = 0; // 当前反弹次数记录

    // --- 构造函数 ---
    public Bullet(Boolean isEnemy, int damage, int direction, double speedx, double speedy, double x, double y, double width, double height) {
        super(x, y, width, height);
        this.isEnemy = isEnemy;
        this.damage = damage;
        this.direction = direction;
        this.speedx = speedx;
        this.speedy = speedy;
    }

    /**
     * 每一帧调用的更新方法
     * 核心逻辑：采用分离轴定律（Separating Axis Theorem）的简化版。
     * 必须将 X轴移动 和 Y轴移动 分开计算，否则在墙角会产生"双重反弹"导致子弹原路返回的BUG。
     */
    @Override
    public void update(Tile[][] map) {
        if (!alive) return;

        // 步骤 1：先尝试水平移动 (X轴)
        // 如果撞墙，会在方法内部反转 speedx，且不更新 x 坐标
        handleXMovement(map);

        // 步骤 2：如果子弹在 X 轴移动后还活着（没撞到砖块销毁），再尝试垂直移动 (Y轴)
        // 基于 X 轴处理后的安全位置进行 Y 轴判定，确保角落判定准确
        if (alive) {
            handleYMovement(map);
        }

        // 步骤 3：处理屏幕边缘反弹
        handleBoundaryBounce();
    }

    // --- X 轴移动逻辑 ---
    private void handleXMovement(Tile[][] map) {
        // 1. 预测下一帧的 X 坐标
        double nextX = x + speedx;

        // 2. 计算检测点：使用子弹的【中心点】进行碰撞检测比左上角更精准
        double justifyX = nextX + GameConfig.BULLET_RADIUS;
        double justifyY = y + GameConfig.BULLET_RADIUS;

        // 3. 获取该点所在的地图瓦片
        Tile tile = getTileAt(justifyX, justifyY, map);

        if (tile != null) {
            TileType type = tile.getType();

            // 情况 A：遇到可以通过的地形（空地、水、草丛）
            if (type == TileType.EMPTY || type == TileType.WATER || type == TileType.GRASS) {
                x = nextX; // 允许移动，更新坐标
            }
            // 情况 B：遇到坚硬物体（铁墙/石头） -> 物理反弹
            else if (type == TileType.STONE) {
                speedx = -speedx; // 核心：X轴速度取反
                onBounce();       // 增加反弹计数
                // 注意：这里【不更新】x 到 nextX。
                // 效果：子弹被挡在墙外一帧，下一帧它就会以反方向飞离。这防止了子弹"嵌"入墙体。
            }
            // 情况 C：遇到易碎物体（砖墙） -> 双方销毁
            else if (type == TileType.BRICK) {
                this.alive = false;       // 子弹销毁
                tile.setDestroyed(true);  // 砖块销毁
            }
        } else {
            // 地图外区域，允许移动（具体的出界销毁由 handleBoundaryBounce 处理）
            x = nextX;
        }
    }

    // --- Y 轴移动逻辑 (原理同 X 轴) ---
    private void handleYMovement(Tile[][] map) {
        double nextY = y + speedy;

        // 同样使用中心点检测
        double justifyX = x + GameConfig.BULLET_RADIUS;
        double justifyY = nextY + GameConfig.BULLET_RADIUS;

        Tile tile = getTileAt(justifyX, justifyY, map);

        if (tile != null) {
            TileType type = tile.getType();

            if (type == TileType.EMPTY || type == TileType.WATER || type == TileType.GRASS) {
                y = nextY;
            }
            else if (type == TileType.STONE) {
                speedy = -speedy; // 核心：Y轴速度取反
                onBounce();
                // 同样不更新 y 坐标，实现阻挡效果
            }
            else if (type == TileType.BRICK) {
                this.alive = false;
                tile.setDestroyed(true);
            }
        } else {
            y = nextY;
        }
    }

    // --- 辅助方法：反弹计数管理 ---
    private void onBounce() {
        bounceCount++;
        // 如果反弹次数超过配置上限（如3次），子弹碎裂
        if (bounceCount > GameConfig.MAX_BULLET_BOUNCES) {
            alive = false;
        }
    }

    // --- 辅助方法：像素坐标 -> 地图瓦片对象 ---
    private Tile getTileAt(double px, double py, Tile[][] map) {
        // 将像素坐标转换为网格索引 (例如: 100px / 40 = index 2)
        int col = (int) (px / GameConfig.GRID_SIZE);
        int row = (int) (py / GameConfig.GRID_SIZE);

        // 边界安全检查，防止数组越界异常
        if (row >= 0 && row < GameConfig.MAP_ROWS && col >= 0 && col < GameConfig.MAP_COLS) {
            return map[row][col];
        }
        return null; // 超出地图范围返回 null
    }

    // --- 屏幕边缘反弹逻辑 ---
    private void handleBoundaryBounce() {
        boolean bounced = false;

        // 左右边界检测
        if (x <= 0 || x >= GameConfig.SCREEN_WIDTH - width) {
            speedx = -speedx;
            x += speedx; // 修正坐标：把它推回屏幕内一点点，防止粘连在边界上
            bounced = true;
        }
        // 上下边界检测
        if (y <= 0 || y >= GameConfig.SCREEN_HEIGHT - height) {
            speedy = -speedy;
            y += speedy; // 修正坐标
            bounced = true;
        }

        if (bounced) {
            onBounce();
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // 根据敌我阵营设置颜色：红色为敌，黄色为友
        gc.setFill(isEnemy ? Color.RED : Color.YELLOW);
        gc.fillOval(x, y, width, height);
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
}