package item;
/**
 * 道具类型枚举
 * 定义游戏中三种道具类型及其属性
 */
public enum ItemType {
    HEAL(0, "回血", "images/lives.png",  0),     // 回血道具，立即生效
    INVINCIBLE(1, "无敌", "images/buff.png",  3000), // 无敌道具，持续3秒
    BOMB(2, "炸弹", "images/bullet.png", 0); // 炸弹道具，立即生效

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