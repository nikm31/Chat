import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("sample.fxml").openStream());
        Controller controller = fxmlLoader.getController();
        controller.setStage(primaryStage); // назначаем контроллеру сцену
        primaryStage.setTitle("Chat");
        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.setOnCloseRequest(event -> controller.sendCloseReq());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
