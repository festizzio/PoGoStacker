package Controller;

import Model.DataSource;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;

import java.util.ArrayList;
import java.util.List;

public class AddLegacyRewardDialogController extends AddRewardDialogController {

    @FXML
    public void initialize() {
        List<String> legacyList = FXCollections.observableList(new ArrayList<>(
                DataSource.getInstance().getLegacyResearchRewards().keySet())).sorted();
        generateToggleButtons(legacyList, 10);
    }
}

