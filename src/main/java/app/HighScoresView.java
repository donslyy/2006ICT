package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class HighScoresView {

    public static Scene create(Stage stage) {
        String[][] data = {
                {"Devlin Hampson","12500"},
                {"Ava Thompson","11300"},
                {"Liam Murphy","10600"},
                {"Olivia Nguyen","9800"},
                {"Noah Patel","9200"},
                {"Ethan Williams","8800"},
                {"Isla Chen","8300"},
                {"Mason Taylor","7800"},
                {"Sofia Rossi","7300"},
                {"Lucas Johnson","6900"}
        };

        VBox list = new VBox(10);
        list.setAlignment(Pos.CENTER);
        for (int i = 0; i < data.length; i++) {
            Label row = new Label(String.format("%2d. %-18s  %s", i + 1, data[i][0], data[i][1]));
            row.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 15px;");
            list.getChildren().add(row);
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
