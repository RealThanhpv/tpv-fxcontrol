package tpv.fxcontrol.skin;

import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import tpv.fxcontrol.FlowIndexedCell;
import tpv.fxcontrol.FlowView;

import java.util.Random;


public class SheetTest2 extends ApplicationTest {
    private FlowView<Rectangle> view;
    private FlowViewSkin<Rectangle> skin;
    private VirtualFlow  flow;
    private Sheet sheet;

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     */
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        stage.setScene(new Scene(root, 401, 600));

        view = new FlowView<>();

        skin = (FlowViewSkin) view.getSkin();
        flow = skin.flow;
        sheet = flow.sheet;
        sheet.setViewPortWidth(401);
        sheet.setViewPortHeight(600);

        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            Rectangle rect  = new Rectangle();
            rect.setWidth(85); //100
            rect.setHeight(43); // 50
            rect.setFill(Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            view.getItems().add(rect);
        }
        System.out.println("view port width: "+ sheet.getViewPortWidth());
        root.getChildren().add(view);
        stage.show();

    }

    @Test
    public void testSetup(){
        Assert.assertNotNull(view);
        Assert.assertNotNull(skin);
        Assert.assertNotNull(flow);
        Assert.assertNotNull(sheet);
    }



    @Test
    public void computePositions(){
        sheet.setViewPortHeight(401);
        FlowIndexedCell<Rectangle> first = sheet.getFirst();
        System.out.println(first.getLayoutBounds());
        Point2D p0  = sheet.computePosition(first);
        System.out.println("p[0]: "+ p0);
        Assert.assertTrue(p0.getX() == 0.0);
        Assert.assertTrue(p0.getY() == 0.0);

        FlowIndexedCell<Rectangle> second = sheet.get(1);
        Point2D p1  = sheet.computePosition(second);
        System.out.println("p[1]: "+ p1);
        Assert.assertTrue(p1.getX() == 100.0);
        Assert.assertTrue(p1.getY() == 0.0);


        FlowIndexedCell<Rectangle> third = sheet.get(2);
        Point2D p2  = sheet.computePosition(third);
        System.out.println("p[2]: "+ p2);
        Assert.assertTrue(p2.getX() == 200.0);
        Assert.assertTrue(p2.getY() == 0.0);


        FlowIndexedCell<Rectangle> forth = sheet.get(3);
        Point2D p3  = sheet.computePosition(forth);
        System.out.println("p[3]: "+ p3);
        Assert.assertTrue(p3.getX() == 300.0);
        Assert.assertTrue(p3.getY() == 0.0);

        FlowIndexedCell<Rectangle> fifth = sheet.get(4);
        Point2D p4  = sheet.computePosition(fifth);
        System.out.println("p[4]: "+ p4);
        Assert.assertTrue(p4.getX() == 0.0);
        Assert.assertTrue(p4.getY() == 50.0);

        FlowIndexedCell<Rectangle> sixth = sheet.get(5);
        Point2D p5  = sheet.computePosition(sixth);
        System.out.println("p[5]: "+ p5);
        Assert.assertTrue(p5.getX() == 100.0);
        Assert.assertTrue(p5.getY() == 50.0);


        FlowIndexedCell<Rectangle> seventh = sheet.get(6);
        Point2D p6  = sheet.computePosition(seventh);
        System.out.println("p[6]: "+ p6);
        Assert.assertTrue(p6.getX() == 200.0);
        Assert.assertTrue(p6.getY() == 50.0);

        FlowIndexedCell<Rectangle> eighth = sheet.get(7);
        Point2D p7  = sheet.computePosition(eighth);
        System.out.println("p[7]: "+ p7);
        Assert.assertTrue(p7.getX() == 300.0);
        Assert.assertTrue(p7.getY() == 50.0);

        FlowIndexedCell<Rectangle> ninth = sheet.get(8);
        Point2D p8  = sheet.computePosition(ninth);
        System.out.println("p[8]: "+ p8);
        Assert.assertTrue(p8.getX() == 0.0);
        Assert.assertTrue(p8.getY() == 100.0);

        FlowIndexedCell<Rectangle> tenth = sheet.get(9);
        Point2D p9  = sheet.computePosition(tenth);
        System.out.println("p[9]: "+ p9);
        Assert.assertTrue(p9.getX() == 100.0);
        Assert.assertTrue(p9.getY() == 100.0);

        sheet.setViewPortWidth(401);
        sheet.setViewPortHeight(600);
        sheet.addTrailingCells();


//        FlowIndexedCell<Rectangle> c59 = sheet.get(59);
//        Point2D p59  = sheet.computePosition(c59);
//        System.out.println("p[59]: "+ p59);
//        Assert.assertTrue(p59.getX() == 300.0);
//        Assert.assertTrue(p59.getY() == 550.0);
//
//        FlowIndexedCell<Rectangle> last = sheet.getLast();
//        System.out.println("last index: "+last.getIndex());
//        Assert.assertEquals(59, last.getIndex());
//
//        Point2D lastPos = sheet.computePosition(last);
//        System.out.println("last pos: "+ lastPos);
//        Assert.assertTrue(lastPos.getX() == 300.0);
//        Assert.assertTrue(lastPos.getY() == 550.0);
    }

    @Test
    public void computeTotalHeight(){
        int[] outCount = new int[2];
        sheet.setViewPortWidth(401);
        double height = sheet.computeTotalHeight(0, 4, -1, outCount);
        Assert.assertEquals(1, outCount[0]);
        System.out.println("height: "+height);
        Assert.assertTrue(height == 50.0);

        height = sheet.computeTotalHeight(0, 5, -1, outCount);
        Assert.assertEquals(2, outCount[0]);
        System.out.println("height with 5: "+height);
        Assert.assertTrue(height == 100.0);


        height = sheet.computeTotalHeight(0, 8, -1, outCount);
        System.out.println("sheet cached size: "+ sheet.getCacheSize());
        Assert.assertEquals(2, outCount[0]);
        System.out.println("height with 8: "+height);
        Assert.assertTrue(height == 100.0);

        height = sheet.computeTotalHeight(0, 12, -1, outCount);
        Assert.assertEquals(3, outCount[0]);
        Assert.assertTrue(height ==  150.0);


        height = sheet.computeTotalHeight( 0, 16, -1, outCount);
        Assert.assertEquals(4, outCount[0]);
        Assert.assertTrue(height ==  200.0);

        height = sheet.computeTotalHeight( 0, 20, -1, outCount);
//        System.out.println("count at 100: "+ outCount[0]);
        Assert.assertEquals(5, outCount[0]);
//        System.out.println("height with 12: "+height);
        Assert.assertTrue(height ==  250.0);

        height = sheet.computeTotalHeight(0, 24, -1, outCount);
        Assert.assertEquals(6, outCount[0]);
        Assert.assertTrue(height ==  300.0);

        height = sheet.computeTotalHeight( 0, 25, -1, outCount);
        Assert.assertEquals(7, outCount[0]);
        Assert.assertTrue(height ==  350.0);

        height = sheet.computeTotalHeight( 0, 28, -1, outCount);
        Assert.assertEquals(7, outCount[0]);
        Assert.assertTrue(height ==  350.0);

        height = sheet.computeTotalHeight( 0, 32, -1, outCount);
        Assert.assertEquals(8, outCount[0]);
        Assert.assertTrue(height ==  400.0);

        height = sheet.computeTotalHeight(0, 36, -1, outCount);
        Assert.assertEquals(9, outCount[0]);
        Assert.assertEquals(36, outCount[1]);
        Assert.assertTrue(height ==  450.0);

        height = sheet.computeTotalHeight(0, 40, -1, outCount);
        Assert.assertEquals(10, outCount[0]);
        Assert.assertEquals(40, outCount[1]);
        Assert.assertTrue(height ==  500.0);

        height = sheet.computeTotalHeight( 0, 44, -1, outCount);
        Assert.assertEquals(11, outCount[0]);
        Assert.assertEquals(44, outCount[1]);
        Assert.assertTrue(height ==  550.0);

        /*
        height = sheet.computeTotalHeight( 0, 48, -1, outCount);
        Assert.assertEquals(12, outCount[0]);
        Assert.assertEquals(48, outCount[1]);
        Assert.assertTrue(height ==  600.0);

        height = sheet.computeTotalHeight(0, 52, -1, outCount);
        Assert.assertEquals(13, outCount[0]);
        Assert.assertEquals(48, outCount[1]);
        Assert.assertTrue(height ==  650.0);

        //reach viewport height
        height = sheet.computeTotalHeight(0, 56, -1, outCount);
        Assert.assertEquals(13, outCount[0]);
        Assert.assertEquals(48, outCount[1]);
        Assert.assertTrue(height ==  650.0);

        height = sheet.computeTotalHeight(0, 100, -1, outCount);
        Assert.assertEquals(13, outCount[0]);
        Assert.assertEquals(48, outCount[1]);
        Assert.assertTrue(height ==  650.0);

         */
    }

    @Test
    public void estimateHeight(){
        sheet.setViewPortWidth(401);
        double height = sheet.estimateLength(0, flow.getItemsCount());
        double shouldHeight = 100*650/48;
        System.out.println("item count: "+ flow.getItemsCount());
        System.out.println("estimated height: "+ height);
        System.out.println("should height: "+ shouldHeight);
    }



    /**
     * Compute start index  for a absolute offset
     */
    @Test
    public void computeStartIndex(){
       int start =  sheet.computeStartIndex(1000, 0);
       Assert.assertEquals(0, start);

       start =  sheet.computeStartIndex(1000, 50);
       System.out.println("start at 50: "+ start);
        Assert.assertEquals(4, start);

        start =  sheet.computeStartIndex(1000, 10);
        System.out.println("start at 50: "+ start);
        Assert.assertEquals(4, start);


        start =  sheet.computeStartIndex(1000, 100);
        System.out.println("start at 100: "+ start);
        Assert.assertEquals(8, start);
    }


//    @Test
    public void estimateLength() {
        Assert.assertNotNull(sheet);


        double shouldLength = 25*50;
        double length =  sheet.estimateLength(0, 4);
        System.out.println("length: "+length);
        Assert.assertTrue(length == shouldLength);

    }

//    @Test
    public void estimateLength2() {
        Assert.assertNotNull(sheet);

        sheet.setViewPortWidth(320);
        for (int i = 0; i < 6; i++) {
            sheet.itemSizeCache.add(new double[]{101, 20});
        }

        double shouldLength = ((int)6/3)*20;

        double length =  sheet.estimateLength(0, 100);
        Assert.assertTrue(length == shouldLength);

    }
















}