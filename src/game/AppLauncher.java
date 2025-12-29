package game;

import javafx.application.Application;
import javafx.stage.Stage;
import view.StartScene;
import view.ModeSelectScene; // 如果StartScene会跳转到ModeSelectScene

public class AppLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. 直接创建并使用 StartScene（最简洁的方案）
        StartScene startScene = new StartScene(primaryStage);

        // 2. 设置舞台
        primaryStage.setTitle("坦克大战 2025");
        primaryStage.setScene(startScene.getScene());
        primaryStage.setResizable(false); // 可选：固定窗口大小
        primaryStage.show();

        // 3. 如果StartScene需要知道如何切换场景，可以通过构造函数传入回调
        // 或者保持现有设计，让StartScene自己创建ModeSelectScene
    }

    public static void main(String[] args) {
        launch(args);
    }
}