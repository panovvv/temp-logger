package com.epam.vadim_panov;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("scene.fxml"));
		Parent root = loader.load();
		Scene scene =new Scene(root, 470, 320);
        primaryStage.setTitle("Temperature logger utility");
        primaryStage.setScene(scene);
        primaryStage.show();
		Controller controller = loader.getController();
		primaryStage.setOnCloseRequest((we -> controller.close()));
		primaryStage.show();
		controller.start();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
