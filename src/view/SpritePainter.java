package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import model.Bullet;
import model.Tank;
import infra.GameConfig;
import model.Tile;
import model.TileType;

/**
 * 精灵绘制器 (Canvas版本)
 * 核心职责：每一帧被 render() 调用，负责在 Canvas 上画出地图、坦克和子弹
 */
public class SpritePainter {

    // 如果你有 ResourceManager，可以在这里引入
    // private ResourceManager resourceManager = ResourceManager.getInstance();

    public SpritePainter() {
        // 初始化
    }

    /**
     * 1. 绘制地图底层 (墙、水、砖)
     * 这些东西应该被坦克踩在脚下，或者阻挡坦克
     */
    public void drawMapBackground(GraphicsContext gc, Tile[][] map) {
        if (map == null) return;

        double size = GameConfig.GRID_SIZE;

        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length; c++) {
                Tile tile = map[r][c];
                TileType type = tile.getType();

                // 跳过草地 (草地要在顶层画)
                if (type == TileType.GRASS) continue;
                // 跳过空地 (不用画，背景已经是黑色的了)
                if (type == TileType.EMPTY) continue;

                double x = c * size;
                double y = r * size;

                // 根据类型画不同颜色的方块 (以后可以在这里换成 gc.drawImage)
                switch (type) {
                    case BRICK:
                        gc.setFill(Color.web("#b15e32")); // 砖红
                        gc.fillRect(x, y, size, size);
                        gc.setStroke(Color.BLACK);
                        gc.strokeRect(x, y, size, size);
                        break;
                    case STONE:
                        gc.setFill(Color.GRAY); // 铁灰
                        gc.fillRect(x, y, size, size);
                        gc.setStroke(Color.WHITE);
                        gc.strokeRect(x, y, size, size);
                        // 画个 X 代表坚固
                        gc.strokeLine(x, y, x + size, y + size);
                        gc.strokeLine(x, y + size, x + size, y);
                        break;
                    case WATER:
                        gc.setFill(Color.web("#3498db")); // 蓝水
                        gc.fillRect(x, y, size, size);
                        break;
                }
            }
        }
    }

    /**
     * 2. 绘制地图顶层 (草地)
     * 这些东西会画在坦克上面，实现"隐身"效果
     */
    public void drawMapForeground(GraphicsContext gc, Tile[][] map) {
        if (map == null) return;

        double size = GameConfig.GRID_SIZE;

        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length; c++) {
                if (map[r][c].getType() == TileType.GRASS) {
                    double x = c * size;
                    double y = r * size;

                    // 草地画成半透明绿色
                    gc.setFill(Color.rgb(46, 204, 113, 0.9)); // 0.9 透明度
                    gc.fillRect(x, y, size, size);
                }
            }
        }
    }

    /**
     * 3. 绘制坦克 (支持旋转)
     * 难点：Canvas 旋转图片需要变换矩阵
     */
    public void drawTank(GraphicsContext gc, Tank tank) {
        if (tank == null || !tank.isAlive()) return; // 死了就不画

        // 保存当前的画布状态 (以免旋转影响到后续绘制)
        gc.save();

        // 1. 移动画布原点到坦克的【中心点】
        double centerX = tank.getX() + tank.getWidth() / 2;
        double centerY = tank.getY() + tank.getHeight() / 2;

        // 移动原点
        gc.translate(centerX, centerY);

        // 2. 旋转画布
        gc.rotate(tank.getDisplayRotation());

        // 3. 绘制坦克 (此时坐标系中心已经是坦克中心了，所以要画在 -w/2, -h/2 处)
        // 区分敌我颜色
        if (tank instanceof model.PlayerTank) {
            gc.setFill(Color.YELLOW); // 玩家：黄色
        } else {
            gc.setFill(Color.RED);    // 敌人：红色
        }

        // 画车身
        double w = tank.getWidth();
        double h = tank.getHeight();
        gc.fillRect(-w / 2, -h / 2, w, h);

        // 画个炮管指示方向 (在右侧，因为0度默认向右)
        gc.setFill(Color.BLACK);
        gc.fillRect(0, -5, w / 2 + 5, 10);

        // 恢复画布状态
        gc.restore();
    }
}