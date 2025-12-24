package view;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.MapModel;
import model.Tank;
import controller.InputHandler;
import static infra.GameConfig.SCREEN_WIDTH;
import static infra.GameConfig.SCREEN_HEIGHT;

/**
 * 游戏战斗场景类
 * 功能：整合地图渲染、坦克/子弹绘制、输入监听、UI显示等核心游戏场景逻辑
 */
public abstract class BaseGameScene {
    protected Stage primaryStage;
    protected Scene scene;
    protected final double WIDTH = SCREEN_WIDTH;
    protected final double HEIGHT = SCREEN_HEIGHT;
    protected Canvas mapCanvas;
    protected GraphicsContext mapGc;
    protected StackPane gameRoot;
    protected SpritePainter spritePainter;
    protected InputHandler inputHandler;

    public BaseGameScene(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.spritePainter = new SpritePainter();
        // 初始化通用UI（所有模式都需要的画布、布局）
        initCommonUI();
        // 初始化通用输入（基础按键监听）
        initCommonInput();
        // 初始化模式专属逻辑（子类实现）
        initModeSpecificLogic();
        // 创建场景并显示
        createScene();
    }

    /**
     * 初始化所有模式通用的UI（画布、根布局）
     */
    private void initCommonUI() {
        // 根布局（叠加地图、坦克、子弹）
        gameRoot = new StackPane();
        gameRoot.setStyle("-fx-background-color: #000000;");
        // 地图画布
        mapCanvas = new Canvas(WIDTH, HEIGHT);
        mapGc = mapCanvas.getGraphicsContext2D();
        gameRoot.getChildren().add(mapCanvas);
    }

    /**
     * 初始化所有模式通用的输入（基础事件绑定）
     */
    private void initCommonInput() {
        inputHandler = new InputHandler(this);
    }

    /**
     * 创建场景并设置到主舞台
     */
    private void createScene() {
        scene = new Scene(gameRoot, WIDTH, HEIGHT);
        // 绑定场景的键盘事件（所有模式都需要）
        scene.setOnKeyPressed(inputHandler::handleKeyPressed);
        scene.setOnKeyReleased(inputHandler::handleKeyReleased);
    }

    /**
     * 模式专属逻辑（由子类实现）
     * 如：加载专属地图、初始化坦克、绑定专属输入等
     */
    protected abstract void initModeSpecificLogic();

    // ========== 通用工具方法（子类可直接调用） ==========
    protected void drawMap(MapModel mapModel) {
        spritePainter.drawMap(mapGc, mapModel);
    }

    protected void drawTank(Tank tank) {
        gameRoot.getChildren().add(spritePainter.createTankView(tank));
    }

    // Getter方法（供外部获取场景）
    public Scene getScene() {
        return scene;
    }

    // 通用Getter（供InputHandler调用）
    public Scene getGameScene() {
        return scene;
    }

    public StackPane getGameRoot() {
        return gameRoot;
    }

    public double getWIDTH() {
        return WIDTH;
    }

    public double getHEIGHT() {
        return HEIGHT;
    }
}
