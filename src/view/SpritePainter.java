package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import model.Bullet;
import model.MapModel;
import model.Tank;
import infra.GameConfig;


/**
 * 精灵绘制器：处理坦克/子弹的旋转、位置更新、视觉渲染
 */
public class SpritePainter {
    // 资源管理器单例（加载图片资源）
    private ResourceManager resourceManager;
    // 可选：存储当前活跃的坦克/子弹View（便于批量管理）
    private ImageView currentTankView;
    private ImageView currentBulletView;

    /**
     * 构造方法：初始化资源管理器
     */
    public SpritePainter() {
        // 单例模式获取资源管理器（确保全局唯一）
        this.resourceManager = ResourceManager.getInstance();
    }

    /**
     * 创建坦克的 ImageView（初始化坦克外观）
     * @param tank 坦克实体（包含颜色、位置、角度等数据）
     * @return 带图片+绑定位置的 ImageView（可直接添加到场景）
     */
    public ImageView createTankView(Tank tank) {
        // 1. 校验坦克实体非空
        if (tank == null) {
            System.err.println("创建坦克View失败：坦克实体不能为空！");
            return new ImageView(resourceManager.loadImage("images/default_tank.png"));
        }

        // 2. 根据坦克颜色加载对应图片（兼容大小写，避免文件名问题）
        String tankImagePath = "images/tank_" + tank.getColor().toLowerCase() + ".png";
        ImageView tankView = new ImageView(resourceManager.loadImage(tankImagePath));

        // 3. 设置坦克尺寸（适配地图格子，从配置类读取常量）
        tankView.setFitWidth(GameConfig.TANK_SIZE);
        tankView.setFitHeight(GameConfig.TANK_SIZE);
        tankView.setPreserveRatio(true); // 保持图片宽高比，避免拉伸

        // 4. 绑定坦克位置（实体数据变化时，UI自动同步）
        tankView.xProperty().bind(tank.xProperty());
        tankView.yProperty().bind(tank.yProperty());

        // 5. 初始化旋转（匹配坦克初始角度）
        rotateTank(tankView, tank.getAngle());

        // 6. 保存当前坦克View（便于后续操作）
        this.currentTankView = tankView;

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
        rotate.setAngle(angle % 360);          // 角度取模，确保在0~360范围内

        // 4. 应用旋转（清空原有变换，避免叠加偏移）
        tankView.getTransforms().clear();
        tankView.getTransforms().add(rotate);
    }

    /**
     * 创建子弹的 ImageView
     * @param bullet 子弹实体（包含所属方、位置、角度等数据）
     * @return 子弹的 ImageView（绑定位置+匹配角度）
     */
    public ImageView createBulletView(Bullet bullet) {
        // 1. 校验子弹实体非空
        if (bullet == null) {
            System.err.println("创建子弹View失败：子弹实体不能为空！");
            return new ImageView(resourceManager.loadImage("images/default_bullet.png"));
        }

        // 2. 根据子弹所属方加载对应图片
        String bulletImagePath = bullet.isEnemy()
                ? "images/EnemyTankBullet.png"
                : "images/MyTankBullet.png";
        ImageView bulletView = new ImageView(resourceManager.loadImage(bulletImagePath));

        // 3. 设置子弹尺寸（从配置类读取常量）
        bulletView.setFitWidth(GameConfig.BULLET_RADIUS);
        bulletView.setFitHeight(GameConfig.BULLET_SPEED);
        bulletView.setPreserveRatio(true);

        // 4. 绑定子弹位置（实体数据变化时，UI自动同步）
        bulletView.xProperty().bind(bullet.xProperty());
        bulletView.yProperty().bind(bullet.yProperty());


        // 6. 保存当前子弹View（便于后续操作）
        this.currentBulletView = bulletView;

        return bulletView;
    }


    /**
     * 绘制游戏地图（核心逻辑：遍历地图数组，绘制不同类型的格子）
     * @param gc 画布绘图上下文（从GameScene传入）
     * @param mapModel 地图数据模型（包含格子类型、尺寸等）
     */
    public void drawMap(GraphicsContext gc, MapModel mapModel) {
        // 1. 校验参数非空
        if (gc == null || mapModel == null) {
            System.err.println("绘制地图失败：绘图上下文/地图模型不能为空！");
            return;
        }

        // 2. 获取地图基础参数（从配置类/模型读取）
        int gridSize = GameConfig.GRID_SIZE; // 地图格子尺寸
        int[][] mapData = mapModel.getMapData(); // 地图数据数组（0=空地，1=墙壁，2=草地）

        // 3. 遍历地图数组，逐格绘制
        for (int row = 0; row < mapData.length; row++) {
            for (int col = 0; col < mapData[row].length; col++) {
                // 计算格子的坐标（行/列转像素坐标）
                double x = col * gridSize;
                double y = row * gridSize;

                // 根据格子类型设置颜色/图片
                switch (mapData[row][col]) {
                    case 0: // 空地：黑色背景
                        gc.setFill(Color.BLACK);
                        gc.fillRect(x, y, gridSize, gridSize);
                        break;
                    case 1: // 墙壁：灰色填充+边框
                        gc.setFill(Color.GRAY);
                        gc.fillRect(x, y, gridSize, gridSize);
                        gc.setStroke(Color.DARKGRAY);
                        gc.strokeRect(x, y, gridSize, gridSize);
                        break;
                    case 2: // 草地：绿色半透明
                        gc.setFill(Color.rgb(0, 200, 0, 0.7));
                        gc.fillRect(x, y, gridSize, gridSize);
                        break;
                    case 3: // 河流：蓝色半透明
                        gc.setFill(Color.rgb(0, 0, 200, 0.7));
                        gc.fillRect(x, y, gridSize, gridSize);
                        break;
                    default: // 未知类型：默认空地
                        gc.setFill(Color.BLACK);
                        gc.fillRect(x, y, gridSize, gridSize);
                        break;
                }
            }
        }
    }

    // ========== Getter方法（供外部类安全访问） ==========
    public ImageView getCurrentTankView() {
        return currentTankView;
    }

    public ImageView getCurrentBulletView() {
        return currentBulletView;
    }
}
