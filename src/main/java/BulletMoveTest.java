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

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 1. 鼠标点击：仅负责“产生”数据
        canvas.setOnMouseClicked(e -> {
            double angle = Math.random() * 360;
            double rad = Math.toRadians(angle);
            // 计算分速度
            double vx = Math.cos(rad) * GameConfig.BULLET_SPEED;
            double vy = Math.sin(rad) * GameConfig.BULLET_SPEED;

            double size = GameConfig.BULLET_RADIUS * 2;

            // 修正：让子弹中心对准鼠标点击位置
            double startX = e.getX() - GameConfig.BULLET_RADIUS;
            double startY = e.getY() - GameConfig.BULLET_RADIUS;

            bullets.add(new Bullet(
                    false,              // isEnemy
                    1,                  // damage
                    (int)angle,         // direction
                    vx,                 // speedx
                    vy,                 // speedy
                    startX,             // x
                    startY,             // y
                    size,               // width
                    size                // height
            ));
        });

        // 2. 核心动画计时器：独立运行，负责“处理”数据
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 清屏：使用你定义的深蓝色
                gc.setFill(Color.web("#2c3e50"));
                gc.fillRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

                // 更新与绘制 (倒序遍历或使用 removeIf)
                bullets.removeIf(b -> !b.alive);
                for (Bullet b : bullets) {
                    b.update();
                    b.draw(gc);
                }

                // 实时调试文字
                gc.setFill(Color.WHITE);
                gc.fillText("当前屏内子弹数: " + bullets.size(), 20, 30);
            }
        }.start(); // 启动一次即可

        stage.setScene(new Scene(new Group(canvas)));
        stage.setTitle("坦克大战子弹物理模型测试 - 指挥官 Lekee");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}