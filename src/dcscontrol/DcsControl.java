package dcscontrol;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Standard application launcher main class
 */
public class DcsControl extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainDialog.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("DCS Controls Helper");
        stage.getIcons().add( new Image(getClass().getResourceAsStream("/resource/ico32.png")) );
        stage.getIcons().add( new Image(getClass().getResourceAsStream("/resource/ico64.png")) );
        stage.getIcons().add( new Image(getClass().getResourceAsStream("/resource/ico128.png")) );
        stage.show();       
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
