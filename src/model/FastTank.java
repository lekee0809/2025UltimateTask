package model;

import infra.GameConfig;
import javafx.scene.image.Image;

public class FastTank extends Tank {

    public FastTank(double x, double y) {
        super(x, y,
                TankType.ENEMY_FAST,
                GameConfig.TANK_SPEED * GameConfig.FAST_SPEED_MULTIPLIER,
                GameConfig.TANK_ROTATION_SPEED * GameConfig.FAST_ROTATION_MULTIPLIER,
                GameConfig.FAST_HEALTH,
                GameConfig.FAST_FIRE_COOLDOWN,
                GameConfig.FAST_BULLET_DAMAGE,
                GameConfig.BULLET_SPEED * GameConfig.FAST_BULLET_SPEED_MULTIPLIER,
                GameConfig.FAST_SCORE_VALUE);

        setSmoothFactor(0.3); // 快速坦克旋转灵敏
    }

    @Override
    protected void loadImage() {
        try {
            this.imagePath = GameConfig.IMAGE_PATH + "tank_purple.png";
            this.tankImage = new Image(this.imagePath);
        } catch (Exception e) {
            System.err.println("快速坦克图片加载失败");
        }
    }

    @Override
    public String getColorDescription() {
        return "紫色快速坦克";
    }
}