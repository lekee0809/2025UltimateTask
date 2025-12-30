package ranking;

import infra.GameConfig; // 导入GameConfig，获取屏幕宽高
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// 普通类，不继承Application
public class RankingSelectUI {
    // 显示选择界面（窗口大小为SCREEN_WIDTH, SCREEN_HEIGHT）
    public static void showSelectWindow() {
        Stage stage = new Stage();
        stage.setTitle("选择游戏排行榜");

        // 1. 创建3个模式选择按钮（保持原有样式，可按需调整大小）
        Button singleBtn = createRankingButton("单人闯关排行榜");
        Button doubleBtn = createRankingButton("双人对战排行榜");
        Button endlessBtn = createRankingButton("无尽模式排行榜");

        // 2. 按钮点击事件
        singleBtn.setOnAction(e -> RankingDisplay.showRankingWindow(PlayerRecord.GameMode.SINGLE_CHALLENGE));
        doubleBtn.setOnAction(e -> RankingDisplay.showRankingWindow(PlayerRecord.GameMode.DOUBLE_BATTLE));
        endlessBtn.setOnAction(e -> RankingDisplay.showRankingWindow(PlayerRecord.GameMode.ENDLESS_MODE));

        // 3. 布局（与主菜单风格一致，更美观）
        VBox root = new VBox(40, singleBtn, doubleBtn, endlessBtn); // 增大间距，适配大窗口
        root.setPadding(new Insets(60));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2c3e50;"); // 与主菜单背景统一

        // 4. 设置场景大小为 SCREEN_WIDTH, SCREEN_HEIGHT
        Scene scene = new Scene(root, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    // 封装按钮创建方法，保持样式统一且适配大窗口
    private static Button createRankingButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(300, 60); // 与主菜单按钮大小一致
        btn.setFont(new javafx.scene.text.Font("Consolas", 18));
        btn.setStyle(
                "-fx-background-color: #ecf0f1; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 10;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #bdc3c7; -fx-background-radius: 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;"));
        return btn;
    }
}