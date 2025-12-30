package infra; // <--- 改成了 infra

import javafx.animation.AnimationTimer;

/**
 * 固定时间步长游戏循环 (Fixed Time-Step Loop)
 * 放在 infra 包很合适，因为它是驱动游戏的“发动机”，属于基础设置。
 */
public abstract class GameLoop extends AnimationTimer {

    private long lastTime = 0;
    private double accumulatedTime = 0;

    // 直接使用同包下的配置，不需要 import GameConfig
    // 逻辑帧时间：1秒 / 60帧 = 16,666,666 纳秒
    private static final long TIME_PER_FRAME = 1_000_000_000L / GameConfig.TARGET_FPS;

    @Override
    public void handle(long now) {

        // 暂停状态不执行任何逻辑
        if (GameConfig.isGamePaused()) {
            return;
        }

        if (lastTime == 0) {
            lastTime = now;
            return;
        }

        long elapsed = now - lastTime;
        lastTime = now;

        accumulatedTime += elapsed;

        // 追赶逻辑：如果卡顿了，就连续计算多次物理逻辑，直到时间追平
        while (accumulatedTime >= TIME_PER_FRAME) {
            onUpdate();
            accumulatedTime -= TIME_PER_FRAME;
        }

        onRender();
    }

    // 抽象方法：交给 BaseGameScene 去实现
    public abstract void onUpdate(); // 逻辑 (60Hz)
    public abstract void onRender(); // 绘图 (屏幕刷新率)

    public double getAccumulatedTime() {
        return accumulatedTime;
    }

    public void setAccumulatedTime(double accumulatedTime) {
        this.accumulatedTime = accumulatedTime;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }
}