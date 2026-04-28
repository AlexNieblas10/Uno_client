module org.borradoruno {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.prefs;

    opens org.borradoruno to javafx.fxml;
    opens org.borradoruno.controller to javafx.fxml;
    opens org.borradoruno.model to com.google.gson;
    opens org.borradoruno.network to com.google.gson;

    exports org.borradoruno;
    exports org.borradoruno.model;
    exports org.borradoruno.controller;
    exports org.borradoruno.network;
}
