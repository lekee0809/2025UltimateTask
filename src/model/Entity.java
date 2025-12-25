package model;

import javafx.scene.canvas.GraphicsContext;

public abstract class Entity {
    // 基础坐标
    public double x;
    public double y;

    // 基础尺寸（用于碰撞检测）
    public double width;
    public double height;

    // 状态控制
    public boolean alive = true;

    public Entity(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // 每一帧逻辑更新（位移、AI等）
    public abstract void update(Tile[][] map);

    // 每一帧画面渲染
    public abstract void draw(GraphicsContext gc);

    // 简单的矩形碰撞检测逻辑，可以放在父类复用
    public boolean intersects(Entity other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y;
    }

   public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}