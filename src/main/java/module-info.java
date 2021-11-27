module tpv.fxcontrol {
    exports tpv.fxcontrol;

    opens tpv.fxcontrol to javafx.fxml;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.web;

}