package item;

/**
 * 道具类型枚举
 * 定义游戏中四种道具类型及其属性
 */
public enum ItemType {
    // 0: 回血 (立即生效)
    HEAL(0, "回血", "images/lives.png",  0),

    // 1: 无敌 (持续5秒) - 已修正图片路径大小写(Buff.png)，防止加载失败
    INVINCIBLE(1, "无敌", "images/Buff.png",  5000),

    // 2: 炸弹 (立即生效) - 注意这里是逗号
    BOMB(2, "炸弹", "images/bullet.png", 0),

    // 3: 强化 (持续10秒) - 新增！用于提升射速或伤害
    // 这里的图片用 bulletLimit.png 表示火力增强
    BUFF(3, "强化", "images/bulletLimit.png", 10000);

    private final int code;           // 道具编码
    private final String name;        // 道具名称
    private final String imagePath;   // 道具图片路径
    private final int duration;       // 道具作用持续时间（毫秒）

    ItemType(int code, String name, String imagePath, int duration) {
        this.code = code;
        this.name = name;
        this.imagePath = imagePath;
        this.duration = duration;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getDuration() {
        return duration;
    }

    /**
     * 根据编码获取道具类型
     */
    public static ItemType fromCode(int code) {
        for (ItemType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return HEAL; // 默认返回回血道具
    }
}