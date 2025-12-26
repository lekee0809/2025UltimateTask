package view;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import model.*;
import map.MapModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 三关闯关模式游戏场景
 * 功能：3关卡切换 + 玩家血量显示 + 计时显示 + 敌方剩余血量显示
 */
public class StageGameScene extends BaseGameScene {

    // 游戏核心数据
    private Tank player;
    private List<Tank> enemies;
    private List<Bullet> bullets;
    private Tile[][] map;
    private AnimationTimer gameLoop;

    // 关卡配置
    private int currentLevel = 1; // 当前关卡（1/2/3）
    private final int TOTAL_LEVEL = 3; // 总关卡数

    // UI组件（血量、计时器、敌方血量）
    private Label playerHpLabel;    // 左上角玩家血量
    private Label timerLabel;       // 顶部中间计时器
    private Label enemyHpLabel;     // 右上角敌方血量

    // 计时相关
    private long startTime;         // 关卡开始时间（纳秒）
    private double elapsedTime;     // 已流逝时间（秒）

    public StageGameScene(Stage primaryStage) {
        super(primaryStage);
        // 初始化UI组件
        initGameUI();
    }

    /**
     * 初始化游戏专属UI（血量、计时器、敌方血量）
     */
    private void initGameUI() {
        // 1. 玩家血量标签（左上角）
        playerHpLabel = new Label();
        playerHpLabel.setFont(Font.font("Arial", 18));
        playerHpLabel.setTextFill(Color.RED);
        playerHpLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 5px;");
        playerHpLabel.setLayoutX(10);
        playerHpLabel.setLayoutY(10);

        // 2. 计时器标签（顶部中间）
        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("Arial", 18));
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 5px;");
        // 居中显示（基于屏幕宽度）
        timerLabel.setLayoutX((WIDTH - 60) / 2);
        timerLabel.setLayoutY(10);

        // 3. 敌方血量标签（右上角）
        enemyHpLabel = new Label();
        enemyHpLabel.setFont(Font.font("Arial", 18));
        enemyHpLabel.setTextFill(Color.ORANGE);
        enemyHpLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 5px;");
        enemyHpLabel.setLayoutX(WIDTH - 150);
        enemyHpLabel.setLayoutY(10);

