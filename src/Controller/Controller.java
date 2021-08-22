package Controller;

import Model.DataSource;
import Model.PokemonGridPane;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class Controller {

    @FXML
    private TableView<Pokemon> stackTable;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private Label totalPokemon;
    @FXML
    private Label stardustValue;

    @FXML
    private void loadRewardDialog() {
        showAddRewardDialog("addRewardDialog");
    }

    @FXML
    private void loadLegacyRewardDialog() {
        showAddRewardDialog("addLegacyRewardDialog");
    }

    private Dialog<ButtonType> updateDialog;
    private PokemonUpdate updateController;
    private ButtonType closeWindowButton;
    private Dialog<ButtonType> addRewardDialog;
    private Dialog<ButtonType> addLegacyRewardDialog;

    @FXML
    public void initialize() {
        stackTable.setItems(FXCollections.observableList(DataSource.getInstance().getStack()));
        stardustValue.textProperty().bind(DataSource.getInstance().getStackStardustValue());
        totalPokemon.textProperty().bind(DataSource.getInstance().getTotalPokemon());
//        updateProgress.progressProperty().bind(updateTask.progressProperty());
//        updateLabel.textProperty().bind(PokemonUpdate.getInstance().getProgressLabel());
    }

    @FXML
    public void catchReward() {
        if(!DataSource.getInstance().getStack().isEmpty()) {
            Alert catchAlert = new Alert(Alert.AlertType.CONFIRMATION);
            catchAlert.setContentText("Are you sure you want to catch: " + DataSource.getInstance().getStack().get(0) + "?");
            Optional<ButtonType> result = catchAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                DataSource.getInstance().catchReward();
            }
            stackTable.refresh();
        }
    }

    @FXML
    public void catchALl() {
        if(!DataSource.getInstance().getStack().isEmpty()) {
            Alert catchAlert = new Alert(Alert.AlertType.CONFIRMATION);
            catchAlert.setContentText("Are you sure you want to catch ALL Pokemon in your stack?\nThis will empty your stack.");
            Optional<ButtonType> result = catchAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                DataSource.getInstance().catchAll();
            }
            stackTable.refresh();
        }
    }

    // Can we somehow create the loader and controller when the program first runs, so when it's loaded there is no waiting?
    // Maybe we can have a dialog pop-up with a progress bar showing the loading progress.

    public void showAddRewardDialog(String view) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        dialog.setTitle("Claim New Reward");
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(getClass().getResource("../View/" + view + ".fxml"));

        try {
            dialog.getDialogPane().setContent(loader.load());
        } catch(IOException e) {
            System.out.println("Loading dialog failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        ButtonType submit = new ButtonType("Submit");
        ButtonType submitAndAddAnother = new ButtonType("Submit and claim another");
        dialog.getDialogPane().getButtonTypes().addAll(submit, submitAndAddAnother, ButtonType.CANCEL);


        AddRewardDialogController controller = loader.getController();

        Optional<ButtonType> result = dialog.showAndWait();

        while(result.isPresent() && (result.get() == submit || result.get() == submitAndAddAnother)) {
            try {
                if(controller.processResults()) {
                    stackTable.refresh();
                    if (result.get() == submit) {
                        break;
                    }
                    result = dialog.showAndWait();
                }
            } catch (NullPointerException e) {
                Alert nullAlert = new Alert(Alert.AlertType.ERROR);
                nullAlert.setHeaderText("Pokemon or CP not selected");
                nullAlert.setContentText("Both a Pokemon and a CP must be selected.");
                nullAlert.showAndWait();
                break;
            }
        }
    }

    public void createAddRewardDialog(String view) {
        addRewardDialog = new Dialog<>();
        addRewardDialog.initOwner(mainBorderPane.getScene().getWindow());
        addRewardDialog.setTitle("Claim new reward");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("../View/" + view + ".fxml"));

    }

    public void updateResearchTable() {

        Alert updateAlert = new Alert(Alert.AlertType.CONFIRMATION);
        updateAlert.setContentText("Are you sure you want to update the research rewards? \n" +
                "This will download from TSR and pokemongodb.net.");
        Optional<ButtonType> result = updateAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DataSource.getInstance().close();
            updateDialog = new Dialog<>();
            updateDialog.initOwner(mainBorderPane.getScene().getWindow());
            updateDialog.setTitle("Updating Rewards Lists");
            FXMLLoader loader = new FXMLLoader();

            loader.setLocation(getClass().getResource("../View/pokemonUpdateDialog.fxml"));

            try {
                updateDialog.getDialogPane().setContent(loader.load());
            } catch(IOException e) {
                System.out.println("Loading dialog failed: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            updateController = loader.getController();
//            ButtonType updateButton = new ButtonType("Update Rewards", ButtonBar.ButtonData.OK_DONE);
            closeWindowButton = new ButtonType("Close Window", ButtonBar.ButtonData.CANCEL_CLOSE);
            updateDialog.getDialogPane().getButtonTypes().addAll(closeWindowButton);
            updateDialog.getDialogPane().lookupButton(closeWindowButton).disableProperty()
                    .bind(Bindings.createBooleanBinding(
                            () -> updateController.getTaskProgress() < 1,
                            updateController.updateTask.progressProperty()
                    ));
//            Button update = (Button) dialog.getDialogPane().lookupButton(updateButton);
//            update.addEventFilter(ActionEvent.ACTION, event -> {
//                if(PokemonUpdate.getInstance().checkThreadStatus()) {
//                    event.consume();
//                }
//            });
            updateController.updateRewardsTable();
            updateDialog.showAndWait();
            if(DataSource.getInstance().reopen()) {
                DataSource.getInstance().loadResearchRewardsFromSql();
            } else {
                System.out.println("Error re-opening and loading research rewards");
            }
        }
    }

    public void updateLegacyResearchTable() {
        Alert updateAlert = new Alert(Alert.AlertType.CONFIRMATION);
        updateAlert.setContentText("Are you sure you want to update the legacy rewards? \n" +
                "This will download from pokemongo.fandom.com");
        Optional<ButtonType> result = updateAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DataSource.getInstance().close();
            PokemonUpdate.getInstance().updateLegacyRewardsTable();
            if(DataSource.getInstance().reopen()) {
                System.out.println("Successfully re-opened database");
            } else {
                System.out.println("FATAL ERROR: Couldn't re-open database after updating list");
                Platform.exit();
            }
        }
    }

    public void exitProgram() {
        Platform.exit();
    }
}