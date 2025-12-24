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

        setSmoothFactor(0.25); // 玩家坦克旋转更灵敏
    }

    @Override
    protected void loadImage() {
        try {
            this.imagePath = GameConfig.IMAGE_PATH + "tank_green.png";
            this.tankImage = new Image(this.imagePath);
        } catch (Exception e) {
            System.err.println("玩家坦克图片加载失败");
        }
    }

    @Override
    public String getColorDescription() {
        return "绿色玩家坦克";
    }
}