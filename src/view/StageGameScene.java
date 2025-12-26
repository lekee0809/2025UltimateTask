package view;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import infra.GameConfig;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import map.MapModel;
import map.GameLevelConfig;
import map.EnemySpawn;
import model.*;
import model.Tank.TankType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 闯关模式游戏场景类
 * 核心功能：
 * 1. 加载不同关卡的地图配置
 * 2. 根据关卡难度生成不同数量和类型的敌人坦克
 * 3. 显示玩家血量、游戏时间、当前分数
 * 4. 过关判定：消灭所有敌人且达到目标分数
 * 5. 多关卡支持（当前3关）
 */
public class StageGameScene extends BaseGameScene {

    // ========== 游戏核心对象 ==========
    private PlayerTank player;              // 玩家坦克
    private List<Tank> enemyTanks;         // 敌人坦克列表
    private List<Bullet> bullets;          // 子弹列表
    private MapModel mapModel;             // 地图模型
    private Tile[][] map;                  // 地图瓦片数组

    // ========== 游戏状态变量 ==========
    private int currentLevel;              // 当前关卡编号（1-3）
    private int playerScore;               // 玩家当前得分
    private int playerHealth;              // 玩家当前血量（显示用）
    private long levelStartTime;           // 关卡开始时间戳（毫秒）
    private long gameElapsedTime;          // 游戏已进行时间（秒）
    private boolean isGameOver;            // 游戏结束标志
    private boolean isLevelComplete;       // 关卡完成标志
    private int targetScore;               // 当前关卡目标分数
    // ========== 随机数生成器 ==========
    private Random random;                 // 修复：延迟初始化

    // ========== 敌人AI相关 ==========
    private static final long ENEMY_AI_UPDATE_INTERVAL = 1000; // 敌人AI更新间隔（毫秒）
    private long lastEnemyAIUpdateTime = 0; // 上次AI更新时间

    // ========== 界面常量 ==========
    private static final Color HUD_TEXT_COLOR = Color.WHITE;
    private static final Color HEALTH_COLOR = Color.RED;
    private static final Color SCORE_COLOR = Color.GOLD;
    private static final Color TIME_COLOR = Color.CYAN;
    private static final Color LEVEL_COLOR = Color.LIMEGREEN;
    private static final Color GAME_OVER_COLOR = Color.RED;
    private static final Color LEVEL_COMPLETE_COLOR = Color.YELLOW;

    private static final Font HUD_FONT_SMALL = Font.font("Arial", 16);
    private static final Font HUD_FONT_MEDIUM = Font.font("Arial", 20);
    private static final Font HUD_FONT_LARGE = Font.font("Arial Bold", 32);

    // ========== 构造函数 ==========
    public StageGameScene(Stage stage) {
        super(stage);
    }

    @Override
    protected void resetModeSpecificData() {
        //
    }

    @Override
    protected void initModeSpecificLogic() {
        // 初始化随机数生成器（修复NullPointerException）
        random = new Random();

        // 初始化游戏状态
        currentLevel = 1;
        playerScore = 0;
        playerHealth = GameConfig.PLAYER_HEALTH;
        isGameOver = false;
        isLevelComplete = false;

        // 初始化对象列表
        enemyTanks = new ArrayList<>();
        bullets = new ArrayList<>();

        System.out.println("🚀 开始初始化闯关模式...");

        try {
            // 加载第一关
            loadLevel(currentLevel);

            // 启动游戏主循环
            System.out.println("✅ 闯关模式初始化完成");
        } catch (Exception e) {
            System.err.println("❌ 闯关模式初始化失败: " + e.getMessage());
            e.printStackTrace();
            // 出错时返回主菜单
            returnToMainMenu();
        }
    }

    @Override
    protected void resetModeSpecificData() {

    }

