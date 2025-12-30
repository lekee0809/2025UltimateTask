package item;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 粒子特效类
 * 用于道具拾取和生成时的视觉特效
 */
public class ParticleEffect {

    private double x, y;                    // 特效中心位置
    private List<Particle> particles;       // 粒子列表
    private float duration;                 // 特效持续时间（秒）
    private float elapsedTime;              // 已过时间（秒）
    private boolean finished;               // 特效是否结束

    private static final Random random = new Random();

    /**
     * 构造函数
     * @param x 中心X坐标
     * @param y 中心Y坐标
     * @param particleCount 粒子数量
     * @param color 粒子颜色
     * @param duration 特效持续时间（秒）
     */
    public ParticleEffect(double x, double y, int particleCount, Color color, float duration) {
        this.x = x;
        this.y = y;
        this.duration = duration;
        this.elapsedTime = 0;
        this.finished = false;

        // 创建粒子
        particles = new ArrayList<>();
        for (int i = 0; i < particleCount; i++) {
            particles.add(createParticle(color));
        }
    }

    /**
     * 创建单个粒子
     */
    private Particle createParticle(Color color) {
        // 随机角度和速度
        double angle = random.nextDouble() * 2 * Math.PI;
        double speed = 50 + random.nextDouble() * 100;
        double vx = Math.cos(angle) * speed;
        double vy = Math.sin(angle) * speed;

        // 随机大小和生命周期
        float size = 2 + random.nextFloat() * 4;
        float particleLife = 0.5f + random.nextFloat() * 0.5f;

        return new Particle(x, y, vx, vy, size, color, particleLife);
    }

    /**
     * 更新特效
     * @param deltaTime 距上一帧的时间（秒）
     */
    public void update(float deltaTime) {
        if (finished) return;

        elapsedTime += deltaTime;

        // 检查特效是否结束
        if (elapsedTime >= duration) {
            finished = true;
            return;
        }

        // 更新所有粒子
        for (Particle particle : particles) {
            particle.update(deltaTime);
        }
    }

    /**
     * 渲染特效
     * @param gc 图形上下文
     */
    public void render(GraphicsContext gc) {
        if (finished) return;

        for (Particle particle : particles) {
            particle.render(gc);
        }
    }

    /**
     * 检查特效是否结束
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * 粒子类（内部类）
     */
    private static class Particle {
        double x, y;
        double vx, vy;
        float size;
        Color color;
        float life;
        float maxLife;

        Particle(double x, double y, double vx, double vy, float size, Color color, float life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.life = life;
            this.maxLife = life;
        }

        void update(float deltaTime) {
            // 更新位置
            x += vx * deltaTime;
            y += vy * deltaTime;

            // 应用重力
            vy += 98 * deltaTime; // 模拟重力加速度

            // 减少生命值
            life -= deltaTime;

            // 根据剩余生命调整大小
            size *= 0.95f;
        }

        void render(GraphicsContext gc) {
            if (life <= 0) return;

            // 根据剩余生命计算透明度
            float alpha = life / maxLife;
            Color renderColor = new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    alpha
            );

            // 绘制粒子
            gc.setFill(renderColor);
            gc.fillOval(x - size/2, y - size/2, size, size);
        }
    }
}
