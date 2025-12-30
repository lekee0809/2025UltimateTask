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
import view.SoundManager;
import view.StageGameScene;
import view.TwoPlayerGameScene;
import view.EndlessGameScene;
// 导入排行榜选择界面（请确保该类的包名与你的项目一致，若不在默认包，需补充完整包名）
import ranking.RankingSelectUI;

public class AppLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. 创建主菜单布局
        VBox menuRoot = new VBox(20);
        menuRoot.setAlignment(Pos.CENTER);
        menuRoot.setStyle("-fx-background-color: #2c3e50;");

        // 2. 标题
        Label title = new Label("FaZe LeKee's TANK WAR");
        title.setFont(new Font("Impact", 40));
        title.setTextFill(Color.WHITE);

        // 3. 按钮：闯关模式
        Button btnStageMode = createMenuButton("闯关模式 (Stage Mode)");
        btnStageMode.setOnAction(e -> {
            try {
                System.out.println("正在进入闯关模式...");
                StageGameScene stageScene = new StageGameScene(primaryStage);
                primaryStage.setScene(stageScene.getScene());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ================= 【新增：无尽模式】 =================
        Button btnEndlessMode = createMenuButton("无尽模式 (Endless Mode)");
        btnEndlessMode.setOnAction(e -> {
            try {
                System.out.println("正在进入无尽模式...");
                EndlessGameScene endlessScene = new EndlessGameScene(primaryStage);
                primaryStage.setScene(endlessScene.getScene());
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("进入无尽模式失败，请检查 EndlessGameScene 类是否存在！");
            }
        });

        // 4. 按钮：双人对战
        Button btnPvPMode = createMenuButton("双人对战 (2 Players)");
        btnPvPMode.setOnAction(e -> {
            try {
                System.out.println("正在进入双人模式...");
                TwoPlayerGameScene pvpScene = new TwoPlayerGameScene(primaryStage);
                primaryStage.setScene(pvpScene.getScene());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ================= 【核心新增：查看排行榜按钮】 =================
        // 【核心修改：排行榜按钮点击事件】
        Button btnRanking = createMenuButton("查看排行榜 (Ranking)");
        btnRanking.setOnAction(e -> {
            try {
                System.out.println("正在打开排行榜选择界面...");
                // 直接调用普通类的静态方法，无需launch
                RankingSelectUI.showSelectWindow();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("打开排行榜失败，请检查 RankingSelectUI 类是否存在！");
            }
        });
        // 5. 按钮：退出
        Button btnExit = createMenuButton("退出游戏 (Exit)");
        btnExit.setOnAction(e -> System.exit(0));

        // 6. 组装并显示（注意：将 btnRanking 加入布局列表，位置可按需调整）
        menuRoot.getChildren().addAll(title, btnStageMode, btnEndlessMode, btnPvPMode, btnRanking, btnExit);

        Scene menuScene = new Scene(menuRoot, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        primaryStage.setTitle("Tank War 2025");
        primaryStage.setScene(menuScene);
        primaryStage.show();
        // 新增：主菜单启动时播放背景音乐
        SoundManager.getInstance().playBackgroundMusic(); // 或 playBGM()
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(300, 60);
        btn.setFont(new Font("Consolas", 18));
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

    public static void main(String[] args) {
        launch(args);
    }
}