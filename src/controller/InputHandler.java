package controller;

import com.sun.javafx.collections.MappingChange;
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

    // --- P1 状态 (WASD + J) ---
    private boolean w, s, a, d, j;

    // --- P2 状态 (方向键 + Enter) ---
    private boolean up, down, left, right, enter;

    public InputHandler(BaseGameScene scene) {
        this.scene = scene;
    }

    public void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
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
    }

    public void handleKeyReleased(KeyEvent event) {
        KeyCode code = event.getCode();
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
    }
    // 按键状态存储（示例）
    private Set<KeyCode> pressedKeys = new HashSet<>();
    // 重置按键状态
    public void resetKeyStates() {
        pressedKeys.clear();
        // 如需重置其他输入状态（如鼠标位置、按钮点击状态）可在此补充
    }
    /**
     * 绑定一次性按键监听（按下指定按键后执行回调，执行后自动解绑）
     * @param keyCode 监听的按键（如ENTER、R）
     * @param callback 按键按下后执行的回调函数
     */
    public void bindKeyPressOnce(KeyCode keyCode, Runnable callback) {
        // 1. 先移除该按键已有的一次性监听（避免重复绑定）
        onceKeyListeners.remove(keyCode);

        // 2. 存储新的一次性回调
        onceKeyListeners.put(keyCode, callback);

        // 3. 绑定临时按键监听（核心：一次性执行后解绑）
        Scene scene = this.scene.getGameScene();
        if (scene != null) {
            // 使用匿名内部类绑定，方便后续移除
            KeyEventHandler onceKeyHandler = new KeyEventHandler(keyCode);
            scene.addEventHandler(KeyEvent.KEY_PRESSED, onceKeyHandler);
        }
    }

    /**
     * 一次性按键事件处理器（内部类，用于自动解绑）
     */
    private class KeyEventHandler implements javafx.event.EventHandler<KeyEvent> {
        private KeyCode targetKey;

        public KeyEventHandler(KeyCode targetKey) {
            this.targetKey = targetKey;
        }

        @Override
        public void handle(KeyEvent event) {
            // 1. 匹配目标按键
            if (event.getCode() == targetKey) {
                // 2. 获取并执行回调
                Runnable callback = onceKeyListeners.get(targetKey);
                if (callback != null) {
                    callback.run();
                }

                // 3. 执行后立即解绑（核心：一次性）
                onceKeyListeners.remove(targetKey);
                event.getSource();
                Scene scene = (Scene) event.getSource();
                scene.removeEventHandler(KeyEvent.KEY_PRESSED, this);

                // 4. 消费事件，避免传递给其他监听
                event.consume();
            }
        }
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

    // ========== [重要] 单人模式兼容接口 ==========
    // 之前写的 StageGameScene 用的是这些名字，加在这里防止报错
    public boolean isWPressed() { return w; }
    public boolean isSPressed() { return s; }
    public boolean isAPressed() { return a; }
    public boolean isDPressed() { return d; }
    public boolean isJPressed() { return j; }
}