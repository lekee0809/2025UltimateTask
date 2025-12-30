package view;

import game.AppLauncher;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import infra.GameConfig;
import map.MapModel;
import map.MapTileView;
import model.*;

import java.util.ArrayList;
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

    // 修复1：构造代码块（优先于所有构造方法执行，强制初始化mapTileView）
    {
        mapTileView = new MapTileView();
        // 提前设置布局属性，避免后续重复设置
        mapTileView.setLayoutX(0);
        mapTileView.setLayoutY(0);
        mapTileView.setWidth(GameConfig.SCREEN_WIDTH);
        mapTileView.setHeight(GameConfig.SCREEN_HEIGHT);
    }

    public TwoPlayerGameScene(Stage primaryStage) {
        super(primaryStage); // 此时mapTileView已通过构造代码块初始化，非null
        initScene();
        // 新增：首次进入双人模式时，播放背景音乐
        settingsWindow = new SettingsWindow(primaryStage);
        SoundManager.getInstance().playGameMusic();
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
    }

    @Override
    protected void resetModeSpecificData() {
        gameOver = false;
        winner = "";
        player1Lives = 3;
        player2Lives = 3;
        bulletList.clear();
        initTwoPlayers();
        twoPlayerMap.reset(TWO_PLAYER_LEVEL);
        convertMapModelToTileArray();
        mapTileView.render(twoPlayerMap);
        mapTileView.reloadImages();
        twoPlayerMap.setCampaignMode(false);

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

        Alert gameOverAlert = new Alert(Alert.AlertType.CONFIRMATION);
        gameOverAlert.setTitle("游戏结束");
        gameOverAlert.setHeaderText(winner);
        gameOverAlert.setContentText("请选择后续操作：");
        gameOverAlert.initStyle(StageStyle.UTILITY);
        gameOverAlert.initOwner(primaryStage);

        ButtonType restartBtn = new ButtonType("重新开始");
        ButtonType backToMainBtn = new ButtonType("返回主界面");
        gameOverAlert.getButtonTypes().setAll(restartBtn, backToMainBtn);

        Optional<ButtonType> result = gameOverAlert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == restartBtn) {
                this.resetModeSpecificData();
                this.resumeGameProcess();
            } else if (result.get() == backToMainBtn) {
                // 1. 停止游戏背景音乐，避免与主菜单音频冲突
                SoundManager.getInstance().stopGameMusic();
                // 播放主菜单背景音
                SoundManager.getInstance().playBackgroundMusic();
                // 2. 核心修改：重新初始化 AppLauncher 主菜单
                AppLauncher mainMenu = new AppLauncher();
                mainMenu.start(primaryStage); // 调用 start 方法重建主菜单场景
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

            // 新增：ESC 键触发暂停
            if (e.getCode() == KeyCode.ESCAPE) {
                if (!GameConfig.isGamePaused()) { // 未暂停时才触发
                    this.pauseGameProcess();
                    settingsWindow.show(); // 显示设置窗口
                }
                return; // 避免和其他按键冲突
            }

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
        drawPlayerLives(tankGc);
        if (gameOver) drawGameOverUI(tankGc);
    }

    private void clearCanvas(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawPlayerLives(GraphicsContext gc) {
        gc.setFont(Font.font("微软雅黑", FontWeight.BOLD, 24));
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.setFill(Color.GREEN);
        String player1LifeText = "玩家1：" + player1Lives + " 命";
        gc.fillText(player1LifeText, 20, 40);
        gc.strokeText(player1LifeText, 20, 40);

        gc.setFill(Color.RED);
        String player2LifeText = "玩家2：" + player2Lives + " 命";
        double player2TextWidth = getTextWidth(gc, player2LifeText);
        gc.fillText(player2LifeText, GameConfig.SCREEN_WIDTH - player2TextWidth - 20, 40);
        gc.strokeText(player2LifeText, GameConfig.SCREEN_WIDTH - player2TextWidth - 20, 40);
    }

    private void drawGameOverUI(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        gc.setFill(Color.ORANGE);
        gc.setFont(Font.font("微软雅黑", FontWeight.BOLD, 48));
        double winnerWidth = getTextWidth(gc, winner);
        gc.fillText(winner, (GameConfig.SCREEN_WIDTH - winnerWidth) / 2, (GameConfig.SCREEN_HEIGHT / 2) - 50);
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.strokeText(winner, (GameConfig.SCREEN_WIDTH - winnerWidth) / 2, (GameConfig.SCREEN_HEIGHT / 2) - 50);
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
            this.pauseGameProcess();
            Platform.runLater(this::showGameOverDialog);
        } else if (player2Lives <= 0 && !gameOver) {
            gameOver = true;
            winner = "玩家1（蓝色坦克）胜利！";
            this.pauseGameProcess();
            Platform.runLater(this::showGameOverDialog);
        }
    }

    @Override
    protected void pauseGameProcess() {
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
}