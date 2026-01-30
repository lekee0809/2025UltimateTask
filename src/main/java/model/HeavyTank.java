package model;

import infra.GameConfig;
import javafx.scene.image.Image;

/**
 * 重型坦克 - 防御型AI
 */
public class HeavyTank extends EnemyTank {

    public HeavyTank(double x, double y) {
        super(x, y,
                Tank.TankType.ENEMY_HEAVY,
                GameConfig.TANK_SPEED * GameConfig.HEAVY_SPEED_MULTIPLIER,
                GameConfig.TANK_ROTATION_SPEED * GameConfig.HEAVY_ROTATION_MULTIPLIER,
                GameConfig.HEAVY_HEALTH,
                GameConfig.HEAVY_FIRE_COOLDOWN,
                GameConfig.HEAVY_BULLET_DAMAGE,
                GameConfig.BULLET_SPEED * GameConfig.HEAVY_BULLET_SPEED_MULTIPLIER,
                GameConfig.HEAVY_SCORE_VALUE);

        // 重型坦克AI参数
        this.sightRange = 350.0;       // 视野较短
        this.chaseRange = 250.0;
        this.attackRange = 300.0;      // 远程攻击
        this.attackAngleThreshold = 10.0; // 需要精确瞄准

        setSmoothFactor(0.1);
    }

    @Override
    protected void loadImage() {
        try {
            this.imagePath = "/images/tank_blue.png.png";
            this.tankImage = new Image(this.imagePath);

            if (tankImage.isError()) {
                System.err.println("❌ 重型坦克图片加载失败: " + imagePath);
                loadFallbackImage("blue");
            } else {
                System.out.println("✅ 重型坦克图片加载成功: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("⚠️ 重型坦克图片加载异常: " + e.getMessage());
            loadFallbackImage("blue");
        }
    }

    private void loadFallbackImage(String color) {
        String[] fallbackPaths = {
                "/images/tank_blue.png",
                "/images/" + color + "_tank.png",
                "/images/heavy_tank.png",
                "/images/enemy_tank.png",
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

        System.err.println("❌ 重型坦克所有图片尝试失败");
        this.tankImage = null;
    }

    @Override
    public String getColorDescription() {
        return "蓝色重型坦克";
    }

    @Override
    public String getAIType() {
        return "防御型AI - 高生命值，远程精准射击";
    }

    @Override
    public double getAIAggressiveness() {
        return 0.4; // 侵略性较低，偏防御
    }


}