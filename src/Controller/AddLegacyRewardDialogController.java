package Controller;

import Model.DataSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import java.util.ArrayList;
import java.util.List;

public class AddLegacyRewardDialogController extends AddRewardDialogController {
    
    @FXML
    public void initialize() {
//        legacyGridPane = DataSource.getInstance().generateToggleButtons(legacyList, legacyGridPane, cpListView, buttonToggleGroup);
//        generateToggleButtons(legacyList);
        List<String> legacyList = FXCollections.observableList(new ArrayList<>(
                DataSource.getInstance().getLegacyResearchRewards().keySet())).sorted();
        generateToggleButtons(legacyList, 10);
    }
}

