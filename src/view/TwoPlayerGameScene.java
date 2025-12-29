package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import infra.GameConfig;
import map.MapModel;
import map.MapTileView;
import map.MapConstants;
import map.LevelData;
import model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 双人对战模式：双玩家对抗，WASD+J控制玩家1，方向键+K控制玩家2
 * 完整适配新版 MapModel（关卡加载、纯数据模型）和 MapTileView 地图渲染
 */
public class TwoPlayerGameScene extends BaseGameScene {
    // 双玩家坦克
    private Tank player1;
    private Tank player2;
    // 子弹列表
    private List<Bullet> bulletList = new ArrayList<>();
    // 胜负状态
    private boolean gameOver = false;
    private String winner = "";
    // 双人对战地图核心组件（适配新版 MapModel）
    private MapModel twoPlayerMap; // 新版 MapModel（纯数据+规则）
    private Tile[][] twoPlayerTileMap; // 适配 Bullet 类的 Tile 二维数组
    private MapTileView mapTileView; // 地图瓦片视图（负责渲染）
    private static final int TWO_PLAYER_LEVEL = 1; // 双人对战使用关卡1（可自定义为2/3）

    public TwoPlayerGameScene(Stage primaryStage) {
        super(primaryStage);
    }

    // ========== 实现 BaseGameScene 抽象方法1：initModeSpecificLogic（模式专属初始化） ==========
    @Override
    protected void initModeSpecificLogic() {
        // 1. 初始化 MapTileView 并添加到场景层级（替换原有 mapCanvas）
        initMapTileView();

        // 2. 初始化新版 MapModel（使用关卡构造，兼容 LevelData）
        initMapModel();

        // 3. 转换 MapModel 的 Tile 数组（适配 Bullet 类的碰撞逻辑）
        convertMapModelToTileArray();

        // 4. 使用 MapTileView 渲染地图（核心：调用 mapTileView.render()）
        mapTileView.render(twoPlayerMap);

        // 5. 初始化双玩家坦克
        initTwoPlayers();

        // 6. 绑定双玩家输入
        bindTwoPlayerInput();

        // 7. 播放启动音效
        SoundManager.getInstance().playSoundEffect("game");
    }

    /**
     * 初始化 MapTileView（地图渲染核心，替换原有简易地图）
     */
    private void initMapTileView() {
        // 创建 MapTileView 实例
        mapTileView = new MapTileView();
        // 设置 MapTileView 位置与大小（适配游戏场景）
        mapTileView.setLayoutX(0);
        mapTileView.setLayoutY(0);
        mapTileView.setWidth(WIDTH);
        mapTileView.setHeight(HEIGHT);
        // 调整层级：移除原有 mapCanvas，添加 MapTileView 到底层
        gameRoot.getChildren().remove(mapCanvas);
        gameRoot.getChildren().add(0, mapTileView);
    }

    /**
     * 初始化新版 MapModel（适配其构造方法和关卡加载逻辑）
     */
    private void initMapModel() {
        // 新版 MapModel 构造：传入关卡编号（1/2/3，对应 LevelData）
        twoPlayerMap = new MapModel(TWO_PLAYER_LEVEL);
        // 双人对战模式：关闭闯关模式（草丛不隐身，公平对抗）
        twoPlayerMap.setCampaignMode(false);
    }

    /**
     * 转换新版 MapModel 的 Tile 数组（适配 Bullet 类，无需手动创建 Tile）
     * 直接复用 MapModel 内部维护的 tiles 二维数组
     */
    private void convertMapModelToTileArray() {
        // 新版 MapModel 已自带 Tile[][]，直接获取即可，无需手动映射数据
        twoPlayerTileMap = twoPlayerMap.getTiles();
    }

    // ========== 实现 BaseGameScene 抽象方法2：updateGameLogic（游戏逻辑更新） ==========
    @Override
    protected void updateGameLogic() {
        if (gameOver) {
            return; // 游戏结束后停止逻辑更新
        }

        // 1. 更新坦克位置（调用 Tank 原有 update 方法，兼容 MapModel 规则）
        player1.update(twoPlayerTileMap);
        player2.update(twoPlayerTileMap);

        // 2. 更新子弹状态（调用 Bullet 原有 update 方法，触发 MapModel 子弹规则）
        updateBullets();

        // 3. 碰撞检测（子弹-坦克、坦克-坦克，兼容 MapModel 地形规则）
        checkCollisions();

        // 4. 检测游戏胜负
        checkGameOver();

        // 5. 实时更新地图（当瓦片被销毁时，重新渲染）
        mapTileView.render(twoPlayerMap);
    }

