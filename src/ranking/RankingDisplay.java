package ranking;

import infra.GameConfig;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 封装带排名的排行榜数据
class RankedPlayerRecord {
    private final SimpleStringProperty rank;
    private final SimpleIntegerProperty score;
    private final SimpleIntegerProperty playTime;
    private final SimpleStringProperty finishTimeStr;

    public RankedPlayerRecord(int rank, PlayerRecord originalRecord) {
        this.rank = new SimpleStringProperty(String.format("%02d", rank));
        this.score = new SimpleIntegerProperty(originalRecord.getScore());
        this.playTime = new SimpleIntegerProperty(originalRecord.getPlayTime());
        this.finishTimeStr = new SimpleStringProperty(originalRecord.getFinishTimeStr());
    }

    // Getter方法
    public String getRank() { return rank.get(); }
    public int getScore() { return score.get(); }
    public int getPlayTime() { return playTime.get(); }
    public String getFinishTimeStr() { return finishTimeStr.get(); }
}

public class RankingDisplay {

    public static void showRankingWindow(PlayerRecord.GameMode gameMode) {
        Stage stage = new Stage();
        stage.setTitle(gameMode.getModeName() + " 排行榜");

        // 标题
        Label title = new Label(gameMode.getModeName().toUpperCase() + " · TOP 50");
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 36));
        title.setTextFill(Color.WHITE);
        title.setEffect(new DropShadow(20, Color.web("#00d4ff")));

        // 排序控件
        ComboBox<String> sortComboBox = new ComboBox<>();
        sortComboBox.getItems().addAll(
                "分数从高到低（默认）",
                "分数从低到高",
                "时间从新到旧",
                "时间从旧到新"
        );
        sortComboBox.setValue("分数从高到低（默认）");
        sortComboBox.setPrefWidth(200);
        sortComboBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;"
        );

        Button sortButton = new Button("执行排序");
        sortButton.setStyle(
                "-fx-background-color: #00d4ff;" +
                        "-fx-text-fill: black;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5 15;" +
                        "-fx-background-radius: 5;"
        );

        HBox sortBox = new HBox(10, sortComboBox, sortButton);
        sortBox.setAlignment(Pos.CENTER);
        sortBox.setPadding(new Insets(0, 0, 20, 0));

        // 表格主体（泛型指定为 RankedPlayerRecord）
        TableView<RankedPlayerRecord> table = new TableView<>();
        table.setPrefSize(GameConfig.SCREEN_WIDTH - 80, GameConfig.SCREEN_HEIGHT - 220);
        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;"
        );

        // --- 列定义（泛型与 TableView 保持一致） ---
        TableColumn<RankedPlayerRecord, String> rankCol = new TableColumn<>("排名");
        rankCol.setPrefWidth((GameConfig.SCREEN_WIDTH - 100) * 0.15);
        rankCol.setStyle("-fx-alignment: CENTER; -fx-font-size: 15px; -fx-text-fill: #e0e0e0;");
        rankCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRank()));

        TableColumn<RankedPlayerRecord, Integer> scoreCol = new TableColumn<>("得分");
        scoreCol.setPrefWidth((GameConfig.SCREEN_WIDTH - 100) * 0.25);
        scoreCol.setStyle("-fx-alignment: CENTER; -fx-font-size: 15px; -fx-text-fill: #e0e0e0;");
        scoreCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getScore()).asObject());

        TableColumn<RankedPlayerRecord, Integer> timeCol = new TableColumn<>("时长(s)");
        timeCol.setPrefWidth((GameConfig.SCREEN_WIDTH - 100) * 0.25);
        timeCol.setStyle("-fx-alignment: CENTER; -fx-font-size: 15px; -fx-text-fill: #e0e0e0;");
        timeCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getPlayTime()).asObject());

        TableColumn<RankedPlayerRecord, String> finishCol = new TableColumn<>("达成时间");
        finishCol.setPrefWidth((GameConfig.SCREEN_WIDTH - 100) * 0.35);
        finishCol.setStyle("-fx-alignment: CENTER; -fx-font-size: 15px; -fx-text-fill: #e0e0e0;");
        finishCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFinishTimeStr()));

        table.getColumns().addAll(rankCol, scoreCol, timeCol, finishCol);

        // 加载并排序数据
        List<PlayerRecord> rawRecords = RankingManager.loadAllRecords(gameMode);
        List<PlayerRecord> defaultSortedPlayerRecords = new ArrayList<>(rawRecords);
        defaultSortedPlayerRecords.sort(
                Comparator.comparingInt(PlayerRecord::getScore).reversed()
                        .thenComparingLong(PlayerRecord::getFinishTimeStamp).reversed()
        );

        ObservableList<RankedPlayerRecord> defaultRankedRecords = FXCollections.observableArrayList();
        for (int i = 0; i < defaultSortedPlayerRecords.size(); i++) {
            defaultRankedRecords.add(new RankedPlayerRecord(i + 1, defaultSortedPlayerRecords.get(i)));
        }
        table.setItems(defaultRankedRecords);

        // 行样式
        table.setRowFactory(tv -> {
            TableRow<RankedPlayerRecord> row = new TableRow<>() {
                @Override
                protected void updateItem(RankedPlayerRecord item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        applyEnhancedRowStyle(this);
                    }
                }
            };

            row.hoverProperty().addListener((obs, oldVal, hovering) -> {
                if (!row.isEmpty()) {
                    if (hovering) {
                        row.setStyle("-fx-background-color: rgba(0, 212, 255, 0.2); -fx-cursor: hand;");
                    } else {
                        applyEnhancedRowStyle(row);
                    }
                }
            });
            return row;
        });

        // 排序按钮事件
        sortButton.setOnAction(e -> {
            if (rawRecords.isEmpty()) {
                return;
            }

            List<PlayerRecord> tempPlayerRecords = new ArrayList<>(rawRecords);
            String selectedSort = sortComboBox.getValue();
            if (selectedSort != null) {
                switch (selectedSort) {
                    case "分数从高到低（默认）":
                        tempPlayerRecords.sort(
                                Comparator.comparingInt(PlayerRecord::getScore)
                                        .thenComparingLong(PlayerRecord::getFinishTimeStamp).reversed()
                        );
                        break;
                    case "分数从低到高":
                        tempPlayerRecords.sort(
                                Comparator.comparingInt(PlayerRecord::getScore)
                                        .thenComparingLong(PlayerRecord::getFinishTimeStamp)
                        );
                        break;
                    case "时间从新到旧":
                        tempPlayerRecords.sort(
                                Comparator.comparingLong(PlayerRecord::getFinishTimeStamp).reversed()
                        );
                        break;
                    case "时间从旧到新":
                        tempPlayerRecords.sort(
                                Comparator.comparingLong(PlayerRecord::getFinishTimeStamp)
                        );
                        break;
                }
            }

            ObservableList<RankedPlayerRecord> tempRankedRecords = FXCollections.observableArrayList();
            for (int i = 0; i < tempPlayerRecords.size(); i++) {
                tempRankedRecords.add(new RankedPlayerRecord(i + 1, tempPlayerRecords.get(i)));
            }
            table.setItems(tempRankedRecords);
            table.refresh();
        });

        // 整体布局
        VBox root = new VBox(35, title, sortBox, table);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0a0e17, #1a202c);");

        Scene scene = new Scene(root, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        scene.getRoot().applyCss();
        styleHeader(table);

        stage.setScene(scene);
        stage.show();
    }

    // 行样式
    private static void applyEnhancedRowStyle(TableRow<?> row) {
        int index = row.getIndex();
        String baseStyle = "-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0; ";

        if (index == 0) {
            row.setStyle(baseStyle + "-fx-background-color: rgba(255, 215, 0, 0.1); -fx-text-background-color: #ffda44;");
        } else if (index == 1) {
            row.setStyle(baseStyle + "-fx-background-color: rgba(192, 192, 192, 0.08); -fx-text-background-color: #ffffff;");
        } else if (index == 2) {
            row.setStyle(baseStyle + "-fx-background-color: rgba(205, 127, 50, 0.08); -fx-text-background-color: #ffab6e;");
        } else {
            row.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-background-color: #d1d5db;");
        }
    }

    // 表头样式
    private static void styleHeader(TableView<?> table) {
        Node headerBg = table.lookup(".column-header-background");
        if (headerBg != null) {
            headerBg.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 8;");
        }
        table.lookupAll(".column-header").forEach(header -> {
            header.setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent rgba(255,255,255,0.1) transparent;");
            Label label = (Label) header.lookup(".label");
            if (label != null) {
                label.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 16px;");
            }
        });
    }
}