package view;

import game.AppLauncher;
import item.Item;
import item.ItemSpawner;
import item.ItemType;
import item.ParticleEffect;
import model.PlayerTank;
import ranking.PlayerRecord;
import ranking.RankingManager;
import view.SoundManager;
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
import infra.GameConfig; // 新增导入
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
// ========== 【新增】UI 和输入相关引用 ==========
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
// ===========================================


import java.util.ArrayList;
import java.util.List;

import static infra.GameConfig.SCREEN_WIDTH;
import static infra.GameConfig.SCREEN_HEIGHT;

/**
 * 游戏战斗场景类
 * 功能：整合地图渲染、坦克/子弹绘制、输入监听、UI显示等核心游戏场景逻辑
 * 
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


    protected VBox pauseMenu;          // 暂停菜单容器
    protected boolean isPaused = false; // 暂停状态标记


    // 提示文本相关
    private Text tipText;
    private Animation currentTipAnimation;
    // ===================== 新增道具管理属性 =====================
    protected ItemSpawner itemSpawner;               // 道具生成器
    protected List<ParticleEffect> particleEffects;  // 粒子特效列表

    private long pauseStartTime;
    // 构造方法（初始化流程优化）
    public BaseGameScene(Stage primaryStage) {
        GameConfig.setGamePaused(false);

        this.primaryStage = primaryStage;
        this.spritePainter = new SpritePainter();
        this.itemSpawner = new ItemSpawner();
        this.particleEffects = new ArrayList<>();

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
        // ========== 【新增】初始化暂停菜单 ==========
        createPauseMenu();
        // ============
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
                // ========== 【修改】暂停时不更新逻辑 ==========
                if (!isPaused) {
                    updateGameLogic();
                }
                // ==========================================
            }

            @Override
            public void onRender() {
                clearAllLayers();
                renderGameFrame();
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

        scene.setOnKeyPressed(e -> {
            // ========== 【新增】优先拦截 ESC 键 ==========
            if (e.getCode() == KeyCode.ESCAPE) {
                togglePause();
                return; // 拦截，不传给 InputHandler
            }

            // 如果暂停了或者游戏结束，禁止操作坦克
            if (isPaused || GameConfig.isGameOver()) {
                return;
            }
            // ==========================================

            inputHandler.handleKeyPressed(e);
        });

        scene.setOnKeyReleased(e -> {
            if (isPaused || GameConfig.isGameOver()) return;
            inputHandler.handleKeyReleased(e);
        });
    }

    // ========== 【新增/替换】暂停控制逻辑 ==========

    /**
     * 切换暂停/继续状态
     */
    protected void togglePause() {
        if (GameConfig.isGameOver()) return; // 游戏结束不能暂停

        if (isPaused) {
            resumeGameProcess();
        } else {
            pauseGameProcess();
        }
    }

    /**
     * 暂停：停止逻辑、显示菜单、暂停音乐
     */
    public void pauseGameProcess() {
        isPaused = true;
        GameConfig.setGamePaused(true);

        // 显示菜单 (添加到界面最上层)
        if (!gameRoot.getChildren().contains(pauseMenu)) {
            gameRoot.getChildren().add(pauseMenu);
        }

        // 暂停音乐
        SoundManager.getInstance().pauseBGM();
        SoundManager.getInstance().pauseGameMusic();

        showTipText("GAME PAUSED", 0);
    }

    /**
     * 恢复：恢复逻辑、移除菜单、恢复音乐
     */
    protected void resumeGameProcess() {
        isPaused = false;
        if (gameLoop != null && !GameConfig.isGameOver()) {
            gameLoop.start();
            GameConfig.setGamePaused(false);
        }

        // 移除菜单
        if (gameRoot.getChildren().contains(pauseMenu)) {
            gameRoot.getChildren().remove(pauseMenu);
        }

        // 【修复1】强制隐藏提示文字
        if (tipText != null) {
            tipText.setOpacity(0);
            tipText.setText("");
        }
        stopCurrentTipAnimation();

        SoundManager.getInstance().playBGM();
        SoundManager.getInstance().resumeGameMusic();
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

    // 1. 原有 writeGameFinalRecord 方法：修改为兼容PlayerRecord（核心修改）
    /**
     * 游戏结束时写入最终记录（兼容PlayerRecord对象，无缝衔接）
     * @param isWin 是否胜利
     * @param score 最终得分
     * @param survivalTime 存活时间（秒）
     * @param itemCount 拾取道具数量
     */
    protected void writeGameFinalRecord(boolean isWin, int score, long survivalTime, int itemCount) {
        // 1. 获取当前游戏模式（子类已实现抽象方法）
        PlayerRecord.GameMode gameMode = getCurrentGameMode();
        // 2. 核心：调用 RankingManager 添加记录（自动分文件存储+排序+截断）
        // 注意：survivalTime 是long类型，这里强转int（与 RankingManager 参数一致，若需long可修改 RankingManager）
        RankingManager.addRecord(score, (int) survivalTime, gameMode);

        // 可选：保留原有道具拾取记录（如需）
        String propRecord = String.format("拾取时间：%s, 道具类型：%s, 玩家位置：(%.2f, %.2f), 玩家血量：%d",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()),
                "通用道具",
                getPlayerTank().getX(),
                getPlayerTank().getY(),
                getPlayerTank().getHealth()
        );
        writeRecordToFile(propRecord, "game_prop_record.txt");}

    // 2. 新增：获取当前游戏模式（抽象方法，子类实现）
    protected abstract PlayerRecord.GameMode getCurrentGameMode();

    // 3. 原有 writeRecordToFile 方法：完全保留，不变（无需修改）
    /**
     * 通用记录写入文件工具方法（原有逻辑，直接复用）
     * @param content 要写入的记录内容
     * @param fileName 文件名
     */
    private void writeRecordToFile(String content, String fileName) {
        new Thread(() -> {
            java.io.BufferedWriter writer = null;
            try {
                java.io.File file = new java.io.File(fileName);
                if (!file.exists()) {
                    file.createNewFile();
                }
                writer = new java.io.BufferedWriter(
                        new java.io.OutputStreamWriter(
                                new java.io.FileOutputStream(file, true),
                                "UTF-8"
                        )
                );
                writer.write(content);
                writer.newLine();
                writer.flush();
            } catch (java.io.IOException e) {
                System.err.println("记录写入文件失败：" + e.getMessage());
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // 4. 新增：PlayerRecord对象专用写入方法（推荐，便于排行榜解析）
    /**
     * 写入PlayerRecord对象到文件（异步、追加、UTF-8编码）
     * @param playerRecord 玩家记录对象
     * @param fileName 文件名
     */
    protected void writePlayerRecordToFile(PlayerRecord playerRecord, String fileName) {
        if (playerRecord == null) {
            System.err.println("玩家记录对象为null，无法写入");
            return;
        }
        // 复用原有异步写入逻辑，直接写入对象的toString（兼容格式）
        writeRecordToFile(playerRecord.toString(), fileName);
    }


    // ========== 新增：获取主舞台（给设置窗口用） ==========
    public Stage getPrimaryStage() {
        return primaryStage;
    }



    /**
     * 父类统一更新方法：处理道具拾取和特效逻辑
     */
    // 5. 原有道具拾取记录：修改为兼容PlayerRecord（可选，优化）
    protected void updateBaseElements() {
        PlayerTank player = getPlayerTank();
        if (player == null) return;

        itemSpawner.update(player);

        for (Item item : itemSpawner.getCollectedItems()) {
            particleEffects.add(new ParticleEffect(
                    item.getX() + item.getWidth()/2,
                    item.getY() + item.getHeight()/2,
                    15, Color.GOLD, 0.5f
            ));

            // 可选：道具拾取记录也使用PlayerRecord（或保留原有字符串，这里演示兼容）
            String propRecord = String.format("拾取时间：%s, 道具类型：%s, 玩家位置：(%.2f, %.2f), 玩家血量：%d",
                    new PlayerRecord(0,0,PlayerRecord.GameMode.SINGLE_CHALLENGE).getFinishTimeStr(),
                    item.getType().name(),
                    player.getX(),
                    player.getY(),
                    player.getHealth()
            );
            writeRecordToFile(propRecord, "game_prop_record.txt");

            if (item.getType() == ItemType.BOMB) {
                handleBombEffect(item);
            }
        }

        particleEffects.removeIf(ParticleEffect::isFinished);
        for (ParticleEffect effect : particleEffects) {
            effect.update(0.016f);
        }
    }
    /**
     * 父类统一渲染方法
     */
    protected void renderBaseElements() {
        // 绘制道具到坦克层
        for (Item item : itemSpawner.getActiveItems()) {
            spritePainter.drawItem(tankGc, item);
        }

        // 绘制特效到子弹层（最顶层）
        for (ParticleEffect effect : particleEffects) {
            spritePainter.drawParticleEffect(bulletGc, effect);
        }
    }

    // 子类必须实现：返回当前的玩家坦克
    protected abstract PlayerTank getPlayerTank();

    // 子类选择实现：处理炸弹爆炸对敌人的伤害
    protected void handleBombEffect(Item item) {
        // 默认留空，由具体场景类重写来传入敌人列表
    }

    // 补充：修复 pause/resume 中 GameConfig 状态同步
    /**
     * 【升级版】创建带设置功能的暂停菜单
     */
    private void createPauseMenu() {
        pauseMenu = new VBox(25); // 增加组件间距
        pauseMenu.setAlignment(Pos.CENTER);
        // 背景色加深一点，更清晰
        pauseMenu.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-padding: 40;");

        // 1. 标题
        Label title = new Label("PAUSE & SETTINGS");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 40));
        title.setTextFill(Color.WHITE);

        // ==================== 音量调节 ====================
        Label volLabel = new Label("音量 Volume");
        volLabel.setTextFill(Color.LIGHTGRAY);
        volLabel.setFont(Font.font(16));

        // 音量滑块 (0.0 到 1.0，默认 0.5)
        Slider volSlider = new Slider(0, 1, 0.5); // 假设默认音量是 0.5
        volSlider.setMaxWidth(300);
        // 【关键】禁止滑块获取焦点，防止按方向键时误触滑块
        volSlider.setFocusTraversable(false);

        // 监听滑块变化，实时修改音量
        volSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double vol = newVal.doubleValue();
            // 调用 SoundManager 调整 BGM 和 音效
            SoundManager.getInstance().setGlobalVolume(vol);
        });

        VBox volBox = new VBox(5, volLabel, volSlider);
        volBox.setAlignment(Pos.CENTER);

        // ==================== 全屏设置 ====================
        CheckBox fullScreenBox = new CheckBox("全屏模式 Fullscreen");
        fullScreenBox.setTextFill(Color.WHITE);
        fullScreenBox.setFont(Font.font(18));
        fullScreenBox.setFocusTraversable(false); // 禁止获取焦点

        // 初始化勾选状态
        fullScreenBox.setSelected(primaryStage.isFullScreen());

        // 勾选事件
        fullScreenBox.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            primaryStage.setFullScreen(isSelected);
        });

        // ==================== 游戏控制按钮 ====================
        String btnStyle = "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 200px; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 200px; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;";

        // 继续按钮
        Button btnResume = new Button("▶ 继续游戏 (Resume)");
        btnResume.setStyle(btnStyle);
        btnResume.setOnMouseEntered(e -> btnResume.setStyle(hoverStyle));
        btnResume.setOnMouseExited(e -> btnResume.setStyle(btnStyle));
        btnResume.setOnAction(e -> togglePause());
        btnResume.setFocusTraversable(false);

        // 重开按钮
        Button btnRestart = new Button("🔄 重新开始 (Restart)");
        btnRestart.setStyle(btnStyle);
        btnRestart.setOnMouseEntered(e -> btnRestart.setStyle(hoverStyle));
        btnRestart.setOnMouseExited(e -> btnRestart.setStyle(btnStyle));
        btnRestart.setOnAction(e -> {
            togglePause();
            resetScene();
            resumeGameProcess();
        });
        btnRestart.setFocusTraversable(false);

        // 退出按钮
        Button btnExit = new Button("🏠 返回主页 (Exit)");
        btnExit.setStyle(btnStyle);
        btnExit.setOnMouseEntered(e -> btnExit.setStyle(hoverStyle));
        btnExit.setOnMouseExited(e -> btnExit.setStyle(btnStyle));
        btnExit.setOnAction(e -> {
            // 【修复3】离开场景前，彻底停止当前游戏循环
            if (gameLoop != null) {
                gameLoop.stop();
            }
            // 重置暂停状态，防止污染下一次游戏
            GameConfig.setGamePaused(false);
            isPaused = false;

            SoundManager.getInstance().stopGameMusic();
            SoundManager.getInstance().playBackgroundMusic();

            AppLauncher mainMenu = new AppLauncher();
            try {
                mainMenu.start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btnExit.setFocusTraversable(false);

        // 添加所有组件到容器
        pauseMenu.getChildren().addAll(
                title,
                new Label(""), // 占位空行
                volBox,
                fullScreenBox,
                new Label(""), // 占位空行
                btnResume,
                btnRestart,
                btnExit
        );
    }



}