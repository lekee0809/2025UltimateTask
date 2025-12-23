import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class BulletMoveTest extends Application {
    private List<Bullet> bullets = new ArrayList<>();
    // 增加：定义测试地图
    private Tile[][] map = new Tile[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];

    @Override
    public void start(Stage stage) {
        // --- 初始化测试地图数据 ---
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                // 默认全是空地
                map[r][c] = new Tile(r, c, TileType.EMPTY);

                // 随机放一些石墙 (STONE) 用来测试反弹
                if (Math.random() < 0.05) {
                    map[r][c] = new Tile(r, c, TileType.STONE);
                }
                // 随机放一些砖墙 (BRICK) 用来测试破坏
                else if (Math.random() < 0.05) {
                    map[r][c] = new Tile(r, c, TileType.BRICK);
                }
            }
        }

        Canvas canvas = new Canvas(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvas.setOnMouseClicked(e -> {// 1. 计算随机角度和分速度
            double angle = Math.random() * 360;
            double rad = Math.toRadians(angle);
            double vx = Math.cos(rad) * GameConfig.BULLET_SPEED;
            double vy = Math.sin(rad) * GameConfig.BULLET_SPEED;

            double radius = GameConfig.BULLET_RADIUS;
            double size = radius * 2;

            // 2. 修正初始坐标，让子弹中心对准鼠标点击处
            double startX = e.getX() - radius;
            double startY = e.getY() - radius;

            // 3. 创建子弹并加入列表
            Bullet b = new Bullet(
                    false,              // isEnemy
                    1,                  // damage
                    (int)angle,         // direction
                    vx,                 // speedx
                    vy,                 // speedy
                    startX,             // x
                    startY,             // y
                    size,               // width
                    size                // height
            );

            bullets.add(b);

            // 【调试技巧】如果控制台能打印出这句话，说明点击事件没问题
            System.out.println("发射子弹！位置: " + startX + ", " + startY + " | 当前总数: " + bullets.size());
        });

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 背景
                gc.setFill(Color.web("#2c3e50"));
                gc.fillRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

                // 绘制地图：这样你才能看到子弹撞到了什么
                drawMap(gc);

                // 更新与绘制子弹
                bullets.removeIf(b -> !b.alive);
                for (Bullet b : bullets) {
                    // 【关键点】传入 map 触发碰撞逻辑
                    b.update(map);
                    b.draw(gc);
                }

                // 调试信息
                gc.setFill(Color.YELLOW);
                gc.fillText("子弹数: " + bullets.size() + " | 点击屏幕发射", 20, 30);
            }
        }.start();

        stage.setScene(new Scene(new Group(canvas)));
        stage.show();
    }

    // 辅助方法：绘制简易测试地图
    private void drawMap(GraphicsContext gc) {
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                Tile tile = map[r][c];
                if (tile.getType() == TileType.STONE) {
                    gc.setFill(Color.GRAY);
                    gc.fillRect(c * GameConfig.GRID_SIZE, r * GameConfig.GRID_SIZE,
                            GameConfig.GRID_SIZE - 1, GameConfig.GRID_SIZE - 1);
                } else if (tile.getType() == TileType.BRICK) {
                    gc.setFill(Color.BROWN);
                    gc.fillRect(c * GameConfig.GRID_SIZE, r * GameConfig.GRID_SIZE,
                            GameConfig.GRID_SIZE - 1, GameConfig.GRID_SIZE - 1);
                }
            }
        }
    }

    public static void main(String[] args) {
        // launch 会启动 JavaFX 声明周期，最终调用 start(Stage stage) 方法
        launch(args);
    }
}