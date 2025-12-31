package ranking;

import infra.GameConfig;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class RankingDisplay {

    public static void showRankingWindow(PlayerRecord.GameMode gameMode) {
        Stage stage = new Stage();
        stage.setTitle(gameMode.getModeName() + " 排行榜");

        // ===== 标题 =====
        Label title = new Label(gameMode.getModeName() + " · TOP 10");
        title.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 32));
        title.setTextFill(Color.web("#e0e0ff"));
        title.setEffect(new DropShadow(18, Color.web("#6a89ff")));

        // ===== 表格 =====
        TableView<PlayerRecord> table = new TableView<>();
        table.setPrefSize(
                GameConfig.SCREEN_WIDTH - 60,
                GameConfig.SCREEN_HEIGHT - 160
        );

        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-control-inner-background: rgba(255,255,255,0.05);" +
                        "-fx-table-cell-border-color: transparent;"
        );

        // 排名列
        TableColumn<PlayerRecord, String> rankCol = new TableColumn<>("排名");
        rankCol.setPrefWidth(table.getPrefWidth() / 4);
        rankCol.setCellValueFactory(cell -> {
            int rank = table.getItems().indexOf(cell.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(rank));
        });

        // 分数列
        TableColumn<PlayerRecord, Integer> scoreCol = new TableColumn<>("分数");
        scoreCol.setPrefWidth(table.getPrefWidth() / 4);
        scoreCol.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getScore()).asObject());

        // 时长列
        TableColumn<PlayerRecord, Integer> timeCol = new TableColumn<>("游玩时长（秒）");
        timeCol.setPrefWidth(table.getPrefWidth() / 4);
        timeCol.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getPlayTime()).asObject());

        // 时间列
        TableColumn<PlayerRecord, String> finishCol = new TableColumn<>("完成时间");
        finishCol.setPrefWidth(table.getPrefWidth() / 4);
        finishCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFinishTimeStr()));

        table.getColumns().addAll(rankCol, scoreCol, timeCol, finishCol);
        table.getItems().addAll(RankingManager.loadAllRecords(gameMode));

        // ===== 行样式 + Hover + 前三名 =====
        table.setRowFactory(tv -> {
            TableRow<PlayerRecord> row = new TableRow<>();

            row.hoverProperty().addListener((obs, oldVal, hovering) -> {
                if (!row.isEmpty()) {
                    if (hovering) {
                        row.setStyle("-fx-background-color: rgba(120,160,255,0.25);");
                    } else {
                        applyRowStyle(row);
                    }
                }
            });

            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    applyRowStyle(row);
                }
            });

            return row;
        });

        // ===== 布局 =====
        VBox root = new VBox(25, title, table);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));
        root.setStyle(
                "-fx-background-color: radial-gradient(radius 100%, #1a1f3c, #000000);"
        );

        Scene scene = new Scene(root, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

        // ===== 表头美化（关键！）=====
        scene.getRoot().applyCss();
        styleTableHeader(table);

        stage.setScene(scene);
        stage.show();
    }

    // ===== 根据排名设置行颜色 =====
    private static void applyRowStyle(TableRow<PlayerRecord> row) {
        int index = row.getIndex();
        if (index == 0) {
            row.setStyle("-fx-background-color: rgba(255,215,0,0.28);");
        } else if (index == 1) {
            row.setStyle("-fx-background-color: rgba(192,192,192,0.25);");
        } else if (index == 2) {
            row.setStyle("-fx-background-color: rgba(205,127,50,0.25);");
        } else {
            row.setStyle("-fx-background-color: rgba(255,255,255,0.06);");
        }
    }

    // ===== 纯 Java 改表头样式 =====
    private static void styleTableHeader(TableView<?> table) {
        Node headerBg = table.lookup(".column-header-background");
        if (headerBg != null) {
            headerBg.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        }

        table.lookupAll(".column-header .label").forEach(node -> {
            Label lbl = (Label) node;
            lbl.setTextFill(Color.web("#e0e0ff"));
            lbl.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));
        });
    }
}
