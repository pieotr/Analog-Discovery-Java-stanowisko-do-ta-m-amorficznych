module org.example.demo2 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    requires java.desktop;
    requires static org.knowm.xchart;
    requires waveforms4j;   // <-- add this line

    opens org.example.demo2 to javafx.fxml;
    exports org.example.demo2;
    exports org.example.demo2.Test;
    opens org.example.demo2.Test to javafx.fxml;
    exports org.example.demo2.controller;
    opens org.example.demo2.controller to javafx.fxml;
    exports org.example.demo2.model;
    exports org.example.demo2.service;
}