package Controller;

import Model.DataSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import java.util.ArrayList;

public class AddLegacyRewardDialogController extends AddRewardDialogController {

    private ObservableList<String> legacyList;

    @FXML
    public void initialize() {
//        legacyGridPane = DataSource.getInstance().generateToggleButtons(legacyList, legacyGridPane, cpListView, buttonToggleGroup);
//        generateToggleButtons(legacyList);
        legacyList = FXCollections.observableList(new ArrayList<>(
                DataSource.getInstance().getLegacyResearchRewards().keySet())).sorted();
        generateToggleButtons(legacyList, 10);
    }
}

