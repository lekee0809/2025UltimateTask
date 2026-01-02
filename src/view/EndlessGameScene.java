package view;

import infra.GameConfig;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.*;
import map.MapFactory; // ✅ 1. 引入工厂
import map.MapModel;
import model.*;
import model.Tank.TankType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import item.Item;
import item.ItemType;
import ranking.PlayerRecord; // 新增：导入PlayerRecord
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
    private long gameStartTime;       // 新增：游戏开始时间戳（用于计算游玩时长）

    // ========== 界面常量 ==========
    private static final Font HUD_FONT = Font.font("Microsoft YaHei", FontWeight.BOLD, 20);
    private static final Font GAME_OVER_FONT = Font.font("Impact", 60);

    public EndlessGameScene(Stage stage) {
        super(stage);
        gameStartTime = System.currentTimeMillis(); // 初始化游戏开始时间
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
        // ========== 新增：播放无尽模式背景音乐 ==========
        SoundManager.getInstance().playGameMusic(); // 与闯关模式共用同一首音乐，可自定义路径
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
        gameStartTime = System.currentTimeMillis(); // 重置游戏开始时间
        startWave(currentWave);
    }

    @Override
    protected PlayerTank getPlayerTank() {
        return player;
    }

    /**
     * 核心：实现父类抽象方法，返回无尽模式
     */
    @Override
    protected PlayerRecord.GameMode getCurrentGameMode() {
        return PlayerRecord.GameMode.ENDLESS_MODE;
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
        // 1. 寻找玩家的安全出生点 (true 表示只在地图下方找)
        // 如果实在是运气差找不到，findFreeGridTile 会返回默认点 (1,1)
        int[] validPos = findFreeGridTile(true);

        // 兜底：如果连随机都失败，就回退到 (1,1) 或者固定点，
        // 只有这种极端情况才需要考虑破坏地形，但通常不需要
        if (validPos == null) {
            validPos = new int[]{1, 1}; // 左上角保底
        }

        int gridR = validPos[0];
        int gridC = validPos[1];

        // 居中计算
        double offset = (GameConfig.GRID_SIZE - GameConfig.TANK_SIZE) / 2.0;
        double startX = gridC * GameConfig.GRID_SIZE + offset;
        double startY = gridR * GameConfig.GRID_SIZE + offset;

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
        player.activateShield(3.0);
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
        // 先调用父类更新道具逻辑
        super.updateBaseElements();

        // 如果达成目标，延迟进入下一波
        if (enemiesKilledInWave >= targetKills && !isWaveClearing) {
            isWaveClearing = true;
            System.out.println("🎉 波次完成！即将进入下一波...");
            showTipText("WAVE COMPLETE!", 2.0);

            // 2秒后进入下一波 (使用 JavaFX 线程安全方式)
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
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
            if (!isGameOver) { // 确保只触发一次
                isGameOver = true;
                System.out.println("💀 游戏结束！");

                // 停止背景音乐
                view.SoundManager.getInstance().stopBackgroundMusic();

                // --- 核心：直接在这里绑定一次性按键逻辑 ---

                // 1. 绑定 R 键 -> 重新开始
                inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.R, () -> {
                    System.out.println("🔄 正在重新部署坦克...");
                    this.resetScene(); // 调用父类的重置
                });

                // 2. 绑定 ESC 键 -> 返回主菜单
                // 虽然 InputHandler 默认 ESC 是打开设置，但 bindKeyPressOnce 优先级更高且会 consume 事件
                inputHandler.bindKeyPressOnce(javafx.scene.input.KeyCode.ESCAPE, () -> {
                    System.out.println("🏠 撤离战场...");
                    gameLoop.stop(); // 停止发动机
                    // 方案 B: 直接跳转回 AppLauncher 重新展示主菜单
                    try {
                        game.AppLauncher mainMenu = new game.AppLauncher();
                        mainMenu.start(primaryStage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            return;
        }
    }
    /**
     * 新增：封装无尽模式游戏记录写入逻辑
     */
    private void writeGameRecord() {
        // 计算游玩时长（秒）
        long playTime = (System.currentTimeMillis() - gameStartTime) / 1000;
        // 拾取道具总数（从itemSpawner中获取）
        int itemCount = itemSpawner.getActiveItems().size();
        // 无尽模式无"胜利"概念，isWin传false（可自定义为：波次>=10算胜利）
        boolean isWin = currentWave >= 10; // 自定义胜负规则

        // 调用父类方法写入记录
        writeGameFinalRecord(isWin, score, playTime, itemCount);
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
        // 1. 寻找一个合法的网格坐标 (Row, Col)
        // 我们尝试 100 次，如果地图实在太挤找不到，就放弃这次生成，或者生在默认点
        int[] validPos = findFreeGridTile(false); // false 表示不是玩家，可以生在地图任意位置(通常上半区)

        if (validPos == null) {
            System.out.println("⚠️ 警告：当前地图太拥挤，无法生成新敌人");
            return;
        }

        int gridR = validPos[0];
        int gridC = validPos[1];

        // 2. 将网格坐标转换为像素坐标
        // 居中计算：(格子宽 - 坦克宽) / 2
        double offset = (GameConfig.GRID_SIZE - GameConfig.TANK_SIZE) / 2.0;
        double spawnX = gridC * GameConfig.GRID_SIZE + offset;
        double spawnY = gridR * GameConfig.GRID_SIZE + offset;

        // 3. 生成坦克 (完全不需要 forceClearArea 了！)
        TankType type = TankType.ENEMY_NORMAL;
        double roll = random.nextDouble();

        // ... (原有的难度判断代码保持不变) ...
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
     * 在地图上随机寻找一个空闲的格子
     * @param isPlayer true=只在地图下方找; false=全图(或上半区)找
     * @return int[]{row, col} 或者 null (没找到)
     */
// 修改 findFreeGridTile 方法
// 修改 findFreeGridTile 方法
    private int[] findFreeGridTile(boolean isPlayer) {
        if (mapModel == null) return new int[]{1, 1};

        int maxAttempts = 100;
        // 设定最小安全距离（比如 10 个格子，防止贴脸）
        double minSafeDistance = 10.0 * GameConfig.GRID_SIZE;

        for (int i = 0; i < maxAttempts; i++) {
            int c = random.nextInt(GameConfig.MAP_COLS);
            int r = random.nextInt(GameConfig.MAP_ROWS);

            // 1. 如果是生成玩家，强制限制在左上角 (比如 6x6 区域)
            if (isPlayer) {
                c = random.nextInt(6);
                r = random.nextInt(6);
            }
            // 2. 如果是生成敌人，必须远离玩家
            else if (player != null && player.isAlive()) {
                double dx = (c * GameConfig.GRID_SIZE) - player.getX();
                double dy = (r * GameConfig.GRID_SIZE) - player.getY();
                double dist = Math.sqrt(dx*dx + dy*dy);

                // 如果离玩家太近，或者随机到了左上角老家，直接重来
                if (dist < minSafeDistance || (c < 6 && r < 6)) {
                    continue;
                }
            }

            // 3. 基础检查：越界、是否是墙、是否重叠
            if (r < 0 || r >= GameConfig.MAP_ROWS || c < 0 || c >= GameConfig.MAP_COLS) continue;
            Tile t = mapModel.getTile(r, c);

            // 只要是空地或草地
            if (t != null && (t.getType() == TileType.EMPTY || t.getType() == TileType.GRASS)) {
                if (!isPositionOccupiedByTank(c, r)) {
                    return new int[]{r, c}; // 找到完美位置
                }
            }
        }
        return null;
    }
    /**
     * 检查某个网格坐标上是否已经有坦克霸占了
     */
    private boolean isPositionOccupiedByTank(int col, int row) {
        // 转换成中心点像素用于检测
        double centerX = col * GameConfig.GRID_SIZE + GameConfig.GRID_SIZE / 2.0;
        double centerY = row * GameConfig.GRID_SIZE + GameConfig.GRID_SIZE / 2.0;
        double checkRadius = GameConfig.GRID_SIZE / 1.5; // 检查半径

        // 检查玩家
        if (player != null && player.isAlive()) {
            if (Math.abs(player.getCenterX() - centerX) < checkRadius &&
                    Math.abs(player.getCenterY() - centerY) < checkRadius) {
                return true;
            }
        }

        // 检查其他敌人
        for (Tank t : enemyTanks) {
            if (t.isAlive()) {
                if (Math.abs(t.getCenterX() - centerX) < checkRadius &&
                        Math.abs(t.getCenterY() - centerY) < checkRadius) {
                    return true;
                }
            }
        }
        return false;
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
                Bullet b = player.tryFire(map);
                if (b != null) bullets.add(b);
                // ========== 新增：播放子弹发射音效 ==========
                SoundManager.getInstance().playSoundEffect("explosion.wav"); // 复用子弹音效
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
                            // 【新增】触发道具掉落
                            if (e instanceof EnemyTank) {
                                itemSpawner.onEnemyDestroyed((EnemyTank) e);
                            }

                            enemyTanks.remove(i);
                        }
                        break; // 一颗子弹只打一个敌人
                    }
                }
            }
        }
    }
    /**
     * 处理炸弹效果
     */
    @Override
    protected void handleBombEffect(Item item) {
        if (item.getType() != ItemType.BOMB) return;

        System.out.println("💣 炸弹爆炸！对全图敌人造成50点伤害");

        // 创建临时列表收集被炸死的敌人
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
                    // 增加分数和击杀计数
                    score += enemy.getScoreValue();
                    enemiesKilledInWave++;
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

// 5. 调用父类绘制道具和粒子特效
        super.renderBaseElements();
        // 6. 画 UI / HUD
        drawHUD(bulletGc);

        // 7. 游戏结束画面
        if (isGameOver) {
            drawGameOver(bulletGc);
        }
    }



    // 替换原有drawHUD方法
    private void drawHUD(GraphicsContext gc) {
        gc.save();

        // 左侧面板：波次和击杀数
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRoundRect(15, 15, 200, 90, 10, 10);
        gc.setStroke(Color.web("#e67e22"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(15, 15, 200, 90, 10, 10);

        gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20));
        gc.setFill(Color.web("#f39c12"));
        gc.fillText("WAVE " + currentWave, 35, 45);

        gc.setFont(Font.font("Consolas", 18));
        gc.setFill(Color.WHITE);
        String progress = String.format("Kills: %d / %d", enemiesKilledInWave, targetKills);
        gc.fillText(progress, 35, 75);

        // 右侧面板：血量和分数
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRoundRect(WIDTH - 215, 15, 200, 90, 10, 10);
        gc.setStroke(Color.web("#e67e22"));
        gc.strokeRoundRect(WIDTH - 215, 15, 200, 90, 10, 10);

        if (player != null) {
            gc.setFill(player.getHealthPercentage() > 0.3 ? Color.web("#2ecc71") : Color.web("#e74c3c"));
            gc.fillText("HP: " + player.getHealth() + " / " + player.getMaxHealth(), WIDTH - 195, 45);
        }

        gc.setFill(Color.web("#f1c40f"));
        gc.fillText("Score: " + score, WIDTH - 195, 75);

        gc.restore();
    }

    // 优化drawGameOver方法（保留原有逻辑，增强视觉效果）
    private void drawGameOver(GraphicsContext gc) {
        double screenW = GameConfig.SCREEN_WIDTH;
        double screenH = GameConfig.SCREEN_HEIGHT;
        double centerX = screenW / 2;
        double centerY = screenH / 2;

        gc.save();

        // 增强版背景：深红色径向渐变 + 战术网格
        gc.setFill(new RadialGradient(0, 0, centerX, centerY, screenW * 0.8, false,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.rgb(80, 0, 0, 0.9)),
                new javafx.scene.paint.Stop(1, Color.BLACK)));
        gc.fillRect(0, 0, screenW, screenH);

        // 更密集的战术网格
        gc.setStroke(Color.rgb(255, 255, 255, 0.08));
        gc.setLineWidth(1);
        for(int i=0; i<screenW; i+=30) gc.strokeLine(i, 0, i, screenH);
        for(int i=0; i<screenH; i+=30) gc.strokeLine(0, i, screenW, i);

        // 标题增强：双层阴影 + 金属质感
        gc.setFont(Font.font("Impact", 110));

        // 第一层：深红色外发光
        gc.setEffect(new javafx.scene.effect.DropShadow(40, Color.RED));
        gc.setFill(Color.web("#800000"));
        gc.fillText("MISSION FAILED", centerX - 300, centerY - 120);

        // 第二层：亮红色主体 + 斜面效果
        gc.setEffect(new javafx.scene.effect.Lighting());
        gc.setFill(new LinearGradient(0, 0, 0, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.RED),
                new javafx.scene.paint.Stop(1, Color.web("#500000"))));
        gc.fillText("MISSION FAILED", centerX - 303, centerY - 123);

        // 数据面板增强
        double panelW = 550;
        double panelH = 200;
        double px = centerX - panelW / 2;
        double py = centerY - 40;

        // 面板底色：深色渐变
        gc.setFill(new LinearGradient(0, 0, 0, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.rgb(30, 30, 30, 0.95)),
                new javafx.scene.paint.Stop(1, Color.rgb(10, 10, 10, 0.95))));
        gc.fillRoundRect(px, py, panelW, panelH, 15, 15);

        // 金属边框：金色渐变
        gc.setStroke(new LinearGradient(0, 0, 1, 0, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#fbc531")),
                new javafx.scene.paint.Stop(1, Color.web("#e1b12c"))));
        gc.setLineWidth(4);
        gc.strokeRoundRect(px, py, panelW, panelH, 15, 15);

        // 装饰角标增强
        gc.setFill(Color.web("#fbc531"));
        gc.fillPolygon(new double[]{px, px+30, px}, new double[]{py, py+30, py+30}, 3); // 左上
        gc.fillPolygon(new double[]{px+panelW, px+panelW-30, px+panelW},
                new double[]{py+panelH, py+panelH-30, py+panelH}, 3); // 右下

        // 文字增强
        gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 32));
        gc.setFill(Color.WHITE);
        gc.fillText("SURVIVED WAVES:", px + 50, py + 75);
        gc.fillText("TOTAL SCORE:", px + 50, py + 135);

        // 数值使用霓虹效果
        gc.setFill(Color.web("#fbc531"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 40));
        gc.setEffect(new javafx.scene.effect.Glow(0.8));
        gc.fillText(String.valueOf(currentWave), px + 350, py + 75);
        gc.fillText(String.valueOf(score), px + 350, py + 135);
        gc.setEffect(null);

        // 底部操作栏增强
        gc.setFill(Color.rgb(255, 255, 255, 0.15));
        gc.fillRoundRect(0, screenH - 120, screenW, 120, 0, 0);

        // 按键提示增强
        drawModernHint(gc, "R", "REDEPLOY (重新部署)", centerX - 280, screenH - 50, Color.web("#2ecc71"));
        drawModernHint(gc, "ESC", "ABORT (撤离)", centerX + 50, screenH - 50, Color.web("#3498db"));

        gc.restore();
    }

    // 优化按键提示绘制
    private void drawModernHint(GraphicsContext gc, String key, String text, double x, double y, Color themeColor) {
        gc.save();

        // 按键背景渐变
        LinearGradient keyGrad = new LinearGradient(0, 0, 1, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, themeColor),
                new javafx.scene.paint.Stop(1, themeColor.darker()));
        gc.setFill(keyGrad);
        gc.fillRoundRect(x, y - 35, 70, 45, 8, 8);

        // 按键边框
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y - 35, 70, 45, 8, 8);

        // 按键文字
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Consolas", FontWeight.BLACK, 26));
        gc.fillText(key, x + (key.length() == 1 ? 25 : 12), y - 5);

        // 说明文字
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
        gc.fillText(text, x + 85, y - 5);

        // 呼吸效果
        gc.setEffect(new javafx.scene.effect.DropShadow(15, themeColor));
        gc.setStroke(themeColor);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y - 35, 70, 45, 8, 8);

        gc.restore();
    }

    /**
     * 现代感按键提示绘制
     */


    // EndlessGameScene 中添加 resetScene 重写
    @Override
    public void resetScene() {
        super.resetScene(); // 父类重置
        // 重置无尽模式数据
        score = 0;
        currentWave = 1;
        isGameOver = false;
        enemiesKilledInWave = 0;
        enemyTanks.clear();
        bullets.clear();
        gameStartTime = System.currentTimeMillis(); // 重置游戏开始时间
        startWave(currentWave);
        resumeGameProcess(); // 重置后恢复游戏

        // ========== 新增：重置时恢复背景音乐播放（防止重置后音乐停止） ==========
        SoundManager.getInstance().resumeGameMusic();
    }

    public PlayerTank getPlayer() {
        return player;
    }

    public void setPlayer(PlayerTank player) {
        this.player = player;
    }

    public List<Tank> getEnemyTanks() {
        return enemyTanks;
    }

    public void setEnemyTanks(List<Tank> enemyTanks) {
        this.enemyTanks = enemyTanks;
    }

    public List<Bullet> getBullets() {
        return bullets;
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

    public int getCurrentWave() {
        return currentWave;
    }

    public void setCurrentWave(int currentWave) {
        this.currentWave = currentWave;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getEnemiesKilledInWave() {
        return enemiesKilledInWave;
    }

    public void setEnemiesKilledInWave(int enemiesKilledInWave) {
        this.enemiesKilledInWave = enemiesKilledInWave;
    }

    public int getTargetKills() {
        return targetKills;
    }

    public void setTargetKills(int targetKills) {
        this.targetKills = targetKills;
    }

    public int getMaxEnemiesOnScreen() {
        return maxEnemiesOnScreen;
    }

    public void setMaxEnemiesOnScreen(int maxEnemiesOnScreen) {
        this.maxEnemiesOnScreen = maxEnemiesOnScreen;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public boolean isWaveClearing() {
        return isWaveClearing;
    }

    public void setWaveClearing(boolean waveClearing) {
        isWaveClearing = waveClearing;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public long getLastSpawnTime() {
        return lastSpawnTime;
    }

    public void setLastSpawnTime(long lastSpawnTime) {
        this.lastSpawnTime = lastSpawnTime;
    }

    public long getGameStartTime() {
        return gameStartTime;
    }

    public void setGameStartTime(long gameStartTime) {
        this.gameStartTime = gameStartTime;
    }
}