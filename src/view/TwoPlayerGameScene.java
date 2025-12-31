package view;
import ranking.RankingManager;
import game.AppLauncher;
import item.Item;
import item.ItemSpawner;
import item.ItemType;
import item.ParticleEffect;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import infra.GameConfig;
import map.MapModel;
import map.MapTileView;
import model.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import ranking.PlayerRecord; // 新增：导入PlayerRecord

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class TwoPlayerGameScene extends BaseGameScene {

    // ======== 新增：SettingsWindow 成员变量 ========
    private SettingsWindow settingsWindow;

    private Tank player1;
    private Tank player2;
    private final double PLAYER1_BIRTH_X = 80;
    private final double PLAYER1_BIRTH_Y = 300;
    private final double PLAYER2_BIRTH_X = 700;
    private final double PLAYER2_BIRTH_Y = 280;
    private List<Bullet> bulletList = new ArrayList<>();
    private boolean gameOver = false;
    private String winner = "";
    private int player1Lives = 3;
    private int player2Lives = 3;
    private MapModel twoPlayerMap;
    private Tile[][] twoPlayerTileMap;
    private MapTileView mapTileView;
    private static final int TWO_PLAYER_LEVEL = 1;
    private Scene scene;
    private ItemSpawner itemSpawner;
    private List<ParticleEffect> particleEffects;
    // 新增：游戏开始时间戳（用于计算游玩时长）
    private long gameStartTime;

    // 修复1：构造代码块（优先于所有构造方法执行，强制初始化mapTileView）
    {
        mapTileView = new MapTileView();
        // 提前设置布局属性，避免后续重复设置
        mapTileView.setLayoutX(0);
        mapTileView.setLayoutY(0);
        mapTileView.setWidth(GameConfig.SCREEN_WIDTH);
        mapTileView.setHeight(GameConfig.SCREEN_HEIGHT);
        // 初始化游戏开始时间
        gameStartTime = System.currentTimeMillis();
    }

    public TwoPlayerGameScene(Stage primaryStage) {
        super(primaryStage); // 此时mapTileView已通过构造代码块初始化，非null
        initScene();
        // 新增：首次进入双人模式时，播放背景音乐
        settingsWindow = new SettingsWindow(primaryStage);
        SoundManager.getInstance().playGameMusic();
        // 新增：初始化道具系统
        itemSpawner = new ItemSpawner();
        particleEffects = new ArrayList<>();
        // 新增：启动道具生成
        scheduleItemSpawn();
    }

    // 3. 添加道具更新方法
    private void updateItems() {
        // 检查玩家1的道具拾取
        Iterator<Item> iterator = itemSpawner.getActiveItems().iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();

            // 更新道具动画
            item.updateAnimation();

            // 检查玩家1是否拾取道具
            if (player1.isAlive() && item.checkCollision(player1)) {
                if (item.applyEffect((PlayerTank) player1)) {
                    // 生成金色粒子特效
                    particleEffects.add(new ParticleEffect(
                            item.getX() + item.getWidth()/2,
                            item.getY() + item.getHeight()/2,
                            15, Color.GOLD, 0.5f
                    ));

                    // 如果是炸弹，对玩家2造成伤害
                    if (item.getType() == ItemType.BOMB) {
                        applyBombEffect(item, player1);
                    }

                    System.out.println("🎁 玩家1拾取道具: " + item.getType().getName());
                    iterator.remove();
                    continue;
                }
            }

            // 检查玩家2是否拾取道具
            if (player2.isAlive() && item.checkCollision(player2)) {
                if (item.applyEffect((PlayerTank) player2)) {
                    // 生成金色粒子特效
                    particleEffects.add(new ParticleEffect(
                            item.getX() + item.getWidth()/2,
                            item.getY() + item.getHeight()/2,
                            15, Color.GOLD, 0.5f
                    ));

                    // 如果是炸弹，对玩家1造成伤害
                    if (item.getType() == ItemType.BOMB) {
                        applyBombEffect(item, player2);
                    }

                    System.out.println("🎁 玩家2拾取道具: " + item.getType().getName());
                    iterator.remove();
                }
            }

            // 检查道具是否过期
            if (item.isExpired()) {
                iterator.remove();
                System.out.println("⏰ 道具过期消失: " + item.getType().getName());
            }
        }

        // 更新粒子特效
        particleEffects.removeIf(ParticleEffect::isFinished);
        for (ParticleEffect effect : particleEffects) {
            effect.update(0.016f); // 约60FPS
        }
    }

    // 4. 添加炸弹效果处理方法
    private void applyBombEffect(Item item, Tank picker) {
        // 双人模式中，炸弹只对对方玩家造成20点伤害
        if (item.getType() != ItemType.BOMB) return;

        if (picker == player1) {
            // 玩家1拾取了炸弹，对玩家2造成20点伤害
            if (player2.isAlive()) {
                player2.takeDamage(20);
                System.out.println("💣 玩家1拾取炸弹，对玩家2造成20点伤害");
            }
        } else if (picker == player2) {
            // 玩家2拾取了炸弹，对玩家1造成20点伤害
            if (player1.isAlive()) {
                player1.takeDamage(20);
                System.out.println("💣 玩家2拾取炸弹，对玩家1造成20点伤害");
            }
        }
    }

    private void initScene() {
        StackPane root = new StackPane();
        Canvas tankCanvas = new Canvas(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        Canvas bulletCanvas = new Canvas(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        // mapTileView已非null，可安全添加
        root.getChildren().addAll(mapTileView, tankCanvas, bulletCanvas);
        scene = new Scene(root, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        bindTwoPlayerInput();
    }

    public Scene getScene() {
        return scene;
    }

    @Override
    protected void initModeSpecificLogic() {
        // 修复2：方法内非空校验兜底，万无一失
        if (mapTileView == null) {
            mapTileView = new MapTileView();
            mapTileView.setLayoutX(0);
            mapTileView.setLayoutY(0);
            mapTileView.setWidth(GameConfig.SCREEN_WIDTH);
            mapTileView.setHeight(GameConfig.SCREEN_HEIGHT);
        }
        if (twoPlayerMap == null) {
            initMapModel();
        }
        convertMapModelToTileArray();
        mapTileView.render(twoPlayerMap); // 此时绝对非null，不会报错
        initTwoPlayers();
        // 新增：启动道具生成
        scheduleItemSpawn();
    }

    @Override
    protected void resetModeSpecificData() {
        gameOver = false;
        winner = "";
        player1Lives = 3;
        player2Lives = 3;
        bulletList.clear();
        // 新增：清理道具
        itemSpawner.clear();
        particleEffects.clear();
        initTwoPlayers();
        twoPlayerMap.reset(TWO_PLAYER_LEVEL);
        convertMapModelToTileArray();
        mapTileView.render(twoPlayerMap);
        mapTileView.reloadImages();
        twoPlayerMap.setCampaignMode(false);

        // 重置游戏开始时间
        gameStartTime = System.currentTimeMillis();

        SoundManager.getInstance().playBGM();
    }

    private void shootBullet(Tank tank) {
        if (gameOver || tank == null) return;

        boolean isEnemy = (tank.getType() != Tank.TankType.PLAYER_GREEN);
        int damage = tank.getBulletDamage();
        int direction = (int) tank.getDisplayRotation();
        double bulletSpeed = tank.getBulletSpeed();
        double radians = Math.toRadians(direction);
        double speedx = Math.sin(radians) * bulletSpeed;
        double speedy = -Math.cos(radians) * bulletSpeed;
        double muzzleX = tank.getCenterX() - GameConfig.BULLET_RADIUS;
        double muzzleY = tank.getCenterY() - GameConfig.BULLET_RADIUS;
        double bulletWidth = GameConfig.BULLET_RADIUS * 2;
        double bulletHeight = GameConfig.BULLET_RADIUS * 2;
        Bullet bullet = new Bullet(isEnemy, damage, direction, speedx, speedy, muzzleX, muzzleY, bulletWidth, bulletHeight);
        bulletList.add(bullet);

        SoundManager.getInstance().playSoundEffect("shoot");
    }

    // 8. 添加双人模式道具生成逻辑（例如通过随机事件生成）
    private void spawnItemRandomly() {
        // 双人模式的道具生成逻辑
        // 例如：每30秒有一定概率生成道具
        long currentTime = System.currentTimeMillis();
        long lastSpawnTime = 0;

        if (currentTime - lastSpawnTime > 30000) { // 30秒
            if (Math.random() < 0.3) { // 30%概率
                double x = Math.random() * (GameConfig.SCREEN_WIDTH - GameConfig.GRID_SIZE);
                double y = Math.random() * (GameConfig.SCREEN_HEIGHT - GameConfig.GRID_SIZE);
                Item item = Item.createRandomItem(x, y);

                // 需要修改ItemSpawner以支持手动添加道具
                // 这里先简单添加到activeItems（需要修改ItemSpawner的访问权限）
                itemSpawner.getActiveItems().add(item);
                lastSpawnTime = currentTime;
            }
        }
    }

    // 添加计时器定期生成道具
    private void scheduleItemSpawn() {
        // 使用 JavaFX 的 Timeline 代替 AnimationTimer，更简单
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(20 + Math.random() * 10), // 20-30秒间隔
                        e -> spawnRandomItem()
                )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void spawnRandomItem() {
        if (gameOver) return;

        // 在地图上随机位置生成道具
        double x = 50 + Math.random() * (GameConfig.SCREEN_WIDTH - 100);
        double y = 50 + Math.random() * (GameConfig.SCREEN_HEIGHT - 100);

        // 随机选择道具类型
        ItemType type = getRandomItemType();

        // 生成道具
        itemSpawner.spawnItemAt(x, y, type);
        System.out.println("🎁 双人模式生成随机道具: " + type.getName() + " 在位置 (" + x + ", " + y + ")");
    }

    // 添加辅助方法获取随机道具类型
    private ItemType getRandomItemType() {
        double rand = Math.random();
        if (rand < 0.4) {
            return ItemType.HEAL;           // 40% 概率
        } else if (rand < 0.7) {
            return ItemType.INVINCIBLE;     // 30% 概率
        } else {
            return ItemType.BOMB;           // 30% 概率
        }
    }

    private void checkCollisions() {
        List<Bullet> removeBulletList = new ArrayList<>();
        for (Bullet bullet : bulletList) {
            if (!bullet.alive) continue;

            if (!bullet.isEnemy && player2.isAlive() && isCollide(bullet, player2)) {
                player2.takeDamage(bullet.getDamage());
                bullet.alive = false;
                removeBulletList.add(bullet);
                SoundManager.getInstance().playSoundEffect("explosion");
                continue;
            }

            if (bullet.isEnemy && player1.isAlive() && isCollide(bullet, player1)) {
                player1.takeDamage(bullet.getDamage());
                bullet.alive = false;
                removeBulletList.add(bullet);
                SoundManager.getInstance().playSoundEffect("explosion");
                continue;
            }
        }
        bulletList.removeAll(removeBulletList);

        if (player1.isAlive() && player2.isAlive() && isCollide(player1, player2)) {
            resolveTankOverlap(player1, player2);
        }
    }

    private void showGameOverDialog() {
        if (!gameOver) {
            return;
        }

        // 创建自定义对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(primaryStage);
        dialog.setTitle("游戏结束");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        // 设置对话框位置（固定在左上角）
        dialog.setOnShown(e -> {
            Window window = dialog.getDialogPane().getScene().getWindow();
            window.setX(primaryStage.getX() + 50);  // 距离主窗口左边50像素
            window.setY(primaryStage.getY() + 50);  // 距离主窗口上边50像素
        });

        // 创建自定义内容面板
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new javafx.geometry.Insets(20));
        contentBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #f39c12;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;");

        // 标题
        Label titleLabel = new Label("🎮 游戏结束");
        titleLabel.setFont(Font.font("微软雅黑", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.WHITE);

        // 胜利者信息
        Label winnerLabel = new Label(winner);
        winnerLabel.setFont(Font.font("微软雅黑", FontWeight.BOLD, 20));

        // 根据胜利者设置颜色
        if (winner.contains("玩家1")) {
            winnerLabel.setTextFill(Color.rgb(0, 180, 255)); // 蓝色
        } else {
            winnerLabel.setTextFill(Color.rgb(255, 80, 80)); // 红色
        }

        // 提示文本
        Label hintLabel = new Label("请选择后续操作：");
        hintLabel.setFont(Font.font("微软雅黑", FontWeight.NORMAL, 14));
        hintLabel.setTextFill(Color.rgb(180, 180, 180));

        // 添加内容到面板
        contentBox.getChildren().addAll(titleLabel, winnerLabel, hintLabel);

        // 创建自定义按钮
        ButtonType restartBtn = new ButtonType("🔄 重新开始", ButtonBar.ButtonData.OK_DONE);
        ButtonType backBtn = new ButtonType("🏠 返回主界面", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(restartBtn, backBtn);

        // 获取按钮并自定义样式
        Button restartButton = (Button) dialog.getDialogPane().lookupButton(restartBtn);
        Button backButton = (Button) dialog.getDialogPane().lookupButton(backBtn);

        restartButton.setStyle("-fx-background-color: #2ecc71;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5;" +
                "-fx-padding: 8 15;");

        backButton.setStyle("-fx-background-color: #3498db;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5;" +
                "-fx-padding: 8 15;");

        // 设置对话框内容
        dialog.getDialogPane().setContent(contentBox);
        dialog.getDialogPane().setPrefSize(400, 200);

        // 显示对话框并处理结果
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == restartBtn) {
                this.resetModeSpecificData();
                this.resumeGameProcess();
            } else if (result.get() == backBtn) {
                // 1. 停止游戏背景音乐，避免与主菜单音频冲突
                SoundManager.getInstance().stopGameMusic();
                SoundManager.getInstance().playBackgroundMusic();
                // 2. 核心修改：重新初始化 AppLauncher 主菜单
                AppLauncher mainMenu = new AppLauncher();
                mainMenu.start(primaryStage);
            }
        }
    }

    private void initMapModel() {
        twoPlayerMap = new MapModel(TWO_PLAYER_LEVEL);
        twoPlayerMap.setCampaignMode(false);
    }

    private void convertMapModelToTileArray() {
        twoPlayerTileMap = twoPlayerMap.getTiles();
    }

    private void initTwoPlayers() {
        player1 = new PlayerTank(PLAYER1_BIRTH_X, PLAYER1_BIRTH_Y);
        player1.setSpeed(5);
        player1.setHealth(3);
        player1.setAlive(true);

        player2 = new NormalTank(PLAYER2_BIRTH_X, PLAYER2_BIRTH_Y);
        player2.setSpeed(5);
        player2.setHealth(3);
        player2.setAlive(true);
        player2.setLogicRotation(180.0);
        player2.setDisplayRotation(180.0);
    }

    private void bindTwoPlayerInput() {
        if (scene == null) {
            System.err.println("警告：scene为空，无法绑定双玩家输入！");
            return;
        }

        scene.setOnKeyPressed(e -> {
            if (gameOver) return;


            switch (e.getCode()) {
                case W: if (player1.isAlive()) player1.setMovingForward(true); break;
                case S: if (player1.isAlive()) player1.setMovingBackward(true); break;
                case A: if (player1.isAlive()) player1.setRotatingLeft(true); break;
                case D: if (player1.isAlive()) player1.setRotatingRight(true); break;
                case J: if (player1.isAlive()) shootBullet(player1); break;
                case UP: if (player2.isAlive()) player2.setMovingForward(true); break;
                case DOWN: if (player2.isAlive()) player2.setMovingBackward(true); break;
                case LEFT: if (player2.isAlive()) player2.setRotatingLeft(true); break;
                case RIGHT: if (player2.isAlive()) player2.setRotatingRight(true); break;
                case K: if (player2.isAlive()) shootBullet(player2); break;
            }
        });

        scene.setOnKeyReleased(e -> {
            if (gameOver) return;
            switch (e.getCode()) {
                case W: player1.setMovingForward(false); break;
                case S: player1.setMovingBackward(false); break;
                case A: player1.setRotatingLeft(false); break;
                case D: player1.setRotatingRight(false); break;
                case UP: player2.setMovingForward(false); break;
                case DOWN: player2.setMovingBackward(false); break;
                case LEFT: player2.setRotatingLeft(false); break;
                case RIGHT: player2.setRotatingRight(false); break;
            }
        });
    }

    private boolean isCollide(Bullet bullet, Tank tank) {
        return bullet.getX() >= tank.getX() && bullet.getX() <= tank.getX() + tank.getWidth()
                && bullet.getY() >= tank.getY() && bullet.getY() <= tank.getY() + tank.getHeight();
    }

    private boolean isCollide(Tank t1, Tank t2) {
        return t1.getX() < t2.getX() + t2.getWidth() && t1.getX() + t1.getWidth() > t2.getX()
                && t1.getY() < t2.getY() + t2.getHeight() && t1.getY() + t1.getHeight() > t2.getY();
    }

    private void resolveTankOverlap(Tank t1, Tank t2) {
        double pushForce = t1.getSpeed();
        double dx = t1.getCenterX() - t2.getCenterX();
        double dy = t1.getCenterY() - t2.getCenterY();
        if (Math.abs(dx) > Math.abs(dy)) {
            t1.setX(t1.getX() + (dx > 0 ? pushForce : -pushForce));
        } else {
            t1.setY(t1.getY() + (dy > 0 ? pushForce : -pushForce));
        }
    }

    private double getTextWidth(GraphicsContext gc, String text) {
        try {
            javafx.scene.text.Text tempText = new javafx.scene.text.Text(text);
            tempText.setFont(gc.getFont());
            return tempText.getLayoutBounds().getWidth();
        } catch (Exception e) {
            System.err.println("计算文本宽度异常: " + e.getMessage());
            return text.length() * 15;
        }
    }

    @Override
    protected void updateGameLogic() {
        if (gameOver) return;
        if (player1.isAlive()) player1.update(twoPlayerTileMap);
        if (player2.isAlive()) player2.update(twoPlayerTileMap);
        updateBullets();
        checkCollisions();
        checkTankDeathAndRebirth();
        checkGameOver();
        mapTileView.render(twoPlayerMap);
        // 新增：更新道具系统
        updateItems();
    }

    @Override
    protected void renderGameFrame() {
        ObservableList<Node> rootChildren = scene.getRoot().getChildrenUnmodifiable();
        Canvas tankCanvas = (Canvas) rootChildren.get(1);
        Canvas bulletCanvas = (Canvas) rootChildren.get(2);

        clearCanvas(tankCanvas);
        clearCanvas(bulletCanvas);

        GraphicsContext tankGc = tankCanvas.getGraphicsContext2D();
        GraphicsContext bulletGc = bulletCanvas.getGraphicsContext2D();

        if (player1.isAlive()) player1.draw(tankGc);
        if (player2.isAlive()) player2.draw(tankGc);
        for (Bullet bullet : bulletList) {
            if (bullet.alive) bullet.draw(bulletGc);
        }
        // 新增：绘制道具
        for (Item item : itemSpawner.getActiveItems()) {
            spritePainter.drawItem(tankGc, item);
        }

        // 新增：绘制粒子特效
        for (ParticleEffect effect : particleEffects) {
            spritePainter.drawParticleEffect(bulletGc, effect);
        }
        drawPlayerHUD(tankGc);
        if (gameOver) drawGameOverUI(tankGc);
    }

    private void clearCanvas(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawPlayerHUD(GraphicsContext gc) {
        gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));

        // 绘制 P1 背景框 (绿色系)
        gc.setFill(Color.web("#27AE60", 0.8));
        gc.fillRoundRect(20, 20, 160, 40, 10, 10);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(20, 20, 160, 40, 10, 10);

        // 绘制 P1 文字
        gc.setFill(Color.WHITE);
        gc.fillText("P1 剩余生命: " + player1Lives, 35, 47);

        // 绘制 P2 背景框 (红色系)
        double p2X = GameConfig.SCREEN_WIDTH - 180;
        gc.setFill(Color.web("#C0392B", 0.8));
        gc.fillRoundRect(p2X, 20, 160, 40, 10, 10);
        gc.setStroke(Color.WHITE);
        gc.strokeRoundRect(p2X, 20, 160, 40, 10, 10);

        // 绘制 P2 文字
        gc.setFill(Color.WHITE);
        gc.fillText("P2 剩余生命: " + player2Lives, p2X + 15, 47);
    }

    private void drawGameOverUI(GraphicsContext gc) {
        // 1. 全屏渐变压暗背景
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0,0,0,0.85)),
                new Stop(1, Color.rgb(20,20,40,0.95)));
        gc.setFill(grad);
        gc.fillRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

        // 2. 准备字体
        gc.setFont(Font.font("Microsoft YaHei", FontWeight.EXTRA_BOLD, 50));
        double tw = getTextWidth(gc, winner);
        double tx = (GameConfig.SCREEN_WIDTH - tw) / 2;
        double ty = GameConfig.SCREEN_HEIGHT / 2;

        // 3. 绘制文字阴影
        gc.setFill(Color.BLACK);
        gc.fillText(winner, tx + 4, ty + 4);

        // 4. 绘制金色渐变文字
        LinearGradient textGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.GOLD),
                new Stop(1, Color.ORANGE));
        gc.setFill(textGrad);
        gc.fillText(winner, tx, ty);

        // 5. 绘制装饰线
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(3);
        gc.strokeLine(tx, ty + 15, tx + tw, ty + 15);
    }

    private void updateBullets() {
        List<Bullet> removeList = new ArrayList<>();
        for (Bullet bullet : bulletList) {
            if (!bullet.alive) {
                removeList.add(bullet);
                continue;
            }
            bullet.update(twoPlayerTileMap);
        }
        bulletList.removeAll(removeList);
    }

    private void checkTankDeathAndRebirth() {
        if (!player1.isAlive() && player1Lives > 0) {
            player1Lives--;
            player1.setX(PLAYER1_BIRTH_X);
            player1.setY(PLAYER1_BIRTH_Y);
            player1.setHealth(3);
            player1.setAlive(true);
            player1.setLogicRotation(0.0);
            player1.setDisplayRotation(0.0);
        }

        if (!player2.isAlive() && player2Lives > 0) {
            player2Lives--;
            player2.setX(PLAYER2_BIRTH_X);
            player2.setY(PLAYER2_BIRTH_Y);
            player2.setHealth(3);
            player2.setAlive(true);
            player2.setLogicRotation(180.0);
            player2.setDisplayRotation(180.0);
        }
    }

    private void checkGameOver() {
        if (player1Lives <= 0 && !gameOver) {
            gameOver = true;
            winner = "玩家2（红色坦克）胜利！";
            // 新增：写入游戏记录
            writeGameRecord(false); // 玩家1失败，对应记录isWin=false
            this.pauseGameProcess();
            Platform.runLater(this::showGameOverDialog);
        } else if (player2Lives <= 0 && !gameOver) {
            gameOver = true;
            winner = "玩家1（蓝色坦克）胜利！";
            // 新增：写入游戏记录
            writeGameRecord(true); // 玩家1胜利，对应记录isWin=true
            this.pauseGameProcess();
            Platform.runLater(this::showGameOverDialog);
        }
    }

    // 新增：封装游戏记录写入逻辑
    private void writeGameRecord(boolean isPlayer1Win) {
        // 计算游玩时长（秒）
        long playTimeSeconds = (System.currentTimeMillis() - gameStartTime) / 1000;
        // 2. 计算最终得分（自定义适配你的业务逻辑）
        int finalScore;
        if (isPlayer1Win) {
            finalScore =  200; // 玩家1胜利得分
        } else {
            finalScore =  200; // 玩家2胜利得分（若需记录获胜方得分，可修改此处）
        }

        // 3. 道具数（无道具系统则传 0）
        int itemCount = 0;
        if (itemSpawner != null) {
            itemCount = itemSpawner.getCollectedItems().size();
        }

        // 4. 核心：调用 RankingManager 写入双人模式记录
        RankingManager.addRecord(
                finalScore,
                (int) playTimeSeconds,
                PlayerRecord.GameMode.DOUBLE_BATTLE
        );

    }

    @Override
    public void pauseGameProcess() {
        super.pauseGameProcess();
        SoundManager.getInstance().pauseGameMusic();
    }

    @Override
    protected void resumeGameProcess() {
        super.resumeGameProcess();
        if (gameLoop != null && !gameOver) {
            gameLoop.start();
            SoundManager.getInstance().resumeGameMusic();
        }
    }

    @Override
    protected PlayerTank getPlayerTank() {
        // 双人模式返回玩家1（或按需返回，不影响记录逻辑）
        return (PlayerTank) player1;
    }

    // 核心：实现父类抽象方法，返回双人对战模式
    @Override
    protected PlayerRecord.GameMode getCurrentGameMode() {
        return PlayerRecord.GameMode.DOUBLE_BATTLE;
    }
}