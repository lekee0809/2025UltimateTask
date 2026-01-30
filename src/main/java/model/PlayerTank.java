package model;

import infra.GameConfig;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.util.Timer;
import java.util.TimerTask;

public class PlayerTank extends Tank {

    // ===================== 新增属性 =====================
    private boolean invincible = false;      // 是否处于无敌状态
    private long invincibleEndTime = 0;      // 无敌结束的时间戳
    public PlayerTank(double x, double y) {
        super(x, y,
                TankType.PLAYER_GREEN,
                GameConfig.TANK_SPEED,
                GameConfig.TANK_ROTATION_SPEED,
                GameConfig.PLAYER_HEALTH,
                GameConfig.PLAYER_FIRE_COOLDOWN,
                GameConfig.PLAYER_BULLET_DAMAGE,
                GameConfig.BULLET_SPEED,
                0);

        setSmoothFactor(0.25);
    }

    @Override
    protected void loadImage() {
        try {
            // 注意：你的图片文件名是 tank_green.png.png（双重.png）
            this.imagePath = "/images/tank_green.png.png";
            this.tankImage = new Image(this.imagePath);

            if (tankImage.isError()) {
                System.err.println("❌ 玩家坦克图片加载失败，路径: " + imagePath);
                System.out.println("尝试加载备用资源...");
                loadFallbackImage();
            } else {
                System.out.println("✅ 玩家坦克图片加载成功: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("⚠️ 玩家坦克图片加载异常: " + e.getMessage());
            loadFallbackImage();
        }
    }

    /**
     * 加载备用图片（如果主图片失败）
     */
    private void loadFallbackImage() {
        String[] fallbackPaths = {
                "/images/tank_green.png",      // 尝试去掉重复的.png
                "/images/green_tank.png",      // 可能的其他命名
                "/images/player_tank.png",     // 通用玩家坦克
        };

        for (String path : fallbackPaths) {
            try {
                Image fallback = new Image(path);
                if (!fallback.isError()) {
                    this.tankImage = fallback;
                    this.imagePath = path;
                    System.out.println("✅ 使用备用图片: " + path);
                    return;
                }
            } catch (Exception e) {
                // 继续尝试下一个
            }
        }

        System.err.println("❌ 所有图片尝试失败，将使用图形绘制");
        this.tankImage = null;
    }

    @Override
    public String getColorDescription() {
        return "绿色玩家坦克";
    }

    /**
     * 激活无敌状态
     * @param duration 持续时间（毫秒）
     */
    public void activateInvincibility(int duration) {
        this.invincible = true;
        // 计算结束时间：当前时间 + 持续时间
        this.invincibleEndTime = System.currentTimeMillis() + duration;

        System.out.println("玩家开启无敌模式！持续: " + duration + "ms");

        // 使用定时器在时间到达后解除无敌
        // 也可以在 update() 方法中每帧检查时间，这里使用 Timer 比较省资源
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // 回到 JavaFX 主线程修改状态
                Platform.runLater(() -> {
                    if (System.currentTimeMillis() >= invincibleEndTime) {
                        invincible = false;
                        System.out.println("无敌状态结束");
                    }
                });
            }
        }, duration);
    }

    /**
     * 重写受伤方法：增加无敌判断
     */
    @Override
    public void takeDamage(int damage) {
        if (invincible) {
            System.out.println("玩家处于无敌状态，免除伤害！");
            return; // 直接返回，不扣血
        }
        super.takeDamage(damage); // 调用父类原本的扣血逻辑
    }

    // ===================== Getter =====================
    public boolean isInvincible() {
        return invincible;
    }

    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
    }

    public long getInvincibleEndTime() {
        return invincibleEndTime;
    }

    public void setInvincibleEndTime(long invincibleEndTime) {
        this.invincibleEndTime = invincibleEndTime;
    }
}