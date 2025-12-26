package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
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
    private ResourceManager resourceManager = ResourceManager.getInstance();
    private Image floorImage;
    private Image brickImage;
    private Image stoneImage;
    private Image waterImage;
    private Image grassImage;
    private Image brokenBrickImage;

    public SpritePainter() {
        // 初始化时加载所有地形图片
        loadImages();
    }

    /** 加载地形图片（增加异常处理，避免图片加载失败导致崩溃） */
    private void loadImages() {
        try {
            floorImage = resourceManager.loadImage("images/floor.png");
            brickImage = resourceManager.loadImage("images/brick.png");
            stoneImage = resourceManager.loadImage("images/stone.png");
            waterImage = resourceManager.loadImage("images/river.png");
            grassImage = resourceManager.loadImage("images/grass.png");
            brokenBrickImage = resourceManager.loadImage("images/floor.png");
        } catch (Exception e) {
            // 图片加载失败时打印日志，保证程序不崩溃
            System.err.println("地形图片加载失败：" + e.getMessage());
            // 兜底：将图片置空，后续用颜色填充
            floorImage = null;
            brickImage = null;
            stoneImage = null;
            waterImage = null;
            grassImage = null;
            brokenBrickImage = null;
        }
    }

    /**
     * 1. 绘制地图底层 (墙、水、砖、地板)
     * 这些东西应该被坦克踩在脚下，或者阻挡坦克
     */
    public void drawMapBackground(GraphicsContext gc, Tile[][] map) {
        if (map == null) return;

        double gridSize = GameConfig.GRID_SIZE;

        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length; c++) {
                Tile tile = map[r][c];
                TileType type = tile.getType();

                // 计算格子左上角坐标
                double x = c * gridSize;
                double y = r * gridSize;

                // 优先绘制地板（空地上显示地板图片）
                if (type == TileType.EMPTY) {
                    drawTileImage(gc, floorImage, x, y, gridSize, Color.BLACK);
                    continue;
                }

                // 跳过草地 (草地要在顶层画)
                if (type == TileType.GRASS) continue;

                // 根据地形类型绘制对应图片
                switch (type) {
                    case BRICK:
                        // 若砖块已损坏，绘制破损砖块图片
                        drawTileImage(gc, tile.isDestroyed() ? brokenBrickImage : brickImage,
                                x, y, gridSize, Color.web("#b15e32"));
                        break;
                    case STONE:
                        drawTileImage(gc, stoneImage, x, y, gridSize, Color.GRAY);
                        // 画X标记（保持原有逻辑，增强辨识度）
                        gc.setStroke(Color.WHITE);
                        gc.strokeLine(x, y, x + gridSize, y + gridSize);
                        gc.strokeLine(x, y + gridSize, x + gridSize, y);
                        break;
                    case WATER:
                        drawTileImage(gc, waterImage, x, y, gridSize, Color.web("#3498db"));
                        break;
                }

                // 绘制格子边框（增强网格感）
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1);
                gc.strokeRect(x, y, gridSize, gridSize);
            }
        }
    }

    /**
     * 2. 绘制地图顶层 (草地)
     * 这些东西会画在坦克上面，实现"隐身"效果
     */
    public void drawMapForeground(GraphicsContext gc, Tile[][] map) {
        if (map == null) return;

        double gridSize = GameConfig.GRID_SIZE;

        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length; c++) {
                Tile tile = map[r][c];
                if (tile.getType() == TileType.GRASS) {
                    double x = c * gridSize;
                    double y = r * gridSize;
                    // 绘制草地图片（半透明效果）
                    drawGrassImage(gc, x, y, gridSize);
                }
            }
        }
    }

    /**
     * 辅助方法：绘制地形图片（适配网格大小，图片加载失败时用颜色兜底）
     * @param gc 画布上下文
     * @param image 要绘制的图片
     * @param x 格子左上角X坐标
     * @param y 格子左上角Y坐标
     * @param gridSize 格子大小
     * @param fallbackColor 图片加载失败时的兜底颜色
     */
    private void drawTileImage(GraphicsContext gc, Image image, double x, double y, double gridSize, Color fallbackColor) {
        if (image != null) {
            // 绘制图片并拉伸至网格大小
            gc.drawImage(image, x, y, gridSize, gridSize);
        } else {
            // 图片加载失败时用颜色填充兜底
            gc.setFill(fallbackColor);
            gc.fillRect(x, y, gridSize, gridSize);
        }
    }

    /**
     * 辅助方法：绘制草地图片（半透明效果）
     * @param gc 画布上下文
     * @param x 格子左上角X坐标
     * @param y 格子左上角Y坐标
     * @param gridSize 格子大小
     */
    private void drawGrassImage(GraphicsContext gc, double x, double y, double gridSize) {
        gc.save(); // 保存画布状态，避免透明度影响其他绘制
        if (grassImage != null) {
            // 绘制半透明草地图片
            gc.setGlobalAlpha(0.9);
            gc.drawImage(grassImage, x, y, gridSize, gridSize);
        } else {
            // 图片加载失败时用半透明绿色兜底
            gc.setFill(Color.rgb(46, 204, 113, 0.9));
            gc.fillRect(x, y, gridSize, gridSize);
        }
        gc.restore(); // 恢复画布透明度
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