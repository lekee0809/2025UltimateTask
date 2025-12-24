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

        // 2. 标题文字
        Text title = new Text("坦克大战");
        title.setFont(Font.font("微软雅黑", 48));
        title.setFill(Color.ORANGE);

        // 3. 按钮样式（统一风格）
        String buttonStyle = "-fx-font-size: 20; -fx-width: 200; -fx-background-color: #333; -fx-text-fill: white; -fx-border-color: orange; -fx-border-width: 2;";

        // 4. 开始游戏按钮
        Button startBtn = new Button("开始游戏");
        startBtn.setStyle(buttonStyle);
        startBtn.setOnAction(e -> {
            // 切换到模式选择界面（替代直接跳游戏场景）
        ModeSelectScene modeSelectScene = new ModeSelectScene(primaryStage);
        primaryStage.setScene(modeSelectScene.getScene());
    });

        // 5. 排行榜按钮
        Button rankBtn = new Button("排行榜");
        rankBtn.setStyle(buttonStyle);
        rankBtn.setOnAction(e -> {
            // 后续对接 infra/DBManager.java 显示排行榜
            showRankUI();
        });

        // 6. 退出按钮
        Button exitBtn = new Button("退出游戏");
        exitBtn.setStyle(buttonStyle);
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