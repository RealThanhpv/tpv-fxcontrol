//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package tpv.fxcontrol;

import javafx.scene.Node;

public interface DataViewConverter<E, T extends Node> {
    T toView(E var1);

    E toData(T var1);
}
