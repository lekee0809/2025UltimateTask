package model;

import infra.GameConfig;
import javafx.scene.image.Image;

public class PlayerTank extends Tank {

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
}