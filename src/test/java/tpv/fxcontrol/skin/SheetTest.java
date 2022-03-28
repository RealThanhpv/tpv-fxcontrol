package tpv.fxcontrol.skin;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import tpv.fxcontrol.FlowIndexedCell;
import tpv.fxcontrol.FlowView;


public class SheetTest extends ApplicationTest {
    private FlowView<Rectangle> view;
    private FlowViewSkin<Rectangle> skin;
    private VirtualFlow  flow;
    private Sheet sheet;

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




//    @Test
    public void sampleAverageRowHeight1() {
        Assert.assertNotNull(sheet);

        for (int i = 0; i < 100; i++) {
            sheet.itemSizeCache.add(new double[]{100, 20});
        }

        double averageHeight =  sheet.sampleAverageRowHeight(201, 10);
        Assert.assertTrue(averageHeight == 20.0);

    }

//    @Test
    public void sampleAverageRowHeight2() {
        Assert.assertNotNull(sheet);

        for (int i = 0; i < 100; i++) {
            sheet.itemSizeCache.add(new double[]{100, 20});
        }

        double averageHeight =  sheet.sampleAverageRowHeight(201, 1);
        Assert.assertTrue(averageHeight == 20.0);

    }

//    @Test
    public void sampleAverageRowHeight3() {
        Assert.assertNotNull(sheet);

        sheet.itemSizeCache.add(new double[]{100, 20});
        sheet.itemSizeCache.add(new double[]{100, 26});

        double averageHeight =  sheet.sampleAverageRowHeight(205, 1);
        System.out.println("height: "+averageHeight);
        Assert.assertTrue(averageHeight == 26.0);

    }

//    @Test
    public void sampleAverageRowHeight4() {
        Assert.assertNotNull(sheet);

        sheet.itemSizeCache.add(new double[]{100, 20});
        sheet.itemSizeCache.add(new double[]{100, 26});

        double averageHeight =  sheet.sampleAverageRowHeight(100, 1);
        System.out.println(averageHeight);
        Assert.assertTrue(averageHeight == 20.0);

    }

//    @Test
    public void sampleAverageRowHeight5() {
        Assert.assertNotNull(sheet);

        sheet.itemSizeCache.add(new double[]{100, 20});
        sheet.itemSizeCache.add(new double[]{100, 26});

        double averageHeight =  sheet.sampleAverageRowHeight(100, 3);

        Assert.assertTrue(averageHeight == 23.0);

    }

//    @Test
    public void sampleAverageRowHeight6() {
        Assert.assertNotNull(sheet);

        sheet.itemSizeCache.add(new double[]{100, 20});
        sheet.itemSizeCache.add(new double[]{100, 26});

        double averageHeight =  sheet.sampleAverageRowHeight(100, 10);
        Assert.assertTrue(averageHeight == 23.0);

    }






}