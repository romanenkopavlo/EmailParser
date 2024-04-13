module org.example.parseremails {
    requires javafx.controls;
    requires javafx.fxml;

    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.jsoup;

    opens org.example.parseremails to javafx.fxml;
    exports org.example.parseremails;
}