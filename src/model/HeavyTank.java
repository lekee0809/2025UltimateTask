package model;

import infra.GameConfig;
import javafx.scene.image.Image;

public class HeavyTank extends Tank {

    public HeavyTank(double x, double y) {
        super(x, y,
                TankType.ENEMY_HEAVY,
                GameConfig.TANK_SPEED * GameConfig.HEAVY_SPEED_MULTIPLIER,
                GameConfig.TANK_ROTATION_SPEED * GameConfig.HEAVY_ROTATION_MULTIPLIER,
                GameConfig.HEAVY_HEALTH,
                GameConfig.HEAVY_FIRE_COOLDOWN,
                GameConfig.HEAVY_BULLET_DAMAGE,
                GameConfig.BULLET_SPEED * GameConfig.HEAVY_BULLET_SPEED_MULTIPLIER,
                GameConfig.HEAVY_SCORE_VALUE);

        setSmoothFactor(0.1);
    }

    @Override
    protected void loadImage() {
        try {
            // 注意：你的图片文件名是 tank_blue.png.png（双重.png）
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
                "/images/tank_blue.png",           // 去掉重复的.png
                "/images/" + color + "_tank.png",  // blue_tank.png
                "/images/heavy_tank.png",          // 重型坦克通用
                "/images/enemy_tank.png",          // 通用敌人坦克
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
}