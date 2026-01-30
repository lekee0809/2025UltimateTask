package view;

import item.Item;
import item.ItemType;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import infra.GameConfig;

import javafx.scene.paint.LinearGradient;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import map.MapModel;
import map.GameLevelConfig;
import map.EnemySpawn;
import model.*;
import model.Tank.TankType;
import ranking.PlayerRecord;
import ranking.RankingManager;

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
    // StageGameScene 类的成员变量中新增（在 levelStartTime 附近）
    private long gameGlobalStartTime; // 全局游戏开始时间戳（整个闯关流程的开始时间）
    private boolean isRecordWritten;
    // ========== 敌人AI相关 ==========
    private static final long ENEMY_AI_UPDATE_INTERVAL = 1000; // 敌人AI更新间隔（毫秒）
    private long lastEnemyAIUpdateTime = 0; // 上次AI更新时间

    // ========== 界面常量 ==========
    private static final Color HUD_TEXT_COLOR = ThemeManager.Colors.LIGHT_TEXT;
    private static final Color HEALTH_COLOR = ThemeManager.Colors.DANGER;
    private static final Color SCORE_COLOR = ThemeManager.Colors.WARNING;
    private static final Color TIME_COLOR = ThemeManager.Colors.PRIMARY;
    private static final Color LEVEL_COLOR = ThemeManager.Colors.SUCCESS;
    private static final Color GAME_OVER_COLOR = ThemeManager.Colors.DANGER;
    private static final Color LEVEL_COMPLETE_COLOR = ThemeManager.Colors.WARNING;

    private static final Font HUD_FONT_SMALL = ThemeManager.Fonts.HUD_SMALL;
    private static final Font HUD_FONT_MEDIUM = ThemeManager.Fonts.HUD_MEDIUM;
    private static final Font HUD_FONT_LARGE = ThemeManager.Fonts.HUD_LARGE;

    // ========== 构造函数 ==========
    public StageGameScene(Stage stage) {
        super(stage);
    }

    @Override
    protected void resetModeSpecificData() {
        // 重置时清空道具
        itemSpawner.clear();
        particleEffects.clear();

        // 重置游戏状态
        playerScore = 0;
        playerHealth = GameConfig.PLAYER_HEALTH;
        isGameOver = false;
        isLevelComplete = false;
        isRecordWritten = false;
        enemyTanks.clear();
        bullets.clear();
    }

    @Override
    protected PlayerTank getPlayerTank() {
        return player;
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
        isRecordWritten = false;
        // 【核心重置点】：只有重新开始战役时，才同步当前系统时间
        long now = System.currentTimeMillis();
        gameGlobalStartTime = now;
        gameElapsedTime = 0; // 界面立即显示 0
        System.out.println("🚀 战役重启：时间已归零，从第一关开始...");
        // 初始化对象列表
        enemyTanks = new ArrayList<>();
        bullets = new ArrayList<>();

        System.out.println("🚀 开始初始化闯关模式...");
        // ========== 新增：播放背景音乐 ==========
        SoundManager.getInstance().playGameMusic();
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


    // ========== 返回主菜单方法 ==========
    private void returnToMainMenu() {
        System.out.println("⚠️ 返回主菜单");
        if (gameLoop != null) {
            gameLoop.stop();
        }
        // ========== 新增：停止背景音乐 ==========
        SoundManager.getInstance().stopBackgroundMusic();
        // 这里需要调用返回主菜单的逻辑，你需要根据你的项目结构来实现
        // 例如：StartScene startScene = new StartScene(primaryStage);
        // primaryStage.setScene(startScene.getScene());
    }

    // ========== 关卡加载系统 ==========

    /**
     * 加载指定关卡
     *
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

            // 4. 清空子弹和道具
            bullets.clear();
            itemSpawner.clear();
            particleEffects.clear();

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
    /**
     * 【修复】初始化玩家坦克
     * 增加防卡墙检测：如果预设位置有墙，自动在附近寻找空地
     */
    /**
     * 【已优化】初始化玩家坦克
     */
    private void initializePlayerTank(int level) {
        double x, y;

        // 1. 尝试使用智能查找获取安全位置
        double[] safePos = findFreeSpawnPoint(true);

        if (safePos != null) {
            x = safePos[0];
            y = safePos[1];
        } else {
            // 兜底：如果实在找不到，强制放在地图中间下方，并清除该处障碍
            System.out.println("⚠️ 警告：无法找到玩家安全点，使用强制坐标");
            x = GameConfig.SCREEN_WIDTH / 2.0 - 20;
            y = GameConfig.SCREEN_HEIGHT - 100;
            // 暴力清除出生点周围的墙 (防止卡死)
            forceClearAreaAt(x, y);
        }

        // 2. 创建或重置玩家
        if (player == null || level == 1) {
            player = new PlayerTank(x, y);
            playerHealth = GameConfig.PLAYER_HEALTH;
        } else {
            player.x = x;
            player.y = y;
            // 停止运动
            player.setMovingForward(false);
            player.setMovingBackward(false);
            player.setRotatingLeft(false);
            player.setRotatingRight(false);
            // 回血奖励
            int heal = (int) (GameConfig.PLAYER_HEALTH * 0.3);
            player.setHealth(Math.min(GameConfig.PLAYER_HEALTH, player.getHealth() + heal));
            playerHealth = player.getHealth();
        }

        // 给玩家 3秒无敌
        player.activateShield(3.0);

        System.out.println("✅ 玩家初始化于: " + (int) x + "," + (int) y);
    }

    // 辅助：强制清理一片区域（兜底用）
    private void forceClearAreaAt(double pixelX, double pixelY) {
        int c = (int) (pixelX / GameConfig.GRID_SIZE);
        int r = (int) (pixelY / GameConfig.GRID_SIZE);
        if (r >= 0 && r < GameConfig.MAP_ROWS && c >= 0 && c < GameConfig.MAP_COLS) {
            map[r][c].setType(model.TileType.EMPTY);
        }
    }

    /**
     * 【通用工具】检查某个坐标放置坦克是否安全
     * (这个方法可以直接复用给敌人生成逻辑)
     */
    private boolean isPositionSafe(double x, double y) {
        if (map == null) return true;

        // 检查坦克的四个角
        double size = GameConfig.TANK_SIZE;
        double[] cornersX = {x, x + size, x, x + size};
        double[] cornersY = {y, y, y + size, y + size};

        for (int i = 0; i < 4; i++) {
            // 算出格子坐标
            int col = (int) (cornersX[i] / GameConfig.GRID_SIZE);
            int row = (int) (cornersY[i] / GameConfig.GRID_SIZE);

            // 1. 检查边界
            if (row < 0 || row >= GameConfig.MAP_ROWS || col < 0 || col >= GameConfig.MAP_COLS) {
                return false;
            }

            // 2. 检查是否有障碍物 (墙、水、石)
            Tile tile = map[row][col];
            if (tile != null && !tile.getType().isTankPassable()) {
                return false; // 只要有一个角碰到障碍，就不安全
            }
        }
        return true; // 四个角都安全
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
    /**
     * 【已优化】创建单个敌人坦克
     */
    private Tank createEnemyTank(TankType type, int level) {
        // 1. 使用智能算法寻找位置
        double[] pos = findFreeSpawnPoint(false); // false 表示寻找敌人位置

        double x, y;
        if (pos != null) {
            x = pos[0];
            y = pos[1];
        } else {
            // 实在找不到位置（地图太满了），就不生成这个敌人了
            System.out.println("❌ 地图太拥挤，无法生成敌人: " + type);
            return null;
        }

        // 2. 生成具体坦克对象
        try {
            switch (type) {
                case ENEMY_HEAVY:
                    return new HeavyTank(x, y);
                case ENEMY_FAST:
                    return new FastTank(x, y);
                default:
                    return new NormalTank(x, y);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }    /**
     * 调整出生位置，确保不在地图障碍物上
     */
    /**
     * 【核心修复】调整出生位置，确保不在地图障碍物上
     * 使用 GameConfig.GRID_SIZE 和 TileType.isTankPassable 进行双重校验
     */
    private double adjustSpawnPosition(double x, double y, boolean isPlayer) {
        // 如果地图还没加载好，直接返回原坐标
        if (map == null || map.length == 0) return x;

        double safeX = x;
        double safeY = y;
        int maxAttempts = 50; // 尝试50次，找不到就放弃
        int attempt = 0;

        // 坦克半径 (用于计算中心点)
        double halfSize = GameConfig.TANK_SIZE / 2;

        while (attempt < maxAttempts) {
            boolean isSafe = true;

            // 1. 计算坦克中心点所在的格子行列
            // 注意：这里加上 halfSize 是为了用坦克的中心点来判断，而不是左上角
            int col = (int) ((safeX + halfSize) / GameConfig.GRID_SIZE);
            int row = (int) ((safeY + halfSize) / GameConfig.GRID_SIZE);

            // 2. 检查边界 (是否超出地图)
            if (row < 0 || row >= GameConfig.MAP_ROWS || col < 0 || col >= GameConfig.MAP_COLS) {
                isSafe = false;
            }
            // 3. 检查地形 (是否撞墙/撞水)
            else if (map[row][col] != null && !map[row][col].getType().isTankPassable()) {
                isSafe = false;
            }

            // 4. 如果是安全的，直接返回这个坐标
            if (isSafe) {
                return safeX;
            }

            // 5. 如果不安全，重新随机一个新坐标
            if (isPlayer) {
                // 玩家：在地图下半部分随机找点
                safeX = 100 + random.nextDouble() * (GameConfig.SCREEN_WIDTH - 200);
                safeY = GameConfig.SCREEN_HEIGHT - 200 + random.nextDouble() * 100;
            } else {
                // 敌人：在地图上半部分随机找点 (避开玩家出生区)
                safeX = 50 + random.nextDouble() * (GameConfig.SCREEN_WIDTH - 100);
                safeY = 50 + random.nextDouble() * (GameConfig.SCREEN_HEIGHT / 2);
            }

            attempt++;
        }

        // 如果实在找不到安全点（比如地图全是墙），为了防止报错，只能返回原来的坐标
        System.out.println("⚠️ 警告：尝试了50次也没找到安全出生点，将强制生成在: " + x + "," + y);
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
        // 【修改核心】：将所有逻辑（包括时间计算）全部锁在状态判断之后
        if (isGameOver || isLevelComplete) {
            return; // 一旦死了或通关，直接退出方法，不执行任何代码
        }

        // 只有没死的时候，才会执行到这里
        try {
            // 2. 【核心修改】：计算从第一关开始到现在的累计总时间
            // 使用 gameGlobalStartTime 而不是 levelStartTime
            gameElapsedTime = (System.currentTimeMillis() - gameGlobalStartTime) / 1000;

            // 2. 调用父类更新道具逻辑
            super.updateBaseElements();

            // 3. 执行游戏物理逻辑
            updatePlayerTank();
            updateEnemyTanks();
            updateBullets();
            checkCollisions();
            cleanupObjects();

            // 4. 检查游戏状态（如果在这里判定玩家死亡，下一次进入方法就会被顶部的 if 拦截）
            checkGameState();

        } catch (Exception e) {
            e.printStackTrace();
        }
     /* // 1. 调用父类通用道具逻辑
        super.updateBaseElements();

        // 2. 检测敌人死亡掉落道具
        enemyTanks.removeIf(e -> {
            if (!e.isAlive()) {
                itemSpawner.onEnemyDestroyed((EnemyTank) e); // 触发掉落逻辑
                return true;
            }
            return false;
        });*/
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
// 调用父类绘制道具和粒子
            super.renderBaseElements();
            // 5. 绘制道具和粒子特效 (调用父类方法)
            super.renderBaseElements();

            if (player != null) {
                playerHealth = player.getHealth();
            }



            // 6. 绘制 HUD (建议画在 bulletGc 上，或者你再加一个 uiCanvas)
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
            Bullet bullet = player.tryFire(map);
            if (bullet != null) {
                bullets.add(bullet);
                // ========== 新增：播放子弹发射音效 ==========
                SoundManager.getInstance().playSoundEffect("explosion.wav"); // 替换为你的子弹音效文件路径
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
        double deltaTime = 0.016;

        for (int i = 0; i < enemyTanks.size(); i++) {
            Tank enemy = enemyTanks.get(i);
            if (!enemy.isAlive()) continue;

            // 1. AI 思考
            if (enemy instanceof EnemyTank) {
                EnemyTank aiTank = (EnemyTank) enemy;
                aiTank.updateAI(map, player, deltaTime);

                // 【核心修复点】: 检查 AI 有没有发射子弹
                // 如果 AI 的口袋里有子弹，拿出来，加到游戏世界的 bullets 列表里
                Bullet newBullet = aiTank.consumePendingBullet();
                if (newBullet != null) {
                    bullets.add(newBullet);
                    // System.out.println("⚠️ 敌人开火了！"); // 测试用
                }
            }

            // 2. 物理移动
            enemy.update(map);

            // 3. 坦克碰撞处理
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
    /**
     * 【核心修复】安全的碰撞推挤逻辑
     * 防止把坦克推到墙里面
     */
    private void resolveOverlap(Tank t1, Tank t2) {
        double dx = t1.getCenterX() - t2.getCenterX();
        double dy = t1.getCenterY() - t2.getCenterY();

        // 如果完全重叠（极为罕见），给一个随机方向
        if (dx == 0 && dy == 0) {
            dx = 1;
        }

        // 计算推力力度 (比如每次推开 2 像素)
        double pushSpeed = 2.0;

        // 归一化向量，确定推的方向
        double distance = Math.sqrt(dx * dx + dy * dy);
        double unitX = dx / distance;
        double unitY = dy / distance;

        double moveX = unitX * pushSpeed;
        double moveY = unitY * pushSpeed;

        // === 策略 1: 尝试移动 T1 (被撞者/主动者) ===
        // 计算 T1 的新位置
        double t1NewX = t1.x + moveX;
        double t1NewY = t1.y + moveY;

        // 如果 T1 移动后是安全的 (不撞墙)，就移动 T1
        if (isValidPosition(t1NewX, t1NewY)) {
            t1.x = t1NewX;
            t1.y = t1NewY;
        }
        // === 策略 2: 如果 T1 后面是墙，尝试移动 T2 (反向推) ===
        else {
            double t2NewX = t2.x - moveX;
            double t2NewY = t2.y - moveY;

            // 如果 T2 反方向移动是安全的，就移动 T2
            if (isValidPosition(t2NewX, t2NewY)) {
                t2.x = t2NewX;
                t2.y = t2NewY;
            }
            // === 策略 3: 如果两人后面都是墙 (夹心饼干) ===
            // 谁都别动，防止穿墙。
        }
    }

    /**
     * 检查坦克的某个位置是否合法 (不会撞墙/越界)
     * 检查坦克的四个角
     */
    private boolean isValidPosition(double x, double y) {
        // 1. 边界检查
        if (x < 0 || x + GameConfig.TANK_SIZE > GameConfig.SCREEN_WIDTH ||
                y < 0 || y + GameConfig.TANK_SIZE > GameConfig.SCREEN_HEIGHT) {
            return false;
        }

        // 2. 墙壁碰撞检查 (检查四个角)
        double[] cornersX = {x, x + GameConfig.TANK_SIZE, x, x + GameConfig.TANK_SIZE};
        double[] cornersY = {y, y, y + GameConfig.TANK_SIZE, y + GameConfig.TANK_SIZE};

        for (int i = 0; i < 4; i++) {
            int col = (int) (cornersX[i] / GameConfig.GRID_SIZE);
            int row = (int) (cornersY[i] / GameConfig.GRID_SIZE);

            // 防止数组越界
            if (row >= 0 && row < GameConfig.MAP_ROWS && col >= 0 && col < GameConfig.MAP_COLS) {
                Tile tile = map[row][col];
                // 如果碰到了不可通行的格子 (墙/水)
                if (tile != null && !tile.getType().isTankPassable()) {
                    return false;
                }
            }
        }
        return true; // 所有检查通过，位置合法
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
                        // 【新增】触发道具掉落
                        if (enemy instanceof EnemyTank) {
                            itemSpawner.onEnemyDestroyed((EnemyTank) enemy);
                        }
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void handleBombEffect(Item item) {
        if (item.getType() != ItemType.BOMB) return;

        System.out.println("💣 炸弹爆炸！对全图敌人造成50点伤害");

        // 创建临时列表收集被炸死的敌人（用于触发道具掉落）
        List<EnemyTank> killedEnemies = new ArrayList<>();

        // 对当前所有敌人造成伤害
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive()) {
                enemy.takeDamage(50);
                System.out.println("  敌方坦克受到炸弹伤害，剩余血量: " + enemy.getHealth());

                // 检查是否被炸死
                if (!enemy.isAlive()) {
                    if (enemy instanceof EnemyTank) {
                        killedEnemies.add((EnemyTank) enemy);
                    }
                    // 增加分数
                    playerScore += enemy.getScoreValue();
                    System.out.println("  炸弹击杀敌人，得分: " + enemy.getScoreValue());
                }
            }
        }

        // 触发被炸死敌人的道具掉落
        for (EnemyTank killedEnemy : killedEnemies) {
            itemSpawner.onEnemyDestroyed(killedEnemy);
        }

        // 移除死亡的敌人
        enemyTanks.removeIf(e -> !e.isAlive());
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
            // 新增：触发单人闯关记录写入（false 表示未通关）
            // --- 新增：死亡后的按键监听 ---
            inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.R, () -> {
                currentLevel = 1;
                this.resetScene();
            });

            inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.ESCAPE, () -> {
                gameLoop.stop();
                try {
                    new game.AppLauncher().start(primaryStage);
                } catch (Exception e) { e.printStackTrace(); }
            });
            writeSingleGameRecord(false);
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

            // ⚠️【删掉这行】不要在这里写记录！会导致后续关卡无法写入
            // writeSingleGameRecord(true);

            // 如果是最后一关 (第3关)
            if (currentLevel >= 3) {
                // 【移到这里】只有通关全服才写入胜利记录 + 计算时间分
                writeSingleGameRecord(true);

                isGameOver = true;
                this.pauseGameProcess();
                Platform.runLater(this::showGameOverDialog);
                return;
            }

            // 延迟2秒后进入下一关
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(this::nextLevel);
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
            isLevelComplete = false; // 允许 updateGameLogic 继续运行
        } else {
            // --- 修改重点：通关游戏后的交互逻辑 ---
            System.out.println("🎊🎊🎊 恭喜通关所有关卡！ 🎊🎊🎊");
            isLevelComplete = true; // 确保触发渲染
            isGameOver = true;     // 借用 gameOver 状态停止逻辑更新

            // 1. 停止背景音乐
            view.SoundManager.getInstance().stopBackgroundMusic();

            // 2. 绑定 R 键：从第一关重新开始整个战役
            inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.R, () -> {
                System.out.println("🔄 重新开始完整挑战...");
                currentLevel = 1;
                this.resetScene(); // 调用父类重置方法
            });

            // 3. 绑定 ESC 键：返回 AppLauncher 主界面
            inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.ESCAPE, () -> {
                System.out.println("🏠 返回主基地...");
                gameLoop.stop();
                try {
                    game.AppLauncher mainMenu = new game.AppLauncher();
                    mainMenu.start(primaryStage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // 触发记录写入
            writeSingleGameRecord(true);
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
        gc.moveTo(0, -size / 2);
        gc.bezierCurveTo(size / 2, -size, size, 0, 0, size);
        gc.bezierCurveTo(-size, 0, -size / 2, -size, 0, -size / 2);
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
        gc.moveTo(0, -size / 2);
        gc.bezierCurveTo(size / 2, -size, size, 0, 0, size);
        gc.bezierCurveTo(-size, 0, -size / 2, -size, 0, -size / 2);
        gc.closePath();
        gc.stroke();

        gc.restore();
    }

    /**
     * 绘制游戏状态信息
     */


    // 替换原有drawHUD方法
    private void drawHUD(GraphicsContext gc) {
        if (gc == null) return;

        try {
            // 绘制左侧信息面板 - 关卡和分数
            drawInfoPanel(gc, 10, 10, 240, 120, "关卡信息", () -> {
                // 关卡信息
                gc.setFill(ThemeManager.Colors.LEVEL_COLOR);
                gc.fillText("关卡: " + currentLevel + "/3", 30, 45);

                // 分数信息
                gc.setFill(ThemeManager.Colors.SCORE_COLOR);
                gc.fillText("得分: " + playerScore, 30, 75);

                // 目标分数
                gc.setFill(ThemeManager.Colors.WARNING);
                gc.fillText("目标: " + targetScore, 30, 105);
            });

            // 绘制右侧信息面板 - 时间和状态
            drawInfoPanel(gc, WIDTH - 250, 10, 240, 120, "状态信息", () -> {
                // 游戏时间
                gc.setFill(ThemeManager.Colors.TIME_COLOR);
                String timeText = String.format("时间: %02d:%02d",
                        gameElapsedTime / 60, gameElapsedTime % 60);
                gc.fillText(timeText, WIDTH - 230, 45);

                // 血量显示（心形图标）
                drawHealthHearts(gc, WIDTH - 230, 75, playerHealth);

                // 剩余敌人
                gc.setFill(ThemeManager.Colors.ENEMY_NORMAL);
                gc.fillText("敌人: " + enemyTanks.size(), WIDTH - 230, 105);
            });

        } catch (Exception e) {
            System.err.println("❌ 绘制HUD异常: " + e.getMessage());
        }
    }

    /**
     * 绘制信息面板
     */
    private void drawInfoPanel(GraphicsContext gc, double x, double y, double width, double height, 
                              String title, Runnable contentRenderer) {
        // 绘制半透明背景板
        gc.setFill(Color.rgb(10, 14, 23, 0.85));
        gc.fillRoundRect(x, y, width, height, 10, 10);

        // 绘制边框
        gc.setStroke(ThemeManager.Colors.PRIMARY);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 10, 10);

        // 绘制标题
        gc.setFill(ThemeManager.Colors.PRIMARY);
        gc.setFont(ThemeManager.Fonts.HUD_MEDIUM);
        gc.fillText(title, x + 15, y + 25);

        // 绘制内容
        gc.setFont(ThemeManager.Fonts.HUD_SMALL);
        contentRenderer.run();
    }

    /**
     * 【修复版】绘制心形血量（支持任意血量数值）
     * 原理：计算血量百分比，映射到3颗心上
     */
    /**
     * 【完美修复版】绘制心形血量
     * 修复问题：解决血量减少后，红色爱心没有消失（残影）的问题
     */
    /**
     * 【最终版】绘制心形血量
     * 特性：
     * 1. 自动根据血量百分比显示 0-3 颗心
     * 2. 扣血后爱心变暗（无残留）
     * 3. 不显示具体的数字文本
     */
    private void drawHealthHearts(GraphicsContext gc, double x, double y, int health) {
        gc.setFill(Color.web("#e74c3c"));
        gc.fillText("血量: ", x, y);

        // 1. 获取最大血量和UI显示的爱心总数
        int maxHealth = GameConfig.PLAYER_HEALTH;
        int totalHearts = 5;

        // 2. 计算需要亮几颗心 (向上取整)
        int filledHearts = 0;
        if (health > 0) {
            filledHearts = (int) Math.ceil((double) health / maxHealth * totalHearts);
            // 兜底：只要没死，至少显示半颗心/一颗心，避免误导玩家
            if (filledHearts == 0) filledHearts = 1;
        }

        double heartX = x + 60;

        // 3. 循环绘制 3 颗心
        for (int i = 0; i < totalHearts; i++) {
            double hx = heartX + i * 30;
            double hy = y - 10;
            double hSize = 15;

            if (i < filledHearts) {
                // =========== 存活状态：画红心 ===========
                gc.setFill(Color.web("#e74c3c")); // 亮红色
                drawHeart(gc, hx, hy, hSize);
            } else {
                // =========== 扣血状态：画空心槽 ===========

                // 1. 先用深灰色涂黑（擦除原来的红色）
                gc.setFill(Color.rgb(40, 40, 40));
                drawHeart(gc, hx, hy, hSize);

                // 2. 再画灰色的边框
                gc.setStroke(Color.GRAY);
                gc.setLineWidth(1);
                drawHeartOutline(gc, hx, hy, hSize);

                // 恢复画笔颜色，防止影响后续绘制
                gc.setFill(Color.web("#e74c3c"));
            }
        }

        // 此处已删除 text 绘制代码
    }




    // 美化游戏状态提示
    private void drawGameStateMessages(GraphicsContext gc) {
        if (gc == null || (!isGameOver && !isLevelComplete)) return;

        double centerX = WIDTH / 2;
        double centerY = HEIGHT / 2;

        gc.save();

        // 半透明黑色遮罩 + 渐变
        javafx.scene.paint.LinearGradient maskGrad = new javafx.scene.paint.LinearGradient(0, 0, 0, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.rgb(10,14,23,0.9)),
                new javafx.scene.paint.Stop(1, Color.rgb(26,32,44,0.95)));
        gc.setFill(maskGrad);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // 标题文字
        String title = isGameOver && playerHealth <= 0 ? "MISSION FAILED" : "LEVEL COMPLETE";
        Color titleColor = isGameOver ? ThemeManager.Colors.DANGER : ThemeManager.Colors.WARNING;

        gc.setFont(ThemeManager.Fonts.GAME_OVER);
        gc.setEffect(new javafx.scene.effect.DropShadow(20, titleColor));
        gc.setFill(titleColor);

        double titleWidth = getTextWidth(gc, title);
        gc.fillText(title, centerX - titleWidth/2, centerY - 80);
        gc.setEffect(null);

        // 数据面板
        gc.setFill(Color.rgb(26, 32, 44, 0.9));
        gc.fillRoundRect(centerX - 250, centerY - 40, 500, 180, 15, 15);
        gc.setStroke(ThemeManager.Colors.SECONDARY);
        gc.setLineWidth(3);
        gc.strokeRoundRect(centerX - 250, centerY - 40, 500, 180, 15, 15);

        // 数据文字
        gc.setFont(ThemeManager.Fonts.MENU_ITEM);
        gc.setFill(ThemeManager.Colors.LIGHT_TEXT);

        gc.fillText("最终得分: " + playerScore, centerX - 220, centerY + 10);
        gc.fillText("总用时: " + gameElapsedTime + " 秒", centerX - 220, centerY + 50);
        gc.fillText("剩余生命: " + (playerHealth > 0 ? playerHealth : 0), centerX - 220, centerY + 90);

        // 按键提示
        drawKeyHint(gc, "R", "重新开始", centerX - 180, HEIGHT - 80, ThemeManager.Colors.SUCCESS);
        drawKeyHint(gc, "ESC", "返回菜单", centerX + 60, HEIGHT - 80, ThemeManager.Colors.LIGHT_TEXT);

        gc.restore();
    }

    // 辅助方法：绘制现代感大标题
    private void drawModernTitle(GraphicsContext gc, String text, Color color, double x, double y) {
        gc.setFont(Font.font("Impact", 80));
        gc.setEffect(new javafx.scene.effect.DropShadow(20, color));
        gc.setFill(color);
        gc.fillText(text, x - 300, y);
        gc.setEffect(null);
    }

    // 辅助方法：绘制按键标签
    private void drawKeyHint(GraphicsContext gc, String key, String action, double x, double y, Color color) {
        gc.setFill(color);
        gc.fillRoundRect(x, y - 30, 60, 40, 5, 5);
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        gc.fillText(key, x + 15, y - 2);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20));
        gc.fillText(action, x + 75, y - 2);
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

    // StageGameScene 中重写 resetScene（如果需要），或确保 resetModeSpecificData 完整
    @Override
    public void resetScene() {
        super.resetScene(); // 调用父类重置
        restartGame(); // 重启当前关卡
    }

    // ==========================================
    //   新增：智能出生点查找逻辑
    // ==========================================

    /**
     * 【核心优化】寻找一个安全的出生坐标
     *
     * @param isPlayer true表示为玩家寻找(底部/左上)，false表示为敌人寻找(顶部/随机)
     * @return double[]{x, y} 或者 null (如果找不到)
     */
    private double[] findFreeSpawnPoint(boolean isPlayer) {
        int maxAttempts = 100;

        // 定义出生区域范围
        // 玩家通常在地图底部出生，敌人通常在顶部或随机
        int startRow, endRow;

        if (isPlayer) {
            // 玩家：尝试在地图最后 5 行寻找
            startRow = GameConfig.MAP_ROWS - 5;
            endRow = GameConfig.MAP_ROWS;
        } else {
            // 敌人：尝试在地图前 2/3 区域寻找，留出底部给玩家缓冲
            startRow = 0;
            endRow = GameConfig.MAP_ROWS * 2 / 3;
        }

        for (int i = 0; i < maxAttempts; i++) {
            // 1. 随机一个网格坐标
            int c = random.nextInt(GameConfig.MAP_COLS);
            int r = startRow + random.nextInt(endRow - startRow);

            // 边界保护
            if (r < 0) r = 0;
            if (r >= GameConfig.MAP_ROWS) r = GameConfig.MAP_ROWS - 1;

            // 2. 【第一层检查】检查地形是否空旷
            // 我们检查 2x2 的小区域（因为坦克体积可能略大于1个格子），或者只检查中心点
            // 这里严谨一点，检查该位置是否适合放坦克
            if (isAreaClearForTank(r, c)) {

                // 计算像素坐标
                double x = c * GameConfig.GRID_SIZE;
                double y = r * GameConfig.GRID_SIZE;

                // 3. 【第二层检查】检查是否与其他坦克重叠
                if (!isPositionOccupied(x, y)) {
                    // 找到完美位置！
                    return new double[]{x, y};
                }
            }
        }

        return null; // 实在找不到
    }

    /**
     * 检查以 (r,c) 为起点的网格区域是否是墙
     */
    private boolean isAreaClearForTank(int r, int c) {
        // 检查当前格子
        if (!isTilePassable(r, c)) return false;

        // 如果坦克比较大，可能还需要检查右边和下边的格子
        // 简单起见，我们假设坦克主要占据当前格子，但为了防卡墙，我们要求四周不能全是墙
        // 这里只检查中心格子必须是 EMPTY 或 GRASS
        Tile t = map[r][c];
        return t != null && (t.getType() == model.TileType.EMPTY || t.getType() == model.TileType.GRASS);
    }

    private boolean isTilePassable(int r, int c) {
        if (r < 0 || r >= GameConfig.MAP_ROWS || c < 0 || c >= GameConfig.MAP_COLS) return false;
        Tile t = map[r][c];
        return t != null && t.getType().isTankPassable();
    }

    /**
     * 检查像素坐标 (x,y) 处是否已经有其他坦克占位了
     */
    private boolean isPositionOccupied(double x, double y) {
        double margin = 5.0; // 容错距离

        // 检查玩家
        if (player != null && player.isAlive()) {
            double dist = Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2));
            if (dist < GameConfig.TANK_SIZE + margin) return true;
        }

        // 检查所有敌人
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive()) {
                double dist = Math.sqrt(Math.pow(x - enemy.getX(), 2) + Math.pow(y - enemy.getY(), 2));
                if (dist < GameConfig.TANK_SIZE + margin) return true;
            }
        }
        return false;
    }

    // 实现新增抽象方法：返回当前游戏模式（单人闯关）
    @Override
    protected PlayerRecord.GameMode getCurrentGameMode() {
        return PlayerRecord.GameMode.SINGLE_CHALLENGE;
    }
    // StageGameScene 类中新增该方法（可放在 checkGameState 方法附近）

    /**
     * 单人闯关记录写入方法（复用现有 playerScore 和全局时长）
     *
     * @param isPassed 是否通关所有关卡
     */
    /**
     * 单人闯关记录写入方法 (含时间积分逻辑)
     */
    private void writeSingleGameRecord(boolean isPassed) {
        System.out.println("===== 进入记录写入方法 =====");

        // 防止重复写入
        if (isRecordWritten) {
            return;
        } else {
            try {
                // 1. 计算全局游玩时长
                long totalPlayTimeMillis = System.currentTimeMillis() - gameGlobalStartTime;
                int totalPlayTimeSeconds = (int) (totalPlayTimeMillis / 1000);
                totalPlayTimeSeconds = Math.max(0, totalPlayTimeSeconds);

                // 2. 计算时间奖励
                int timeBonus = 0;

                // 只有通关才加时间分
                if (isPassed) {
                    int maxTimeBonus = 3000;
                    int decayPerSecond = 10;

                    // 公式：3000 - (10 * 秒数)
                    timeBonus = maxTimeBonus - (totalPlayTimeSeconds * decayPerSecond);

                    // 兜底：奖励不小于0
                    timeBonus = Math.max(0, timeBonus);

                    System.out.println("⏱️ 时间奖励结算: 耗时 " + totalPlayTimeSeconds + "s, 奖励 +" + timeBonus);
                }

                // 【核心修复】：直接把奖励加到全局分数变量里！
                // 这样 drawGameStateMessages 画图时，就能读到加分后的值了
                this.playerScore += timeBonus;

                // 确保不为负数
                if (this.playerScore < 0) this.playerScore = 0;

                // 3. 写入排行榜 (使用更新后的 playerScore)
                RankingManager.addRecord(this.playerScore, totalPlayTimeSeconds, PlayerRecord.GameMode.SINGLE_CHALLENGE);

                System.out.println("📝 写入记录成功: 通关=" + isPassed + ", 最终显示分=" + this.playerScore);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isRecordWritten = true;
                SoundManager.getInstance().stopBackgroundMusic();
            }
        }
    }
    /**
     * 计算最终得分（原始击杀分 + 时间奖励）
     * 逻辑：满分3000，每过1秒扣10分，扣完为止。
     */
    // 在 StageGameScene 类中添加该方法
    private void showGameOverDialog() {

    }

    public PlayerTank getPlayer() {
        return player;
    }

    public void setPlayer(PlayerTank player) {
        this.player = player;
    }

    public void setEnemyTanks(List<Tank> enemyTanks) {
        this.enemyTanks = enemyTanks;
    }

    public void setBullets(List<Bullet> bullets) {
        this.bullets = bullets;
    }

    public MapModel getMapModel() {
        return mapModel;
    }

    public void setMapModel(MapModel mapModel) {
        this.mapModel = mapModel;
    }

    public Tile[][] getMap() {
        return map;
    }

    public void setMap(Tile[][] map) {
        this.map = map;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public void setPlayerScore(int playerScore) {
        this.playerScore = playerScore;
    }

    public void setPlayerHealth(int playerHealth) {
        this.playerHealth = playerHealth;
    }

    public long getLevelStartTime() {
        return levelStartTime;
    }

    public void setLevelStartTime(long levelStartTime) {
        this.levelStartTime = levelStartTime;
    }

    public void setGameElapsedTime(long gameElapsedTime) {
        this.gameElapsedTime = gameElapsedTime;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public void setLevelComplete(boolean levelComplete) {
        isLevelComplete = levelComplete;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public void setTargetScore(int targetScore) {
        this.targetScore = targetScore;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public long getGameGlobalStartTime() {
        return gameGlobalStartTime;
    }

    public void setGameGlobalStartTime(long gameGlobalStartTime) {
        this.gameGlobalStartTime = gameGlobalStartTime;
    }

    public boolean isRecordWritten() {
        return isRecordWritten;
    }

    public void setRecordWritten(boolean recordWritten) {
        isRecordWritten = recordWritten;
    }

    public long getLastEnemyAIUpdateTime() {
        return lastEnemyAIUpdateTime;
    }

    public void setLastEnemyAIUpdateTime(long lastEnemyAIUpdateTime) {
        this.lastEnemyAIUpdateTime = lastEnemyAIUpdateTime;
    }
}