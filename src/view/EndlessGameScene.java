package view;

import infra.GameConfig;
import map.MapFactory; // ✅ 1. 引入工厂
import map.MapModel;
import model.*;
import model.Tank.TankType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 无尽模式场景类
 * 特性：无限波次、随机地图、难度递增
 */
public class EndlessGameScene extends BaseGameScene {

    // ========== 核心对象 ==========
    private PlayerTank player;
    private List<Tank> enemyTanks;
    private List<Bullet> bullets;
    private MapModel mapModel;
    private Tile[][] map;

    // ========== 游戏状态 ==========
    private int currentWave;          // 当前波次
    private int score;                // 总得分
    private int enemiesKilledInWave;  // 当前波次已杀敌数
    private int targetKills;          // 当前波次目标杀敌数
    private int maxEnemiesOnScreen;   // 场上最大同时存在敌人输

    private boolean isGameOver;
    private boolean isWaveClearing;   // 是否正在切换波次中

    private Random random;
    private long lastSpawnTime;       // 上次生成敌人的时间

    // ========== 界面常量 ==========
    private static final Font HUD_FONT = Font.font("Microsoft YaHei", FontWeight.BOLD, 20);
    private static final Font GAME_OVER_FONT = Font.font("Impact", 60);

    public EndlessGameScene(Stage stage) {
        super(stage);
    }

    // ========== 1. 初始化逻辑 ==========

    @Override
    protected void initModeSpecificLogic() {
        random = new Random();
        enemyTanks = new ArrayList<>();
        bullets = new ArrayList<>();

        // 初始状态
        score = 0;
        currentWave = 1;
        isGameOver = false;

        System.out.println("🔥 无尽模式启动！准备迎接挑战...");
        startWave(currentWave);
    }

    @Override
    protected void resetModeSpecificData() {
        // 重玩时调用
        score = 0;
        currentWave = 1;
        isGameOver = false;
        enemiesKilledInWave = 0;
        enemyTanks.clear();
        bullets.clear();
        startWave(currentWave);
    }

    /**
     * 开始新的一波
     */
    private void startWave(int wave) {
        isWaveClearing = false;
        currentWave = wave;
        enemiesKilledInWave = 0;

        // 难度曲线：每波增加杀敌目标，每2波增加场上敌人上限
        targetKills = 5 + (wave * 2);
        maxEnemiesOnScreen = Math.min(10, 3 + (wave / 2));

        System.out.println("\n=== 第 " + wave + " 波开始 ===");
        System.out.println("目标: 消灭 " + targetKills + " 个敌人");

        // 1. 让工厂生产一张新图 (int[][])
        int[][] randomMapData = MapFactory.getMap(wave);

        // 2. 把这张新图塞给 MapModel (这里调用的是接收数组的构造函数)
        mapModel = new MapModel(randomMapData);

        // 3. 把转换好的格子给渲染层
        this.map = mapModel.getTiles();
        // ==========================================

        // 🛠️ 调试代码：如果屏幕还是黑的，请看控制台有没有这句话
        if (map != null && map[0][0] != null) {
            System.out.println("✅ 地图已加载到 Scene, [0][0]类型: " + map[0][0].getType());
        } else {
            System.err.println("❌ 严重错误: map 变量为空！");
        }
        // 2. 清空当前子弹和敌人
        bullets.clear();
        enemyTanks.clear();

        // 3. 初始化/重置玩家
        initializePlayer();

        // 4. 立即生成几个初始敌人
        for(int i = 0; i < Math.min(3, maxEnemiesOnScreen); i++) {
            spawnEnemy();
        }

        // 5. 显示波次提示
        showTipText("WAVE " + wave, 2.0);
    }

    private void initializePlayer() {
        // 设定左上角为默认出生点 (1,1 格子)
        // 注意：使用 GRID_SIZE 确保对齐
        double startX = GameConfig.GRID_SIZE * 1;
        double startY = GameConfig.GRID_SIZE * 1;

        if (player == null) {
            player = new PlayerTank(startX, startY);
            player.setHealth(GameConfig.PLAYER_HEALTH);
        } else {
            player.setX(startX);
            player.setY(startY);
            player.stopAllMovement();
            int heal = (int)(GameConfig.PLAYER_HEALTH * 0.3);
            player.heal(heal);
        }

        if (!player.isAlive()) {
            player.setHealth(GameConfig.PLAYER_HEALTH);
            player.setAlive(true);
        }

        // ==========================================
        // ⭐ 暴力修复：出生点强制拆迁
        // 不管地图生成器有没有清理干净，这里再清理一次，确保万无一失
        // ==========================================
        forceClearArea(startX, startY);
    }