        // 添加到根布局
        gameRoot.getChildren().addAll(playerHpLabel, timerLabel, enemyHpLabel);
    }

    @Override
    protected void initModeSpecificLogic() {
        // 初始化集合，解决空指针
        this.enemies = new ArrayList<>();
        this.bullets = new ArrayList<>();

        // 加载当前关卡资源
        loadLevelResources(currentLevel);

        // 启动游戏循环
        startGameLoop();

        // 显示关卡提示
        showTipText("第" + currentLevel + "关 - 消灭所有敌人！", 3.0);
    }

    /**
     * 加载指定关卡的资源（地图+玩家+敌人）
     * @param level 关卡数（1/2/3）
     */
    private void loadLevelResources(int level) {
        // 1. 重置计时
        startTime = System.nanoTime();
        elapsedTime = 0;

        // 2. 加载对应关卡地图
        MapModel mapModel = switch (level) {
            case 1 -> new MapModel(MapModel.LEVEL_1);
            case 2 -> new MapModel(MapModel.LEVEL_2);
            case 3 -> new MapModel(MapModel.LEVEL_3);
            default -> new MapModel(MapModel.LEVEL_1);
        };
        this.map = mapModel.getTiles();

        // 3. 初始化玩家（不同关卡可调整初始位置/血量）
        double playerX = switch (level) {
            case 1 -> 50;
            case 2 -> 100;
            case 3 -> 150;
            default -> 50;
        };
        player = new PlayerTank(playerX, 50);
        // 重置玩家血量（关卡切换后满血）
        player.resetHealth();

        // 4. 初始化对应关卡敌人（难度递增）
        initEnemiesByLevel(level);

        // 5. 清空子弹（避免关卡切换残留）
        bullets.clear();

        // 6. 更新UI初始值
        updatePlayerHpUI();
        updateEnemyHpUI();
        updateTimerUI();
    }

    /**
     * 根据关卡初始化敌人（难度递增：数量+血量+攻击力）
     * @param level 关卡数
     */
    private void initEnemiesByLevel(int level) {
        enemies.clear(); // 清空上一关敌人
        int enemyCount = switch (level) {
            case 1 -> 3;  // 第1关3个普通敌人
            case 2 -> 5;  // 第2关5个普通敌人
            case 3 -> 7;  // 第3关7个强化敌人
            default -> 3;
        };

        // 生成敌人（不同关卡位置/类型差异化）
        for (int i = 0; i < enemyCount; i++) {
            double enemyX = 200 + (i * 100); // 横向排列
            double enemyY = 100 + (level * 50); // 关卡越高，敌人位置越靠上
            Tank enemy;
            if (level == 3) {
                enemy = new HeavyTank(enemyX, enemyY); // 强化敌人（血量/攻击力更高）
            } else {
                enemy = new NormalTank(enemyX, enemyY); // 普通敌人
            }
            enemies.add(enemy);
        }
    }

    /**
     * 启动游戏循环（包含计时+UI更新）
     */
    private void startGameLoop() {
        // 防重复启动
        if (gameLoop != null) {
            gameLoop.stop();
        }

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                try {
                    // 更新计时
                    elapsedTime = (now - startTime) / 1_000_000_000.0;
                    // 更新游戏状态
                    update();
                    // 渲染画面
                    render();
                    // 更新UI（血量、计时、敌方血量）
                    updateAllUI();
                } catch (Exception e) {
                    System.err.println("游戏循环异常：" + e.getMessage());
                }
            }
        };
        gameLoop.start();
    }

    /**
     * 更新游戏状态（玩家/敌人/子弹/碰撞检测）
     */
    private void update() {
        // 1. 玩家更新
        if (player != null && player.isAlive()) {
            player.setMovingForward(inputHandler.isWPressed());
            player.setMovingBackward(inputHandler.isSPressed());
            player.setRotatingLeft(inputHandler.isAPressed());
            player.setRotatingRight(inputHandler.isDPressed());

            // 发射子弹
            if (inputHandler.isJPressed()) {
                Bullet b = player.tryFire();
                if (b != null) {
                    bullets.add(b);
                }
            }
            player.update(map);
        }

        // 2. 敌人更新（迭代器避免并发修改）
        Iterator<Tank> enemyIter = enemies.iterator();
        while (enemyIter.hasNext()) {
            Tank enemy = enemyIter.next();
            if (enemy.isAlive()) {
                enemy.update(map);
                // 敌人自动射击（难度递增：关卡越高，射击频率越高）
                if (Math.random() < (0.001 * currentLevel)) {
                    Bullet b = enemy.tryFire();
                    if (b != null) bullets.add(b);
                }
            } else {
                enemyIter.remove();
            }
        }

        // 3. 子弹更新+碰撞检测
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet b = bulletIter.next();
            if (b.isAlive()) {
                b.update(map);
                checkBulletCollision(b);
            } else {
                bulletIter.remove();
            }
        }

        // 4. 通关判断
        checkLevelClear();
    }

    /**
     * 子弹碰撞检测（击中玩家/敌人）
     */
    private void checkBulletCollision(Bullet bullet) {
        // 击中敌人
        for (Tank enemy : enemies) {
            if (enemy.isAlive() && isCollided(bullet, enemy)) {
                enemy.takeDamage(bullet.getDamage() * currentLevel); // 关卡越高，子弹伤害越高
                bullet.setAlive(false);
                break;
            }
        }

        // 击中玩家
        if (player != null && player.isAlive() && isCollided(bullet, player)) {
            player.takeDamage(bullet.getDamage());
            bullet.setAlive(false);
        }
    }

    /**
     * 矩形碰撞检测
     */
    private boolean isCollided(Bullet b, Tank t) {
        return b.getX() >= t.getX() && b.getX() <= t.getX() + t.getWidth()
                && b.getY() >= t.getY() && b.getY() <= t.getY() + t.getHeight();
    }

    /**
     * 通关判断（当前关卡所有敌人消灭）
     */
    private void checkLevelClear() {
        if (enemies.isEmpty() && player != null && player.isAlive()) {
            gameLoop.stop(); // 停止循环

            // 最后一关通关
            if (currentLevel == TOTAL_LEVEL) {
                showTipText("恭喜通关所有关卡！总用时：" + formatTime(elapsedTime), 0);
                inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.R, this::restartAllLevels);
            } else {
                // 进入下一关
                showTipText("第" + currentLevel + "关通关！按Enter进入下一关", 0);
                inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.ENTER, this::enterNextLevel);
            }
        }

        // 玩家死亡，关卡失败
        if (player != null && !player.isAlive()) {
            gameLoop.stop();
            showTipText("关卡失败！按R重新开始当前关卡", 0);
            inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.R, this::restartCurrentLevel);
        }
    }

    /**
     * 进入下一关
     */
    private void enterNextLevel() {
        currentLevel++;
        loadLevelResources(currentLevel);
        startGameLoop();
        showTipText("第" + currentLevel + "关开始！", 3.0);
    }

    /**
     * 重启当前关卡
     */
    private void restartCurrentLevel() {
        loadLevelResources(currentLevel);
        startGameLoop();
        showTipText("重新开始第" + currentLevel + "关", 2.0);
    }

    /**
     * 重启所有关卡（从第1关开始）
     */
    private void restartAllLevels() {
        currentLevel = 1;
        loadLevelResources(currentLevel);
        startGameLoop();
        showTipText("重新开始所有关卡", 2.0);
    }

    /**
     * 渲染游戏画面（分层渲染）
     */
    private void render() {
        GraphicsContext mapContext = mapGc;
        GraphicsContext tankContext = tankCanvas.getGraphicsContext2D();
        GraphicsContext bulletContext = bulletCanvas.getGraphicsContext2D();

        // 分层清屏
        mapContext.setFill(Color.BLACK);
        mapContext.fillRect(0, 0, WIDTH, HEIGHT);
        clearCanvas(tankContext);
        clearCanvas(bulletContext);

        // 绘制地图背景
        spritePainter.drawMapBackground(mapContext, map);

        // 绘制坦克
        for (Tank enemy : enemies) {
            if (enemy.isAlive()) {
                spritePainter.drawTank(tankContext, enemy);
            }
        }
        if (player != null && player.isAlive()) {
            spritePainter.drawTank(tankContext, player);
        }

        // 绘制子弹
        for (Bullet b : bullets) {
            if (b.isAlive()) {
                b.draw(bulletContext);
            }
        }

        // 绘制地图前景
        spritePainter.drawMapForeground(mapContext, map);
    }

    /**
     * 更新所有UI（血量+计时+敌方血量）
     */
    private void updateAllUI() {
        updatePlayerHpUI();
        updateTimerUI();
        updateEnemyHpUI();
    }

    /**
     * 更新玩家血量UI（左上角）
     */
    private void updatePlayerHpUI() {
        if (player == null) {
            playerHpLabel.setText("玩家血量：0/0");
            return;
        }
        String hpText = String.format("玩家血量：%.0f/%.0f", player.getHealth(), player.getMaxHealth());
        playerHpLabel.setText(hpText);
    }

    /**
     * 更新计时器UI（顶部中间）
     */
    private void updateTimerUI() {
        timerLabel.setText("计时：" + formatTime(elapsedTime));
    }

    /**
     * 更新敌方血量UI（右上角）
     */
    private void updateEnemyHpUI() {
        if (enemies.isEmpty()) {
            enemyHpLabel.setText("敌方剩余：0");
            return;
        }
        // 计算敌方总血量/剩余血量
        double totalHp = 0;
        double remainHp = 0;
        for (Tank enemy : enemies) {
            totalHp += enemy.getMaxHealth();
            remainHp += enemy.getHealth();
        }
        String enemyText = String.format("敌方血量：%.0f/%.0f", remainHp, totalHp);
        enemyHpLabel.setText(enemyText);
    }

    /**
     * 格式化时间（秒→分:秒）
     */
    private String formatTime(double seconds) {
        int mins = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    /**
     * 重置场景（释放资源）
     */
    @Override
    protected void resetScene() {
        super.resetScene();
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
        enemies.clear();
        bullets.clear();
        player = null;
        map = null;
    }
    /**
     * 重置模式专属数据（核心实现）
     * 子类必须实现的抽象方法
     */
    @Override
    protected void resetModeSpecificData() {
        // ========== 1. 重置数值型数据 ==========
        // 游戏时长归零
        this.elapsedTime = 0;
        // （可选）如果是重新开始当前关卡，保留关卡数；如果是重新开始所有关卡，重置为1
        // this.currentLevel = 1;

        // ========== 2. 重置玩家状态 ==========
        if (player != null) {
            // 重置玩家坦克位置（回到出生点）
            player.setX(WIDTH / 2 - 25);
            player.setY(HEIGHT - 80);
            // 重置坦克血量/状态
            player.setAlive(true);
            player.setHealth(100);

        }

        // ========== 3. 重置敌人数据 ==========
        // 清空现有敌人坦克
        for(Tank enemy:enemies){
          //  enemy.clear();
        }

        // （可选）重置敌人生成计数器/难度
        // enemySpawnCount = 0;

        // ========== 4. 重置玩家生命值（可选） ==========
        // 如果是关卡失败重置，保留剩余生命值；如果是重新开始，重置为初始值
        // this.playerLives = 3;

        // ========== 5. 重置地图相关（可选） ==========
        // 重新加载地图（避免地图元素被破坏后残留）
        //loadLevelMap(currentLevel);
    }
}