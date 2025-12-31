package model;

import infra.GameConfig;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * 完全独立的坦克类，继承Entity
 * 修复版：解决了无限递归崩溃、贴墙射击问题，并实装了无敌护盾
 */
public abstract class Tank extends Entity {

    //  定义可观察的X/Y属性
    private final DoubleProperty xProperty = new SimpleDoubleProperty(0);
    private final DoubleProperty yProperty = new SimpleDoubleProperty(0);

    // ========== 平滑旋转系统 ==========
    private double logicRotation = 0.0;
    private double displayRotation = 0.0;
    private double rotationSpeed;
    private double smoothFactor = 0.15;

    // ========== 移动系统 ==========
    private double vx, vy;
    private double speed;

    // ========== 坦克属性 ==========
    protected int health;
    protected int maxHealth;
    protected int scoreValue;
    protected TankType type;

    // ========== 控制状态 ==========
    protected boolean movingForward = false;
    protected boolean movingBackward = false;
    protected boolean rotatingLeft = false;
    protected boolean rotatingRight = false;

    // ========== 射击系统 ==========
    protected long lastFireTime = 0;
    protected int fireCooldown;
    protected int bulletDamage;
    protected double bulletSpeed;

    // 【新增】初始基准属性 (用于道具过期后恢复)
    protected int baseFireCooldown;
    protected int baseBulletDamage;
    protected double baseBulletSpeed;

    // ========== 视觉资源 ==========
    protected Image tankImage;
    protected String imagePath;

    // ========== 无敌系统 ==========
    private boolean isInvincible = false;
    private long invincibleEndTime = 0;
    private boolean isVisible = true; // 用于无敌闪烁

    public enum TankType {
        PLAYER_GREEN,
        ENEMY_HEAVY,
        ENEMY_FAST,
        ENEMY_NORMAL
    }

    // ========== 构造函数 ==========
    public Tank(double x, double y, TankType type,
                double speed, double rotationSpeed,
                int health, int fireCooldown,
                int bulletDamage, double bulletSpeed,
                int scoreValue) {
        super(x, y, GameConfig.TANK_SIZE, GameConfig.TANK_SIZE);
        this.type = type;
        this.speed = speed;
        this.rotationSpeed = rotationSpeed;
        this.health = health;
        this.maxHealth = health;
        this.fireCooldown = fireCooldown;
        this.bulletDamage = bulletDamage;
        this.bulletSpeed = bulletSpeed;
        this.scoreValue = scoreValue;
        // 2. 【新增】存档基准属性 (备份一份出厂设置)
        this.baseFireCooldown = fireCooldown;
        this.baseBulletDamage = bulletDamage;
        this.baseBulletSpeed = bulletSpeed;

        setInitialRotation();
        loadImage();
    }

    private void setInitialRotation() {
        if (type == TankType.PLAYER_GREEN) {
            logicRotation = 0.0;
        } else {
            logicRotation = 180.0;
        }
        displayRotation = logicRotation;
    }

    protected abstract void loadImage();
    public abstract String getColorDescription();

    // ========== 角度系统 ==========
    public double normalizeAngle(double angle) {
        angle %= 360;
        if (angle < 0) angle += 360;
        return angle;
    }

    private void smoothRotation() {
        double diff = logicRotation - displayRotation;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        displayRotation += diff * smoothFactor;
        displayRotation = normalizeAngle(displayRotation);
    }

    // ========== 无敌逻辑 ==========
    public void activateShield(double seconds) {
        this.isInvincible = true;
        this.invincibleEndTime = System.currentTimeMillis() + (long)(seconds * 1000);
        // System.out.println(type + " 开启了无敌护盾！");
    }

    protected void updateShieldStatus() {
        if (isInvincible) {
            long now = System.currentTimeMillis();
            if (now > invincibleEndTime) {
                isInvincible = false;
                isVisible = true;
            } else {
                // 每 100ms 闪烁一次
                isVisible = (now / 100) % 2 == 0;
            }
        }
    }

    // ========== 核心更新 ==========
    public void update(Tile[][] map) {
        updateShieldStatus();
        handleRotation();
        smoothRotation();
        handleMovement();
        handleMapCollision(map);
        x += vx;
        y += vy;
        checkBounds();
    }

    // ========== 受伤逻辑 (含无敌判断) ==========
    public void takeDamage(int damage) {
        // 如果无敌，免疫伤害
        if (isInvincible) {
            return;
        }

        health -= damage;
        if (health <= 0) {
            alive = false;
            health = 0;
        }
    }

