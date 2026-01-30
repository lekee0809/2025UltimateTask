package ranking;

import infra.GameConfig;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class RankingSelectUI {

    public static void showSelectWindow() {
        Stage stage = new Stage();
        stage.setTitle("游戏排行榜 - 模式选择");

        // ===== 标题 =====
        Label titleLabel = new Label("RANKING  BOARDS");
        titleLabel.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 42));
        titleLabel.setTextFill(Color.web("#e0e0ff"));
        titleLabel.setPadding(new Insets(0, 0, 35, 0));

        DropShadow glow = new DropShadow(25, Color.web("#6a89ff"));
        titleLabel.setEffect(glow);

        playTitleGlow(titleLabel, glow);

        // ===== 按钮 =====
        Button singleBtn = createStyledButton("🎮 单人闯关排行榜", "#4facfe", "#00f2fe");
        Button doubleBtn = createStyledButton("⚔ 双人对战排行榜", "#fa709a", "#fee140");
        Button endlessBtn = createStyledButton("🔥 无尽模式排行榜", "#43e97b", "#38f9d7");

        singleBtn.setOnAction(e ->
                RankingDisplay.showRankingWindow(PlayerRecord.GameMode.SINGLE_CHALLENGE));
        doubleBtn.setOnAction(e ->
                RankingDisplay.showRankingWindow(PlayerRecord.GameMode.DOUBLE_BATTLE));
        endlessBtn.setOnAction(e ->
                RankingDisplay.showRankingWindow(PlayerRecord.GameMode.ENDLESS_MODE));

        // ===== 布局 =====
        VBox root = new VBox(28, titleLabel, singleBtn, doubleBtn, endlessBtn);
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: radial-gradient(radius 100%, #1a1f3c, #000000);" +
                        "-fx-padding: 40;"
        );

        Scene scene = new Scene(root, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    // ================== 按钮样式 ==================
    private static Button createStyledButton(String text, String c1, String c2) {
        Button btn = new Button(text);
        btn.setPrefSize(380, 75);
        btn.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 21));

        String baseStyle =
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 40;" +
                        "-fx-background-radius: 40;" +
                        "-fx-border-color: linear-gradient(to right, " + c1 + ", " + c2 + ");";

        String hoverStyle =
                "-fx-background-color: linear-gradient(to right, " + c1 + ", " + c2 + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 40;";

        btn.setStyle(baseStyle);

        // Hover：浮起 + 发光
        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            playScale(btn, 1.08);
            btn.setTranslateY(-4);
            btn.setEffect(new DropShadow(25, Color.web(c2)));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            playScale(btn, 1.0);
            btn.setTranslateY(0);
            btn.setEffect(null);
        });

        // Click：回弹
        btn.setOnMousePressed(e -> playScale(btn, 0.95));
        btn.setOnMouseReleased(e -> playScale(btn, 1.08));

        return btn;
    }

    // ================== 动画 ==================

    private static void playScale(Button btn, double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(180), btn);
        st.setToX(scale);
        st.setToY(scale);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    // 标题呼吸光
    private static void playTitleGlow(Label label, DropShadow glow) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 20)),
                new KeyFrame(Duration.seconds(2),
                        new KeyValue(glow.radiusProperty(), 45))
        );
        timeline.setAutoReverse(true);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}
