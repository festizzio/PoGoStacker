package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Where do I go after this?
// I had a solid plan, a concrete road ahead of me, but now that the class is finished and this project is wrapping up,
// I don't know where to go next.
// Do I move onto Android? If so, Java or Kotlin? Do I continue making things but using the techniques I already know?
// Unity? Objective-C for iOS? Or Swift? React? JavaScript and HTML?
// How do I know which new techniques to use if they're new to me? How can I use tech of which I am unaware?
// For now, continue with savings goal app since that is actually useful and something I can use now.
// Is this really all I have to do? Continue making applications and add more to them until they have lots of capabilities?

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));

        Parent root = loader.load();
        primaryStage.setTitle("Pokemon Stack");
        primaryStage.setScene(new Scene(root, 500, 600));
        primaryStage.show();
    }

    @Override
    public void init() throws Exception {
        super.init();
        if(!DataSource.getInstance().open()) {
            System.out.println("FATAL ERROR: Couldn't connect to database");
            Platform.exit();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        DataSource.getInstance().close();
    }


    public static void main(String[] args) {
        launch(args);
    }
}

