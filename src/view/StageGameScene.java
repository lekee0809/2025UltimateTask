package view;
import javafx.stage.Stage;
import model.MapModel;
import model.Tank;
public class StageGameScene extends BaseGameScene{

    public StageGameScene(Stage primaryStage) {
        super(primaryStage);
    }

    /**
     * 实现闯关模式专属逻辑
     */
    @Override
    protected void initModeSpecificLogic() {
        // 1. 加载闯关模式专属地图（第一关）
        MapModel stageMap = new MapModel("stage_1"); // 闯关地图资源
        drawMap(stageMap);

        // 2. 初始化玩家坦克（单玩家，蓝色坦克）
        Tank playerTank = new Tank("blue", 100, 500, 0); // 初始位置+角度
        drawTank(playerTank);

        // 3. 初始化AI敌人坦克（闯关模式核心）
        initEnemyTanks();

        // 4. 绑定单人玩家输入（WASD移动+空格射击）
        inputHandler.bindSinglePlayerInput();
    }

    /**
     * 初始化闯关模式的AI敌人坦克
     */
    private void initEnemyTanks() {
        // 示例：生成3个红色敌人坦克（不同位置）
        Tank enemy1 = new Tank("red", 200, 100, 180);
        Tank enemy2 = new Tank("red", 400, 100, 180);
        Tank enemy3 = new Tank("red", 600, 100, 180);
        drawTank(enemy1);
        drawTank(enemy2);
        drawTank(enemy3);

        // 后续可扩展：敌人AI逻辑（自动移动、射击）
    }
}


