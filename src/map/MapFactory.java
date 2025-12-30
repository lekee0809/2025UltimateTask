package map;

/**
 * 地图工厂类
 * 负责根据当前关卡数 (Level) 决定生成哪种类型的地图
 */
public class MapFactory {

    /**
     * 根据关卡生成地图
     * @param level 当前关卡数 (1, 2, 3...)
     * @return 生成好的二维数组地图
     */
    public static int[][] getMap(int level) {
        // 策略：
        // 第 1 关：强制为 "大战场" (Battlefield) - 让玩家熟悉操作，空间大
        // 第 2+ 关：50% 概率是大战场，50% 概率是巷战 (Maze)

        if (level == 1) {
            return new BattlefieldMapGenerator().generate();
        } else {
            // 随机决定
            if (Math.random() < 0.5) {
                // 生成大战场
                return new BattlefieldMapGenerator().generate();
            } else {
                // 生成巷战 (迷宫)
                // 随着关卡增加，我们可以让迷宫更难 (例如 blockSize=1 的细路)
                // 这里暂时保持默认
                return new MazeDigger().generate();
            }
        }
    }
}