package controller;

import infra.GameConfig;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import view.BaseGameScene;
import view.SettingsWindow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 输入处理器
 * 核心功能：监听键盘按下/松开事件，更新布尔状态标记
 * 支持：P1 (WASD+J) 和 P2 (方向键+Enter)
 */
public class InputHandler {
    private BaseGameScene scene;
    // 存储一次性按键监听（KeyCode -> 回调函数）
    private Map<KeyCode, Runnable> onceKeyListeners;
    private Set<KeyCode> pressedKeys = new HashSet<>();

    // --- P1 状态 (WASD + J) ---
    private boolean w, s, a, d, j;

    // --- P2 状态 (方向键 + Enter) ---
    private boolean up, down, left, right, enter;

    // --- 全局状态 ---
    private boolean escPressed; // ESC键状态
    //游戏结束状态，是否返回
    private boolean r;

    public InputHandler(BaseGameScene scene) {
        this.scene = scene;
        // 初始化一次性按键监听容器（修复NullPointerException）
        this.onceKeyListeners = new HashMap<>();
    }

    public void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        pressedKeys.add(code);
        // P1
        if (code == KeyCode.W) w = true;
        else if (code == KeyCode.S) s = true;
        else if (code == KeyCode.A) a = true;
        else if (code == KeyCode.D) d = true;
        else if (code == KeyCode.J) j = true;

            // P2
        else if (code == KeyCode.UP) up = true;
        else if (code == KeyCode.DOWN) down = true;
        else if (code == KeyCode.LEFT) left = true;
        else if (code == KeyCode.RIGHT) right = true;
        else if (code == KeyCode.ENTER) enter = true;

            // ESC键（全局暂停）
        else if (code == KeyCode.ESCAPE) {
            escPressed = true;
            // 触发暂停逻辑
            if (!GameConfig.isGamePaused()) {
                scene.pauseGameProcess();
                // 【修复点】这里改为调用静态 show 方法，并传入 scene 参数
                // 或者写成: new SettingsWindow(scene.getPrimaryStage(), scene).show();
                SettingsWindow.show(scene.getPrimaryStage(), scene);
            }
        }
        // R键（返回主界面）
        else if (code == KeyCode.R) r = true;

        // 【注意】这里你原来的代码有一行 if (code == KeyCode.R) r = false;
        // 这会导致 R 键按下瞬间变 true 又立刻变 false，建议删掉或放在 KeyReleased 里
        // 我这里先保留你的原意，但通常是在 Released 里设为 false

        // 处理一次性按键监听
        if (onceKeyListeners.containsKey(code)) {
            Runnable callback = onceKeyListeners.get(code);
            if (callback != null) callback.run();
            onceKeyListeners.remove(code); // 执行后移除
            event.consume();
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        KeyCode code = event.getCode();
        pressedKeys.remove(code);
        // P1
        if (code == KeyCode.W) w = false;
        else if (code == KeyCode.S) s = false;
        else if (code == KeyCode.A) a = false;
        else if (code == KeyCode.D) d = false;
        else if (code == KeyCode.J) j = false;

            // P2
        else if (code == KeyCode.UP) up = false;
        else if (code == KeyCode.DOWN) down = false;
        else if (code == KeyCode.LEFT) left = false;
        else if (code == KeyCode.RIGHT) right = false;
        else if (code == KeyCode.ENTER) enter = false;

            // ESC键
        else if (code == KeyCode.ESCAPE) escPressed = false;
            // R键
        else if (code == KeyCode.R) r = false;
    }

    // 重置按键状态
    public void resetKeyStates() {
        pressedKeys.clear();
        w = s = a = d = j = false;
        up = down = left = right = enter = false;
        escPressed = false;
        r = false;
    }

    /**
     * 绑定一次性按键监听（按下指定按键后执行回调，执行后自动解绑）
     */
    public void bindKeyPressOnce(KeyCode keyCode, Runnable callback) {
        onceKeyListeners.put(keyCode, callback);
    }

    // ========== 双人模式专用接口 (语义化命名) ==========
    public boolean p1Up() { return w; }
    public boolean p1Down() { return s; }
    public boolean p1Left() { return a; }
    public boolean p1Right() { return d; }
    public boolean p1Fire() { return j; }

    public boolean p2Up() { return up; }
    public boolean p2Down() { return down; }
    public boolean p2Left() { return left; }
    public boolean p2Right() { return right; }
    public boolean p2Fire() { return enter; }

    // ========== 单人模式兼容接口 ==========
    public boolean isWPressed() { return w; }
    public boolean isSPressed() { return s; }
    public boolean isAPressed() { return a; }
    public boolean isDPressed() { return d; }
    public boolean isJPressed() { return j; }
    public boolean isRPressed() { return r; }
    public boolean isEscPressed() { return escPressed; }
}