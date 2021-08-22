package Controller;

import Model.DataSource;
import Model.PokemonGridPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.util.*;

public class AddRewardDialogController {

    public static final int MAX_NUM_POKEMON_IN_DIALOG = 60;

    @FXML
    protected ToggleGroup buttonToggleGroup = new ToggleGroup();

    @FXML
    private GridPane rewardGridPane;

    @FXML
    protected ListView<Integer> cpListView = new ListView<>();

    @FXML
    public void initialize() {

//        rewardGridPane = DataSource.getInstance().generateToggleButtons(researchList, rewardGridPane, cpListView, buttonToggleGroup);
//        generateToggleButtons(researchList);
        List<String> researchList = FXCollections.observableList(new ArrayList<>(
                DataSource.getInstance().getResearchRewards().keySet())).sorted();
        generateToggleButtons(researchList, 10);
    }

    public boolean processResults() {
        String pokemonName = (String) buttonToggleGroup.getSelectedToggle().getUserData();
        int CP = cpListView.getSelectionModel().getSelectedItem();
        Pokemon newStackPokemon;
        if (getClass().getSimpleName().equals("AddLegacyRewardDialogController")) {
            newStackPokemon = DataSource.getInstance().getLegacyResearchRewards().get(pokemonName);
        } else {
            newStackPokemon = DataSource.getInstance().getResearchRewards().get(pokemonName);
        }
        newStackPokemon.setCP(CP);
        DataSource.getInstance().addReward(newStackPokemon);

        return true;
    }

    public void generateToggleButtons(List<String> rewardList, int numColumns) {
        int count = 0;
        boolean quitOuterLoop = false;
        for(int i = 0; i <= (rewardList.size() / numColumns); i++) {
            for(int j = 0; j < numColumns; j++) {
                // Quit if the number of buttons equals or excees the size of the list, or if it reaches 50 total buttons.
                // This prevents crowding of the legacy list and nobody keeps unclaimed rewards that long anyway.
                if(count >= rewardList.size() || count >= MAX_NUM_POKEMON_IN_DIALOG) {
                    quitOuterLoop = true;
                    break;
                } else {
                    String pokemonName = rewardList.get(count);
                    ToggleButton button = new ToggleButton("", new ImageView(
                            new Image("/sprites/" + pokemonName.toLowerCase() + ".png",
                                    50, 50, true, false)));

                    button.setToggleGroup(buttonToggleGroup);
                    button.setUserData(pokemonName);
                    button.setMaxWidth(100);
                    button.setMinWidth(pokemonName.length());
                    button.setMaxHeight(100);
                    if(!(DataSource.getInstance().getResearchRewards().get(pokemonName) == null)) {
                        button.setOnAction(actionEvent -> cpListView.getItems().setAll(DataSource.getInstance()
                                .getResearchRewards().get(pokemonName).getPossibleCPValues()));
                    } else {
                        button.setOnAction(actionEvent -> cpListView.getItems().setAll(DataSource.getInstance()
                                .getLegacyResearchRewards().get(pokemonName).getPossibleCPValues()));
                    }

                    rewardGridPane.add(button, j, i);
                    count++;
                }
            }
            if(quitOuterLoop) {
                System.out.println("Generated toggle buttons for " + rewardList.getClass().getSimpleName());
                break;
            }
        }
    }
}

