package view;

import infra.GameConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 游戏设置窗口 (增强版)
 * 功能：音量调节、全屏切换、游戏速度、继续/重启游戏
 */
public class SettingsWindow {
    private Stage settingsStage;
    private Stage mainStage;
    private BaseGameScene gameScene; // 当前游戏场景 (如果在主菜单则为 null)

    // 构造方法
    public SettingsWindow(Stage mainStage, BaseGameScene gameScene) {
        this.mainStage = mainStage;
        this.gameScene = gameScene; // 直接传入场景引用，更安全
        initWindow();
    }

    // 静态快捷调用：主菜单用
    public static void show(Stage stage) {
        new SettingsWindow(stage, null).show();
    }

    // 静态快捷调用：游戏内用
    public static void show(Stage stage, BaseGameScene scene) {
        new SettingsWindow(stage, scene).show();
    }

    // 初始化设置窗口
    private void initWindow() {
        settingsStage = new Stage();
        settingsStage.setTitle("SYSTEM SETTINGS");
        settingsStage.initModality(Modality.APPLICATION_MODAL); // 模态窗口
        settingsStage.initOwner(mainStage);
        settingsStage.initStyle(StageStyle.TRANSPARENT); // 透明无边框风格，更酷
        settingsStage.setResizable(false);

        // 主面板 (深色背景 + 金色边框)
        VBox mainPane = new VBox(20);
        mainPane.setPadding(new Insets(30));
        mainPane.setAlignment(Pos.CENTER);
        mainPane.setStyle("-fx-background-color: rgba(30, 39, 46, 0.95); " +
                "-fx-border-color: #fbc531; -fx-border-width: 2; " +
                "-fx-background-radius: 10; -fx-border-radius: 10;");

        // 标题
        Label titleLabel = new Label("系统设置");
        titleLabel.setTextFill(Color.web("#fbc531"));
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));

        // 1. 全屏设置
        CheckBox fullScreenBox = new CheckBox("全屏模式 (Fullscreen)");
        fullScreenBox.setTextFill(Color.WHITE);
        fullScreenBox.setFont(Font.font(14));
        fullScreenBox.setSelected(mainStage.isFullScreen());
        fullScreenBox.selectedProperty().addListener((obs, oldVal, newVal) -> mainStage.setFullScreen(newVal));

        // 2. 背景音乐设置
        VBox bgmBox = createSliderBox("背景音乐 (BGM)", 0, 1, SoundManager.getInstance().getBGMVolume(), val -> {
            SoundManager.getInstance().setBGMVolume(val);
        });

        // 3. 音效设置
        VBox sfxBox = createSliderBox("游戏音效 (SFX)", 0, 1, SoundManager.getInstance().getSFXVolume(), val -> {
            SoundManager.getInstance().setSFXVolume(val);
        });

        // 4. 按钮面板
        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        // 通用按钮样式
        String btnStyle = "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;";
        String closeStyle = "-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;";

        // 关闭/返回按钮
        Button closeBtn = new Button(gameScene == null ? "关闭" : "返回游戏");
        closeBtn.setPrefWidth(100);
        closeBtn.setStyle(closeStyle);
        closeBtn.setOnAction(e -> hide());

        // 如果在游戏中，显示 "重新开始" 按钮
        if (gameScene != null) {
            Button restartBtn = new Button("重新开始");
            restartBtn.setPrefWidth(100);
            restartBtn.setStyle(btnStyle);
            restartBtn.setOnAction(e -> {
                gameScene.resetScene();
                gameScene.resumeGameProcess();
                hide();
            });
            btnBox.getChildren().add(restartBtn);
        }

        btnBox.getChildren().add(closeBtn);

        // 组装面板
        mainPane.getChildren().addAll(titleLabel, fullScreenBox, bgmBox, sfxBox, btnBox);

        // 设置场景 (透明背景)
        Scene scene = new Scene(mainPane, 400, 450);
        scene.setFill(Color.TRANSPARENT);
        settingsStage.setScene(scene);

        // 窗口关闭时自动恢复游戏逻辑
        settingsStage.setOnHidden(e -> {
            if (gameScene != null) {
                gameScene.resumeGameProcess();
            }
        });
    }

    // 辅助方法：创建带标签的滑块
    private VBox createSliderBox(String title, double min, double max, double current, SliderCallback callback) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(title);
        label.setTextFill(Color.LIGHTGRAY);

        Slider slider = new Slider(min, max, current);
        slider.setPrefWidth(300);
        slider.setStyle("-fx-control-inner-background: #57606f;"); // 滑块轨道颜色

        slider.valueProperty().addListener((obs, old, val) -> callback.onChange(val.doubleValue()));

        box.getChildren().addAll(label, slider);
        return box;
    }

    // 显示窗口
    public void show() {
        settingsStage.show();
    }

    // 隐藏窗口
    public void hide() {
        settingsStage.close();
    }

    // 简单的回调接口
    interface SliderCallback {
        void onChange(double value);
    }
}