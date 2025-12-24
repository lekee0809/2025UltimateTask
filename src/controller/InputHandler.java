package controller;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

/**
 * 输入处理器：监听键盘事件，处理多键同时按下（如方向键+发射）
 */
public class InputHandler {
    private Set<KeyCode> pressedKeys = new HashSet<>();

    // 按键按下时记录
    public void handleKeyPress(javafx.scene.input.KeyEvent e) {
        pressedKeys.add(e.getCode());
        // 此处后续和 GameScene 协作，驱动坦克移动/旋转
        if (pressedKeys.contains(KeyCode.W)) {
            // 通知 GameScene 向上移动坦克
        }
        if (pressedKeys.contains(KeyCode.D)) {
            // 通知 GameScene 旋转坦克（右转）
        }
    }

    // 按键释放时移除
    public void handleKeyRelease(javafx.scene.input.KeyEvent e) {
        pressedKeys.remove(e.getCode());
    }
}