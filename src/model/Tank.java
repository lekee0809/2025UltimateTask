package model;

import infra.GameConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * 完全独立的坦克类，不继承Entity
 * 实现平滑旋转和完整坦克功能
 */
public abstract class Tank {
    // ========== 基础属性 ==========
    public double x, y;                 // 位置
    public double width, height;        // 尺寸
    public boolean alive = true;        // 存活状态

    // ========== 平滑旋转系统 ==========
    private double logicRotation = 0.0;     // 逻辑角度（0-360度）
    private double displayRotation = 0.0;   // 显示用的平滑角度
    private double rotationSpeed;           // 旋转速度（度/帧）
    private double smoothFactor = 0.15;     // 平滑系数（0.1-0.3）

    // ========== 移动系统 ==========
    private double vx, vy;              // 速度分量
    private double speed;               // 移动速度

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

    // ========== 视觉资源 ==========
    protected Image tankImage;
    protected String imagePath;

    // ========== 坦克类型 ==========
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
        this.x = x;
        this.y = y;
        this.width = GameConfig.TANK_SIZE;
        this.height = GameConfig.TANK_SIZE;
        this.type = type;
        this.speed = speed;
        this.rotationSpeed = rotationSpeed;
        this.health = health;
        this.maxHealth = health;
        this.fireCooldown = fireCooldown;
        this.bulletDamage = bulletDamage;
        this.bulletSpeed = bulletSpeed;
        this.scoreValue = scoreValue;

