package view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import static infra.GameConfig.SCREEN_HEIGHT;
import static infra.GameConfig.SCREEN_WIDTH;

public class ModeSelectScene {
    private Stage primaryStage; // 主舞台（用于场景切换）
    private Scene scene;

    // 模式枚举（便于后续扩展，避免硬编码）
    public enum GameMode {
        STAGE_MODE("闯关模式"), // 闯关模式
        TWO_PLAYER_MODE("双人对战模式"); // 双人对战模式

        private final String modeName;
        GameMode(String modeName) {
            this.modeName = modeName;
        }
        public String getModeName() {
            return modeName;
        }
    }

    public ModeSelectScene(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initModeUI(); // 初始化模式选择UI
    }

    /**
     * 初始化模式选择UI布局（和开始界面风格统一）
     */
    private void initModeUI() {
        // 原有UI初始化逻辑不变（标题、按钮、布局）
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Text title = new Text("选择游戏模式");
        title.setFont(Font.font("微软雅黑", 40));
        title.setFill(Color.ORANGE);

        String buttonStyle = "-fx-font-size: 20; -fx-min-width: 200; -fx-background-color: #333; -fx-text-fill: white; -fx-border-color: orange; -fx-border-width: 2;";

        // 闯关模式按钮（对接StageGameScene）
        Button stageModeBtn = new Button(GameMode.STAGE_MODE.getModeName());
        stageModeBtn.setStyle(buttonStyle);
        stageModeBtn.setOnAction(e -> {
            // 直接创建闯关模式场景
            StageGameScene stageGameScene = new StageGameScene(primaryStage);
            primaryStage.setScene(stageGameScene.getScene());
        });

        // 双人对战模式按钮（对接TwoPlayerGameScene）
        Button twoPlayerModeBtn = new Button(GameMode.TWO_PLAYER_MODE.getModeName());
        twoPlayerModeBtn.setStyle(buttonStyle);
        twoPlayerModeBtn.setOnAction(e -> {
            // 直接创建双人对战模式场景
            TwoPlayerGameScene twoPlayerGameScene = new TwoPlayerGameScene(primaryStage);
            primaryStage.setScene(twoPlayerGameScene.getScene());
        });

        // 返回按钮（不变）
        Button backBtn = new Button("返回主菜单");
        backBtn.setStyle(buttonStyle.replace("-fx-font-size: 20;", "-fx-font-size: 16;"));
        backBtn.setOnAction(e -> {
            StartScene startScene = new StartScene(primaryStage);
            primaryStage.setScene(startScene.getScene());
        });

        root.getChildren().addAll(title, stageModeBtn, twoPlayerModeBtn, backBtn);
        scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    public Scene getScene() {
        return scene;
    }
}