    // ========== 实现 BaseGameScene 抽象方法3：renderGameFrame（游戏画面渲染） ==========
    @Override
    protected void renderGameFrame() {
        // 1. 仅清空坦克层和子弹层（地图层由 MapTileView 负责）
        clearCanvas(tankGc);
        clearCanvas(bulletGc);

        // 2. 绘制存活坦克
        if (player1.isAlive()) {
            player1.draw(tankGc);
        }
        if (player2.isAlive()) {
            player2.draw(tankGc);
        }

        // 3. 绘制存活子弹
        for (Bullet bullet : bulletList) {
            if (bullet.alive) {
                bullet.draw(bulletGc);
            }
        }

        // 4. 游戏结束时绘制结算UI
        if (gameOver) {
            drawGameOverUI();
        }
    }

    // ========== 实现 BaseGameScene 抽象方法4：resetModeSpecificData（模式专属数据重置） ==========
    @Override
    protected void resetModeSpecificData() {
        // 1. 重置胜负状态
        gameOver = false;
        winner = "";
        // 2. 清空子弹列表
        bulletList.clear();
        // 3. 重新初始化双玩家坦克
        initTwoPlayers();
        // 4. 重置 MapModel（重新加载当前关卡）
        twoPlayerMap.reset(TWO_PLAYER_LEVEL);
        // 5. 重新获取 Tile[][] 数组
        convertMapModelToTileArray();
        // 6. 重新渲染地图并加载图片
        mapTileView.render(twoPlayerMap);
        mapTileView.reloadImages();
        // 7. 重置双人对战模式（关闭闯关模式）
        twoPlayerMap.setCampaignMode(false);
    }

    // ========== 原有核心逻辑（适配新版 MapModel，无侵入性修改） ==========
    /**
     * 初始化双玩家坦克
     */
    private void initTwoPlayers() {
        // 玩家1：左侧出生（避开地图边界障碍物）
        player1 = new PlayerTank(80, 300);
        player1.setSpeed(5);
        player1.setHealth(3);

        // 玩家2：右侧出生，朝左（避开地图边界障碍物）
        player2 = new NormalTank(700, 280);
        player2.setSpeed(5);
        player2.setHealth(3);
        player2.setLogicRotation(180.0);
        player2.setDisplayRotation(180.0);
    }

    // TwoPlayerGameScene.java
    /**
     * 绑定双玩家输入（增加scene非空判断，避免空指针）
     */
    private void bindTwoPlayerInput() {
        // 增加非空判断，健壮性优化
        if (scene == null) {
            System.err.println("警告：scene为空，无法绑定双玩家输入！");
            return;
        }

        // 按键按下事件
        scene.setOnKeyPressed(e -> {
            if (gameOver) return;
            switch (e.getCode()) {
                // 玩家1控制
                case W: player1.setMovingForward(true); SoundManager.getInstance().playSoundEffect("move"); break;
                case S: player1.setMovingBackward(true); SoundManager.getInstance().playSoundEffect("move"); break;
                case A: player1.setRotatingLeft(true); SoundManager.getInstance().playSoundEffect("move"); break;
                case D: player1.setRotatingRight(true); SoundManager.getInstance().playSoundEffect("move"); break;
                case J: shootBullet(player1); break;

                // 玩家2控制
                case UP: player2.setMovingForward(true); SoundManager.getInstance().playSoundEffect("move"); break;
                case DOWN: player2.setMovingBackward(true); SoundManager.getInstance().playSoundEffect("move"); break;
                case LEFT: player2.setRotatingLeft(true); SoundManager.getInstance().playSoundEffect("move"); break;
                case RIGHT: player2.setRotatingRight(true); SoundManager.getInstance().playSoundEffect("move"); break;
                case K: shootBullet(player2); break;
            }
        });

        // 按键释放事件
        scene.setOnKeyReleased(e -> {
            if (gameOver) return;
            switch (e.getCode()) {
                // 玩家1停止
                case W: player1.setMovingForward(false); break;
                case S: player1.setMovingBackward(false); break;
                case A: player1.setRotatingLeft(false); break;
                case D: player1.setRotatingRight(false); break;

                // 玩家2停止
                case UP: player2.setMovingForward(false); break;
                case DOWN: player2.setMovingBackward(false); break;
                case LEFT: player2.setRotatingLeft(false); break;
                case RIGHT: player2.setRotatingRight(false); break;
            }
        });
    }
    /**
     * 发射子弹（兼容新版 MapModel 子弹规则）
     */
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

