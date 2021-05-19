package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AddLegacyRewardDialogController extends AddRewardDialogController {

    private final ObservableList<String> legacyList = FXCollections.observableList(new ArrayList<>(
            DataSource.getInstance().getLegacyResearchRewards().keySet())).sorted();

    @FXML
    public void initialize() {
//        legacyGridPane = DataSource.getInstance().generateToggleButtons(legacyList, legacyGridPane, cpListView, buttonToggleGroup);
//        generateToggleButtons(legacyList);
        generateToggleButtons(legacyList, 10);
    }
}

