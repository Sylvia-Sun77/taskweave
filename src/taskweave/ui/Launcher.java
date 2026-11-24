package taskweave.ui;

import javafx.application.Application;

// run this class, not MainApp. starting from a class that does not extend Application
// lets JavaFX run from the normal classpath without extra VM arguments
public class Launcher {

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
