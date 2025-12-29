package game;

import javafx.application.Application;
import javafx.stage.Stage;
<<<<<<< HEAD
import view.StartScene;
import view.ModeSelectScene; // 如果StartScene会跳转到ModeSelectScene
=======
import view.SoundManager;
import view.StageGameScene;
import view.TwoPlayerGameScene;
import view.EndlessGameScene; // 【新增】记得导入你的无尽模式场景类
>>>>>>> b987339d6cdb357ba125aeffc73c53e7d52be0a4

public class AppLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
<<<<<<< HEAD
        // 1. 直接创建并使用 StartScene（最简洁的方案）
        StartScene startScene = new StartScene(primaryStage);

        // 2. 设置舞台
        primaryStage.setTitle("坦克大战 2025");
        primaryStage.setScene(startScene.getScene());
        primaryStage.setResizable(false); // 可选：固定窗口大小
        primaryStage.show();

        // 3. 如果StartScene需要知道如何切换场景，可以通过构造函数传入回调
        // 或者保持现有设计，让StartScene自己创建ModeSelectScene
=======
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

        // ================= 【新增开始】 =================
        // 3.5 按钮：无尽模式
        Button btnEndlessMode = createMenuButton("无尽模式 (Endless Mode)");

        btnEndlessMode.setOnAction(e -> {
            try {
                System.out.println("正在进入无尽模式...");
                // 这里需要你创建一个 EndlessGameScene 类
                // 如果还没有写好，可以先暂时用 StageGameScene 代替测试
                EndlessGameScene endlessScene = new EndlessGameScene(primaryStage);
                primaryStage.setScene(endlessScene.getScene());
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("进入无尽模式失败，请检查 EndlessGameScene 类是否存在！");
            }
        });
        // ================= 【新增结束】 =================

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

        // 5. 按钮：退出
        Button btnExit = createMenuButton("退出游戏 (Exit)");
        btnExit.setOnAction(e -> System.exit(0));

        // 6. 组装并显示 (注意把 btnEndlessMode 加进去)
        menuRoot.getChildren().addAll(title, btnStageMode, btnEndlessMode, btnPvPMode, btnExit);

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
>>>>>>> b987339d6cdb357ba125aeffc73c53e7d52be0a4
    }

    public static void main(String[] args) {
        launch(args);
    }
}