package item;

import model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 道具生成器
 * 负责管理游戏中所有道具的生成、更新和移除
 * 添加了道具自动消失和拾取效果支持
 */
public class ItemSpawner {

    private List<Item> activeItems;           // 当前活跃的道具列表
    private List<Item> collectedItems;        // 本帧被拾取的道具列表（用于特效和音效）
    private Random random;

    /**
     * 构造函数
     */
    public ItemSpawner() {
        activeItems = new ArrayList<>();
        collectedItems = new ArrayList<>();
        random = new Random();
    }

    /**
     * 敌方坦克死亡时调用此方法
     * 根据坦克类型的不同概率决定是否生成道具
     * @param enemy 被摧毁的敌方坦克
     */
    public void onEnemyDestroyed(EnemyTank enemy) {
        // 计算当前坦克的道具掉落概率
        double dropChance = getDropChance(enemy);

        // 使用随机数决定是否掉落道具
        if (random.nextDouble() < dropChance) {
            // 在敌方坦克死亡位置生成道具
            double x = enemy.getX();
            double y = enemy.getY();
            Item item = Item.createRandomItem(x, y);
            activeItems.add(item);

            System.out.println("生成道具: " + item.getType().getName() +
                    " 在位置 (" + x + ", " + y + ")");
        }
    }

    /**
     * 获取不同坦克类型的道具掉落概率
     * @param enemy 敌方坦克对象
     * @return 掉落概率（0.0-1.0）
     */
    private double getDropChance(EnemyTank enemy) {
        // 根据坦克类型返回不同的掉落概率
        if (enemy instanceof NormalTank) {
            return 0.4; // 普通坦克：40%掉落概率
        } else if (enemy instanceof FastTank) {
            return 0.5; // 快速坦克：50%掉落概率
        } else if (enemy instanceof HeavyTank) {
            return 0.7; // 重型坦克：70%掉落概率
        }
        return 0.3; // 默认：30%掉落概率
    }

    /**
     * 更新所有道具状态
     * 1. 更新道具动画
     * 2. 检查道具与玩家的碰撞
     * 3. 移除过期或被拾取的道具
     * @param player 玩家坦克对象
     */
    public void update(PlayerTank player) {
        // 清空上一帧的拾取列表
        collectedItems.clear();

        // 使用迭代器安全地遍历和移除
        Iterator<Item> iterator = activeItems.iterator();

        while (iterator.hasNext()) {
            Item item = iterator.next();

            // 1. 更新道具动画
            item.updateAnimation();

            // 2. 检查道具是否过期
            if (item.isExpired()) {
                iterator.remove();
                System.out.println("道具过期消失: " + item.getType().getName());
                continue;
            }

            // 3. 检查玩家是否拾取道具
            if (item.checkCollision(player)) {
                // 【关键修改】炸弹不调用applyEffect，直接标记拾取
                if (item.getType() == ItemType.BOMB) {
                    collectedItems.add(item);
                    iterator.remove();
                    System.out.println("拾取炸弹道具");
                }
                // 其他道具正常调用applyEffect
                else if (item.applyEffect(player)) {
                    collectedItems.add(item);
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 获取所有活跃的道具
     * @return 活跃道具列表
     */
    public List<Item> getActiveItems() {
        return activeItems;
    }

    /**
     * 获取本帧被拾取的道具
     * @return 被拾取的道具列表
     */
    public List<Item> getCollectedItems() {
        return collectedItems;
    }

    /**
     * 清空所有道具
     * 用于关卡重置或游戏重新开始时
     */
    public void clear() {
        activeItems.clear();
        collectedItems.clear();
    }

    /**
     * 获取道具数量统计信息
     * @return 包含各种道具数量的字符串
     */
    public String getStats() {
        int healCount = 0;
        int invincibleCount = 0;
        int bombCount = 0;

        for (Item item : activeItems) {
            switch (item.getType()) {
                case HEAL:
                    healCount++;
                    break;
                case INVINCIBLE:
                    invincibleCount++;
                    break;
                case BOMB:
                    bombCount++;
                    break;
            }
        }

        return String.format("道具统计: 回血[%d] 无敌[%d] 炸弹[%d]",
                healCount, invincibleCount, bombCount);
    }

    public void setActiveItems(List<Item> activeItems) {
        this.activeItems = activeItems;
    }

    public void setCollectedItems(List<Item> collectedItems) {
        this.collectedItems = collectedItems;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }
}