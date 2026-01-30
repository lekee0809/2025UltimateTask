package game;

import infra.GameConfig;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import view.*;
import ranking.RankingSelectUI;

public class AppLauncher extends Application {

    private VBox menuRoot;

    @Override
    public void start(Stage primaryStage) {
        // 1. 核心容器（背景层 + 扫描线层 + UI层）
        StackPane rootContainer = new StackPane();

        // --- 背景设置 ---
        menuRoot = new VBox(25);
        menuRoot.setAlignment(Pos.CENTER);
        // 深灰蓝到黑色的渐变，非常有金属装甲感
        menuRoot.setStyle("-fx-background-color: linear-gradient(to bottom, #1e272e, #000000);");

        // --- 动态装饰：雷达扫描线 ---
        Rectangle scanLine = new Rectangle(GameConfig.SCREEN_WIDTH, 4, Color.web("#2ecc71", 0.15));
        addScannerAnimation(scanLine);

        // 2. 标题：保持你的原创名字 "FaZe LeKee's TANK WAR"
        Text title = new Text("FaZe LeKee's TANK WAR");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 48));
        title.setFill(Color.web("#fbc531")); // 战术亮黄色

        // 给标题添加外发光效果
        DropShadow titleGlow = new DropShadow(25, Color.web("#fbc531", 0.6));
        titleGlow.setSpread(0.2);
        title.setEffect(titleGlow);

        // 3. 军事风格按钮
        Button btnStageMode = createMenuButton("闯关模式 (Stage Mode)", "#27ae60");
        Button btnEndlessMode = createMenuButton("无尽模式 (Endless Mode)", "#e67e22");
        Button btnPvPMode = createMenuButton("双人对战 (2 Players)", "#2980b9");
        Button btnRanking = createMenuButton("查看排行榜 (Ranking)", "#f1c40f");

        // 【新增】设置按钮 (紫色代表科技/系统)
        Button btnSettings = createMenuButton("系统设置 (Settings)", "#8e44ad");

        Button btnExit = createMenuButton("退出游戏 (Exit)", "#c0392b");

        // 按钮逻辑
        btnStageMode.setOnAction(e -> switchScene(primaryStage, new StageGameScene(primaryStage).getScene()));
        btnEndlessMode.setOnAction(e -> switchScene(primaryStage, new EndlessGameScene(primaryStage).getScene()));
        btnPvPMode.setOnAction(e -> switchScene(primaryStage, new TwoPlayerGameScene(primaryStage).getScene()));
        btnRanking.setOnAction(e -> RankingSelectUI.showSelectWindow());

        // 【修复】点击设置按钮，调用 SettingsWindow.show(primaryStage)
        btnSettings.setOnAction(e -> SettingsWindow.show(primaryStage));

        btnExit.setOnAction(e -> System.exit(0));

        // 组装按钮容器
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().addAll(title, btnStageMode, btnEndlessMode, btnPvPMode, btnRanking, btnSettings, btnExit);

        // ========== 操作说明侧边栏（缩小版） ==========
        VBox controlPanel = createControlInstructionPanel();

        // 创建主容器，按钮区域居左，操作面板靠右
        HBox mainContent = new HBox();
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(20));
        // 设置间距，让操作面板靠右侧显示
        HBox.setHgrow(buttonContainer, Priority.ALWAYS);
        mainContent.getChildren().addAll(buttonContainer, controlPanel);

        menuRoot.getChildren().add(mainContent);
        // =========================================

        rootContainer.getChildren().addAll(menuRoot, scanLine);

        Scene menuScene = new Scene(rootContainer, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        primaryStage.setTitle("Tank War 2025");
        primaryStage.setScene(menuScene);
        primaryStage.show();

        // 播放背景音乐 - 如果 SoundManager 不存在，注释掉下面这行
        // SoundManager.getInstance().playBackgroundMusic();
    }

    // ========== 新增方法：创建缩小版操作说明面板 ==========
    private VBox createControlInstructionPanel() {
        // 主面板 - 缩小尺寸
        VBox panel = new VBox(8);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPrefSize(180, 320); // 大幅缩小尺寸
        panel.setPadding(new Insets(15));
        panel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.5); " +
                        "-fx-border-color: #fbc531; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 3; " +
                        "-fx-background-radius: 3;"
        );

        // 标题 - 缩小字体
        Text panelTitle = new Text("操作说明");
        panelTitle.setFont(Font.font("Impact", FontWeight.BOLD, 16));
        panelTitle.setFill(Color.web("#fbc531"));
        panelTitle.setWrappingWidth(150);
        panelTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 分割线 - 缩短
        Rectangle divider = new Rectangle(140, 1, Color.web("#fbc531", 0.6));

        // Player 1 说明 - 缩小字体
        VBox player1Box = createPlayerInstructionBox("玩家 1", "WASD", "J");

        // Player 2 说明 - 缩小字体，发射键改为K
        VBox player2Box = createPlayerInstructionBox("玩家 2", "↑↓←→", "K");

        // 通用说明 - 删除移动和发射相关内容
        VBox commonBox = createCommonInstructionBox();

        // 组装面板
        panel.getChildren().addAll(
                panelTitle,
                divider,
                player1Box,
                createSmallDivider(),
                player2Box,
                createSmallDivider(),
                commonBox
        );

        return panel;
    }

    // 创建缩小版玩家操作说明子面板
    private VBox createPlayerInstructionBox(String playerLabel, String moveKeys, String fireKey) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER_LEFT);

        // 玩家标签 - 更小字体
        Text playerText = new Text(playerLabel);
        playerText.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        playerText.setFill(Color.WHITE);

        // 移动按键 - 更小字体，简化显示
        Text moveText = new Text("移动: " + moveKeys);
        moveText.setFont(Font.font("Consolas", 11));
        moveText.setFill(Color.web("#2ecc71"));

        // 发射按键 - 更小字体，简化显示
        Text fireText = new Text("发射: " + fireKey);
        fireText.setFont(Font.font("Consolas", 11));
        fireText.setFill(Color.web("#e74c3c"));

        box.getChildren().addAll(playerText, moveText, fireText);
        return box;
    }

    // 创建通用说明面板（删除移动和发射）
    private VBox createCommonInstructionBox() {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER_LEFT);

        // 通用标签
        Text commonText = new Text("通用");
        commonText.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        commonText.setFill(Color.WHITE);

        // 仅保留ESC说明，删除移动和发射相关
        Text escText = new Text("ESC (暂停/设置)");
        escText.setFont(Font.font("Consolas", 11));
        escText.setFill(Color.web("#fbc531"));

        box.getChildren().addAll(commonText, escText);
        return box;
    }

    // 创建小型分割线 - 缩短
    private Rectangle createSmallDivider() {
        Rectangle divider = new Rectangle(140, 1, Color.web("#fbc531", 0.3));
        return divider;
    }
    // =========================================

    // 场景切换淡入淡出
    private void switchScene(Stage stage, Scene newScene) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), menuRoot);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            stage.setScene(newScene);
            menuRoot.setOpacity(1.0);
        });
        ft.play();
    }

    // 创建硬核金属感按钮
    private Button createMenuButton(String text, String accentColor) {
        Button btn = new Button(text);
        btn.setPrefSize(350, 60);
        btn.setFont(Font.font("Consolas", FontWeight.BOLD, 18));

        // 样式设计：左边厚色块，直角，半透明黑背景
        String baseStyle =
                "-fx-background-color: rgba(255, 255, 255, 0.05); " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: " + accentColor + "; " +
                        "-fx-border-width: 0 0 0 10; " + // 重点：左侧装甲条
                        "-fx-background-radius: 0; " +
                        "-fx-cursor: hand;";

        btn.setStyle(baseStyle);

        // 鼠标交互：悬停时震动反馈
        btn.setOnMouseEntered(e -> {
            btn.setStyle(baseStyle + "-fx-background-color: " + accentColor + "; -fx-text-fill: white;");
            shakeButton(btn);
        });

        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));

        return btn;
    }

    // 按钮震动动画
    private void shakeButton(Button btn) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), btn);
        tt.setFromX(0);
        tt.setToX(5);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }

    // 全屏扫描线动画
    private void addScannerAnimation(Rectangle line) {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(6), line);
        tt.setFromY(-GameConfig.SCREEN_HEIGHT / 2.0);
        tt.setToY(GameConfig.SCREEN_HEIGHT / 2.0);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setInterpolator(Interpolator.LINEAR);
        tt.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}