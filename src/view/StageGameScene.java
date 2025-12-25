package view;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.*;
import model.Tank.TankType;

import java.util.ArrayList;
import java.util.List;

public class StageGameScene extends BaseGameScene {

    // 这里虽然写了 = new ArrayList<>(); 但在父类构造期间它还没运行
    private Tank player;
    private List<Tank> enemies;
    private List<Bullet> bullets;
    private Tile[][] map;
    private AnimationTimer gameLoop;

    public StageGameScene(Stage primaryStage) {
        super(primaryStage);
    }

    @Override
    protected void initModeSpecificLogic() {
        // 【核心修复】必须在这里手动初始化列表，否则报空指针
        this.enemies = new ArrayList<>();
        this.bullets = new ArrayList<>();

        // 1. 加载地图
        MapModel mapModel = new MapModel(MapModel.LEVEL_1);
        this.map = mapModel.getTiles();

        // 2. 创建玩家 (绿色)
        player = new PlayerTank(50, 50);

        // 3. 创建敌人 (作为靶子，分别放在不同位置)
        enemies.add(new NormalTank(300, 300));
        enemies.add(new NormalTank(500, 100));
        enemies.add(new NormalTank(600, 400));

        startGameLoop();
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        gameLoop.start();
    }

    private void update() {
        // --- 1. 玩家更新 ---
        if (player.isAlive()) {
            player.setMovingForward(inputHandler.isWPressed());
            player.setMovingBackward(inputHandler.isSPressed());
            player.setRotatingLeft(inputHandler.isAPressed());
            player.setRotatingRight(inputHandler.isDPressed());

            if (inputHandler.isJPressed()) {
                Bullet b = player.tryFire();
                if (b != null) {
                    bullets.add(b);
                }
            }
            player.update(map);
        }

        // --- 2. 敌人更新 ---
        for (Tank enemy : enemies) {
            if (enemy.isAlive()) {
                enemy.update(map);
            }
        }

        // --- 3. 子弹更新 ---
        bullets.removeIf(b -> !b.alive);

        for (Bullet b : bullets) {
            b.update(map);
        }

        // --- 4. 清理死掉的敌人 ---
        enemies.removeIf(enemy -> !enemy.isAlive());
    }

    private void render() {
        GraphicsContext gc = mapGc;

        // 清屏
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制地图底层
        spritePainter.drawMapBackground(gc, map);

        // 绘制敌人
        for (Tank enemy : enemies) {
            enemy.draw(gc);
        }

        // 绘制玩家
        if (player.isAlive()) {
            player.draw(gc);
        }

        // 绘制子弹
        for (Bullet b : bullets) {
            b.draw(gc);
        }

        // 绘制地图顶层
        spritePainter.drawMapForeground(gc, map);
    }
}