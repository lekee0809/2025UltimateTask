package view;


import javafx.stage.Stage;
import model.MapModel;
import model.Tank;

public class TwoPlayerGameScene extends BaseGameScene{

    public TwoPlayerGameScene(Stage primaryStage) {
        super(primaryStage);
    }

    /**
     * 实现双人对战模式专属逻辑
     */
    @Override
    protected void initModeSpecificLogic() {
        // 1. 加载双人对战专属地图（无AI出生点）
        MapModel twoPlayerMap = new MapModel("two_player"); // 对战地图资源
        drawMap(twoPlayerMap);

        // 2. 初始化两个玩家坦克（区分颜色+位置）
        Tank player1 = new Tank("blue", 100, 500, 0); // 玩家1：WASD控制
        Tank player2 = new Tank("red", 700, 500, 180); // 玩家2：方向键控制
        drawTank(player1);
        drawTank(player2);

        // 3. 绑定双人玩家输入（区分控制键）
        inputHandler.bindTwoPlayerInput();

        // 4. 双人模式无AI敌人（清空敌人逻辑）
        hideEnemyTanks();
    }

    /**
     * 隐藏敌人坦克（双人模式专属）
     */
    private void hideEnemyTanks() {
        // 示例：若有默认敌人，移除或隐藏
        gameRoot.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("enemy-tank"));
    }
}
