package game;

import javafx.application.Application;
import javafx.stage.Stage;
import view.StartScene;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("坦克大战");
        // 初始化开始场景并显示
        StartScene startScene = new StartScene(primaryStage);
        primaryStage.setScene(startScene.getScene());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}