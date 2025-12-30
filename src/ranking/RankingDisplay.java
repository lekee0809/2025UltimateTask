package ranking;

import infra.GameConfig; // 导入GameConfig
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

// 单个模式排行榜展示界面
public class RankingDisplay {
    // 打开指定模式的排行榜窗口（大小为SCREEN_WIDTH, SCREEN_HEIGHT）
    public static void showRankingWindow(PlayerRecord.GameMode gameMode) {
        Stage stage = new Stage();
        stage.setTitle(gameMode.getModeName() + "（前10名）");

        // 1. 创建排行榜表格（适配大窗口，调整列宽）
        TableView<PlayerRecord> rankingTable = new TableView<>();
        rankingTable.setPrefWidth(GameConfig.SCREEN_WIDTH - 40); // 留出内边距
        rankingTable.setPrefHeight(GameConfig.SCREEN_HEIGHT - 80);

        // 排名列
        TableColumn<PlayerRecord, String> rankCol = new TableColumn<>("排名");
        rankCol.setPrefWidth((GameConfig.SCREEN_WIDTH - 40) / 4); // 均分列宽
        rankCol.setCellValueFactory(cellData -> {
            int rank = rankingTable.getItems().indexOf(cellData.getValue()) + 1;
            return new javafx.beans.property.SimpleStringProperty(rank + "");
        });

        // 分数列
        TableColumn<PlayerRecord, Integer> scoreCol = new TableColumn<>("分数");
        scoreCol.setPrefWidth((GameConfig.SCREEN_WIDTH - 40) / 4);
        scoreCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getScore()).asObject());

        // 游玩时长列
        TableColumn<PlayerRecord, Integer> timeCol = new TableColumn<>("游玩时长（秒）");
        timeCol.setPrefWidth((GameConfig.SCREEN_WIDTH - 40) / 4);
        timeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getPlayTime()).asObject());

        // 完成时间列
        TableColumn<PlayerRecord, String> finishCol = new TableColumn<>("完成时间");
        finishCol.setPrefWidth((GameConfig.SCREEN_WIDTH - 40) / 4);
        finishCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFinishTimeStr()));

        // 添加列到表格
        rankingTable.getColumns().addAll(rankCol, scoreCol, timeCol, finishCol);
        // 加载对应模式数据
        rankingTable.getItems().addAll(RankingManager.loadAllRecords(gameMode));

        // 2. 布局（适配大窗口，统一背景风格）
        VBox root = new VBox(rankingTable);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setStyle("-fx-background-color: #2c3e50;"); // 与主菜单背景统一

        // 3. 设置场景大小为 SCREEN_WIDTH, SCREEN_HEIGHT
        Scene scene = new Scene(root, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }
}