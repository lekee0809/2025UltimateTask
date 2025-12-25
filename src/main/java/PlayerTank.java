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
        // 拼接路径：/images/tank_green.png
        this.imagePath = GameConfig.IMAGE_PATH + "tank_green.png";

        try {
            // 【关键修改】使用 getResourceAsStream
            // 这告诉 Java 去 classpath (resources 文件夹) 里找文件，而不是去硬盘绝对路径找
            var imageStream = getClass().getResourceAsStream(this.imagePath);

            if (imageStream != null) {
                this.tankImage = new Image(imageStream);
            } else {
                // 如果找不到图片，打印错误但不崩贵，让它显示备用的方块
                System.err.println("❌ 无法加载图片资源: " + this.imagePath);
                this.tankImage = null;
            }
        } catch (Exception e) {
            System.err.println("❌ 图片加载发生异常: " + e.getMessage());
            this.tankImage = null;
        }
    }

    @Override
    public String getColorDescription() {
        return "绿色玩家坦克";
    }
}