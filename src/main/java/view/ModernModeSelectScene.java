package view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import static infra.GameConfig.SCREEN_HEIGHT;
import static infra.GameConfig.SCREEN_WIDTH;

/**
 * 现代化游戏模式选择界面
 */
public class ModernModeSelectScene {
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

    public ModernModeSelectScene(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initModeUI(); // 初始化模式选择UI
    }

    /**
     * 初始化现代化模式选择UI布局
     */
    private void initModeUI() {
        // 原有UI初始化逻辑不变（标题、按钮、布局）
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setStyle(ThemeManager.PanelStyles.MAIN_MENU);

        Text title = new Text("🎮 选择游戏模式");
        title.setFont(ThemeManager.Fonts.SUBTITLE);
        title.setFill(ThemeManager.Colors.PRIMARY);
        
        // 添加阴影效果
        DropShadow shadow = new DropShadow();
        shadow.setRadius(15);
        shadow.setColor(Color.web("#00d4ff"));
        shadow.setOffsetX(3);
        shadow.setOffsetY(3);
        title.setEffect(shadow);

        // 闯关模式按钮（对接StageGameScene）
        Button stageModeBtn = new Button("🏰 闯关模式");
        stageModeBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY);
        stageModeBtn.setOnMouseEntered(e -> stageModeBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY_HOVER));
        stageModeBtn.setOnMouseExited(e -> stageModeBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY));
        stageModeBtn.setOnAction(e -> {
            // 直接创建闯关模式场景
            StageGameScene stageGameScene = new StageGameScene(primaryStage);
            primaryStage.setScene(stageGameScene.getScene());
        });

        // 双人对战模式按钮（对接TwoPlayerGameScene）
        Button twoPlayerModeBtn = new Button("⚔️ 双人对战");
        twoPlayerModeBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY);
        twoPlayerModeBtn.setOnMouseEntered(e -> twoPlayerModeBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY_HOVER));
        twoPlayerModeBtn.setOnMouseExited(e -> twoPlayerModeBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY));
        twoPlayerModeBtn.setOnAction(e -> {
            // 直接创建双人对战模式场景
            TwoPlayerGameScene twoPlayerGameScene = new TwoPlayerGameScene(primaryStage);
            primaryStage.setScene(twoPlayerGameScene.getScene());
        });

        // 返回按钮（不变）
        Button backBtn = new Button("🔙 返回主菜单");
        backBtn.setStyle(ThemeManager.ButtonStyles.DANGER);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(ThemeManager.ButtonStyles.DANGER_HOVER));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(ThemeManager.ButtonStyles.DANGER));
        backBtn.setOnAction(e -> {
            StartSceneOptimized startScene = new StartSceneOptimized(primaryStage);
            primaryStage.setScene(startScene.getScene());
        });

        VBox buttonContainer = new VBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().addAll(stageModeBtn, twoPlayerModeBtn, backBtn);

        root.getChildren().addAll(title, buttonContainer);
        scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    public Scene getScene() {
        return scene;
    }
}