    // ========== 绘制方法 (已修复 StackOverflowError) ==========
    public void draw(GraphicsContext gc) {
        if (!alive) return;

        // 1. 处理无敌闪烁 (不可见周期变为半透明)
        if (isInvincible && !isVisible) {
            gc.setGlobalAlpha(0.4);
        }

        // 2. 绘制坦克本体 (关键修改：这里直接画，不要调用 draw(gc) !)
        if (tankImage == null) {
            drawFallback(gc);
        } else {
            gc.save();
            gc.translate(x + width / 2, y + height / 2);
            gc.rotate(displayRotation);
            gc.drawImage(tankImage, -width / 2, -height / 2, width, height);
            gc.restore();
        }

        // 3. 恢复透明度
        if (isInvincible) {
            gc.setGlobalAlpha(1.0);

            // 绘制金色护盾圈
            gc.save();
            gc.setStroke(Color.GOLD);
            gc.setLineWidth(3);
            gc.strokeOval(x - 5, y - 5, width + 10, height + 10);
            gc.restore();
        }

        // 4. 敌人血条
        if (type != TankType.PLAYER_GREEN) {
            drawHealthBar(gc);
        }
    }

    // ========== 地图碰撞 ==========
    protected void handleMapCollision(Tile[][] map) {
        if (vx == 0 && vy == 0) return;

        double nextX = x + vx;
        double nextY = y + vy;

        if (isCollidingWithMap(nextX, y, map)) vx = 0;
        if (isCollidingWithMap(x, nextY, map)) vy = 0;
    }

    private boolean isCollidingWithMap(double targetX, double targetY, Tile[][] map) {
        double margin = 2.0;
        double left = targetX + margin;
        double right = targetX + width - margin;
        double top = targetY + margin;
        double bottom = targetY + height - margin;

        return isSolidTile(left, top, map) ||
                isSolidTile(right, top, map) ||
                isSolidTile(left, bottom, map) ||
                isSolidTile(right, bottom, map);
    }

    private boolean isSolidTile(double px, double py, Tile[][] map) {
        int col = (int) (px / GameConfig.GRID_SIZE);
        int row = (int) (py / GameConfig.GRID_SIZE);

        if (row < 0 || row >= GameConfig.MAP_ROWS || col < 0 || col >= GameConfig.MAP_COLS) {
            return true;
        }
        Tile tile = map[row][col];
        return !tile.getType().isTankPassable();
    }

    // ========== 移动逻辑 ==========
    private void handleMovement() {
        vx = 0; vy = 0;
        if (!movingForward && !movingBackward) return;

        double radians = Math.toRadians(displayRotation);
        double dirX = Math.sin(radians);
        double dirY = -Math.cos(radians);

        if (movingForward) {
            vx = dirX * speed;
            vy = dirY * speed;
        } else if (movingBackward) {
            vx = -dirX * speed;
            vy = -dirY * speed;
        }
    }

    private void handleRotation() {
        if (rotatingLeft) logicRotation -= rotationSpeed;
        if (rotatingRight) logicRotation += rotationSpeed;
        logicRotation = normalizeAngle(logicRotation);
    }

    private void checkBounds() {
        if (x < 0) x = 0;
        if (x + width > GameConfig.SCREEN_WIDTH) x = GameConfig.SCREEN_WIDTH - width;
        if (y < 0) y = 0;
        if (y + height > GameConfig.SCREEN_HEIGHT) y = GameConfig.SCREEN_HEIGHT - height;
    }

    // ========== 射击系统 (已修复贴墙穿模) ==========

