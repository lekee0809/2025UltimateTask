package view;

import game.AppLauncher;
import infra.GameConfig;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * 现代化暂停菜单
 */
public class ModernPauseMenu {
    private VBox pauseMenu;
    private boolean isPaused = false;
    private BaseGameScene gameScene;
    private Stage primaryStage;

    public ModernPauseMenu(BaseGameScene gameScene, Stage primaryStage) {
        this.gameScene = gameScene;
        this.primaryStage = primaryStage;
        createPauseMenu();
    }

    /**
     * 创建现代化暂停菜单
     */
    private void createPauseMenu() {
        pauseMenu = new VBox(25); // 增加组件间距
        pauseMenu.setAlignment(Pos.CENTER);
        // 现代化深色背景，更清晰
        pauseMenu.setStyle(ThemeManager.PanelStyles.GAME_PANEL);

        // 1. 标题
        Label title = new Label("⏸️ 暂停菜单");
        title.setFont(ThemeManager.Fonts.SUBTITLE);
        title.setTextFill(ThemeManager.Colors.PRIMARY);

        // ==================== 音量调节 ====================
        Label volLabel = new Label("🔊 音量调节");
        volLabel.setTextFill(ThemeManager.Colors.LIGHT_TEXT);
        volLabel.setFont(ThemeManager.Fonts.MENU_ITEM);

        // 音量滑块 (0.0 到 1.0，默认 0.5)
        Slider volSlider = new Slider(0, 1, SoundManager.getInstance().getBGMVolume());
        volSlider.setMaxWidth(300);
        volSlider.setMajorTickUnit(0.2);
        volSlider.setMinorTickCount(5);
        volSlider.setShowTickLabels(true);
        volSlider.setShowTickMarks(true);
        // 禁止滑块获取焦点，防止按方向键时误触
        volSlider.setFocusTraversable(false);

        // 监听滑块变化，实时修改音量
        volSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double vol = newVal.doubleValue();
            // 调用 SoundManager 调整 BGM 和 音效
            SoundManager.getInstance().setBGMVolume(vol);
            SoundManager.getInstance().setSFXVolume(vol);
        });

        VBox volBox = new VBox(5, volLabel, volSlider);
        volBox.setAlignment(Pos.CENTER);

        // ==================== 全屏设置 ====================
        CheckBox fullScreenBox = new CheckBox("🖥️ 全屏模式");
        fullScreenBox.setTextFill(ThemeManager.Colors.LIGHT_TEXT);
        fullScreenBox.setFont(ThemeManager.Fonts.MENU_ITEM);
        fullScreenBox.setFocusTraversable(false); // 禁止获取焦点

        // 初始化勾选状态
        fullScreenBox.setSelected(primaryStage.isFullScreen());

        // 勾选事件
        fullScreenBox.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            primaryStage.setFullScreen(isSelected);
        });

        // ==================== 游戏控制按钮 ====================
        // 继续按钮
        Button btnResume = new Button("▶ 继续游戏");
        btnResume.setStyle(ThemeManager.ButtonStyles.PRIMARY);
        btnResume.setOnMouseEntered(e -> btnResume.setStyle(ThemeManager.ButtonStyles.PRIMARY_HOVER));
        btnResume.setOnMouseExited(e -> btnResume.setStyle(ThemeManager.ButtonStyles.PRIMARY));
        btnResume.setOnAction(e -> togglePause());
        btnResume.setFocusTraversable(false);

        // 重开按钮
        Button btnRestart = new Button("🔄 重新开始");
        btnRestart.setStyle(ThemeManager.ButtonStyles.PRIMARY);
        btnRestart.setOnMouseEntered(e -> btnRestart.setStyle(ThemeManager.ButtonStyles.PRIMARY_HOVER));
        btnRestart.setOnMouseExited(e -> btnRestart.setStyle(ThemeManager.ButtonStyles.PRIMARY));
        btnRestart.setOnAction(e -> {
            togglePause();
            gameScene.resetScene();
            gameScene.resumeGameProcess();
        });
        btnRestart.setFocusTraversable(false);

        // 设置按钮
        Button btnSettings = new Button("⚙️ 游戏设置");
        btnSettings.setStyle(ThemeManager.ButtonStyles.PRIMARY);
        btnSettings.setOnMouseEntered(e -> btnSettings.setStyle(ThemeManager.ButtonStyles.PRIMARY_HOVER));
        btnSettings.setOnMouseExited(e -> btnSettings.setStyle(ThemeManager.ButtonStyles.PRIMARY));
        btnSettings.setOnAction(e -> {
            // 打开设置窗口
            SettingsWindow.show(primaryStage, gameScene);
        });
        btnSettings.setFocusTraversable(false);

        // 退出按钮
        Button btnExit = new Button("🏠 返回主菜单");
        btnExit.setStyle(ThemeManager.ButtonStyles.DANGER);
        btnExit.setOnMouseEntered(e -> btnExit.setStyle(ThemeManager.ButtonStyles.DANGER_HOVER));
        btnExit.setOnMouseExited(e -> btnExit.setStyle(ThemeManager.ButtonStyles.DANGER));
        btnExit.setOnAction(e -> {
            // 停止游戏循环
            if (gameScene.getGameLoop() != null) {
                gameScene.getGameLoop().stop();
            }
            // 重置暂停状态
            GameConfig.setGamePaused(false);
            isPaused = false;

            SoundManager.getInstance().stopGameMusic();
            SoundManager.getInstance().playBackgroundMusic();

            AppLauncher mainMenu = new AppLauncher();
            try {
                mainMenu.start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btnExit.setFocusTraversable(false);

        // 添加所有组件到容器
        pauseMenu.getChildren().addAll(
                title,
                new Label(""), // 占位空行
                volBox,
                fullScreenBox,
                new Label(""), // 占位空行
                btnResume,
                btnRestart,
                btnSettings,
                btnExit
        );
    }

    /**
     * 切换暂停/继续状态
     */
    public void togglePause() {
        if (GameConfig.isGameOver()) return; // 游戏结束不能暂停

        if (isPaused) {
            resumeGame();
        } else {
            pauseGame();
        }
    }

    /**
     * 暂停游戏
     */
    public void pauseGame() {
        isPaused = true;
        GameConfig.setGamePaused(true);

        // 停止游戏循环
        if (gameScene != null && gameScene.getGameLoop() != null) {
            gameScene.getGameLoop().stop();
        }

        // 暂停音乐
        SoundManager.getInstance().pauseBGM();
        SoundManager.getInstance().pauseGameMusic();

        // 显示暂停菜单
        if (gameScene != null && gameScene.getGameRoot() != null) {
            if (!gameScene.getGameRoot().getChildren().contains(pauseMenu)) {
                gameScene.getGameRoot().getChildren().add(pauseMenu);
            }
        }

        // 显示暂停提示
        if (gameScene != null) {
            gameScene.showTipText("GAME PAUSED", 0);
        }
    }

    /**
     * 恢复游戏
     */
    public void resumeGame() {
        isPaused = false;
        
        // 直接恢复游戏逻辑，而不是调用gameScene.resumeGameProcess()以避免递归
        if (gameScene != null && gameScene.getGameLoop() != null && !GameConfig.isGameOver()) {
            gameScene.getGameLoop().start();
        }
        GameConfig.setGamePaused(false);

        // 移除暂停菜单
        if (gameScene != null && gameScene.getGameRoot() != null) {
            if (gameScene.getGameRoot().getChildren().contains(pauseMenu)) {
                gameScene.getGameRoot().getChildren().remove(pauseMenu);
            }
        }

        // 【修复1】强制隐藏提示文字
        if (gameScene != null && gameScene.getTipText() != null) {
            gameScene.getTipText().setOpacity(0);
            gameScene.getTipText().setText("");
        }
        if (gameScene != null) {
            gameScene.stopCurrentTipAnimation();
        }

        SoundManager.getInstance().playBGM();
        SoundManager.getInstance().resumeGameMusic();
    }

    public VBox getPauseMenu() {
        return pauseMenu;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }
}