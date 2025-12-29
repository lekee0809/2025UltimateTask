package item;
/**
 * 道具动画状态枚举
 * 用于管理道具的视觉动画状态
 */
public enum ItemAnimationState {
    SPAWNING,     // 生成动画中
    IDLE,         // 静止状态
    EXPIRING,     // 消失动画中
    COLLECTED     // 已被拾取
}