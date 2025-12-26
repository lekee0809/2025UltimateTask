package view;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import map.MapModel;
import model.Bullet;
import model.Tank;
import controller.InputHandler;
import infra.GameLoop;

import static infra.GameConfig.SCREEN_WIDTH;
import static infra.GameConfig.SCREEN_HEIGHT;

/**
 * 游戏战斗场景类
 * 功能：整合地图渲染、坦克/子弹绘制、输入监听、UI显示等核心游戏场景逻辑
 */
public abstract class BaseGameScene {
    // 核心场景属性
    protected Stage primaryStage;
    protected Scene scene;
    protected final double WIDTH = SCREEN_WIDTH;
    protected final double HEIGHT = SCREEN_HEIGHT;
    protected GameLoop gameLoop; // <--- 新增这行，用于控制循环启动/暂停
    // ========== 补充分层画布（地图/坦克/子弹） ==========
    // 地图层（底层）
    protected Canvas mapCanvas;
    protected GraphicsContext mapGc; // 原mapContext
    // 坦克层（中间层）
    protected Canvas tankCanvas;
    protected GraphicsContext tankGc; // 原tankContext
    // 子弹层（顶层）
    protected Canvas bulletCanvas;
    protected GraphicsContext bulletGc; // 原bulletContext

    protected StackPane gameRoot;
    protected SpritePainter spritePainter;
    protected InputHandler inputHandler;

    // 提示文本相关
    private Text tipText;
    private Animation currentTipAnimation;

    // 构造方法（初始化流程优化）
    public BaseGameScene(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.spritePainter = new SpritePainter();

        // 1. 初始化提示文本
        initTipText();
        // 2. 初始化分层UI（地图/坦克/子弹画布）
        initCommonUI();
        // 3. 初始化输入监听
        initCommonInput();
        // 4. 初始化模式专属逻辑
        initModeSpecificLogic();
        // 5. 创建并绑定场景
        createScene();
        // 6. 启动游戏主循环
        startGameLoop(); // <--- 新增这行

    }

    // ========== 完整初始化分层UI（地图+坦克+子弹画布） ==========
    private void initCommonUI() {
        gameRoot = new StackPane();
        gameRoot.setStyle("-fx-background-color: #000000;");

        // 1. 创建地图画布（底层）
        mapCanvas = new Canvas(WIDTH, HEIGHT);
        mapGc = mapCanvas.getGraphicsContext2D();

        // 2. 创建坦克画布（中间层）
        tankCanvas = new Canvas(WIDTH, HEIGHT);
        tankGc = tankCanvas.getGraphicsContext2D();

        // 3. 创建子弹画布（顶层）
        bulletCanvas = new Canvas(WIDTH, HEIGHT);
        bulletGc = bulletCanvas.getGraphicsContext2D();

        // 按层级添加：地图 → 坦克 → 子弹 → 提示文本（从上到下=顶层到底层）
        gameRoot.getChildren().addAll(mapCanvas, tankCanvas, bulletCanvas, tipText);
    }

    // ========== 实现通用clearCanvas方法（支持任意画布清空） ==========
    /**
     * 清空指定画布（通用重载方法，解决tankContext/bulletContext清空）
     * @param gc 要清空的画布上下文（地图/坦克/子弹）
     */
    protected void clearCanvas(GraphicsContext gc) {
        if (gc != null) {
            gc.save();
            // 透明清空（不遮挡下层画布）
            gc.clearRect(0, 0, WIDTH, HEIGHT);
            gc.restore();
        }
    }

    // ========== 你的分层清屏逻辑（可直接调用） ==========
    protected void clearAllLayers() {
        // 1. 地图层：用黑色填充（底层背景）
        GraphicsContext mapContext = mapGc; // 兼容你的变量名
        mapContext.setFill(Color.BLACK);
        mapContext.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. 坦克层/子弹层：透明清空（不遮挡下层）
        GraphicsContext tankContext = tankGc; // 兼容你的变量名
        GraphicsContext bulletContext = bulletGc; // 兼容你的变量名
        clearCanvas(tankContext);
        clearCanvas(bulletContext);
    }

