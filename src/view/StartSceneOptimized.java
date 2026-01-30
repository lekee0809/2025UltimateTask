package view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import static infra.GameConfig.SCREEN_HEIGHT;
import static infra.GameConfig.SCREEN_WIDTH;

/**
 * 优化版游戏开始界面：主菜单（开始游戏、排行榜、退出）
 */
public class StartSceneOptimized {
    private Stage primaryStage; // 主舞台（用于切换场景）
    private Scene scene;

    public StartSceneOptimized(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initUI(); // 初始化菜单 UI
    }

    /**
     * 初始化主菜单 UI 布局
     */
    private void initUI() {
        // 1. 根布局（垂直排列按钮）
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setStyle(ThemeManager.PanelStyles.MAIN_MENU);

        // ========== 添加背景图片 ==========
        try {
            // 加载背景图片（确保图片在 resources/images/ 目录下）
            Image backgroundImage = new Image(
                    getClass().getResourceAsStream("/images/start_bg.jpg")
            );

            // 创建BackgroundImage（控制填充方式）
            BackgroundImage bgImage = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,    // 不重复
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,     // 居中
                    new BackgroundSize(
                            BackgroundSize.AUTO,       // 宽度
                            BackgroundSize.AUTO,       // 高度
                            false,                     // 不包含边框
                            false,                     // 不包含边距
                            true,                      // 图片尽可能填充容器
                            true                       // 保持宽高比
                    )
            );

            // 设置背景
            root.setBackground(new Background(bgImage));

        } catch (Exception e) {
            System.err.println("背景图片加载失败，使用渐变背景: " + e.getMessage());
            // 如果失败，保持原来的深色背景
        }
        // ====================================

        // 2. 标题文字
        Text title = new Text("坦克大战");
        title.setFont(ThemeManager.Fonts.TITLE);
        title.setFill(ThemeManager.Colors.PRIMARY);
        
        // 添加阴影效果
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(Color.web("#00d4ff"));
        shadow.setOffsetX(5);
        shadow.setOffsetY(5);
        title.setEffect(shadow);

        // 3. 按钮样式（统一风格）- 使用新的主题样式
        // 开始游戏按钮
        Button startBtn = new Button("🎮 开始游戏");
        startBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY);
        // 添加悬停效果
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY_HOVER));
        startBtn.setOnMouseExited(e -> startBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY));

        startBtn.setOnAction(e -> {
            ModeSelectScene modeSelectScene = new ModeSelectScene(primaryStage);
            primaryStage.setScene(modeSelectScene.getScene());
        });

        // 排行榜按钮
        Button rankBtn = new Button("🏆 排行榜");
        rankBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY);
        rankBtn.setOnMouseEntered(e -> rankBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY_HOVER));
        rankBtn.setOnMouseExited(e -> rankBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY));

        rankBtn.setOnAction(e -> {
            RankingDisplay.showRankingWindow(ranking.PlayerRecord.GameMode.SINGLE_CHALLENGE);
        });

        // 设置按钮
        Button settingsBtn = new Button("⚙️ 设置");
        settingsBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY);
        settingsBtn.setOnMouseEntered(e -> settingsBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY_HOVER));
        settingsBtn.setOnMouseExited(e -> settingsBtn.setStyle(ThemeManager.ButtonStyles.PRIMARY));

        settingsBtn.setOnAction(e -> {
            SettingsWindow.show(primaryStage);
        });

        // 退出按钮
        Button exitBtn = new Button("🚪 退出游戏");
        exitBtn.setStyle(ThemeManager.ButtonStyles.DANGER);
        exitBtn.setOnMouseEntered(e -> exitBtn.setStyle(ThemeManager.ButtonStyles.DANGER_HOVER));
        exitBtn.setOnMouseExited(e -> exitBtn.setStyle(ThemeManager.ButtonStyles.DANGER));

        exitBtn.setOnAction(e -> primaryStage.close());

        // 7. 组装 UI
        VBox buttonContainer = new VBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().addAll(startBtn, rankBtn, settingsBtn, exitBtn);
        
        root.getChildren().addAll(title, buttonContainer);
        scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    // 获取场景对象（供 game/Main.java 调用）
    public Scene getScene() {
        return scene;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }
}