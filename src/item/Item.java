
package item;

import infra.GameConfig;
import java.util.Random;
import model.Tank;
import model.PlayerTank;
import java.util.List;

/**
 * 道具类 - 带生存时间和动画效果
 */
public class Item {

    // ===================== 常量定义 =====================
    public static final long MAX_LIFETIME = 10000;    // 道具最长存在时间10秒
    public static final long SPAWN_ANIMATION_TIME = 500;   // 生成动画持续时间500ms
    public static final long EXPIRE_ANIMATION_TIME = 3000; // 消失动画持续时间3秒
    public static final long BLINK_START_TIME = 5000;     // 开始闪烁的时间（生成后5秒）

    // ===================== 道具属性 =====================
    private ItemType type;                    // 道具类型
    private double x, y;                      // 道具像素坐标
    private double width, height;             // 道具尺寸
    private boolean active;                   // 是否活跃（未被吃掉）
    private long spawnTime;                   // 生成时间

    // ===================== 动画相关属性 =====================
    private ItemAnimationState animationState; // 当前动画状态
    private float alpha;                       // 透明度（0.0-1.0）
    private float scale;                       // 缩放比例
    private boolean visible;                   // 是否可见（用于闪烁效果）
    private long lastBlinkTime;                // 上次闪烁时间
    private long blinkInterval;                // 闪烁间隔

    // ===================== 静态工具 =====================
    private static final Random random = new Random();

    /**
     * 构造函数
     */
    public Item(double x, double y, ItemType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.width = GameConfig.GRID_SIZE;
        this.height = GameConfig.GRID_SIZE;
        this.active = true;
        this.spawnTime = System.currentTimeMillis();

        // 初始化动画状态
        this.animationState = ItemAnimationState.SPAWNING;
        this.alpha = 0.0f;     // 初始完全透明
        this.scale = 0.5f;     // 初始缩放50%
        this.visible = true;
        this.lastBlinkTime = System.currentTimeMillis();
        this.blinkInterval = 500; // 初始闪烁间隔500ms
    }

    /**
     * 随机生成一个道具
     */
    public static Item createRandomItem(double x, double y) {
        double rand = random.nextDouble();
        ItemType type;

        // 调整概率分布
        if (rand < 0.3) {
            type = ItemType.HEAL;           // 30% 回血
        } else if (rand < 0.5) {
            type = ItemType.INVINCIBLE;     // 20% 无敌
        } else if (rand < 0.8) {
            type = ItemType.BOMB;           // 30% 炸弹
        } else {
            type = ItemType.BUFF;           // 20% 属性增强 (新增!)
        }

        return new Item(x, y, type);
    }

    /**
     * 更新道具的动画状态
     */
    public void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        long aliveTime = currentTime - spawnTime;

        // 1. 检查是否应该开始消失动画
        if (aliveTime > MAX_LIFETIME) {
            // 超过最大生存时间，标记为需要移除
            active = false;
            return;
        }