    /**
     * 尝试开火 (需要传入地图进行安全检查)
     */
    public Bullet tryFire(Tile[][] map) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFireTime >= fireCooldown) {
            Bullet b = fire(map);
            if (b != null) { // 只有成功生成子弹才重置冷却
                lastFireTime = currentTime;
                return b;
            }
        }
        return null;
    }

    private Bullet fire(Tile[][] map) {
        double radians = Math.toRadians(displayRotation);

        // 缩短一点检测距离，防止误判
        double offset = (width / 2) + 2.0;
        double muzzleX = getCenterX() + Math.sin(radians) * offset;
        double muzzleY = getCenterY() + -Math.cos(radians) * offset;

        // 【关键修复】检查枪口是否被堵住
        if (isMuzzleBlocked(muzzleX, muzzleY, map)) {
            return null; // 堵住了，不开火
        }

        double bulletVx = Math.sin(radians) * bulletSpeed;
        double bulletVy = -Math.cos(radians) * bulletSpeed;
        boolean isEnemyTank = (this.type != TankType.PLAYER_GREEN);

        return new Bullet(
                isEnemyTank, bulletDamage, (int)displayRotation,
                bulletVx, bulletVy, muzzleX, muzzleY,
                GameConfig.BULLET_RADIUS * 2, GameConfig.BULLET_RADIUS * 2
        );
    }

    // 检查枪口坐标是否在障碍物内
    private boolean isMuzzleBlocked(double x, double y, Tile[][] map) {
        if (map == null) return false;
        int c = (int) (x / GameConfig.GRID_SIZE);
        int r = (int) (y / GameConfig.GRID_SIZE);

        if (r < 0 || r >= GameConfig.MAP_ROWS || c < 0 || c >= GameConfig.MAP_COLS) return true;

        Tile t = map[r][c];
        // 墙和铁块会堵住枪口
        return t != null && (t.getType() == TileType.STONE || t.getType() == TileType.BRICK);
    }

    /**
     * 临时改变射速 (数值越小射速越快)
     * @param newCooldown 新的冷却时间 (ms)
     */
    public void buffFireRate(int newCooldown) {
        this.fireCooldown = newCooldown;
        System.out.println("🔥 射速已提升！当前冷却: " + this.fireCooldown + "ms");
    }

    /**
     * 临时改变伤害
     * @param newDamage 新的伤害值
     */
    public void buffDamage(int newDamage) {
        this.bulletDamage = newDamage;
        System.out.println("💪 伤害已提升！当前伤害: " + this.bulletDamage);
    }

    /**
     * 道具效果结束，恢复所有属性到出厂设置
     */
    public void resetStats() {
        this.fireCooldown = this.baseFireCooldown;
        this.bulletDamage = this.baseBulletDamage;
        this.bulletSpeed = this.baseBulletSpeed;
        // System.out.println("⚡ 道具效果结束，属性已恢复。");
    }

    // ========== 辅助绘制 ==========
    private void drawFallback(GraphicsContext gc) {
        gc.save();
        gc.translate(x + width / 2, y + height / 2);
        gc.rotate(displayRotation);

        Color tankColor;
        switch (type) {
            case PLAYER_GREEN: tankColor = Color.LIMEGREEN; break;
            case ENEMY_HEAVY: tankColor = Color.DODGERBLUE; break;
            case ENEMY_FAST: tankColor = Color.PURPLE; break;
            case ENEMY_NORMAL: tankColor = Color.GOLD; break;
            default: tankColor = Color.GRAY;
        }

        gc.setFill(tankColor);
        gc.fillRect(-width / 2, -height / 2, width, height);
        gc.setFill(Color.BLACK); // 炮管
        gc.fillRect(-2, -height / 2 - 10, 4, 15);
        gc.restore();
    }

    private void drawHealthBar(GraphicsContext gc) {
        double barWidth = 30;
        double barHeight = 4;
        double barX = x + (width - barWidth) / 2;
        double barY = y - 10;
        double healthRatio = (double) health / maxHealth;
        Color barColor = healthRatio > 0.6 ? Color.LIMEGREEN : healthRatio > 0.3 ? Color.YELLOW : Color.RED;

        gc.setFill(Color.rgb(100, 0, 0, 0.7));
        gc.fillRect(barX, barY, barWidth, barHeight);
        gc.setFill(barColor);
        gc.fillRect(barX, barY, barWidth * healthRatio, barHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }

    // ========== Getters/Setters ==========
    public boolean containsPoint(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    public double getCenterX() { return x + width / 2; }
    public double getCenterY() { return y + height / 2; }
    public void setMovingForward(boolean moving) { this.movingForward = moving; }
    public void setMovingBackward(boolean moving) { this.movingBackward = moving; }
    public void setRotatingLeft(boolean rotating) { this.rotatingLeft = rotating; }
    public void setRotatingRight(boolean rotating) { this.rotatingRight = rotating; }

    public void stopAllMovement() {
        movingForward = false; movingBackward = false;
        rotatingLeft = false; rotatingRight = false;
        vx = 0; vy = 0;
    }
    public void setSmoothFactor(double factor) { this.smoothFactor = Math.max(0.05, Math.min(0.5, factor)); }
    public void setHealth(int health) {
        this.health = Math.min(health, maxHealth);
        if (this.health > 0) alive = true;
    }
    public void heal(int amount) {
        health += amount;
        if (health > maxHealth) health = maxHealth;
    }
    public void resetHealth(){ this.health=maxHealth; }
    public boolean isAlive() { return alive && health > 0; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getDisplayRotation() { return displayRotation; }
    public TankType getType() { return type; }
    public int getHealth() { return health; }
    public int getScoreValue() { return scoreValue; }
    public int getBulletDamage() { return bulletDamage; }
    public double getBulletSpeed() { return bulletSpeed; }
    public boolean isPlayer() { return type == TankType.PLAYER_GREEN; }
    public void setX(double x){ this.x=x; }
    public void setY(double y){ this.y=y; }
    public double getLogicRotation() { return logicRotation; }
    public void setLogicRotation(double angle) { this.logicRotation = normalizeAngle(angle); }
    public void setDisplayRotation(double angle) { this.displayRotation = normalizeAngle(angle); }
    public int getMaxHealth() { return maxHealth; }
    public double getHealthPercentage() { return (double) health / maxHealth; }
    public boolean isMovingForward() { return movingForward; }
    public boolean isMovingBackward() { return movingBackward; }
    public boolean isRotatingLeft() { return rotatingLeft; }
    public boolean isRotatingRight() { return rotatingRight; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getRotationSpeed() { return rotationSpeed; }
    public void setRotationSpeed(double rotationSpeed) { this.rotationSpeed = rotationSpeed; }
    public void setVx(double vx) { this.vx = vx; }
    public void setVy(double vy) { this.vy = vy; }
    public DoubleProperty xProperty() { return xProperty; }
    public DoubleProperty yProperty() { return yProperty; }
    public void setImage(Image image) { this.tankImage = image; }
}