package map;
import model.Tank.TankType;

public class GameLevelConfig {
    private GameLevelConfig() {}

    public static EnemySpawn[] getEnemyConfig(int level) {
        switch (level) {
            case 1:
                return new EnemySpawn[]{
                        new EnemySpawn(TankType.ENEMY_NORMAL, 5)
                };
            case 2:
                return new EnemySpawn[]{
                        new EnemySpawn(TankType.ENEMY_NORMAL, 4),
                        new EnemySpawn(TankType.ENEMY_FAST, 2)
                };
            case 3:
                return new EnemySpawn[]{
                        new EnemySpawn(TankType.ENEMY_NORMAL, 5),
                        new EnemySpawn(TankType.ENEMY_FAST, 3),
                        new EnemySpawn(TankType.ENEMY_HEAVY, 2)
                };
            default:
                return new EnemySpawn[0];
        }
    }

    public static int getTargetScore(int level) {
        switch (level) {
            case 1: return 300;
            case 2: return 800;
            case 3: return 1500;
            default: return Integer.MAX_VALUE;
        }
    }


}