        // 初始化角度
        setInitialRotation();
        loadImage();
    }

    private void setInitialRotation() {
        if (type == TankType.PLAYER_GREEN) {
            logicRotation = 0.0;    // 玩家朝上
        } else {
            logicRotation = 180.0;  // 敌人朝下
        }
        displayRotation = logicRotation;
    }

    // ========== 抽象方法 ==========
    protected abstract void loadImage();
    public abstract String getColorDescription();

    // ========== 角度系统 ==========

    /**
     * 角度归一化（0-360度）
     */
    private double normalizeAngle(double angle) {
        angle %= 360;
        if (angle < 0) angle += 360;
        return angle;
    }

    /**
     * 平滑插值角度
     */
    private void smoothRotation() {
        // 计算角度差（考虑360度循环）
        double diff = logicRotation - displayRotation;

        // 选择最短路径
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;

        // 线性插值
        displayRotation += diff * smoothFactor;

        // 归一化
        displayRotation = normalizeAngle(displayRotation);
    }

    // ========== 核心更新逻辑 ==========

    /**
     * 更新坦克状态（每帧调用）
     */
    public void update() {
        // 1. 处理旋转
        handleRotation();

        // 2. 平滑角度插值
        smoothRotation();

        // 3. 处理移动
        handleMovement();

        // 4. 更新位置
        x += vx;
        y += vy;

        // 5. 边界检查
        checkBounds();
    }

    /**
     * 处理旋转输入
     */
    private void handleRotation() {
        if (rotatingLeft) {
            logicRotation -= rotationSpeed;
        }
        if (rotatingRight) {
            logicRotation += rotationSpeed;
        }
        logicRotation = normalizeAngle(logicRotation);
    }

    /**
     * 处理移动输入
     */
    private void handleMovement() {
        vx = 0;
        vy = 0;

        if (!movingForward && !movingBackward) {
            return;
        }

        // 使用平滑角度计算方向
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

    /**
     * 边界检查
     */
    private void checkBounds() {
        if (x < 0) x = 0;
        if (x + width > GameConfig.SCREEN_WIDTH)
            x = GameConfig.SCREEN_WIDTH - width;
        if (y < 0) y = 0;
        if (y + height > GameConfig.SCREEN_HEIGHT)
            y = GameConfig.SCREEN_HEIGHT - height;
    }

    // ========== 绘制方法 ==========

    /**
     * 绘制坦克
     */
    public void draw(GraphicsContext gc) {
        if (tankImage == null || !alive) {
            drawFallback(gc);
            return;
        }

        gc.save();
        gc.translate(x + width / 2, y + height / 2);
        gc.rotate(displayRotation); // 使用平滑角度
        gc.drawImage(tankImage, -width / 2, -height / 2, width, height);
        gc.restore();

        // 敌人显示生命条
        if (type != TankType.PLAYER_GREEN) {
            drawHealthBar(gc);
        }

        // 调试信息
        if (GameConfig.DEBUG_MODE) {
            drawDebugInfo(gc);
        }
    }

    /**
     * 后备绘制（无图片时）
     */
    private void drawFallback(GraphicsContext gc) {
        gc.save();
        gc.translate(x + width / 2, y + height / 2);
        gc.rotate(displayRotation);

        Color tankColor;
        switch (type) {
            case PLAYER_GREEN:
                tankColor = Color.LIMEGREEN;
                break;
            case ENEMY_HEAVY:
                tankColor = Color.DODGERBLUE;
                break;
            case ENEMY_FAST:
                tankColor = Color.PURPLE;
                break;
            case ENEMY_NORMAL:
                tankColor = Color.GOLD;
                break;
            default:
                tankColor = Color.GRAY;
        }

        gc.setFill(tankColor);
        gc.fillRect(-width / 2, -height / 2, width, height);

        // 炮管
        gc.setFill(Color.BLACK);
        gc.fillRect(-2, -height / 2 - 10, 4, 15);

        gc.restore();
    }

    /**
     * 绘制生命条
     */
    private void drawHealthBar(GraphicsContext gc) {
        double barWidth = 30;
        double barHeight = 4;
        double barX = x + (width - barWidth) / 2;
        double barY = y - 10;

        double healthRatio = (double) health / maxHealth;
        Color barColor = healthRatio > 0.6 ? Color.LIMEGREEN :
                healthRatio > 0.3 ? Color.YELLOW : Color.RED;

        gc.setFill(Color.rgb(100, 0, 0, 0.7));
        gc.fillRect(barX, barY, barWidth, barHeight);

        gc.setFill(barColor);
        gc.fillRect(barX, barY, barWidth * healthRatio, barHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }

    /**
     * 绘制调试信息
     */
    private void drawDebugInfo(GraphicsContext gc) {
        gc.setFill(Color.CYAN);
        gc.fillText(String.format("逻辑角度: %.1f°", logicRotation), x, y - 5);
        gc.fillText(String.format("显示角度: %.1f°", displayRotation), x, y - 20);
        gc.fillText(String.format("速度: (%.1f, %.1f)", vx, vy), x, y - 35);
    }

    // ========== 碰撞检测 ==========

    /**
     * 检查与另一个实体的碰撞
     */
    public boolean collidesWith(Tank other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y;
    }

    /**
     * 检查与点的碰撞
     */
    public boolean containsPoint(double px, double py) {
        return px >= x && px <= x + width &&
                py >= y && py <= y + height;
    }

    // ========== 射击系统 ==========

    /**
     * 尝试发射子弹
     */
    public Bullet tryFire() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastFireTime >= fireCooldown) {
            lastFireTime = currentTime;
            return fire();
        }

        return null;
    }

    /**
     * 发射子弹
     */
    private Bullet fire() {
        // 使用平滑角度计算炮口位置
        double radians = Math.toRadians(displayRotation);
        double muzzleX = getCenterX() + Math.sin(radians) * (width / 2);
        double muzzleY = getCenterY() + -Math.cos(radians) * (height / 2);

        return new Bullet(muzzleX, muzzleY, displayRotation, this, bulletSpeed, bulletDamage);
    }

    /**
     * 获取中心坐标
     */
    public double getCenterX() {
        return x + width / 2;
    }

    public double getCenterY() {
        return y + height / 2;
    }

    // ========== 控制方法 ==========

    public void setMovingForward(boolean moving) {
        this.movingForward = moving;
    }

    public void setMovingBackward(boolean moving) {
        this.movingBackward = moving;
    }

    public void setRotatingLeft(boolean rotating) {
        this.rotatingLeft = rotating;
    }

    public void setRotatingRight(boolean rotating) {
        this.rotatingRight = rotating;
    }

    public void stopAllMovement() {
        movingForward = false;
        movingBackward = false;
        rotatingLeft = false;
        rotatingRight = false;
        vx = 0;
        vy = 0;
    }

    /**
     * 设置旋转平滑度
     */
    public void setSmoothFactor(double factor) {
        this.smoothFactor = Math.max(0.05, Math.min(0.5, factor));
    }

    // ========== 生命值管理 ==========

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            alive = false;
            health = 0;
        }
    }

    public void setHealth(int health) {
        this.health = Math.min(health, maxHealth);
        if (this.health > 0) {
            alive = true;
        }
    }

    public void heal(int amount) {
        health += amount;
        if (health > maxHealth) {
            health = maxHealth;
        }
    }

    public boolean isAlive() {
        return alive && health > 0;
    }

    // ========== Getter方法 ==========

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

    public void setImage(Image image) {
        this.tankImage = image;
    }
}