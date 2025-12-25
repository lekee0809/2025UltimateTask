package controller;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import view.BaseGameScene;

/**
 * 输入处理器
 * 核心功能：监听键盘按下/松开事件，更新布尔状态标记
 * 支持：P1 (WASD+J) 和 P2 (方向键+Enter)
 */
public class InputHandler {
    private BaseGameScene scene;

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