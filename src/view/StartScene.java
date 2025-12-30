package view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import static infra.GameConfig.SCREEN_HEIGHT;
import static infra.GameConfig.SCREEN_WIDTH;

/**
 * 游戏开始界面：主菜单（开始游戏、排行榜、退出）
 */
public class StartScene {
    private Stage primaryStage; // 主舞台（用于切换场景）
    private Scene scene;

    public StartScene(Stage primaryStage) {
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
        root.setStyle("-fx-background-color: #1a1a1a;"); // 深色背景

        // ========== 新增：添加背景图片 ==========
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
            System.err.println("背景图片加载失败，使用纯色背景: " + e.getMessage());
            // 如果失败，保持原来的深色背景
        }
        // ====================================

        // 2. 标题文字
        Text title = new Text("坦克大战");
        title.setFont(Font.font("微软雅黑", 48));
        title.setFill(Color.ORANGE);
        title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);"); // 添加阴影效果

        // 3. 按钮样式（统一风格）- 改进按钮样式
        String buttonStyle =
                "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-pref-width: 200px; " +
                        "-fx-pref-height: 50px; " +
                        "-fx-background-color: linear-gradient(to bottom, #444444, #222222); " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #FF9900; " +  // 橙色边框
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-cursor: hand;"; // 鼠标悬停显示手型光标

        // 按钮悬停效果
        String buttonHoverStyle =
                "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-pref-width: 200px; " +
                        "-fx-pref-height: 50px; " +
                        "-fx-background-color: linear-gradient(to bottom, #FF9900, #CC6600); " + // 悬停时变为橙色
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #FFFFFF; " +  // 白色边框
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-cursor: hand;";

        // 4. 开始游戏按钮
        Button startBtn = new Button("开始游戏");
        startBtn.setStyle(buttonStyle);
        // 添加悬停效果
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(buttonHoverStyle));
        startBtn.setOnMouseExited(e -> startBtn.setStyle(buttonStyle));

        startBtn.setOnAction(e -> {
            ModeSelectScene modeSelectScene = new ModeSelectScene(primaryStage);
            primaryStage.setScene(modeSelectScene.getScene());
        });

        // 5. 排行榜按钮
        Button rankBtn = new Button("排行榜");
        rankBtn.setStyle(buttonStyle);
        rankBtn.setOnMouseEntered(e -> rankBtn.setStyle(buttonHoverStyle));
        rankBtn.setOnMouseExited(e -> rankBtn.setStyle(buttonStyle));

        rankBtn.setOnAction(e -> {
            showRankUI();
        });

        // 6. 退出按钮
        Button exitBtn = new Button("退出游戏");
        exitBtn.setStyle(buttonStyle);
        exitBtn.setOnMouseEntered(e -> exitBtn.setStyle(buttonHoverStyle));
        exitBtn.setOnMouseExited(e -> exitBtn.setStyle(buttonStyle));

        exitBtn.setOnAction(e -> primaryStage.close());

        // 7. 组装 UI
        root.getChildren().addAll(title, startBtn, rankBtn, exitBtn);
        scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
    }
    /**
     * 显示排行榜 UI（简易版，后续可扩展）
     */
    private void showRankUI() {
        // 示例：弹出排行榜窗口（实际需对接 DBManager 读取数据）
        Text rankText = new Text("排行榜：\n1. 玩家A - 1000分\n2. 玩家B - 800分");
        rankText.setFont(Font.font("微软雅黑", 24));
        rankText.setFill(Color.WHITE);
        VBox rankRoot = new VBox(rankText);
        rankRoot.setAlignment(Pos.CENTER);
        rankRoot.setStyle("-fx-background-color: #1a1a1a;");
        Scene rankScene = new Scene(rankRoot, 400, 300);
        Stage rankStage = new Stage();
        rankStage.setTitle("排行榜");
        rankStage.setScene(rankScene);
        rankStage.show();
    }

    // 获取场景对象（供 game/Main.java 调用）
    public Scene getScene() {
        return scene;
    }
}