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

        setSmoothFactor(0.1); // 重型坦克旋转较慢
    }

    @Override
    protected void loadImage() {
        try {
            this.imagePath = GameConfig.IMAGE_PATH + "tank_blue.png";
            this.tankImage = new Image(this.imagePath);
        } catch (Exception e) {
            System.err.println("重型坦克图片加载失败");
        }
    }

    @Override
    public String getColorDescription() {
        return "蓝色重型坦克";
    }
}