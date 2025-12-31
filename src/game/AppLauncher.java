package game;

import infra.GameConfig;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
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

        // 组装 UI (别忘了把 btnSettings 加进去)
        menuRoot.getChildren().addAll(title, btnStageMode, btnEndlessMode, btnPvPMode, btnRanking, btnSettings, btnExit);

        rootContainer.getChildren().addAll(menuRoot, scanLine);

        Scene menuScene = new Scene(rootContainer, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        primaryStage.setTitle("Tank War 2025");
        primaryStage.setScene(menuScene);
        primaryStage.show();

        // 播放背景音乐
        SoundManager.getInstance().playBackgroundMusic();
    }

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