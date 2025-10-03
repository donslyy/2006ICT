package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ConfigView {

    public static Scene create(Stage stage) {
        var cfg = ConfigService.getInstance();

        var widthLabel  = new Label("Field Width");
        var widthValue  = new Label(Integer.toString(cfg.getFieldWidth()));
        var widthSlider = sliderWithIntRange(15, 20, cfg.getFieldWidth(), widthValue);
        widthSlider.valueProperty().addListener((obs, a, b) -> { cfg.setFieldWidth(b.intValue()); JsonConfigRepository.save(cfg); });

        var heightLabel  = new Label("Field Height");
        var heightValue  = new Label(Integer.toString(cfg.getFieldHeight()));
        var heightSlider = sliderWithIntRange(16, 24, cfg.getFieldHeight(), heightValue);
        heightSlider.valueProperty().addListener((obs, a, b) -> { cfg.setFieldHeight(b.intValue()); JsonConfigRepository.save(cfg); });

        var levelLabel  = new Label("Game Level");
        var levelValue  = new Label(Integer.toString(cfg.getStartLevel()));
        var levelSlider = sliderWithIntRange(1, 10, cfg.getStartLevel(), levelValue);
        levelSlider.valueProperty().addListener((obs, a, b) -> { cfg.setStartLevel(b.intValue()); JsonConfigRepository.save(cfg); });

        var music  = new CheckBox("Music");
        music.setSelected(cfg.isMusicEnabled());
        music.selectedProperty().addListener((o, ov, nv) -> { cfg.setMusicEnabled(nv); JsonConfigRepository.save(cfg); });

        var sfx    = new CheckBox("Sound Effects");
        sfx.setSelected(cfg.isSfxEnabled());
        sfx.selectedProperty().addListener((o, ov, nv) -> { cfg.setSfxEnabled(nv); JsonConfigRepository.save(cfg); });

        var aiPlay = new CheckBox("AI Play");
        aiPlay.setSelected(cfg.isAiPlay());
        aiPlay.selectedProperty().addListener((o, ov, nv) -> { cfg.setAiPlay(nv); JsonConfigRepository.save(cfg); });

        var extend = new CheckBox("Extended Mode");
        extend.setSelected(cfg.isExtendedMode());
        extend.selectedProperty().addListener((o, ov, nv) -> { cfg.setExtendedMode(nv); JsonConfigRepository.save(cfg); });

        var grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);
        grid.add(row(widthLabel, widthSlider, widthValue), 0, 0);
        grid.add(row(heightLabel, heightSlider, heightValue), 0, 1);
        grid.add(row(levelLabel, levelSlider, levelValue), 0, 2);

        var checks = new VBox(10, music, sfx, aiPlay, extend);
        checks.setAlignment(Pos.CENTER);

        var back = new Button("Back");
        back.setOnAction(e -> stage.setScene(Main.buildMenuScene(stage)));

        var content = new VBox(18, grid, new Separator(), new Label("Options"), checks, back);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(18));

        var center = new StackPane(content);
        StackPane.setAlignment(content, Pos.CENTER);
        center.setPadding(new Insets(12));

        var title = new Label("Configuration");
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

    private static Slider sliderWithIntRange(int min, int max, int init, Label out) {
        var s = new Slider(min, max, init);
        s.setMajorTickUnit((max - min) / 4.0);
        s.setMinorTickCount(3);
        s.setShowTickMarks(true);
        s.setShowTickLabels(true);
        s.setSnapToTicks(true);
        out.setText(Integer.toString((int) Math.round(s.getValue())));
        s.valueProperty().addListener((obs, a, b) -> out.setText(Integer.toString(b.intValue())));
        return s;
    }

    private static HBox row(Label label, Slider slider, Label value) {
        label.setMinWidth(130);
        value.setMinWidth(40);
        var box = new HBox(10, label, slider, value);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
