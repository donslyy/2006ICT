package app;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashView {
    public static Scene create(Stage stage) {
        var title = new Label("2006ICT – Tetris");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        var titleBox = new HBox(title);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(18, 0, 0, 0));

        var name = new Label("Devlin Hampson");
        name.setStyle("-fx-font-size: 16px;");
        var sid = new Label("s5334585");
        var group = new Label("Group 36");
        var subtitle = new Label("Milestone 2 Demo");

        var content = new VBox(8, name, sid, group, subtitle);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(18));

        var center = new StackPane(content);
        center.setPadding(new Insets(12));

        var root = new BorderPane();
        root.setTop(titleBox);
        root.setCenter(center);
        root.setPrefSize(480, 700);

        var pt = new PauseTransition(Duration.seconds(2.5));
        pt.setOnFinished(e -> stage.setScene(Main.buildMenuScene(stage)));
        pt.play();

        return new Scene(root);
    }
}
