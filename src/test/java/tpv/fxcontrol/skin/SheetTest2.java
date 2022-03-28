package tpv.fxcontrol.skin;

import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

        for (int i = 0; i < 20; i++) {
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
    }














}