module org.borradoruno {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.prefs;


    opens org.borradoruno to javafx.fxml;
    opens org.borradoruno.controllers to javafx.fxml;
    opens org.borradoruno.models to com.google.gson;
    opens org.borradoruno.network to com.google.gson;
    
    exports org.borradoruno;
    exports org.borradoruno.models;
    exports org.borradoruno.controllers;
    exports org.borradoruno.network;
}