package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public abstract class ScreenBase {
    protected Scene sceneWithTopTitleAndCenter(Node center, String titleText) {
        var title = new Label(titleText);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        var titleBox = new HBox(title);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(18, 0, 0, 0));

        var root = new BorderPane();
        root.setTop(titleBox);
        root.setCenter(center);
        root.setPrefSize(480, 700);
        return new Scene(root);
    }
}