    // ========== 原有功能（提示文本/重置/输入等，已优化） ==========
    private void initTipText() {
        tipText = new Text();
        tipText.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 36));
        tipText.setFill(Color.WHITE);
        tipText.setStroke(Color.RED);
        tipText.setStrokeWidth(2);
        tipText.setOpacity(0);
        StackPane.setAlignment(tipText, javafx.geometry.Pos.CENTER);
    }

    protected void showTipText(String text, double duration) {
        if (tipText == null) {
            initTipText();
        }
        tipText.setText(text);
        stopCurrentTipAnimation();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), tipText);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        if (duration > 0) {
            PauseTransition pause = new PauseTransition(Duration.seconds(duration));
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), tipText);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            fadeIn.setOnFinished(e -> pause.play());
            pause.setOnFinished(e -> fadeOut.play());
            currentTipAnimation = fadeIn;
        } else {
            currentTipAnimation = null;
        }
        fadeIn.play();
    }

    private void stopCurrentTipAnimation() {
        if (currentTipAnimation != null && currentTipAnimation.getStatus() == Animation.Status.RUNNING) {
            currentTipAnimation.stop();
            currentTipAnimation = null;
            tipText.setOpacity(0);
        }
    }

    // ========== 游戏主循环控制 ==========
    private void startGameLoop() {
        gameLoop = new GameLoop() {
            @Override
            public void onUpdate() {
                // 每秒执行 60 次的物理逻辑
                updateGameLogic();
            }

            @Override
            public void onRender() {
                // 跟随屏幕刷新率的绘图逻辑
                clearAllLayers(); // 先清空
                renderGameFrame(); // 再重绘
            }
        };
        gameLoop.start();
    }

    // 留给子类 (StageGameScene/TwoPlayerGameScene) 去具体实现
    protected abstract void updateGameLogic(); // 这里写坦克移动、碰撞检测
    protected abstract void renderGameFrame(); // 这里调用 drawMap, drawTank 等

    private void initCommonInput() {
        inputHandler = new InputHandler(this);
    }

    private void createScene() {
        scene = new Scene(gameRoot, WIDTH, HEIGHT);
        scene.setOnKeyPressed(inputHandler::handleKeyPressed);
        scene.setOnKeyReleased(inputHandler::handleKeyReleased);
    }

    protected void resetScene() {
        stopCurrentTipAnimation();
        // 重置时调用分层清屏
        clearAllLayers();
        clearDynamicElements();
        resetInputState();
        resetModeSpecificData();
        initModeSpecificLogic();
    }

    // 原有clearCanvas保留（兼容地图层清空）
    protected void clearCanvas() {
        if (mapGc != null) {
            mapGc.save();
            Color bgColor = Color.BLACK;
            try {
                bgColor = (Color) gameRoot.getBackground().getFills().get(0).getFill();
            } catch (Exception e) {}
            mapGc.setFill(bgColor);
            mapGc.fillRect(0, 0, WIDTH, HEIGHT);
            mapGc.restore();
        }
    }

    private void clearDynamicElements() {
        if (gameRoot != null && !gameRoot.getChildren().isEmpty()) {
            // 保留所有分层画布和提示文本
            gameRoot.getChildren().retainAll(mapCanvas, tankCanvas, bulletCanvas, tipText);
        }
    }

    private void resetInputState() {
        if (inputHandler != null) {
            inputHandler.resetKeyStates();
        }
    }

    // 抽象方法（子类实现）
    protected abstract void initModeSpecificLogic();
    protected abstract void resetModeSpecificData();

    // 通用工具方法
    protected void drawMap(MapModel mapModel) {
        // spritePainter.drawMap(mapGc, mapModel);
    }

    protected void drawTank(Tank tank) {
        // 绘制到坦克层画布
        // spritePainter.drawTank(tankGc, tank);
    }

    protected void drawBullet(Bullet bullet) {
        // 绘制到子弹层画布（示例）
        // spritePainter.drawBullet(bulletGc, bullet);
    }

    // Getter方法
    public Scene getScene() {
        return scene;
    }

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