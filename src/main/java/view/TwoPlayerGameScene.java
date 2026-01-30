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
    
    // ======== 新增：暂停菜单 ========
    private ModernPauseMenu pauseMenu;

    private Tank player1;
    private Tank player2;
    // ======== 新增：射击状态控制 ========
    private boolean p1Shooting = false;
    private boolean p2Shooting = false;
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
        settingsWindow = new SettingsWindow(primaryStage, this);
        // 新增：初始化暂停菜单
        pauseMenu = new ModernPauseMenu(this, primaryStage);
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
                if (item.applyEffect((Tank) player2)) {
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
        // 使用父类的gameRoot，而不是创建新的局部变量
        gameRoot = new StackPane();
        Canvas tankCanvas = new Canvas(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        Canvas bulletCanvas = new Canvas(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        // mapTileView已非null，可安全添加
        gameRoot.getChildren().addAll(mapTileView, tankCanvas, bulletCanvas);
        scene = new Scene(gameRoot, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
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

    /**
     * 【修复版】射击逻辑
     * 使用 Tank.tryFire() 来接管射击，自动应用冷却时间和墙壁检测
     */
    private void shootBullet(Tank tank) {
        if (gameOver || tank == null) return;

        // 核心修改：调用 tank 自带的尝试开火方法
        // 它会检查冷却时间 (Fire Cooldown) 和 枪口是否卡墙
        // 如果冷却没好，或者枪口在墙里，它会返回 null
        Bullet bullet = tank.tryFire(twoPlayerTileMap);

        // 只有成功生成了子弹（即满足冷却条件），才添加到列表中并播放音效
        if (bullet != null) {
            bulletList.add(bullet);
            SoundManager.getInstance().playSoundEffect("shoot");
        }
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

    // ==========================================
    //   【优化】道具生成：防卡墙安全检查
    // ==========================================
    private void spawnRandomItem() {
        if (gameOver) return;

        // 尝试 20 次寻找一个合法位置
        for (int i = 0; i < 20; i++) {
            // 随机坐标
            double x = 50 + Math.random() * (GameConfig.SCREEN_WIDTH - 100);
            double y = 50 + Math.random() * (GameConfig.SCREEN_HEIGHT - 100);

            // 检查该位置是否是空地
            if (isValidItemPosition(x, y)) {
                // 随机选择道具类型
                ItemType type = getRandomItemType();

                // 生成道具 (居中修正)
                // 确保道具在格子中间，而不是压在网格线上
                double gridX = (int)(x / GameConfig.GRID_SIZE) * GameConfig.GRID_SIZE;
                double gridY = (int)(y / GameConfig.GRID_SIZE) * GameConfig.GRID_SIZE;

                itemSpawner.spawnItemAt(gridX, gridY, type);
                System.out.println("🎁 双人模式生成道具: " + type.getName() + " @ (" + (int)gridX + "," + (int)gridY + ")");
                return; // 成功生成后直接结束
            }
        }
        System.out.println("⚠️ 道具生成失败：未找到空闲位置");
    }

    /**
     * 【新增】检查坐标是否适合生成道具
     * 必须是空地 (EMPTY) 或 草地 (GRASS)
     */
    private boolean isValidItemPosition(double x, double y) {
        if (twoPlayerTileMap == null) return true; // 防空指针

        int c = (int) (x / GameConfig.GRID_SIZE);
        int r = (int) (y / GameConfig.GRID_SIZE);

        // 边界检查
        if (r < 0 || r >= GameConfig.MAP_ROWS || c < 0 || c >= GameConfig.MAP_COLS) {
            return false;
        }

        Tile tile = twoPlayerTileMap[r][c];
        if (tile == null) return true;

        // 获取地形类型
        TileType type = tile.getType();

        // 允许生成在：空地、草地
        // 禁止生成在：墙、钢块、水
        return type == TileType.EMPTY || type == TileType.GRASS;
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
        
        // 确保在游戏结束后暂停菜单也被移除
        if (gameOver && pauseMenu != null && pauseMenu.isPaused()) {
            pauseMenu.setPaused(false);
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
        player1.setSpeed(3);
        player1.setHealth(3);
        player1.setAlive(true);

        player1.buffFireRate(600);
        player1.activateShield(3.0);

        player2 = new NormalTank(PLAYER2_BIRTH_X, PLAYER2_BIRTH_Y);
        player2.setSpeed(3);
        player2.setHealth(3);
        player2.setAlive(true);
        player2.setLogicRotation(180.0);
        player2.setDisplayRotation(180.0);

        player2.buffFireRate(600);
        player2.activateShield(3.0);
    }

    private void bindTwoPlayerInput() {
        if (scene == null) return;

        // 按下按键：标记为正在射击
        scene.setOnKeyPressed(e -> {
            if (gameOver) return;

            switch (e.getCode()) {
                // P1 移动
                case W: if (player1.isAlive()) player1.setMovingForward(true); break;
                case S: if (player1.isAlive()) player1.setMovingBackward(true); break;
                case A: if (player1.isAlive()) player1.setRotatingLeft(true); break;
                case D: if (player1.isAlive()) player1.setRotatingRight(true); break;
                // P1 射击 (修改这里！)
                case J: if (player1.isAlive()) p1Shooting = true; break;

                // P2 移动
                case UP: if (player2.isAlive()) player2.setMovingForward(true); break;
                case DOWN: if (player2.isAlive()) player2.setMovingBackward(true); break;
                case LEFT: if (player2.isAlive()) player2.setRotatingLeft(true); break;
                case RIGHT: if (player2.isAlive()) player2.setRotatingRight(true); break;
                // P2 射击 (修改这里！)
                case K: if (player2.isAlive()) p2Shooting = true; break;
                
                // 暂停游戏
                case ESCAPE: 
                    if (!gameOver) {
                        pauseMenu.togglePause();
                    }
                    break;
            }
        });

        // 松开按键：取消射击状态
        scene.setOnKeyReleased(e -> {
            if (gameOver) return;
            switch (e.getCode()) {
                case W: player1.setMovingForward(false); break;
                case S: player1.setMovingBackward(false); break;
                case A: player1.setRotatingLeft(false); break;
                case D: player1.setRotatingRight(false); break;
                // P1 停止射击
                case J: p1Shooting = false; break;

                case UP: player2.setMovingForward(false); break;
                case DOWN: player2.setMovingBackward(false); break;
                case LEFT: player2.setRotatingLeft(false); break;
                case RIGHT: player2.setRotatingRight(false); break;
                // P2 停止射击
                case K: p2Shooting = false; break;
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

        // 1. 更新移动
        if (player1.isAlive()) player1.update(twoPlayerTileMap);
        if (player2.isAlive()) player2.update(twoPlayerTileMap);

        // 2. 【新增】处理连续射击逻辑
        // 只要按键按着，且人活着，就尝试开火
        // (Tank.tryFire 内部有冷却时间控制，所以不用担心射速过快，它会自动处理)
        if (player1.isAlive() && p1Shooting) {
            shootBullet(player1);
        }
        if (player2.isAlive() && p2Shooting) {
            shootBullet(player2);
        }

        // 3. 其他更新
        updateBullets();
        checkCollisions();
        checkTankDeathAndRebirth();
        checkGameOver();
        mapTileView.render(twoPlayerMap);
        updateItems();
    }
    @Override
    protected void renderGameFrame() {
        // 使用父类提供的画布和上下文
        clearCanvas(tankCanvas);
        clearCanvas(bulletCanvas);

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


    // 替换原有drawPlayerHUD方法
    private void drawPlayerHUD(GraphicsContext gc) {
        // 玩家1面板（左侧）
        drawPlayerPanel(gc, 20, 20, "P1", player1Lives, Color.web("#3498db"));

        // 玩家2面板（右侧）
        drawPlayerPanel(gc, WIDTH - 220, 20, "P2", player2Lives, Color.web("#e74c3c"));

        // 游戏时间显示（顶部中央）
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRoundRect(WIDTH/2 - 100, 20, 200, 40, 10, 10);
        gc.setStroke(Color.web("#f39c12"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(WIDTH/2 - 100, 20, 200, 40, 10, 10);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        gc.setFill(Color.WHITE);
        long playTime = (System.currentTimeMillis() - gameStartTime) / 1000;
        String timeText = String.format("战斗时间: %02d:%02d", playTime/60, playTime%60);
        double timeWidth = getTextWidth(gc, timeText);
        gc.fillText(timeText, WIDTH/2 - timeWidth/2, 47);
    }

    // 绘制玩家信息面板
    private void drawPlayerPanel(GraphicsContext gc, double x, double y, String player, int lives, Color color) {
        // 面板背景
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRoundRect(x, y, 200, 80, 10, 10);

        // 边框
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, 200, 80, 10, 10);

        // 玩家标识
        gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20));
        gc.setFill(color);
        gc.fillText(player, x + 20, y + 35);

        // 生命值
        gc.setFill(Color.WHITE);
        gc.fillText("生命: ", x + 20, y + 65);

        // 绘制生命图标
        double iconX = x + 80;
        for (int i = 0; i < 3; i++) {
            if (i < lives) {
                drawHeart(gc, iconX + i * 25, y + 55, 12);
            } else {
                gc.setStroke(Color.GRAY);
                drawHeartOutline(gc, iconX + i * 25, y + 55, 12);
            }
        }
        gc.setFill(color); // 恢复颜色
    }

    // 替换原有drawGameOverUI方法
    private void drawGameOverUI(GraphicsContext gc) {
        // 渐变背景遮罩
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.rgb(0,0,0,0.9)),
                new javafx.scene.paint.Stop(1, Color.rgb(30,30,60,0.95)));
        gc.setFill(grad);
        gc.fillRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

        // 胜利标题
        gc.setFont(Font.font("Impact", FontWeight.EXTRA_BOLD, 60));
        double tw = getTextWidth(gc, winner);
        double tx = (GameConfig.SCREEN_WIDTH - tw) / 2;
        double ty = GameConfig.SCREEN_HEIGHT / 2 - 50;

        // 文字阴影
        gc.setFill(Color.BLACK);
        gc.fillText(winner, tx + 5, ty + 5);

        // 渐变文字
        LinearGradient textGrad = new LinearGradient(0, 0, 0, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#f39c12")),
                new javafx.scene.paint.Stop(1, Color.web("#e67e22")));
        gc.setFill(textGrad);
        gc.fillText(winner, tx, ty);

        // 装饰线
        gc.setStroke(Color.web("#f39c12"));
        gc.setLineWidth(3);
        gc.strokeLine(tx - 20, ty + 20, tx + tw + 20, ty + 20);

        // 战斗统计
        gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        gc.setFill(Color.WHITE);

        long playTime = (System.currentTimeMillis() - gameStartTime) / 1000;
        String stats = String.format("战斗时长: %02d:%02d | 剩余生命: P1=%d, P2=%d",
                playTime/60, playTime%60, player1Lives, player2Lives);

        double statsWidth = getTextWidth(gc, stats);
        gc.fillText(stats, (GameConfig.SCREEN_WIDTH - statsWidth)/2, GameConfig.SCREEN_HEIGHT/2 + 20);

        // 按键提示
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        gc.fillText("按 ENTER 继续 | 按 ESC 返回菜单",
                (GameConfig.SCREEN_WIDTH - getTextWidth(gc, "按 ENTER 继续 | 按 ESC 返回菜单"))/2,
                GameConfig.SCREEN_HEIGHT/2 + 80);
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
            player1.activateShield(3.0);
        }

        if (!player2.isAlive() && player2Lives > 0) {
            player2Lives--;
            player2.setX(PLAYER2_BIRTH_X);
            player2.setY(PLAYER2_BIRTH_Y);
            player2.setHealth(3);
            player2.setAlive(true);
            player2.setLogicRotation(180.0);
            player2.setDisplayRotation(180.0);
            player2.activateShield(3.0);
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

    public SettingsWindow getSettingsWindow() {
        return settingsWindow;
    }

    public void setSettingsWindow(SettingsWindow settingsWindow) {
        this.settingsWindow = settingsWindow;
    }

    public Tank getPlayer1() {
        return player1;
    }

    public void setPlayer1(Tank player1) {
        this.player1 = player1;
    }

    public Tank getPlayer2() {
        return player2;
    }

    public void setPlayer2(Tank player2) {
        this.player2 = player2;
    }

    public boolean isP1Shooting() {
        return p1Shooting;
    }

    public void setP1Shooting(boolean p1Shooting) {
        this.p1Shooting = p1Shooting;
    }

    public boolean isP2Shooting() {
        return p2Shooting;
    }

    public void setP2Shooting(boolean p2Shooting) {
        this.p2Shooting = p2Shooting;
    }

    public double getPLAYER1_BIRTH_X() {
        return PLAYER1_BIRTH_X;
    }

    public double getPLAYER1_BIRTH_Y() {
        return PLAYER1_BIRTH_Y;
    }

    public double getPLAYER2_BIRTH_X() {
        return PLAYER2_BIRTH_X;
    }

    public double getPLAYER2_BIRTH_Y() {
        return PLAYER2_BIRTH_Y;
    }

    public List<Bullet> getBulletList() {
        return bulletList;
    }

    public void setBulletList(List<Bullet> bulletList) {
        this.bulletList = bulletList;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public int getPlayer1Lives() {
        return player1Lives;
    }

    public void setPlayer1Lives(int player1Lives) {
        this.player1Lives = player1Lives;
    }

    public int getPlayer2Lives() {
        return player2Lives;
    }

    public void setPlayer2Lives(int player2Lives) {
        this.player2Lives = player2Lives;
    }

    public MapModel getTwoPlayerMap() {
        return twoPlayerMap;
    }

    public void setTwoPlayerMap(MapModel twoPlayerMap) {
        this.twoPlayerMap = twoPlayerMap;
    }

    public Tile[][] getTwoPlayerTileMap() {
        return twoPlayerTileMap;
    }

    public void setTwoPlayerTileMap(Tile[][] twoPlayerTileMap) {
        this.twoPlayerTileMap = twoPlayerTileMap;
    }

    public MapTileView getMapTileView() {
        return mapTileView;
    }

    public void setMapTileView(MapTileView mapTileView) {
        this.mapTileView = mapTileView;
    }

    @Override
    public void setScene(Scene scene) {
        this.scene = scene;
    }

    @Override
    public ItemSpawner getItemSpawner() {
        return itemSpawner;
    }

    @Override
    public void setItemSpawner(ItemSpawner itemSpawner) {
        this.itemSpawner = itemSpawner;
    }

    @Override
    public List<ParticleEffect> getParticleEffects() {
        return particleEffects;
    }

    @Override
    public void setParticleEffects(List<ParticleEffect> particleEffects) {
        this.particleEffects = particleEffects;
    }

    public long getGameStartTime() {
        return gameStartTime;
    }

    public void setGameStartTime(long gameStartTime) {
        this.gameStartTime = gameStartTime;
    }


    /**
     * 绘制实心爱心（用于显示剩余生命）
     * @param gc 图形上下文
     * @param x 左上角 X
     * @param y 左上角 Y
     * @param size 爱心的大小（宽度/高度）
     */
    private void drawHeart(GraphicsContext gc, double x, double y, double size) {
        gc.beginPath();
        // 移动到爱心上方中间的凹陷处
        gc.moveTo(x + size / 2, y + size / 3.5);

        // 左半边曲线
        gc.bezierCurveTo(x + size / 2, y, x, y, x, y + size / 2);
        gc.bezierCurveTo(x, y + size * 0.75, x + size / 2, y + size, x + size / 2, y + size);

        // 右半边曲线
        gc.bezierCurveTo(x + size / 2, y + size, x + size, y + size * 0.75, x + size, y + size / 2);
        gc.bezierCurveTo(x + size, y, x + size / 2, y, x + size / 2, y + size / 3.5);

        gc.fill(); // 填充颜色（颜色由调用前的 gc.setFill 设置）
    }

    /**
     * 绘制空心爱心轮廓（用于显示已损失的生命）
     * @param gc 图形上下文
     * @param x 左上角 X
     * @param y 左上角 Y
     * @param size 爱心的大小
     */
    private void drawHeartOutline(GraphicsContext gc, double x, double y, double size) {
        gc.beginPath();
        // 移动到爱心上方中间的凹陷处
        gc.moveTo(x + size / 2, y + size / 3.5);

        // 左半边曲线
        gc.bezierCurveTo(x + size / 2, y, x, y, x, y + size / 2);
        gc.bezierCurveTo(x, y + size * 0.75, x + size / 2, y + size, x + size / 2, y + size);

        // 右半边曲线
        gc.bezierCurveTo(x + size / 2, y + size, x + size, y + size * 0.75, x + size, y + size / 2);
        gc.bezierCurveTo(x + size, y, x + size / 2, y, x + size / 2, y + size / 3.5);

        gc.stroke(); // 描边（颜色由调用前的 gc.setStroke 设置）
    }


}