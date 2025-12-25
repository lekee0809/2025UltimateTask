package model;

import infra.GameConfig;
import javafx.scene.image.Image;

/**
 * 敌人黄色普通坦克
 */
public class NormalTank extends Tank {

    public NormalTank(double x, double y) {
        super(x, y,
                TankType.ENEMY_NORMAL,
                GameConfig.TANK_SPEED * GameConfig.NORMAL_SPEED_MULTIPLIER,
                GameConfig.TANK_ROTATION_SPEED * GameConfig.NORMAL_ROTATION_MULTIPLIER,
                GameConfig.NORMAL_HEALTH,
                GameConfig.NORMAL_FIRE_COOLDOWN,
                GameConfig.NORMAL_BULLET_DAMAGE,
                GameConfig.BULLET_SPEED,
                GameConfig.NORMAL_SCORE_VALUE);

        // 普通坦克中等灵敏度
        setRotationSmoothness(0.2);
    }

    @Override
    protected void loadImage() {
        try {
            this.imagePath = GameConfig.IMAGE_PATH + "tank_yellow.png";
            this.tankImage = new Image(this.imagePath);
            if (tankImage.isError()) {
                System.err.println("普通坦克图片加载失败: " + imagePath);
                this.tankImage = null;
            }
        } catch (Exception e) {
            System.err.println("普通坦克图片加载异常: " + e.getMessage());
            this.tankImage = null;
        }
    }

    @Override
    public String getColorDescription() {
        return "黄色普通坦克";
    }
}