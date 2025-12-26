package map;

import model.Tank.TankType;

/**
 * 敌人生成描述类
 * 表示：某一关中，某种坦克生成多少个
 */
public class EnemySpawn {

    public TankType type;
    public int count;

    public EnemySpawn(TankType type, int count) {
        this.type = type;
        this.count = count;
    }
}
