package view;

import infra.GameConfig;
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

        // 1. 生成全新随机地图
        mapModel = new MapModel(wave);
        map = mapModel.getTiles();

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
        // 尝试在地图下方寻找安全出生点
        double startX = GameConfig.SCREEN_WIDTH / 2 - 20;
        double startY = GameConfig.SCREEN_HEIGHT - 100;

        // 使用通用的安全位置查找逻辑
        if (!isPositionSafe(startX, startY)) {
            // 如果预设点不行，随机找一个
            startX = findSafeSpawnPoint(true);
            startY = GameConfig.SCREEN_HEIGHT - 100; // Y轴尽量靠下
        }

        if (player == null) {
            player = new PlayerTank(startX, startY);
            player.setHealth(GameConfig.PLAYER_HEALTH);
        } else {
            // 后续波次：继承血量，但给予奖励回复
            player.setX(startX);
            player.setY(startY);
            player.stopAllMovement(); // 停止移动

            // 过关回血 30%
            int heal = (int)(GameConfig.PLAYER_HEALTH * 0.3);
            player.heal(heal);
        }

        // 确保玩家重置后是存活状态
        if (!player.isAlive()) {
            player.setHealth(GameConfig.PLAYER_HEALTH);
            player.setAlive(true); // 确保 Entity 状态也是活的
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
        // 随机在地图上半部分找一个安全点
        double x = findSafeSpawnPoint(false);
        double y = 50 + random.nextDouble() * 300;

        // 确保出生点没撞墙
        if (!isPositionSafe(x, y)) return;

        // 根据波次决定敌人类型
        TankType type = TankType.ENEMY_NORMAL;
        double roll = random.nextDouble();

        // 难度公式
        double heavyChance = Math.min(0.4, currentWave * 0.05); // 每波增加 5% 重坦率
        double fastChance = Math.min(0.3, currentWave * 0.03);  // 每波增加 3% 快坦率

        if (roll < heavyChance) type = TankType.ENEMY_HEAVY;
        else if (roll < heavyChance + fastChance) type = TankType.ENEMY_FAST;

        Tank enemy;
        switch (type) {
            case ENEMY_HEAVY: enemy = new HeavyTank(x, y); break;
            case ENEMY_FAST: enemy = new FastTank(x, y); break;
            default: enemy = new NormalTank(x, y); break;
        }

        enemyTanks.add(enemy);
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