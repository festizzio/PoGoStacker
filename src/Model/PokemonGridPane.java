package Model;

import Controller.Pokemon;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.util.List;

public class PokemonGridPane {
    private ListView<Integer> cpListView;
    private GridPane rewardGridPane;
    private ToggleGroup buttonToggleGroup;
    private final List<String> rewardList;

    public PokemonGridPane(List<String> rewardList, int numColumns) {
        this.rewardList = rewardList;
        cpListView = new ListView<>();
        buttonToggleGroup = new ToggleGroup();
        rewardGridPane = new GridPane();
        generateToggleButtons(numColumns);
    }

    public GridPane getRewardGridPane() {
        return rewardGridPane;
    }

    public void generateToggleButtons(int numColumns) {
        int count = 0;
        boolean quitOuterLoop = false;
        for(int i = 0; i <= (rewardList.size() / numColumns); i++) {
            for(int j = 0; j < numColumns; j++) {
                // Quit if the number of buttons equals or excees the size of the list, or if it reaches 50 total buttons.
                // This prevents crowding of the legacy list and nobody keeps unclaimed rewards that long anyway.
                if(count >= rewardList.size() || count >= 60) {
                    quitOuterLoop = true;
                    break;
                } else {
                    String pokemonName = rewardList.get(count);
                    ToggleButton button = new ToggleButton("", new ImageView(
                            new Image("/sprites/" + pokemonName.toLowerCase() + ".png",
                                    50, 50, true, false)));

                    button.setToggleGroup(buttonToggleGroup);
                    button.setUserData(pokemonName);
                    button.setMaxWidth(Double.MAX_VALUE);
                    button.setMinWidth(pokemonName.length());
                    button.setMaxHeight(Double.MAX_VALUE);
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
