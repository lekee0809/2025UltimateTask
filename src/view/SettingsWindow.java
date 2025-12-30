package view;

import infra.GameConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 游戏设置窗口
 * 功能：音量调节、游戏速度调节、继续/重启游戏
 */
public class SettingsWindow {
    private Stage settingsStage;
    private Slider bgmSlider; // 背景音乐音量
    private Slider sfxSlider; // 音效音量
    private Slider speedSlider; // 游戏速度
    private Stage mainStage; // 主舞台（用于定位）

    public SettingsWindow(Stage mainStage) {
        this.mainStage = mainStage;
        initWindow();
    }

    // 初始化设置窗口
    private void initWindow() {
        settingsStage = new Stage();
        settingsStage.setTitle("游戏设置");
        settingsStage.initModality(Modality.APPLICATION_MODAL); // 模态窗口，阻塞主窗口
        settingsStage.initStyle(StageStyle.UTILITY);
        settingsStage.setWidth(320);
        settingsStage.setHeight(280);
        settingsStage.setResizable(false);

        // 主面板
        VBox mainPane = new VBox(15);
        mainPane.setPadding(new Insets(20));
        mainPane.setAlignment(Pos.CENTER);
        mainPane.setStyle("-fx-background-color: #ecf0f1;");

        // 1. 背景音乐设置
        HBox bgmBox = new HBox(10);
        bgmBox.setAlignment(Pos.CENTER);
        Label bgmLabel = new Label("背景音乐:");
        bgmSlider = new Slider(0, 1, SoundManager.getInstance().getBGMVolume());
        bgmSlider.setPrefWidth(180);
        bgmSlider.valueProperty().addListener((obs, old, val) ->
                SoundManager.getInstance().setBGMVolume(val.doubleValue())
        );
        bgmBox.getChildren().addAll(bgmLabel, bgmSlider);

        // 2. 音效设置
        HBox sfxBox = new HBox(10);
        sfxBox.setAlignment(Pos.CENTER);
        Label sfxLabel = new Label("游戏音效:");
        sfxSlider = new Slider(0, 1, SoundManager.getInstance().getSFXVolume());
        sfxSlider.setPrefWidth(180);
        sfxSlider.valueProperty().addListener((obs, old, val) ->
                SoundManager.getInstance().setSFXVolume(val.doubleValue())
        );
        sfxBox.getChildren().addAll(sfxLabel, sfxSlider);

        // 3. 游戏速度设置
        HBox speedBox = new HBox(10);
        speedBox.setAlignment(Pos.CENTER);
        Label speedLabel = new Label("游戏速度:");
        speedSlider = new Slider(0.5, 2.0, 1.0); // 0.5倍到2倍
        speedSlider.setPrefWidth(180);
        speedSlider.valueProperty().addListener((obs, old, val) -> {
            // 这里可以修改坦克速度等（根据你的需求）
            // 示例：GameConfig.TANK_SPEED * val.doubleValue()
        });
        speedBox.getChildren().addAll(speedLabel, speedSlider);

        // 4. 按钮面板
        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button resumeBtn = new Button("继续游戏");
        resumeBtn.setPrefWidth(100);
        resumeBtn.setOnAction(e -> {
            GameConfig.setGamePaused(false);
            // 通知场景恢复游戏
            ((BaseGameScene) mainStage.getUserData()).resumeGameProcess();
            hide();
        });

        Button restartBtn = new Button("重新开始");
        restartBtn.setPrefWidth(100);
        restartBtn.setOnAction(e -> {
            GameConfig.setGamePaused(false);
            hide();
            // 调用场景重置逻辑
            ((BaseGameScene) mainStage.getUserData()).resetScene();
        });

        btnBox.getChildren().addAll(resumeBtn, restartBtn);

        // 组装面板
        mainPane.getChildren().addAll(bgmBox, sfxBox, speedBox, btnBox);

        // 设置场景
        Scene scene = new Scene(mainPane);
        settingsStage.setScene(scene);

        // 窗口关闭时自动恢复游戏
        settingsStage.setOnCloseRequest(e -> {
            GameConfig.setGamePaused(false);
            ((BaseGameScene) mainStage.getUserData()).resumeGameProcess();
        });
    }

    // 显示窗口（定位到主窗口中央）
    public void show() {
        // 绑定主窗口位置，确保设置窗口居中
        settingsStage.setX(mainStage.getX() + mainStage.getWidth()/2 - 160);
        settingsStage.setY(mainStage.getY() + mainStage.getHeight()/2 - 140);
        settingsStage.show();
    }

    // 隐藏窗口
    public void hide() {
        settingsStage.hide();
    }
}