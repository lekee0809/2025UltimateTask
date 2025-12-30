package map;
import model.Tile;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import view.ResourceManager;
/**
 * 地图瓦片视图类
 *
 * 职责：
 * 1. 根据 MapModel 渲染地图
 * 2. 只关心 Tile 的显示
 *
 * 不负责：
 * - 地图逻辑
 * - 道具系统
 * - 坦克 / 子弹
 */
public class MapTileView extends Canvas {

    private final ResourceManager resourceManager;

    /** 地形图片 */
    private Image floorImage;
    private Image brickImage;
    private Image stoneImage;
    private Image waterImage;
    private Image grassImage;
    private Image brokenBrickImage;

    public MapTileView() {
        setWidth(MapConstants.MAP_PIXEL_WIDTH);
        setHeight(MapConstants.MAP_PIXEL_HEIGHT);
        resourceManager = ResourceManager.getInstance();
        loadImages();
    }

    /** 加载地形图片 */
    private void loadImages() {
        floorImage = resourceManager.loadImage("images/floor.png");
        brickImage = resourceManager.loadImage("images/brick.png");
        stoneImage = resourceManager.loadImage("images/stone.png");
        waterImage = resourceManager.loadImage("images/water.png");
        grassImage = resourceManager.loadImage("images/grass.png");

        // 破坏后的砖墙（可选）
        brokenBrickImage = resourceManager.loadImage("images/wall_broken.png");
    }

    /** 渲染整个地图 */
    public void render(MapModel map) {
        GraphicsContext gc = getGraphicsContext2D();

        // 清空画布
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, getWidth(), getHeight());

        for (int r = 0; r < map.getRows(); r++) {
            for (int c = 0; c < map.getCols(); c++) {
                drawTile(gc, map.getTile(r, c));
            }
        }
    }

    /** 绘制单个 Tile */
    private void drawTile(GraphicsContext gc, Tile tile) {
        if (tile == null) return;

        double x = tile.getCol() * MapConstants.TILE_SIZE;
        double y = tile.getRow() * MapConstants.TILE_SIZE;

        Image img = null;

        switch (tile.getType()) {
            case EMPTY:
                img = floorImage;
                break;
            case BRICK:
                img = tile.isDestroyed()
                        ? brokenBrickImage
                        : brickImage;
                break;
            case STONE:
                img = stoneImage;
                break;
            case WATER:
                img = waterImage;
                break;
            case GRASS:
                img = grassImage;
                break;
        }

        if (img != null) {
            gc.drawImage(img, x, y,
                    MapConstants.TILE_SIZE,
                    MapConstants.TILE_SIZE);
        } else {
            drawFallback(gc, tile, x, y);
        }
    }

    /** 图片缺失时的备用绘制方案 */
    private void drawFallback(GraphicsContext gc, Tile tile, double x, double y) {
        switch (tile.getType()) {
            case EMPTY:
                gc.setFill(Color.DARKGREEN);
                break;
            case BRICK:
                gc.setFill(tile.isDestroyed()
                        ? Color.DARKRED
                        : Color.SADDLEBROWN);
                break;
            case STONE:
                gc.setFill(Color.GRAY);
                break;
            case WATER:
                gc.setFill(Color.DODGERBLUE);
                break;
            case GRASS:
                gc.setFill(Color.GREEN);
                break;
            default:
                gc.setFill(Color.BLACK);
        }
        gc.fillRect(x, y,
                MapConstants.TILE_SIZE,
                MapConstants.TILE_SIZE);
    }

    public void reloadImages() {
        loadImages();
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public Image getFloorImage() {
        return floorImage;
    }

    public void setFloorImage(Image floorImage) {
        this.floorImage = floorImage;
    }

    public Image getBrickImage() {
        return brickImage;
    }

    public void setBrickImage(Image brickImage) {
        this.brickImage = brickImage;
    }

    public Image getStoneImage() {
        return stoneImage;
    }

    public void setStoneImage(Image stoneImage) {
        this.stoneImage = stoneImage;
    }

    public Image getWaterImage() {
        return waterImage;
    }

    public void setWaterImage(Image waterImage) {
        this.waterImage = waterImage;
    }

    public Image getGrassImage() {
        return grassImage;
    }

    public void setGrassImage(Image grassImage) {
        this.grassImage = grassImage;
    }

    public Image getBrokenBrickImage() {
        return brokenBrickImage;
    }

    public void setBrokenBrickImage(Image brokenBrickImage) {
        this.brokenBrickImage = brokenBrickImage;
    }
}
