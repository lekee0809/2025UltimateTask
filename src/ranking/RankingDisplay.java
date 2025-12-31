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

        // ===== 1. 标题：增加霓虹发光感 =====
        Label title = new Label(gameMode.getModeName().toUpperCase() + " · TOP 10");
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 36));
        title.setTextFill(Color.WHITE);
        // 关键：蓝色外发光让文字在深色背景下极度清晰
        title.setEffect(new DropShadow(20, Color.web("#00d4ff")));

        // ===== 2. 表格主体优化 =====
        TableView<PlayerRecord> table = new TableView<>();
        table.setPrefSize(GameConfig.SCREEN_WIDTH - 80, GameConfig.SCREEN_HEIGHT - 180);

        // 彻底去除原生背景，改用深色半透明
        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;"
        );

        // --- 列定义（增加居中和对比度） ---
        TableColumn<PlayerRecord, String> rankCol = createStyledColumn("排名", 0.15);
        rankCol.setCellValueFactory(cell -> {
            int rank = table.getItems().indexOf(cell.getValue()) + 1;
            return new SimpleStringProperty(String.format("%02d", rank));
        });

        TableColumn<PlayerRecord, Integer> scoreCol = createStyledColumn("得分", 0.25);
        scoreCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getScore()).asObject());

        TableColumn<PlayerRecord, Integer> timeCol = createStyledColumn("时长(s)", 0.25);
        timeCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getPlayTime()).asObject());

        TableColumn<PlayerRecord, String> finishCol = createStyledColumn("达成时间", 0.35);
        finishCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFinishTimeStr()));

        table.getColumns().addAll(rankCol, scoreCol, timeCol, finishCol);
        table.getItems().addAll(RankingManager.loadAllRecords(gameMode));

        // ===== 3. 行样式：动态高亮（解决看不清的关键） =====
        table.setRowFactory(tv -> {
            TableRow<PlayerRecord> row = new TableRow<>() {
                @Override
                protected void updateItem(PlayerRecord item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        // 默认行背景：极浅的白色透明，增加层次感
                        applyEnhancedRowStyle(this);
                    }
                }
            };

            // 悬停效果：加强对比
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

        // ===== 4. 整体容器：渐变背景 =====
        VBox root = new VBox(35, title, table);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(40));
        // 使用更深邃的黑蓝渐变，衬托亮色文字
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0a0e17, #1a202c);");

        Scene scene = new Scene(root, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

        // 关键：表头强制美化
        scene.getRoot().applyCss();
        styleHeader(table);

        stage.setScene(scene);
        stage.show();
    }

    // 辅助方法：创建列并设置居中对齐样式
    private static <T> TableColumn<PlayerRecord, T> createStyledColumn(String name, double pct) {
        TableColumn<PlayerRecord, T> col = new TableColumn<>(name);
        col.setPrefWidth((GameConfig.SCREEN_WIDTH - 100) * pct);
        // 让文字在单元格内居中
        col.setStyle("-fx-alignment: CENTER; -fx-font-size: 15px; -fx-text-fill: #e0e0e0;");
        return col;
    }

    // 核心样式逻辑：前三名使用“发光字体色”而非“深背景色”
    private static void applyEnhancedRowStyle(TableRow<PlayerRecord> row) {
        int index = row.getIndex();
        String baseStyle = "-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0; ";

        if (index == 0) { // 冠军：金黄色字
            row.setStyle(baseStyle + "-fx-background-color: rgba(255, 215, 0, 0.1); -fx-text-background-color: #ffda44;");
        } else if (index == 1) { // 亚军：银色字
            row.setStyle(baseStyle + "-fx-background-color: rgba(192, 192, 192, 0.08); -fx-text-background-color: #ffffff;");
        } else if (index == 2) { // 季军：铜色字
            row.setStyle(baseStyle + "-fx-background-color: rgba(205, 127, 50, 0.08); -fx-text-background-color: #ffab6e;");
        } else {
            row.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-background-color: #d1d5db;");
        }
    }

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
