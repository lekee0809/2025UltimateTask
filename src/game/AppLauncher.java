package game;

import infra.GameConfig;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import view.StageGameScene;
import view.TwoPlayerGameScene;

public class AppLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. 创建主菜单布局
        VBox menuRoot = new VBox(20); // 垂直布局，间距20
        menuRoot.setAlignment(Pos.CENTER);
        menuRoot.setStyle("-fx-background-color: #2c3e50;"); // 深色背景

        // 2. 标题
        Label title = new Label("FaZe LeKee's TANK WAR");
        title.setFont(new Font("Impact", 40));
        title.setTextFill(Color.WHITE);

        // 3. 按钮：闯关模式
        Button btnStageMode = createMenuButton("闯关模式 (Stage Mode)");

        // 【核心修复】绑定点击事件 -> 切换到 StageGameScene
        btnStageMode.setOnAction(e -> {
            try {
                System.out.println("正在进入闯关模式...");
                // 创建闯关场景
                StageGameScene stageScene = new StageGameScene(primaryStage);
                // 获取构建好的 Scene 并设置给舞台
                primaryStage.setScene(stageScene.getScene());
            } catch (Exception ex) {
                ex.printStackTrace(); // 如果报错，打印在控制台
                System.err.println("进入闯关模式失败，请检查 MapModel 或 图片资源！");
            }
        });

        // 4. 按钮：双人对战
        Button btnPvPMode = createMenuButton("双人对战 (2 Players)");

        // 【核心修复】绑定点击事件 -> 切换到 TwoPlayerGameScene
        btnPvPMode.setOnAction(e -> {
            try {
                System.out.println("正在进入双人模式...");
                TwoPlayerGameScene pvpScene = new TwoPlayerGameScene(primaryStage);
                primaryStage.setScene(pvpScene.getScene());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 5. 按钮：退出
        Button btnExit = createMenuButton("退出游戏 (Exit)");
        btnExit.setOnAction(e -> System.exit(0));

        // 6. 组装并显示
        menuRoot.getChildren().addAll(title, btnStageMode, btnPvPMode, btnExit);

        // 初始窗口大小 (800x600 或根据你的 GameConfig)

// ...

// 使用 GameConfig 的常量，保持和游戏内画面大小一致
        Scene menuScene = new Scene(menuRoot, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        primaryStage.setTitle("Tank War 2025");
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    // 辅助方法：统一按钮样式
    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(300, 60);
        btn.setFont(new Font("Consolas", 18));
        // 简单的样式
        btn.setStyle(
                "-fx-background-color: #ecf0f1; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 10;"
        );
        // 鼠标悬停变色效果
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #bdc3c7; -fx-background-radius: 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;"));
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}