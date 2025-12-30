package item;

import model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 道具生成器（唯一道具管理模块）
 * 负责：
 * 1. 敌人死亡 → 概率生成道具
 * 2. 道具动画更新
 * 3. 道具拾取与效果触发
 * 4. 道具过期与移除
 */
public class ItemSpawner {

    /** 当前场景中存活的道具 */
    private final List<Item> activeItems = new ArrayList<>();

    /** 本帧被拾取的道具（用于音效 / 特效，可选） */
    private final List<Item> collectedItems = new ArrayList<>();

    private final Random random = new Random();

    /* ======================== 生成逻辑 ======================== */

    /**
     * 敌方坦克死亡时调用
     */
    public void onEnemyDestroyed(EnemyTank enemy) {
        double dropChance = getDropChance(enemy);

        if (random.nextDouble() < dropChance) {
            Item item = Item.createRandomItem(enemy.getX(), enemy.getY());
            activeItems.add(item);

            System.out.println("🎁 掉落道具：" + item.getType().getName()
                    + " @ (" + enemy.getX() + ", " + enemy.getY() + ")");
        }
    }

    private double getDropChance(EnemyTank enemy) {
        if (enemy instanceof NormalTank) return 0.4;
        if (enemy instanceof FastTank)   return 0.5;
        if (enemy instanceof HeavyTank)  return 0.7;
        return 0.3;
    }

    /* ======================== 更新逻辑 ======================== */

    /**
     * 每帧调用（由 StageGameScene 调用）
     *
     * @param player 玩家坦克
     * @param enemies 当前敌人列表（炸弹需要）
     * @return 本帧因道具造成的新增分数（用于加分）
     */
    public int update(PlayerTank player, List<Tank> enemies) {
        collectedItems.clear();
        int scoreFromItems = 0;

        Iterator<Item> iterator = activeItems.iterator();

        while (iterator.hasNext()) {
            Item item = iterator.next();

            // 1️⃣ 更新动画
            item.updateAnimation();

            // 2️⃣ 过期直接移除
            if (item.isExpired()) {
                iterator.remove();
                continue;
            }

            // 3️⃣ 玩家拾取
            if (player != null && player.isAlive() && item.checkCollision(player)) {

                switch (item.getType()) {

                    case BOMB:
                        // 炸弹：对所有敌人造成伤害，并统计击杀得分
                        scoreFromItems += applyBomb(item, enemies);
                        break;

                    default:
                        // HEAL / INVINCIBLE
                        item.applyEffect(player);
                        break;
                }

                collectedItems.add(item);
                iterator.remove();
            }
        }

        return scoreFromItems;
    }

    /**
     * 炸弹效果（统一在 Spawner 内处理）
     * @return 炸弹造成的击杀得分
     */
    private int applyBomb(Item bomb, List<Tank> enemies) {
        int score = 0;

        bomb.applyBombEffect(enemies);

        for (Tank enemy : enemies) {
            if (!enemy.isAlive()) {
                score += enemy.getScoreValue();
            }
        }

        System.out.println("💣 炸弹造成得分：" + score);
        return score;
    }

    /* ======================== Getter / 工具 ======================== */

    public List<Item> getActiveItems() {
        return activeItems;
    }

    public List<Item> getCollectedItems() {
        return collectedItems;
    }

    public void clear() {
        activeItems.clear();
        collectedItems.clear();
    }

    public String getStats() {
        int heal = 0, inv = 0, bomb = 0;
        for (Item item : activeItems) {
            switch (item.getType()) {
                case HEAL: heal++; break;
                case INVINCIBLE: inv++; break;
                case BOMB: bomb++; break;
            }
        }
        return String.format("道具统计：回血[%d] 无敌[%d] 炸弹[%d]", heal, inv, bomb);
    }
}
