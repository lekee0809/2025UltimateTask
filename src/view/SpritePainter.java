package view;

import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;
import model.Tank;

import static infra.GameConfig.*;

/**
 * 精灵绘制器：处理坦克/子弹的旋转、位置更新、视觉渲染
 */
public class SpritePainter {
        private view.ResourceManager resourceManager;

        public SpritePainter() {
            this.resourceManager = view.ResourceManager.getInstance();
        }

        /**
         * 创建坦克的 ImageView（初始化坦克外观）
         * @param tank 坦克实体
         * @return 带图片的 ImageView（可旋转）
         */
        public ImageView createTankView(Tank tank) {
            // 根据坦克类型加载对应图片（如蓝色玩家坦克、红色敌人坦克）
            ImageView tankView = new ImageView(
                    resourceManager.loadImage("images/tank_" + tank.getColor() + ".png")
            );
            // 设置坦克大小（适配地图格子）
            tankView.setFitWidth(TANK_SIZE);
            tankView.setFitHeight(TANK_SIZE);
            // 绑定坦克位置（实体数据变化时，UI 自动更新）
            tankView.xProperty().bind(tank.xProperty());
            tankView.yProperty().bind(tank.yProperty());
            return tankView;
        }


    /**
     * 坦克旋转工具方法（优化版）
     * @param tankView 坦克ImageView控件（不能为空）
     * @param angle 目标旋转角度（单位：度，0~360）
     */
    public void rotateTank(ImageView tankView, double angle) {
        // 1. 空值校验：避免空指针异常
        if (tankView == null) {
            System.err.println("旋转失败：坦克ImageView不能为空！");
            return;
        }

        // 2. 处理图片未加载完成的情况（宽高为0的场景）
        double tankWidth = tankView.getImage() == null ? 0 : tankView.getFitWidth();
        double tankHeight = tankView.getImage() == null ? 0 : tankView.getFitHeight();
        // 若宽高为0，使用图片原始尺寸（避免旋转中心偏移）
        if (tankWidth <= 0 && tankView.getImage() != null) {
            tankWidth = tankView.getImage().getWidth();
        }
        if (tankHeight <= 0 && tankView.getImage() != null) {
            tankHeight = tankView.getImage().getHeight();
        }
        // 3. 创建旋转变换，以坦克中心为旋转轴
        Rotate rotate = new Rotate();
        rotate.setPivotX(tankWidth / 2);       // 水平中心
        rotate.setPivotY(tankHeight / 2);      // 垂直中心
        rotate.setAngle(angle % 360);          // 角度取模，确保在0~360范围内（比如400度→40度）

        // 4. 应用旋转（清空原有变换，避免叠加偏移）
        tankView.getTransforms().clear();
        tankView.getTransforms().add(rotate);
    }

        /**
         * 创建子弹的 ImageView
         * @param bullet 子弹实体
         * @return 子弹的 ImageView
         */
        public ImageView createBulletView(model.Bullet bullet) {
            ImageView bulletView;
            if(bullet.isEnemy){
                bulletView = new ImageView(resourceManager.loadImage("images/EnemyTankBullet.png"));
            } else
            { bulletView = new ImageView(resourceManager.loadImage("images/MyTankBullet.png"));}
            bulletView.setFitWidth(BULLET_RADIUS);
            bulletView.setFitHeight(BULLET_SPEED);
            bulletView.xProperty().bind(bullet.xProperty());
            bulletView.yProperty().bind(bullet.yProperty());
            return bulletView;
        }
}