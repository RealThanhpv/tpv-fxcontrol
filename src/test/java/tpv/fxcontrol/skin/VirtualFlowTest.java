package tpv.fxcontrol.skin;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tpv.fxcontrol.FlowCell;
import tpv.fxcontrol.FlowView;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import static org.junit.Assert.*;

public class VirtualFlowTest extends ApplicationTest {
    private FlowView view;
    private FlowViewSkin skin;
    private VirtualFlow  flow;
    private Sheet  sheet;

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     */
    @Override
    public void start(Stage stage) {
        Button button = new Button("click me!");
        button.setOnAction(actionEvent -> button.setText("clicked!"));
        stage.setScene(new Scene(new StackPane(button), 100, 100));

        view = new FlowView<>();

        skin = (FlowViewSkin) view.getSkin();
        flow = skin.flow;
        sheet = flow.sheet;
        sheet.setViewPortWidth(400);
        sheet.setViewPortHeight(600);

    }

    @Test
    public void testSetup(){
        Assert.assertNotNull(view);
        Assert.assertNotNull(skin);
        Assert.assertNotNull(flow);
        Assert.assertNotNull(sheet);
    }




    @Test
    public void sampleAverageRowHeight1() {
            Assert.assertNotNull(sheet);

        for (int i = 0; i < 100; i++) {
            sheet.itemSizeCache.add(new double[]{100, 20});
        }

        double averageHeight =  sheet.sampleAverageRowHeight(201, 10);
        System.out.println(averageHeight);
        Assert.assertTrue(averageHeight == 20.0);

    }

    @Test
    public void sampleAverageRowHeight2() {
        Assert.assertNotNull(sheet);

        for (int i = 0; i < 100; i++) {
            sheet.itemSizeCache.add(new double[]{100, 20});
        }

        double averageHeight =  sheet.sampleAverageRowHeight(201, 1);
        Assert.assertTrue(averageHeight == 20.0);

    }

    @Test
    public void sampleAverageRowHeight3() {
        Assert.assertNotNull(sheet);

        sheet.itemSizeCache.add(new double[]{100, 20});
        sheet.itemSizeCache.add(new double[]{100, 26});

        double averageHeight =  sheet.sampleAverageRowHeight(205, 1);
        System.out.println("test3: "+averageHeight);
        Assert.assertTrue(averageHeight == 26.0);

    }

    @Test
    public void sampleAverageRowHeight4() {
        Assert.assertNotNull(sheet);

        sheet.itemSizeCache.add(new double[]{100, 20});
        sheet.itemSizeCache.add(new double[]{100, 26});

        double averageHeight =  sheet.sampleAverageRowHeight(100, 1);
        System.out.println(averageHeight);
        Assert.assertTrue(averageHeight == 20.0);

    }

    @Test
    public void sampleAverageRowHeight5() {
        Assert.assertNotNull(sheet);

        sheet.itemSizeCache.add(new double[]{100, 20});
        sheet.itemSizeCache.add(new double[]{100, 26});

        double averageHeight =  sheet.sampleAverageRowHeight(100, 2);
        System.out.println(averageHeight);
        Assert.assertTrue(averageHeight == 23.0);

    }

    @Test
    public void sampleAverageRowHeight6() {
        Assert.assertNotNull(sheet);

        sheet.itemSizeCache.add(new double[]{100, 20});
        sheet.itemSizeCache.add(new double[]{100, 26});

        double averageHeight =  sheet.sampleAverageRowHeight(100, 10);
        System.out.println(averageHeight);
        Assert.assertTrue(averageHeight == 23.0);

    }
}