        // 2. 根据生存时间更新动画状态
        if (aliveTime < SPAWN_ANIMATION_TIME) {
            // 生成动画阶段
            animationState = ItemAnimationState.SPAWNING;
            float progress = (float)aliveTime / SPAWN_ANIMATION_TIME;
            alpha = progress;                    // 透明度从0到1
            scale = 0.5f + 0.5f * progress;      // 缩放从50%到100%
        } else if (aliveTime > MAX_LIFETIME - EXPIRE_ANIMATION_TIME) {
            // 消失动画阶段
            animationState = ItemAnimationState.EXPIRING;
            float progress = (float)(aliveTime - (MAX_LIFETIME - EXPIRE_ANIMATION_TIME)) / EXPIRE_ANIMATION_TIME;
            alpha = 1.0f - progress;             // 透明度从1到0
            scale = 1.0f - 0.5f * progress;      // 缩放从100%到50%
        } else if (aliveTime > BLINK_START_TIME) {
            // 闪烁警告阶段
            animationState = ItemAnimationState.IDLE;

            // 计算闪烁间隔（随着时间推移越来越快）
            long timeUntilExpire = MAX_LIFETIME - aliveTime;
            if (timeUntilExpire < 2000) {
                blinkInterval = 100;  // 最后2秒快速闪烁
            } else if (timeUntilExpire < 4000) {
                blinkInterval = 200;  // 最后2-4秒中速闪烁
            }

            // 更新闪烁状态
            if (currentTime - lastBlinkTime > blinkInterval) {
                visible = !visible;
                lastBlinkTime = currentTime;
            }
        } else {
            // 正常显示阶段
            animationState = ItemAnimationState.IDLE;
            alpha = 1.0f;
            scale = 1.0f;
            visible = true;
        }
    }

    /**
     * 检查道具是否过期
     */
    public boolean isExpired() {
        long aliveTime = System.currentTimeMillis() - spawnTime;
        return aliveTime > MAX_LIFETIME;
    }

    /**
     * 检查道具是否与坦克发生碰撞
     */
    public boolean checkCollision(Tank tank) {
        if (!active || isExpired() || !tank.isAlive()) return false;

        double tankLeft = tank.getX();
        double tankRight = tank.getX() + tank.getWidth();
        double tankTop = tank.getY();
        double tankBottom = tank.getY() + tank.getHeight();

        double itemLeft = x;
        double itemRight = x + width;
        double itemTop = y;
        double itemBottom = y + height;

        return tankLeft < itemRight && tankRight > itemLeft &&
                tankTop < itemBottom && tankBottom > itemTop;
    }



    /**
     * 应用道具效果到玩家坦克
     * 返回true表示道具被成功使用
     */
    /**
     * 应用道具效果到玩家坦克
     * 返回true表示道具被成功使用
     */
    public boolean applyEffect(PlayerTank player) {
        if (!active || isExpired()) return false;

        active = false;  // 标记道具已被拾取
        animationState = ItemAnimationState.COLLECTED; // 更新动画状态

        switch (type) {
            case HEAL:
                // ... (回血逻辑保持不变) ...
                int healAmount = 50;
                int newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healAmount);
                player.setHealth(newHealth);
                System.out.println("拾取回血道具，恢复" + healAmount + "点生命值");
                return true;

            case INVINCIBLE:
                // ================= 修改开始 =================
                // 原来的代码可能是：player.activateInvincibility(type.getDuration());

                // 【修改 1 & 2】：
                // 直接调用 activateShield 方法 (这个方法在 Tank 类里，包含了无敌逻辑 + 金圈特效 + 闪烁)
                // 传入 5.0 表示 5 秒
                player.activateShield(5.0);

                System.out.println("拾取无敌道具，获得 5 秒无敌护盾！");
                // ================= 修改结束 =================
                return true;

            case BOMB:
                // ... (炸弹逻辑保持不变) ...
                System.out.println("拾取炸弹道具");
                return true;

            default:
                return false;
        }
    }


    /**
     * 应用炸弹效果到所有敌人坦克
     */
    public void applyBombEffect(List<Tank> enemyTanks) {
        if (type != ItemType.BOMB || !active) return;

        active = false;
        animationState = ItemAnimationState.COLLECTED;

        System.out.println("💣 炸弹爆炸！所有敌方坦克受到50点伤害");

        // 遍历所有敌方坦克，造成50点伤害
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive()) {
                enemy.takeDamage(50);
                System.out.println("  敌方坦克受到炸弹伤害，剩余血量: " + enemy.getHealth());
            }
        }
    }

    // ===================== Getter方法 =====================

    public ItemType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean isActive() {
        return active && !isExpired();
    }

    public long getSpawnTime() {
        return spawnTime;
    }

    public long getMaxLifetime() {
        return MAX_LIFETIME;
    }

    // ===================== 动画相关Getter方法 =====================

    public ItemAnimationState getAnimationState() {
        return animationState;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getScale() {
        return scale;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * 获取道具生存时间（毫秒）
     */
    public long getAliveTime() {
        return System.currentTimeMillis() - spawnTime;
    }

    /**
     * 获取道具剩余时间（毫秒）
     */
    public long getRemainingTime() {
        return Math.max(0, MAX_LIFETIME - getAliveTime());
    }

    /**
     * 获取道具作用时间（毫秒）
     */
    public int getEffectDuration() {
        return type.getDuration();
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setSpawnTime(long spawnTime) {
        this.spawnTime = spawnTime;
    }

    public void setAnimationState(ItemAnimationState animationState) {
        this.animationState = animationState;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public long getLastBlinkTime() {
        return lastBlinkTime;
    }

    public void setLastBlinkTime(long lastBlinkTime) {
        this.lastBlinkTime = lastBlinkTime;
    }

    public long getBlinkInterval() {
        return blinkInterval;
    }

    public void setBlinkInterval(long blinkInterval) {
        this.blinkInterval = blinkInterval;
    }
}

