import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

// 确保导入你的自定义类 (如果它们在不同的包里，请取消注释并修改包名)
// import map.*;
// import model.*;
// import infra.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TankMoveTest extends Application {

    private PlayerTank player;
    private Tile[][] map;

    // 定义子弹列表
    private List<Bullet> bullets = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        initMap();
        // 确保玩家出生在安全区域 (简单处理: 1,1)
        player = new PlayerTank(40, 40);

        Canvas canvas = new Canvas(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new Group(canvas));

        scene.setOnKeyPressed(e -> handleKey(e.getCode(), true));
        scene.setOnKeyReleased(e -> handleKey(e.getCode(), false));

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // A. 清屏
                gc.setFill(Color.web("#2c3e50"));
                gc.fillRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

                // B. 绘制地图
                drawMap(gc);

                // C. 更新并绘制所有子弹
                bullets.removeIf(b -> !b.alive);

                for (Bullet b : bullets) {
                    b.update(map);
                    b.draw(gc);
                }

                // D. 更新玩家
                player.update(map);
                player.draw(gc);

                // E. 绘制 HUD
                drawHUD(gc);
            }
        }.start();

        stage.setTitle("FaZe Lekee's Tank Warfare v0.2 [Random Map | R to Reset]");
        stage.setScene(scene);
        stage.show();
    }

    private void handleKey(KeyCode code, boolean isPressed) {
        switch (code) {
            case W: player.setMovingForward(isPressed); break;
            case S: player.setMovingBackward(isPressed); break;
            case A: player.setRotatingLeft(isPressed); break;
            case D: player.setRotatingRight(isPressed); break;
            case J:
                if (isPressed) {
                    Bullet newBullet = player.tryFire();
                    if (newBullet != null) bullets.add(newBullet);
                }
                break;
            case R:
                if (isPressed) {
                    System.out.println("♻️ 正在重新生成地图...");
                    initMap();
                    player = new PlayerTank(40, 40); // 重置玩家
                    bullets.clear(); // 清空子弹
                }
                break;
        }
    }

    // ✅ 修复后的 initMap 方法
    private void initMap() {
        MazeDigger digger = new MazeDigger(); // 你的迷宫生成器
        BattlefieldMapGenerator mapGenerator =new BattlefieldMapGenerator();
        Random random = new Random();

        // 1. 【核心修复】先在外部声明变量
        int[][] levelData;

        // 2. 随机二选一进行赋值
        if (random.nextBoolean()) {
            System.out.println("🗺️ 当前模式: 城市迷宫 (Maze)");
            levelData = digger.generate(); // 赋值
        } else {
            System.out.println("🗺️ 当前模式: 野外战场 (Battlefield)");
            levelData = mapGenerator.generate(); // 赋值
        }

        // 3. 将 int[][] 转换为 Tile 对象
        map = new Tile[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];

        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                int typeCode = 0;
                // 防止数组越界检查
                if (levelData != null && r < levelData.length && c < levelData[r].length) {
                    typeCode = levelData[r][c];
                }

                TileType type;
                switch (typeCode) {
                    case GameConfig.TILE_BRICK: type = TileType.BRICK; break;
                    case GameConfig.TILE_STONE: type = TileType.STONE; break;
                    case GameConfig.TILE_WATER: type = TileType.WATER; break;
                    case GameConfig.TILE_GRASS: type = TileType.GRASS; break;
                    default: type = TileType.EMPTY; break;
                }
                map[r][c] = new Tile(r, c, type);
            }
        }
    }

    private void drawMap(GraphicsContext gc) {
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                Tile t = map[r][c];
                if (t == null || t.getType() == TileType.EMPTY) continue;

                double x = c * GameConfig.GRID_SIZE;
                double y = r * GameConfig.GRID_SIZE;

                switch (t.getType()) {
                    case BRICK:
                        gc.setFill(Color.web("#b15e32"));
                        gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        gc.setStroke(Color.BLACK);
                        gc.strokeRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        break;
                    case STONE:
                        gc.setFill(Color.web("#7f8c8d"));
                        gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        gc.setStroke(Color.WHITE);
                        gc.strokeRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        // 画个 X
                        gc.strokeLine(x, y, x + GameConfig.GRID_SIZE, y + GameConfig.GRID_SIZE);
                        break;
                    case WATER:
                        gc.setFill(Color.web("#3498db"));
                        gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        break;
                    case GRASS:
                        gc.setFill(Color.web("#2ecc71"));
                        gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        break;
                }
            }
        }
    }

    private void drawHUD(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 16));
        gc.fillText(String.format("Bullets: %d", bullets.size()), 10, 20);
        gc.fillText("Map: Random (Press R)", 10, 40);
    }

    public static void main(String[] args) {
        launch(args);
    }
}