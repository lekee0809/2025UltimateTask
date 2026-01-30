package model;

import infra.GameConfig;
import javafx.scene.image.Image;
import model.Tank;
import model.Tile;

/**
 * 敌人紫色快速坦克
 */
public class FastTank extends EnemyTank {

    public FastTank(double x, double y) {
        super(x, y,
                Tank.TankType.ENEMY_FAST,
                GameConfig.TANK_SPEED * GameConfig.FAST_SPEED_MULTIPLIER,
                GameConfig.TANK_ROTATION_SPEED * GameConfig.FAST_ROTATION_MULTIPLIER,
                GameConfig.FAST_HEALTH,
                GameConfig.FAST_FIRE_COOLDOWN,
                GameConfig.FAST_BULLET_DAMAGE,
                GameConfig.BULLET_SPEED * GameConfig.FAST_BULLET_SPEED_MULTIPLIER,
                GameConfig.FAST_SCORE_VALUE);

        // 快速坦克AI参数
        this.sightRange = 450.0;       // 视野较远
        this.chaseRange = 350.0;
        this.attackRange = 200.0;      // 近距离攻击
        this.attackAngleThreshold = 25.0; // 允许较大角度差开火

        // 快速坦克旋转灵敏
        setSmoothFactor(0.3);
    }

    @Override
    protected void loadImage() {
        try {
            this.imagePath = "/images/tank_purple.png.png";
            this.tankImage = new Image(this.imagePath);
            if (tankImage.isError()) {
                System.err.println("快速坦克图片加载失败: " + imagePath);
                this.tankImage = null;
            }
        } catch (Exception e) {
            System.err.println("快速坦克图片加载异常: " + e.getMessage());
            this.tankImage = null;
        }
    }

    @Override
    public String getColorDescription() {
        return "紫色快速坦克";
    }

    @Override
    public String getAIType() {
        return "敏捷型AI - 高速移动，擅长侧翼攻击";
    }

    @Override
    public double getAIAggressiveness() {
        return 0.8; // 侵略性中等偏高
    }

    // 注意：这里只有一个参数！
    @Override
    protected void executeAttack(Tile[][] map) {
        super.executeAttack(map);  // 调用父类方法，只传一个参数

        // 快速坦克特有的侧翼移动
        if (targetPlayer != null && stateTimer % 2 < 1) {
            // 尝试绕到玩家侧面
            double sideAngle = calculateAngleToPlayer() + 90;
            rotateTowardsAngle(sideAngle);
        }
    }


}