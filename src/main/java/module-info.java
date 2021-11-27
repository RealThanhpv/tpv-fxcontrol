module tpv.fxcontrol {
    exports tpv.fxcontrol;
    exports tpv.fxcontrol.cell;

    opens tpv.fxcontrol to javafx.fxml;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.web;

}