    // ========== 返回主菜单方法 ==========
    private void returnToMainMenu() {
        System.out.println("⚠️ 返回主菜单");
        if (gameLoop != null) {
            gameLoop.stop();
        }
        // 这里需要调用返回主菜单的逻辑，你需要根据你的项目结构来实现
        // 例如：StartScene startScene = new StartScene(primaryStage);
        // primaryStage.setScene(startScene.getScene());
    }

    // ========== 关卡加载系统 ==========
    /**
     * 加载指定关卡
     * @param level 关卡编号（1-3）
     */
    private void loadLevel(int level) {
        System.out.println("🚀 开始加载第 " + level + " 关...");

        // 重置关卡状态
        isLevelComplete = false;
        levelStartTime = System.currentTimeMillis();
        gameElapsedTime = 0;

        // 获取当前关卡的目标分数
        targetScore = GameLevelConfig.getTargetScore(level);

        try {
            // 1. 加载地图
            System.out.println("📝 加载地图...");
            mapModel = new MapModel(level);
            map = mapModel.getTiles();
            System.out.println("✅ 地图加载完成，尺寸: " + map.length + "x" + (map.length > 0 ? map[0].length : 0));

            // 2. 初始化玩家坦克
            System.out.println("🎮 初始化玩家坦克...");
            initializePlayerTank(level);

            // 3. 生成敌人坦克
            System.out.println("🤖 生成敌人坦克...");
            generateEnemyTanks(level);

            // 4. 清空子弹
            bullets.clear();

            System.out.println("✅ 第 " + level + " 关加载完成！");
            System.out.println("   目标分数: " + targetScore);
            System.out.println("   敌人数量: " + enemyTanks.size());
            System.out.println("   玩家血量: " + playerHealth);
            System.out.println("   地图大小: " + GameConfig.MAP_ROWS + "x" + GameConfig.MAP_COLS);

        } catch (Exception e) {
            System.err.println("❌ 加载第 " + level + " 关失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("关卡加载失败", e);
        }
    }

    /**
     * 初始化玩家坦克
     * 根据关卡不同，玩家可能出现在不同位置
     */
    private void initializePlayerTank(int level) {
        double playerX, playerY;

        // 根据不同关卡设置不同的出生点
        switch (level) {
            case 1:
                playerX = 100; // 左下角
                playerY = GameConfig.SCREEN_HEIGHT - 150;
                break;
            case 2:
                playerX = GameConfig.SCREEN_WIDTH / 2 - 50; // 中间偏左
                playerY = GameConfig.SCREEN_HEIGHT - 100;
                break;
            case 3:
                playerX = GameConfig.SCREEN_WIDTH - 150; // 右下角
                playerY = GameConfig.SCREEN_HEIGHT - 150;
                break;
            default:
                playerX = 100;
                playerY = 100;
        }

        // 确保出生点不在地图障碍物上
        playerX = adjustSpawnPosition(playerX, playerY, true);

        player = new PlayerTank(playerX, playerY);
        playerHealth = player.getHealth(); // 同步血量显示

        System.out.println("✅ 玩家坦克初始化完成，位置: (" + playerX + ", " + playerY + ")");
    }

    /**
     * 生成敌人坦克
     * 根据关卡配置生成不同数量和类型的敌人
     */
    private void generateEnemyTanks(int level) {
        enemyTanks.clear();

        // 获取当前关卡的敌人配置
        EnemySpawn[] enemyConfigs = GameLevelConfig.getEnemyConfig(level);

        System.out.println("📊 敌人配置: " + (enemyConfigs != null ? enemyConfigs.length : 0) + " 种类型");

        for (EnemySpawn config : enemyConfigs) {
            TankType type = config.type;
            int count = config.count;

            System.out.println("   - " + type + ": " + count + " 辆");

            for (int i = 0; i < count; i++) {
                // 生成敌人坦克
                Tank enemy = createEnemyTank(type, level);
                if (enemy != null) {
                    enemyTanks.add(enemy);
                }
            }
        }

        System.out.println("✅ 生成 " + enemyTanks.size() + " 个敌人坦克");
    }

    /**
     * 创建单个敌人坦克
     */
    private Tank createEnemyTank(TankType type, int level) {
        if (random == null) {
            System.err.println("❌ 随机数生成器未初始化！");
            random = new Random(); // 紧急初始化
        }

        double enemyX, enemyY;

        // 根据不同关卡设置不同的敌人出生区域
        switch (level) {
            case 1: // 第一关：敌人在上半部分随机生成
                enemyX = 100 + random.nextDouble() * (GameConfig.SCREEN_WIDTH - 200);
                enemyY = 100 + random.nextDouble() * (GameConfig.SCREEN_HEIGHT / 2 - 150);
                break;
            case 2: // 第二关：敌人在两侧生成
                if (random.nextBoolean()) {
                    enemyX = 50 + random.nextDouble() * 100; // 左侧
                } else {
                    enemyX = GameConfig.SCREEN_WIDTH - 150 + random.nextDouble() * 100; // 右侧
                }
                enemyY = 100 + random.nextDouble() * (GameConfig.SCREEN_HEIGHT / 2);
                break;
            case 3: // 第三关：敌人在上半部分和两侧都有
                if (random.nextBoolean()) {
                    enemyX = 100 + random.nextDouble() * (GameConfig.SCREEN_WIDTH - 200);
                    enemyY = 80 + random.nextDouble() * 100;
                } else {
                    enemyX = random.nextBoolean() ?
                            50 + random.nextDouble() * 100 :
                            GameConfig.SCREEN_WIDTH - 150 + random.nextDouble() * 100;
                    enemyY = 150 + random.nextDouble() * 200;
                }
                break;
            default:
                enemyX = 100 + random.nextDouble() * 500;
                enemyY = 100 + random.nextDouble() * 300;
        }

        // 确保敌人不在障碍物上生成
        enemyX = adjustSpawnPosition(enemyX, enemyY, false);

        // 根据类型创建不同的敌人坦克
        Tank enemy = null;
        try {
            switch (type) {
                case ENEMY_NORMAL:
                    enemy = new NormalTank(enemyX, enemyY);
                    break;
                case ENEMY_FAST:
                    enemy = new FastTank(enemyX, enemyY);
                    break;
                case ENEMY_HEAVY:
                    enemy = new HeavyTank(enemyX, enemyY);
                    break;
                default:
                    System.err.println("❌ 未知的坦克类型: " + type);
                    return null;
            }

            if (enemy != null) {
                System.out.println("   ✓ 创建 " + type + " 坦克，位置: (" + enemyX + ", " + enemyY + ")");
            }

        } catch (Exception e) {
            System.err.println("❌ 创建敌人坦克失败: " + e.getMessage());
            e.printStackTrace();
        }

        return enemy;
    }

    /**
     * 调整出生位置，确保不在地图障碍物上
     */
    private double adjustSpawnPosition(double x, double y, boolean isPlayer) {
        if (map == null || map.length == 0) {
            System.err.println("⚠️ 地图未初始化，无法调整出生位置");
            return x;
        }

        double adjustedX = x;
        double adjustedY = y;
        int maxAttempts = 10; // 减少尝试次数以提高性能
        int attempt = 0;

        while (attempt < maxAttempts) {
            // 检查坦克四个角是否在可通行区域
            boolean canSpawn = true;

            // 检查坦克矩形区域的四个角
            double[] cornersX = {adjustedX, adjustedX + GameConfig.TANK_SIZE,
                    adjustedX, adjustedX + GameConfig.TANK_SIZE};
            double[] cornersY = {adjustedY, adjustedY,
                    adjustedY + GameConfig.TANK_SIZE, adjustedY + GameConfig.TANK_SIZE};

            for (int i = 0; i < 4; i++) {
                int col = (int) (cornersX[i] / GameConfig.GRID_SIZE);
                int row = (int) (cornersY[i] / GameConfig.GRID_SIZE);

                // 边界检查
                if (row < 0 || row >= GameConfig.MAP_ROWS ||
                        col < 0 || col >= GameConfig.MAP_COLS) {
                    canSpawn = false;
                    break;
                }

                // 检查瓦片是否可通行
                if (map[row][col] != null) {
                    Tile tile = map[row][col];
                    if (tile != null && !tile.getType().isTankPassable()) {
                        canSpawn = false;
                        break;
                    }
                }
            }

            if (canSpawn) {
                return adjustedX; // 找到合适位置
            }

            // 尝试新位置
            if (isPlayer) {
                // 玩家：在底部区域随机尝试
                adjustedX = 100 + random.nextDouble() * (GameConfig.SCREEN_WIDTH - 200);
                adjustedY = GameConfig.SCREEN_HEIGHT - 200 + random.nextDouble() * 100;
            } else {
                // 敌人：在上半区域随机尝试
                adjustedX = 100 + random.nextDouble() * (GameConfig.SCREEN_WIDTH - 200);
                adjustedY = 100 + random.nextDouble() * (GameConfig.SCREEN_HEIGHT / 2 - 100);
            }

            attempt++;
        }

        // 如果找不到合适位置，返回原始位置（游戏会处理碰撞）
        System.out.println("⚠️ 无法找到理想出生点，使用原始位置 (" + x + ", " + y + ")");
        return x;
    }


    /**
     * 更新游戏逻辑
     */
    /**
     * 这里是 60Hz 的物理逻辑更新
     * 对应以前的 updateGame()
     */
    @Override
    protected void updateGameLogic() {
        // 1. 计算时间
        gameElapsedTime = (System.currentTimeMillis() - levelStartTime) / 1000;

        // 2. 检查游戏状态（结束就不更新了）
        if (isGameOver || isLevelComplete) {
            return;
        }

        try {
            // 直接调用你原本写的逻辑方法
            updatePlayerTank();
            updateEnemyTanks();
            updateBullets();
            checkCollisions();
            cleanupObjects();

            // 检查过关
            checkGameState();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里是 渲染逻辑
     * 对应以前的 renderGame()
     */
    @Override
    protected void renderGameFrame() {
        // 【注意】不需要再写 gc.fillRect(Color.BLACK) 了，父类已经帮你清空了！

        // 我们需要分别获取不同层的画笔
        // 你的 BaseGameScene 提供了 mapGc, tankGc, bulletGc

        try {
            // 1. 绘制地图底层 (画在 mapGc 上)
            if (map != null) {
                spritePainter.drawMapBackground(mapGc, map);
            }

            // 2. 绘制坦克 (画在 tankGc 上)
            // 敌人
            for (Tank enemy : enemyTanks) {
                if (enemy.isAlive()) {
                    // 确保 Tank 类的 draw 方法支持传入 GraphicsContext
                    // 或者使用 spritePainter.drawTank(tankGc, enemy);
                    enemy.draw(tankGc);
                }
            }
            // 玩家
            if (player != null && player.isAlive()) {
                player.draw(tankGc);
            }

            // 3. 绘制子弹 (画在 bulletGc 上)
            for (Bullet bullet : bullets) {
                if (bullet.alive) {
                    bullet.draw(bulletGc);
                }
            }

            // 4. 绘制地图前景 (草丛) (画在 tankGc 或 bulletGc 上均可，看遮挡关系)
            if (map != null) {
                spritePainter.drawMapForeground(tankGc, map);
            }

            // 5. 绘制 HUD (建议画在 bulletGc 上，或者你再加一个 uiCanvas)
            // 这里暂时画在最顶层的 bulletGc 上，确保文字在最上面
            drawHUD(bulletGc);
            drawGameStateMessages(bulletGc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ... 此时你可以把旧的 updateGame() 和 renderGame() 方法删掉了 ...
    // ... restartGame, pauseGame 方法里对 gameLoop 的调用也要改 ...

    /**
     * 更新玩家坦克
     */
    private void updatePlayerTank() {
        if (player == null || !player.isAlive()) {
            return;
        }

        // 设置移动状态（基于输入）
        player.setMovingForward(inputHandler.isWPressed());
        player.setMovingBackward(inputHandler.isSPressed());
        player.setRotatingLeft(inputHandler.isAPressed());
        player.setRotatingRight(inputHandler.isDPressed());

        // 处理射击（J键）
        if (inputHandler.isJPressed()) {
            Bullet bullet = player.tryFire();
            if (bullet != null) {
                bullets.add(bullet);
                // System.out.println("🔫 玩家发射子弹");
            }
        }

        // 更新玩家坦克位置
        player.update(map);

        // 同步血量显示
        playerHealth = player.getHealth();
    }

    /**
     * 更新敌人坦克（AI）
     */
// 在 StageGameScene.java 中替换这个方法

    /**
     * 更新敌人坦克（AI + 物理移动）
     */
    private void updateEnemyTanks() {
        // 1. 计算两帧之间的时间差 (秒)，用于 AI 计时器
        // 假设是 60FPS，每次大约 0.016 秒
        double deltaTime = 0.016;

        for (int i = 0; i < enemyTanks.size(); i++) {
            Tank enemy = enemyTanks.get(i);
            if (!enemy.isAlive()) continue;

            // ========== 修复点 1：激活 AI 大脑 ==========
            // 只有调用了 updateAI，坦克的 isMovingForward 等状态才会被改变
            if (enemy instanceof EnemyTank) {
                // 传入地图、玩家对象、时间差
                ((EnemyTank) enemy).updateAI(map, player, deltaTime);
            }

            // ========== 修复点 2：物理移动 & 撞墙检测 ==========
            // 这一步会根据上面 AI 设定的方向移动，并处理与地图墙壁的碰撞
            // 前提是你的 Tank.update(map) 里写了撞墙逻辑
            enemy.update(map);

            // ========== 修复点 3：坦克与坦克之间的碰撞 ==========
            // 防止敌人重叠，或者敌人穿过玩家
            checkTankTankCollision(enemy);
        }
    }

    /**
     * 简单的坦克与坦克碰撞处理（推挤效果）
     */
    private void checkTankTankCollision(Tank currentTank) {
        // 1. 检查与玩家的碰撞
        if (player != null && player.isAlive() && currentTank != player) {
            if (isColliding(currentTank, player)) {
                resolveOverlap(currentTank, player);
            }
        }

        // 2. 检查与其他敌人的碰撞
        for (Tank other : enemyTanks) {
            if (other != currentTank && other.isAlive()) {
                if (isColliding(currentTank, other)) {
                    resolveOverlap(currentTank, other);
                }
            }
        }
    }

    /**
     * 处理重叠：简单地把 currentTank 弹回去一点点
     */
    private void resolveOverlap(Tank t1, Tank t2) {
        // 计算中心点距离
        double dx = t1.getCenterX() - t2.getCenterX();
        double dy = t1.getCenterY() - t2.getCenterY();

        // 简单的弹开逻辑：往反方向推
        // 推的力度可以根据需要调整，这里设为 2 像素
        double pushForce = 2.0;

        if (Math.abs(dx) > Math.abs(dy)) {
            t1.x += (dx > 0) ? pushForce : -pushForce;
        } else {
            t1.y += (dy > 0) ? pushForce : -pushForce;
        }
    }
    /**
     * 更新子弹
     */
    private void updateBullets() {
        for (Bullet bullet : bullets) {
            if (bullet.alive) {
                bullet.update(map);
            }
        }
    }

    /**
     * 检查碰撞
     */
    private void checkCollisions() {
        // 子弹与坦克碰撞
        checkBulletTankCollisions();

        // 坦克与坦克碰撞（可选，防止重叠）
        // checkTankTankCollisions(); // 暂时禁用，可能有问题
    }

    /**
     * 检查子弹与坦克的碰撞
     */
    private void checkBulletTankCollisions() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            if (!bullet.alive) continue;

            // 检查子弹与玩家坦克碰撞
            if (player != null && player.isAlive() && bullet.isEnemy &&
                    isColliding(bullet, player)) {
                // 敌人子弹击中玩家
                player.takeDamage(bullet.damage);
                bullet.alive = false;
                System.out.println("💥 玩家被击中，剩余血量: " + player.getHealth());
                continue;
            }

            // 检查子弹与敌人坦克碰撞
            for (int j = enemyTanks.size() - 1; j >= 0; j--) {
                Tank enemy = enemyTanks.get(j);
                if (enemy.isAlive() && !bullet.isEnemy &&
                        isColliding(bullet, enemy)) {
                    // 玩家子弹击中敌人
                    enemy.takeDamage(bullet.damage);
                    bullet.alive = false;

                    // 如果敌人死亡，增加分数
                    if (!enemy.isAlive()) {
                        playerScore += enemy.getScoreValue();
                        System.out.println("🎯 击毁敌人！得分: " + enemy.getScoreValue() +
                                "，总分: " + playerScore);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 检查两个实体是否碰撞
     */
    private boolean isColliding(Entity a, Entity b) {
        if (a == null || b == null) return false;

        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    /**
     * 清理已销毁的对象
     */
    private void cleanupObjects() {
        // 清理死亡敌人
        enemyTanks.removeIf(enemy -> !enemy.isAlive());

        // 清理无效子弹
        bullets.removeIf(bullet -> !bullet.alive);
    }

    /**
     * 检查游戏状态
     */
    private void checkGameState() {
        // 检查玩家是否死亡
        if (player != null && !player.isAlive()) {
            isGameOver = true;
            System.out.println("💀 游戏结束！玩家被击败");
            return;
        }

        // 检查关卡是否完成
        if (!isLevelComplete) {
            checkLevelCompletion();
        }
    }

    /**
     * 检查关卡完成条件
     */
    private void checkLevelCompletion() {
        // 条件1：消灭所有敌人
        boolean allEnemiesDefeated = enemyTanks.isEmpty();

        // 条件2：达到目标分数
        boolean scoreReached = playerScore >= targetScore;

        // 过关条件：消灭所有敌人且达到目标分数
        if (allEnemiesDefeated && scoreReached) {
            isLevelComplete = true;
            System.out.println("🎉 第 " + currentLevel + " 关完成！");
            System.out.println("   得分: " + playerScore + " / " + targetScore);
            System.out.println("   用时: " + gameElapsedTime + " 秒");

            // 延迟2秒后进入下一关
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::nextLevel);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * 进入下一关
     */
    private void nextLevel() {
        if (currentLevel < 3) {
            currentLevel++;
            System.out.println("\n=====================");
            System.out.println("进入第 " + currentLevel + " 关");
            System.out.println("=====================\n");

            loadLevel(currentLevel);
        } else {
            // 通关游戏
            System.out.println("🎊🎊🎊 恭喜通关所有关卡！ 🎊🎊🎊");
            System.out.println("最终得分: " + playerScore);
            System.out.println("总用时: " + gameElapsedTime + " 秒");

            // 这里可以添加通关画面或返回主菜单
            isGameOver = true;

            // 显示通关消息
            showGameCompleteMessage();
        }
    }

    /**
     * 显示游戏完成消息
     */
    private void showGameCompleteMessage() {
        System.out.println("\n🎮 游戏通关！");
        System.out.println("🎯 最终得分: " + playerScore);
        System.out.println("⏱️  总用时: " + gameElapsedTime + " 秒");
        System.out.println("👑 恭喜你完成了所有挑战！");

        // 可以在这里添加返回主菜单的逻辑
        // returnToMainMenu();
    }

    /**
     * 绘制HUD（抬头显示）
     */
    private void drawHUD(GraphicsContext gc) {
        if (gc == null) return;

        try {
            // 设置字体
            gc.setFont(HUD_FONT_MEDIUM);

            // 1. 绘制关卡信息（左上角）
            gc.setFill(LEVEL_COLOR);
            gc.fillText("第 " + currentLevel + " 关", 20, 30);

            // 2. 绘制分数（左上角，关卡下方）
            gc.setFill(SCORE_COLOR);
            gc.fillText("分数: " + playerScore + " / " + targetScore, 20, 60);

            // 3. 绘制游戏时间（右上角）
            gc.setFill(TIME_COLOR);
            String timeText = String.format("时间: %02d:%02d",
                    gameElapsedTime / 60, gameElapsedTime % 60);
            gc.fillText(timeText, WIDTH - 150, 30);

            // 4. 绘制玩家血量（右上角，时间下方）
            drawPlayerHealth(gc);

            // 5. 绘制敌人数量（右上角）
            gc.setFill(HUD_TEXT_COLOR);
            gc.fillText("剩余敌人: " + enemyTanks.size(), WIDTH - 150, 90);

        } catch (Exception e) {
            System.err.println("❌ 绘制HUD异常: " + e.getMessage());
        }
    }

    /**
     * 绘制玩家血量（用红心表示）
     */
    private void drawPlayerHealth(GraphicsContext gc) {
        if (gc == null) return;

        try {
            gc.setFill(HEALTH_COLOR);
            gc.setFont(HUD_FONT_SMALL);
            gc.fillText("血量: ", WIDTH - 150, 60);

            // 绘制血量条或红心图标
            int maxHealth = GameConfig.PLAYER_HEALTH;
            int currentHealth = playerHealth;

            // 方法1：绘制红心图标
            double heartX = WIDTH - 80;
            double heartY = 45;

            for (int i = 0; i < maxHealth; i++) {
                if (i < currentHealth) {
                    // 绘制实心红心（存活的血量）
                    gc.setFill(Color.RED);
                    drawHeart(gc, heartX + i * 25, heartY, 10);
                } else {
                    // 绘制空心红心（失去的血量）
                    gc.setStroke(Color.GRAY);
                    gc.setLineWidth(1);
                    drawHeartOutline(gc, heartX + i * 25, heartY, 10);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 绘制血量异常: " + e.getMessage());
        }
    }

    /**
     * 绘制实心红心
     */
    private void drawHeart(GraphicsContext gc, double x, double y, double size) {
        gc.save();
        gc.translate(x, y);

        // 创建心形路径
        gc.beginPath();
        gc.moveTo(0, -size/2);
        gc.bezierCurveTo(size/2, -size, size, 0, 0, size);
        gc.bezierCurveTo(-size, 0, -size/2, -size, 0, -size/2);
        gc.closePath();
        gc.fill();

        gc.restore();
    }

    /**
     * 绘制空心红心
     */
    private void drawHeartOutline(GraphicsContext gc, double x, double y, double size) {
        gc.save();
        gc.translate(x, y);

        // 创建心形路径
        gc.beginPath();
        gc.moveTo(0, -size/2);
        gc.bezierCurveTo(size/2, -size, size, 0, 0, size);
        gc.bezierCurveTo(-size, 0, -size/2, -size, 0, -size/2);
        gc.closePath();
        gc.stroke();

        gc.restore();
    }

    /**
     * 绘制游戏状态信息
     */
    private void drawGameStateMessages(GraphicsContext gc) {
        if (gc == null) return;

        try {
            gc.setFont(HUD_FONT_LARGE);

            if (isGameOver) {
                // 游戏结束画面
                gc.setFill(GAME_OVER_COLOR);
                String gameOverText = "游戏结束";
                double textWidth = getTextWidth(gc, gameOverText);
                gc.fillText(gameOverText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 30);

                gc.setFont(HUD_FONT_MEDIUM);
                gc.setFill(HUD_TEXT_COLOR);
                String scoreText = "最终得分: " + playerScore;
                double scoreWidth = getTextWidth(gc, scoreText);
                gc.fillText(scoreText, (WIDTH - scoreWidth) / 2, HEIGHT / 2 + 20);

                String timeText = "用时: " + gameElapsedTime + " 秒";
                double timeWidth = getTextWidth(gc, timeText);
                gc.fillText(timeText, (WIDTH - timeWidth) / 2, HEIGHT / 2 + 50);

                String restartText = "按 R 重新开始，按 ESC 返回主菜单";
                double restartWidth = getTextWidth(gc, restartText);
                gc.fillText(restartText, (WIDTH - restartWidth) / 2, HEIGHT / 2 + 90);

            } else if (isLevelComplete) {
                // 关卡完成画面
                gc.setFill(LEVEL_COMPLETE_COLOR);
                String completeText = "第 " + currentLevel + " 关完成！";
                double textWidth = getTextWidth(gc, completeText);
                gc.fillText(completeText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 30);

                gc.setFont(HUD_FONT_MEDIUM);
                gc.setFill(HUD_TEXT_COLOR);

                String scoreText = "得分: " + playerScore + " / " + targetScore;
                double scoreWidth = getTextWidth(gc, scoreText);
                gc.fillText(scoreText, (WIDTH - scoreWidth) / 2, HEIGHT / 2 + 20);

                if (currentLevel < 3) {
                    String nextLevelText = "即将进入第 " + (currentLevel + 1) + " 关...";
                    double nextWidth = getTextWidth(gc, nextLevelText);
                    gc.fillText(nextLevelText, (WIDTH - nextWidth) / 2, HEIGHT / 2 + 60);
                } else {
                    String congratsText = "恭喜通关所有关卡！";
                    double congratsWidth = getTextWidth(gc, congratsText);
                    gc.fillText(congratsText, (WIDTH - congratsWidth) / 2, HEIGHT / 2 + 60);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 绘制游戏状态信息异常: " + e.getMessage());
        }
    }

    /**
     * 计算文本宽度
     */
    private double getTextWidth(GraphicsContext gc, String text) {
        try {
            // 创建一个临时的Text对象来测量宽度
            javafx.scene.text.Text tempText = new javafx.scene.text.Text(text);
            tempText.setFont(gc.getFont());
            return tempText.getLayoutBounds().getWidth();
        } catch (Exception e) {
            System.err.println("❌ 计算文本宽度异常: " + e.getMessage());
            return text.length() * 10; // 粗略估计
        }
    }

    // ========== 游戏控制方法 ==========

    public void restartGame() {
        // 父类也有 gameLoop 对象，调用它的 stop
        if (super.gameLoop != null) {
            super.gameLoop.stop();
        }

        // ... 重置变量逻辑不变 ...

        loadLevel(currentLevel);

        // 父类循环重新开始
        if (super.gameLoop != null) {
            super.gameLoop.start();
        }
        System.out.println("🔄 游戏已重新开始");
    }

    public void pauseGame() {
        if (super.gameLoop != null) {
            super.gameLoop.stop();
        }
    }

    public void resumeGame() {
        if (super.gameLoop != null) {
            super.gameLoop.start();
        }
    }

    // ========== Getter方法 ==========

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public long getGameElapsedTime() {
        return gameElapsedTime;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isLevelComplete() {
        return isLevelComplete;
    }

    public List<Tank> getEnemyTanks() {
        return enemyTanks;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }
}