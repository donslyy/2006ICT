package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class HighScoresView {

    public static Scene create(Stage stage) {
        List<HighScores.Score> data = HighScores.top();

        VBox list = new VBox(10);
        list.setAlignment(Pos.CENTER);
        if (data.isEmpty()) {
            list.getChildren().add(new Label("No scores yet"));
        } else {
            for (int i = 0; i < data.size(); i++) {
                var row = data.get(i);
                Label line = new Label(String.format("%2d. %-18s %5d", i + 1, row.name(), row.score()));
                line.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 15px;");
                list.getChildren().add(line);
            }
        }

        Button back = new Button("Back");
        back.setOnAction(e -> stage.setScene(Main.buildMenuScene(stage)));

        VBox content = new VBox(18, list, back);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(18));

        StackPane center = new StackPane(content);
        center.setPadding(new Insets(12));

        Label title = new Label("High Scores");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(title);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(18, 0, 0, 0));

        BorderPane root = new BorderPane();
        root.setTop(titleBox);
        root.setCenter(center);
        root.setPrefSize(480, 700);

        return new Scene(root);
    }
}
