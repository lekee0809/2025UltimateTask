package game;

import javafx.application.Application;
import javafx.stage.Stage;
import view.StartSceneOptimized; // 使用优化版启动界面

/**
 * 优化版游戏启动器
 * 使用现代化UI组件
 */
public class OptimizedAppLauncher extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("坦克大战 - 优化版");
        
        // 使用优化版启动场景
        StartSceneOptimized startScene = new StartSceneOptimized(primaryStage);
        primaryStage.setScene(startScene.getScene());
        
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}