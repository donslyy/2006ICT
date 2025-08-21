package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Tetris");
        stage.setResizable(false);
        stage.setScene(buildMenuScene(stage));
        stage.show();
    }


    public static Scene buildMenuScene(Stage stage) {
      
        // title
        Label title = new Label("Main Menu");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // buttons
        Button play = new Button("Play");
        Button config = new Button("Configuration");
        Button scores = new Button("High Scores");
        Button exit = new Button("Exit");

        play.setMaxWidth(Double.MAX_VALUE);
        config.setMaxWidth(Double.MAX_VALUE);
        scores.setMaxWidth(Double.MAX_VALUE);
        exit.setMaxWidth(Double.MAX_VALUE);

        play.setOnAction(e ->
                new Alert(Alert.AlertType.INFORMATION, "Play screen coming next.", ButtonType.OK).showAndWait());

        config.setOnAction(e -> stage.setScene(ConfigView.create(stage)));

        scores.setOnAction(e -> stage.setScene(HighScoresView.create(stage)));

        exit.setOnAction(e -> {
            var alert = new Alert(Alert.AlertType.CONFIRMATION, "Exit the program?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Confirm Exit");
            alert.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) stage.close(); });
        });

        VBox buttons = new VBox(12, play, config, scores, exit);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(20));
        buttons.setPrefWidth(360);

        // footer
        Label author = new Label("Author: Devlin Hampson s5334585");
        author.setPadding(new Insets(6));
        author.setStyle("-fx-text-fill: #555;");

        BorderPane root = new BorderPane();
        root.setCenter(buttons);
        root.setTop(wrapCenter(title, new Insets(20, 0, 0, 0)));
        root.setBottom(wrapCenter(author, new Insets(0, 0, 10, 0)));
        root.setStyle("-fx-background-color: #ededed;");

        return new Scene(root, 480, 700);
    }

    // small for padding
    private static VBox wrapCenter(javafx.scene.Node n, Insets pad) {
        VBox box = new VBox(n);
        box.setAlignment(Pos.CENTER);
        box.setPadding(pad);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
