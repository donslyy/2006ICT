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

        // ---- Staged (local) values for fields that can cause navigation elsewhere ----
        boolean stagedExtended = cfg.isExtendedMode();
        ConfigService.Mode stagedMode = cfg.getMode();
        ConfigService.PlayerType stagedP1 = cfg.getPlayer1Type();
        ConfigService.PlayerType stagedP2 = cfg.getPlayer2Type();

        // === Field size & level === (these can remain immediate, or buffer them too if you prefer)
        var widthLabel  = new Label("Field Width");
        var widthValue  = new Label(Integer.toString(cfg.getFieldWidth()));
        var widthSlider = sliderWithIntRange(8, 12, cfg.getFieldWidth(), widthValue);
        widthSlider.valueProperty().addListener((obs, a, b) -> { cfg.setFieldWidth(b.intValue()); JsonConfigRepository.save(cfg); });

        var heightLabel  = new Label("Field Height");
        var heightValue  = new Label(Integer.toString(cfg.getFieldHeight()));
        var heightSlider = sliderWithIntRange(16, 24, cfg.getFieldHeight(), heightValue);
        heightSlider.valueProperty().addListener((obs, a, b) -> { cfg.setFieldHeight(b.intValue()); JsonConfigRepository.save(cfg); });

        var levelLabel  = new Label("Game Level");
        var levelValue  = new Label(Integer.toString(cfg.getStartLevel()));
        var levelSlider = sliderWithIntRange(1, 10, cfg.getStartLevel(), levelValue);
        levelSlider.valueProperty().addListener((obs, a, b) -> { cfg.setStartLevel(b.intValue()); JsonConfigRepository.save(cfg); });

        // === Toggles that are safe to save live ===
        var music  = new CheckBox("Music");
        music.setSelected(cfg.isMusicEnabled());
        music.selectedProperty().addListener((o, ov, nv) -> { cfg.setMusicEnabled(nv); JsonConfigRepository.save(cfg); });

        var sfx    = new CheckBox("Sound Effects");
        sfx.setSelected(cfg.isSfxEnabled());
        sfx.selectedProperty().addListener((o, ov, nv) -> { cfg.setSfxEnabled(nv); JsonConfigRepository.save(cfg); });

        var aiPlay = new CheckBox("AI Play");
        aiPlay.setSelected(cfg.isAiPlay());
        aiPlay.selectedProperty().addListener((o, ov, nv) -> { cfg.setAiPlay(nv); JsonConfigRepository.save(cfg); });

        // === Extended / Mode / Player Types — STAGED (not saved until Save/Back) ===
        var extend = new CheckBox("Extended Mode");
        extend.setSelected(stagedExtended);

        var modeLbl = new Label("Mode");
        var modeBox = new ComboBox<ConfigService.Mode>();
        modeBox.getItems().setAll(ConfigService.Mode.values());
        modeBox.getSelectionModel().select(stagedMode);

        var p1Lbl = new Label("Player 1 Type");
        var p1Box = new ComboBox<ConfigService.PlayerType>();
        p1Box.getItems().setAll(ConfigService.PlayerType.values());
        p1Box.getSelectionModel().select(stagedP1);

        var p2Lbl = new Label("Player 2 Type");
        var p2Box = new ComboBox<ConfigService.PlayerType>();
        p2Box.getItems().setAll(ConfigService.PlayerType.values());
        p2Box.getSelectionModel().select(stagedP2);

        // Rows we show/hide
        var modeRow = labeledRow(modeLbl, modeBox);
        var p1Row   = labeledRow(p1Lbl, p1Box);
        var p2Row   = labeledRow(p2Lbl, p2Box);

        // --- Local staged updates only (no cfg writes here) ---
        extend.selectedProperty().addListener((obs, ov, nv) -> {
            // Update staged flag only
            // If you want to automatically force ONE_PLAYER when disabling extended, do it staged here:
            // if (!nv) modeBox.getSelectionModel().select(ConfigService.Mode.ONE_PLAYER);
            refreshVisibility(nv, modeBox.getValue(), modeRow, p2Row);
        });

        modeBox.valueProperty().addListener((o, ov, nv) -> {
            // If extended is off, prevent staging TWO_PLAYER by snapping back visually (still staged, not saved)
            if (!extend.isSelected() && nv == ConfigService.Mode.TWO_PLAYER) {
                modeBox.getSelectionModel().select(ConfigService.Mode.ONE_PLAYER);
            }
            refreshVisibility(extend.isSelected(), modeBox.getValue(), modeRow, p2Row);
        });

        // no immediate saves for p1/p2 either—purely staged
        // (no listeners needed other than initial selection)

        // Layout
        var grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);
        grid.add(row(widthLabel, widthSlider, widthValue), 0, 0);
        grid.add(row(heightLabel, heightSlider, heightValue), 0, 1);
        grid.add(row(levelLabel, levelSlider, levelValue), 0, 2);

        var checks = new VBox(10, music, sfx, aiPlay, extend);
        checks.setAlignment(Pos.CENTER);

        var sel = new VBox(10,
                modeRow,
                p1Row,
                p2Row
        );
        sel.setAlignment(Pos.CENTER);

        // Buttons
        var save = new Button("Save");
        save.setOnAction(e -> {
            // Commit staged values to cfg NOW (single write point)
            boolean newExtended = extend.isSelected();
            ConfigService.Mode newMode = modeBox.getValue();
            // If extended is off, force ONE_PLAYER on save
            if (!newExtended && newMode == ConfigService.Mode.TWO_PLAYER) {
                newMode = ConfigService.Mode.ONE_PLAYER;
            }

            ConfigService cs = ConfigService.getInstance();
            cs.setExtendedMode(newExtended);
            cs.setMode(newMode);
            cs.setPlayer1Type(p1Box.getValue());
            cs.setPlayer2Type(p2Box.getValue());
            JsonConfigRepository.save(cs);
        });

        var back = new Button("Back");
        back.setOnAction(e -> {
            // Optional: also commit on Back, or remove this block if you prefer Back to discard
            boolean newExtended = extend.isSelected();
            ConfigService.Mode newMode = modeBox.getValue();
            if (!newExtended && newMode == ConfigService.Mode.TWO_PLAYER) {
                newMode = ConfigService.Mode.ONE_PLAYER;
            }

            ConfigService cs = ConfigService.getInstance();
            cs.setExtendedMode(newExtended);
            cs.setMode(newMode);
            cs.setPlayer1Type(p1Box.getValue());
            cs.setPlayer2Type(p2Box.getValue());
            JsonConfigRepository.save(cs);

            stage.setScene(Main.buildMenuScene(stage));
        });

        var btns = new HBox(10, save, back);
        btns.setAlignment(Pos.CENTER);

        var content = new VBox(18, grid, new Separator(), new Label("Options"), checks, new Separator(), sel, btns);
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
        root.setPrefSize(560, 720);

        // Initial visibility based on staged values
        refreshVisibility(stagedExtended, stagedMode, modeRow, p2Row);

        return new Scene(root);
    }

    /** Show/hide Mode and P2 rows based on staged Extended + Mode */
    private static void refreshVisibility(boolean extended,
                                          ConfigService.Mode modeVal,
                                          HBox modeRow, HBox p2Row) {
        boolean isTwoP = extended && modeVal == ConfigService.Mode.TWO_PLAYER;

        // Mode row visible only when Extended is ON
        modeRow.setManaged(extended);
        modeRow.setVisible(extended);

        // Player 2 row visible only when Extended is ON AND TWO_PLAYER selected
        p2Row.setManaged(isTwoP);
        p2Row.setVisible(isTwoP);
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

    private static HBox labeledRow(Label l, Control c) {
        l.setMinWidth(130);
        var box = new HBox(10, l, c);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