    /**
     * 强制清理指定像素坐标周围的障碍物
     * 确保坦克出生时绝对不会卡在墙里
     */
    private void forceClearArea(double x, double y) {
        if (mapModel == null) return;

        // 坦克的尺寸
        double size = GameConfig.TANK_SIZE;
        // 稍微扩大一点清理范围，防止边缘摩擦
        double margin = 5.0;

        // 计算坦克占据的左上角和右下角所在的格子行列
        int startCol = (int)((x - margin) / GameConfig.GRID_SIZE);
        int endCol = (int)((x + size + margin) / GameConfig.GRID_SIZE);
        int startRow = (int)((y - margin) / GameConfig.GRID_SIZE);
        int endRow = (int)((y + size + margin) / GameConfig.GRID_SIZE);

        // 遍历这些格子，全部设为空地
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                // 边界检查
                if (r >= 0 && r < GameConfig.MAP_ROWS && c >= 0 && c < GameConfig.MAP_COLS) {
                    Tile t = mapModel.getTile(r, c);
                    if (t != null && !t.getType().isTankPassable()) {
                        // 发现障碍物！强制销毁！
                        // 注意：这里需要 MapModel 支持修改，或者直接修改 Tile 对象
                        // 如果 Tile 对象有 setType 方法最好，如果没有，可以用 destroy()

                        // 方案 A: 如果是砖块，直接 destroy
                        if (t.getType() == TileType.BRICK) {
                            t.destroy();
                        }
                        // 方案 B: 如果是铁墙/水，我们需要更底层的修改 (假设 Tile 有 setDestroyed 或我们重新生成一个空 Tile)
                        else {
                            // 简单粗暴：直接覆盖一个新的空 Tile
                            // 这需要 map 数组是 public 或者有 setTile 方法，
                            // 这里演示直接修改 mapModel 内部引用的方式 (如果 map 是直接引用的)
                            if (map != null) {
                                map[r][c] = new Tile(r, c, TileType.EMPTY);
                            }
                        }
                    }
                }
            }
        }
    }
    // ========== 2. 游戏循环 (Update) ==========

    @Override
    protected void updateGameLogic() {
        if (isGameOver) return;

        // 如果达成目标，延迟进入下一波
        if (enemiesKilledInWave >= targetKills && !isWaveClearing) {
            isWaveClearing = true;
            System.out.println("🎉 波次完成！即将进入下一波...");
            showTipText("WAVE COMPLETE!", 2.0);

            // 2秒后进入下一波 (使用 JavaFX 线程安全方式)
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> startWave(currentWave + 1));
            }).start();
            return;
        }

        if (isWaveClearing) return; // 转场中不更新逻辑

        // 1. 动态生成敌人
        spawnEnemyLogic();

        // 2. 更新实体
        updatePlayer();
        updateEnemies();
        updateBullets();

        // 3. 碰撞检测
        checkCollisions();

        // 4. 检查玩家存活
        if (player != null && !player.isAlive()) {
            isGameOver = true;
            System.out.println("💀 游戏结束！最终波次: " + currentWave + ", 得分: " + score);
        }
    }

    /**
     * 敌人生成管理器
     */
    private void spawnEnemyLogic() {
        // 只有当场上敌人少于上限，且还有剩余目标未击杀时，才生成
        int enemiesLeftToSpawn = targetKills - enemiesKilledInWave - enemyTanks.size();

        if (enemyTanks.size() < maxEnemiesOnScreen && enemiesLeftToSpawn > 0) {
            long now = System.currentTimeMillis();
            // 间隔 2 秒生成一只
            if (now - lastSpawnTime > 2000) {
                spawnEnemy();
                lastSpawnTime = now;
            }
        }
    }

    private void spawnEnemy() {
        // 1. 核心修复：不再随机像素坐标，而是随机 "格子索引"
        // 这样保证坦克永远在格子的正中间，不会一半在墙里一半在外面
        int gridX = random.nextInt(GameConfig.MAP_COLS);
        int gridY = random.nextInt(GameConfig.MAP_ROWS / 2); // 敌人通常在地图上半部分生成

        // 2. 尝试找一个原本就是空的地方 (尝试 10 次)
        for (int i = 0; i < 10; i++) {
            int testC = random.nextInt(GameConfig.MAP_COLS);
            int testR = random.nextInt(GameConfig.MAP_ROWS / 2);
            // 检查这个格子本身以及右边、下边的格子是否安全
            if (isAreaClear(testR, testC)) {
                gridX = testC;
                gridY = testR;
                break;
            }
        }

        // 3. 计算实际像素坐标 (严格对齐网格左上角)
        double spawnX = gridX * GameConfig.GRID_SIZE;
        double spawnY = gridY * GameConfig.GRID_SIZE;

        // ==========================================
        // ⭐ 关键：不管刚才找的位置是不是空的，
        // 既然决定在这里出生，就强制把这里挖空！
        // ==========================================
        forceClearArea(spawnX, spawnY);

        // 4. 生成坦克实体
        TankType type = TankType.ENEMY_NORMAL;
        double roll = random.nextDouble();
        // 难度公式
        double heavyChance = Math.min(0.4, currentWave * 0.05);
        double fastChance = Math.min(0.3, currentWave * 0.03);

        if (roll < heavyChance) type = TankType.ENEMY_HEAVY;
        else if (roll < heavyChance + fastChance) type = TankType.ENEMY_FAST;

        Tank enemy;
        switch (type) {
            case ENEMY_HEAVY: enemy = new HeavyTank(spawnX, spawnY); break;
            case ENEMY_FAST: enemy = new FastTank(spawnX, spawnY); break;
            default: enemy = new NormalTank(spawnX, spawnY); break;
        }

        enemyTanks.add(enemy);
    }
    /**
     * 检查以 (row, col) 为左上角的 2x2 格子区域是否为空
     * (因为坦克大小接近 40px，可能会稍微蹭到右边或下边的格子，保险起见查 2x2)
     */
    private boolean isAreaClear(int row, int col) {
        if (mapModel == null) return false;

        // 检查 2x2 区域
        for (int r = row; r <= row + 1; r++) {
            for (int c = col; c <= col + 1; c++) {
                // 越界检查
                if (r < 0 || r >= GameConfig.MAP_ROWS || c < 0 || c >= GameConfig.MAP_COLS) {
                    continue; // 忽略越界
                }

                Tile t = mapModel.getTile(r, c);
                // 如果有障碍物，返回 false
                if (t != null && !t.getType().isTankPassable()) {
                    return false;
                }
            }
        }
        return true;
    }
    // ========== 3. 更新与碰撞 (复用逻辑) ==========

    private void updatePlayer() {
        if (player.isAlive()) {
            // InputHandler 在 BaseGameScene 中定义
            player.setMovingForward(inputHandler.isWPressed());
            player.setMovingBackward(inputHandler.isSPressed());
            player.setRotatingLeft(inputHandler.isAPressed());
            player.setRotatingRight(inputHandler.isDPressed());

            if (inputHandler.isJPressed()) {
                Bullet b = player.tryFire();
                if (b != null) bullets.add(b);
            }
            player.update(map);
        }
    }

    private void updateEnemies() {
        // 使用 Config 中的时间步长计算 deltaTime (秒)
        double deltaTime = GameConfig.TIME_PER_FRAME / 1_000_000_000.0;

        for (Tank enemy : enemyTanks) {
            if (!enemy.isAlive()) continue;

            if (enemy instanceof EnemyTank) {
                EnemyTank ai = (EnemyTank) enemy;
                ai.updateAI(map, player, deltaTime);
                Bullet b = ai.consumePendingBullet(); // 取出AI发射的子弹
                if (b != null) bullets.add(b);
            }
            enemy.update(map);
            checkTankTankCollision(enemy); // 防止重叠
        }
    }

    private void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update(map);
            // 检查 Entity 的存活状态
            if (!b.isAlive()) {
                it.remove();
            }
        }
    }

    private void checkCollisions() {
        for (Bullet b : bullets) {
            if (!b.isAlive()) continue;

            // 1. 敌方子弹打玩家
            if (b.isEnemy && player.isAlive()) {
                // 使用 Entity 自带的 intersects 方法
                if (b.intersects(player)) {
                    player.takeDamage(b.getDamage());
                    b.setAlive(false);
                }
            }
            // 2. 我方子弹打敌人
            else if (!b.isEnemy) {
                for (int i = enemyTanks.size() - 1; i >= 0; i--) {
                    Tank e = enemyTanks.get(i);
                    if (e.isAlive() && b.intersects(e)) {
                        e.takeDamage(b.getDamage());
                        b.setAlive(false);

                        if (!e.isAlive()) {
                            score += e.getScoreValue();
                            enemiesKilledInWave++;
                            enemyTanks.remove(i);
                        }
                        break; // 一颗子弹只打一个敌人
                    }
                }
            }
        }
    }

    // 简单的坦克防重叠
    private void checkTankTankCollision(Tank t1) {
        // 同样使用 Entity 自带的 intersects 方法
        if (player.isAlive() && t1 != player && t1.intersects(player)) {
            resolveOverlap(t1, player);
        }
        for (Tank t2 : enemyTanks) {
            if (t1 != t2 && t1.intersects(t2)) {
                resolveOverlap(t1, t2);
            }
        }
    }

    private void resolveOverlap(Tank t1, Tank t2) {
        double dx = t1.getCenterX() - t2.getCenterX();
        double dy = t1.getCenterY() - t2.getCenterY();
        // 简单的推开逻辑
        t1.setX(t1.getX() + (dx > 0 ? 1 : -1));
        t1.setY(t1.getY() + (dy > 0 ? 1 : -1));
    }

    // ========== 4. 辅助方法：安全出生点查找 ==========

    private double findSafeSpawnPoint(boolean isPlayer) {
        int maxAttempts = 50;
        for (int i = 0; i < maxAttempts; i++) {
            double x = 50 + random.nextDouble() * (GameConfig.SCREEN_WIDTH - 100);
            double y = isPlayer ?
                    GameConfig.SCREEN_HEIGHT - 150 + random.nextDouble() * 100 :
                    50 + random.nextDouble() * 300;

            if (isPositionSafe(x, y)) return x;
        }
        return GameConfig.SCREEN_WIDTH / 2; // 兜底中间
    }

    private boolean isPositionSafe(double x, double y) {
        if (map == null) return true;
        double size = GameConfig.TANK_SIZE;
        // 检查坦克占用的四个角所在的格子
        return checkTile(x, y) && checkTile(x + size, y) &&
                checkTile(x, y + size) && checkTile(x + size, y + size);
    }

    private boolean checkTile(double x, double y) {
        int c = (int)(x / GameConfig.GRID_SIZE);
        int r = (int)(y / GameConfig.GRID_SIZE);
        // 越界检查
        if (r < 0 || r >= GameConfig.MAP_ROWS || c < 0 || c >= GameConfig.MAP_COLS) return false;

        Tile t = map[r][c];
        // 如果是空或者是可以通过的地形
        return t == null || t.getType().isTankPassable();
    }

    // ========== 5. 渲染 (Render) ==========

    @Override
    protected void renderGameFrame() {
        // 1. 画地图 (调用 SpritePainter, 假设 BaseGameScene 中已初始化)
        if (spritePainter != null) {
            spritePainter.drawMapBackground(mapGc, map);
        }

        // 2. 画坦克 (绘制到 tankGc 中间层)
        if (player != null && player.isAlive()) {
            player.draw(tankGc);
        }
        for (Tank e : enemyTanks) {
            e.draw(tankGc);
        }

        // 3. 画子弹 (绘制到 bulletGc 顶层)
        for (Bullet b : bullets) {
            b.draw(bulletGc);
        }

        // 4. 画前景 (如草丛遮挡)
        if (spritePainter != null) {
            spritePainter.drawMapForeground(bulletGc, map);
        }

        // 5. 画 UI / HUD
        drawHUD(bulletGc);

        // 6. 游戏结束画面
        if (isGameOver) {
            drawGameOver(bulletGc);
        }
    }

    private void drawHUD(GraphicsContext gc) {
        gc.save();
        gc.setFont(HUD_FONT);

        // 左上：波次进度
        gc.setFill(Color.GOLD);
        gc.fillText("WAVE " + currentWave, 20, 30);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 18));
        String progress = String.format("Kills: %d / %d", enemiesKilledInWave, targetKills);
        gc.fillText(progress, 20, 60);

        // 右上：血量和分数
        if (player != null) {
            gc.setFill(player.getHealthPercentage() > 0.3 ? Color.LIME : Color.RED);
            gc.fillText("HP: " + player.getHealth() + " / " + player.getMaxHealth(), WIDTH - 250, 30);
        }

        gc.setFill(Color.CYAN);
        gc.fillText("Score: " + score, WIDTH - 250, 60);

        gc.restore();
    }

    private void drawGameOver(GraphicsContext gc) {
        gc.save();
        gc.setFill(Color.rgb(0, 0, 0, 0.6)); // 半透明黑底
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.RED);
        gc.setFont(GAME_OVER_FONT);
        gc.fillText("GAME OVER", WIDTH/2 - 140, HEIGHT/2);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 30));
        gc.fillText("Final Wave: " + currentWave, WIDTH/2 - 100, HEIGHT/2 + 60);
        gc.fillText("Total Score: " + score, WIDTH/2 - 100, HEIGHT/2 + 100);
        gc.restore();
    }
}