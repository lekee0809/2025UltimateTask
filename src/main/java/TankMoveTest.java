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


import java.util.ArrayList; // 【新增】
import java.util.List;      // 【新增】

public class TankMoveTest extends Application {

    private PlayerTank player;
    private Tile[][] map;

    // ✅ 2. 定义子弹列表（弹药库）
    private List<Bullet> bullets = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        initMap();
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

                // ✅ 3. 更新并绘制所有子弹 (核心逻辑)
                // 3.1 清理死掉的子弹 (撞墙或出界)
                bullets.removeIf(b -> !b.alive);

                // 3.2 让每一颗子弹飞
                for (Bullet b : bullets) {
                    b.update(map); // 传入地图进行碰撞检测
                    b.draw(gc);    // 绘制子弹
                }

                // C. 更新玩家 (处理移动和撞墙)
                player.update(map);
                player.draw(gc);

                // D. 绘制 HUD
                drawHUD(gc);
            }
        }.start();

        stage.setTitle("FaZe Lekee's Tank Warfare v0.1 [WASD Move | J Fire]");
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

            // ✅ 新增：按 R 键重新生成地图 (仅在按下瞬间触发)
            case R:
                if (isPressed) {
                    initMap(); // 重新生成地图
                    // 重置玩家位置到出生点
                    player = new PlayerTank(40, 40);
                    // 清空场上残留子弹
                    bullets.clear();
                    System.out.println("Map Regenerated! (Map Pool Updated)");
                }
                break;
        }
    }

// 在 TankMoveTest 类中替换这两个方法

    private void initMap() {
        // 使用纯走廊生成器
        CorridorOnlyGenerator generator = new CorridorOnlyGenerator();
        int[][] levelData = generator.generate();

        // 下面解析代码不变...
        map = new Tile[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        // 后面解析 Tile 的代码不变...
        map = new Tile[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                int typeCode = 0;
                if (r < levelData.length && c < levelData[r].length) {
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
                if (t.getType() == TileType.EMPTY) continue; // 空地不画

                double x = c * GameConfig.GRID_SIZE;
                double y = r * GameConfig.GRID_SIZE;

                switch (t.getType()) {
                    case BRICK:
                        gc.setFill(Color.web("#b15e32")); // 砖红色
                        gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        // 画点纹理细节
                        gc.setStroke(Color.BLACK);
                        gc.strokeRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        break;

                    case STONE:
                        gc.setFill(Color.web("#7f8c8d")); // 铁灰色
                        gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        // 画个 "X" 代表坚硬
                        gc.setStroke(Color.WHITE);
                        gc.strokeRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        gc.strokeLine(x, y, x + GameConfig.GRID_SIZE, y + GameConfig.GRID_SIZE);
                        break;

                    case WATER:
                        gc.setFill(Color.web("#3498db")); // 蓝色
                        gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        break;

                    case GRASS:
                        gc.setFill(Color.web("#2ecc71")); // 绿色
                        // 草地通常需要一点透明度或者画在坦克上面（进阶优化），这里先简单画
                        gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                        break;
                }
            }
        }
    }


    private void drawHUD(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 16));
        gc.fillText(String.format("Bullets: %d", bullets.size()), 10, 20); // 显示当前子弹数
        gc.fillText(String.format("Angle: %.1f°", player.getDisplayRotation()), 10, 40);

        // 冷却状态提示
        // 这里只是简单的逻辑判断演示，如果你想更精确可以在 HUD 里加 CD 条
        gc.fillText("Fire: [J]", 10, 60);
    }

    public static void main(String[] args) {
        launch(args);
    }
}