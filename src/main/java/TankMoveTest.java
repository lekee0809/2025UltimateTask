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
        player = new PlayerTank(100, 100);

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

            // ✅ 4. 处理开火 (按下 J 键)
            case J:
                if (isPressed) {
                    // 调用 PlayerTank 的射击方法 (内部会自动处理冷却时间)
                    Bullet newBullet = player.tryFire();
                    if (newBullet != null) {
                        bullets.add(newBullet); // 将新子弹加入战场
                    }
                }
                break;
        }
    }

    private void initMap() {
        map = new Tile[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                map[r][c] = new Tile(r, c, TileType.EMPTY);

                // 障碍物配置
                if (c == 15 && r > 5 && r < 18) map[r][c] = new Tile(r, c, TileType.BRICK);
                if (Math.random() < 0.05) map[r][c] = new Tile(r, c, TileType.STONE);
                if (r == 10 && c < 5) map[r][c] = new Tile(r, c, TileType.BRICK);
            }
        }
    }

    private void drawMap(GraphicsContext gc) {
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                Tile t = map[r][c];
                double x = c * GameConfig.GRID_SIZE;
                double y = r * GameConfig.GRID_SIZE;

                if (t.getType() == TileType.BRICK) {
                    gc.setFill(Color.BROWN);
                    gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                } else if (t.getType() == TileType.STONE) {
                    gc.setFill(Color.GRAY);
                    gc.fillRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
                    gc.setStroke(Color.WHITE);
                    gc.strokeRect(x, y, GameConfig.GRID_SIZE, GameConfig.GRID_SIZE);
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