        // 创建子弹（兼容新版 MapModel 交互规则）
        Bullet bullet = new Bullet(isEnemy, damage, direction, speedx, speedy, muzzleX, muzzleY, bulletWidth, bulletHeight);
        bulletList.add(bullet);
        SoundManager.getInstance().playSoundEffect("shoot");
    }

    /**
     * 更新子弹状态（触发 MapModel 的 handleBullet 规则）
     */
    private void updateBullets() {
        List<Bullet> removeList = new ArrayList<>();
        for (Bullet bullet : bulletList) {
            if (!bullet.alive) {
                removeList.add(bullet);
                continue;
            }
            // 子弹 update 内部会调用 MapModel 的 handleBullet 方法，处理穿透/反弹/销毁
            bullet.update(twoPlayerTileMap);
        }
        bulletList.removeAll(removeList);
    }

    /**
     * 碰撞检测（兼容新版 MapModel 地形规则）
     */
    private void checkCollisions() {
        List<Bullet> removeBulletList = new ArrayList<>();

        for (Bullet bullet : bulletList) {
            if (!bullet.alive) continue;

            // 玩家1子弹击中玩家2
            if (!bullet.isEnemy && player2.isAlive() && isCollide(bullet, player2)) {
                player2.takeDamage(bullet.getDamage());
                bullet.alive = false;
                removeBulletList.add(bullet);
                SoundManager.getInstance().playSoundEffect("explosion");
                continue;
            }

            // 玩家2子弹击中玩家1
            if (bullet.isEnemy && player1.isAlive() && isCollide(bullet, player1)) {
                player1.takeDamage(bullet.getDamage());
                bullet.alive = false;
                removeBulletList.add(bullet);
                SoundManager.getInstance().playSoundEffect("explosion");
                continue;
            }
        }

        bulletList.removeAll(removeBulletList);

        // 坦克与坦克碰撞处理
        if (player1.isAlive() && player2.isAlive() && isCollide(player1, player2)) {
            resolveTankOverlap(player1, player2);
        }
    }

    /**
     * 检测游戏胜负
     */
    private void checkGameOver() {
        if (player1.getHealth() <= 0 && player1.getHealth() < player2.getHealth()) {
            gameOver = true;
            winner = "玩家2（红色坦克）胜利！";
            this.pauseGameProcess(); // 调用父类暂停方法
        } else if (player2.getHealth() <= 0 && player2.getHealth() < player1.getHealth()) {
            gameOver = true;
            winner = "玩家1（蓝色坦克）胜利！";
            this.pauseGameProcess(); // 调用父类暂停方法
        }
    }

    /**
     * 绘制游戏结束UI
     */
    private void drawGameOverUI() {
        GraphicsContext gc = mapGc;
        // 半透明遮罩
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // 胜利文字（居中）
        gc.setFill(Color.ORANGE);
        gc.setFont(Font.font("微软雅黑", 48));
        double winnerWidth = getTextWidth(gc, winner);
        gc.fillText(winner, (WIDTH - winnerWidth) / 2, HEIGHT / 2);

        // 提示文字
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("微软雅黑", 24));
        String tipText = "按ESC返回模式选择界面";
        double tipWidth = getTextWidth(gc, tipText);
        gc.fillText(tipText, (WIDTH - tipWidth) / 2, HEIGHT / 2 + 60);
    }

    // ========== 辅助方法 ==========
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

    // ========== 重写暂停/恢复方法（补充gameOver判断） ==========
    @Override
    protected void pauseGameProcess() {
        super.pauseGameProcess();
    }

    @Override
    protected void resumeGameProcess() {
        super.resumeGameProcess();
        // 仅当游戏未结束时恢复循环
        if (gameLoop != null && !gameOver) {
            gameLoop.start();
        }
    }
}