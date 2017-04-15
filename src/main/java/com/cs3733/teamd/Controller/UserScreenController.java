package com.cs3733.teamd.Controller;

import com.cs3733.teamd.Main;
import com.cs3733.teamd.Model.*;
import com.cs3733.teamd.Model.Entities.Directory;
import com.cs3733.teamd.Model.Entities.Node;
import com.cs3733.teamd.Model.Entities.Tag;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import javafx.scene.layout.AnchorPane;
import org.controlsfx.control.textfield.TextFields;

//import javax.xml.soap.Text;

import java.awt.Point;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Anh Dao on 4/6/2017.
 */
public class UserScreenController extends AbsController{

    //connect to facade
    Directory dir = Directory.getInstance();

    //shift nodes to align with image
    private static int USERSCREEN_X_OFFSET = -5;
    private static int USERSCREEN_Y_OFFSET = -90;

    //Boundary objects
    @FXML
    public Button LoginButton;
    public Button SpanishButton;
    public Button SearchButton;
    public TextField TypeDestination;
    public Text EnterDest;
    public Text floor;
    public Label directionLabel;
    @FXML
    private Slider floorSlider;
    public ImageView floorMap;
    public Canvas MapCanvas;
    public AnchorPane MMGpane;
    @FXML
    private TextArea directions;
    public GraphicsContext gc;

    //proxy pattern for maps
    ImageInterface imgInt = new ProxyImage();
    public int floorNum = Main.currentFloor;

    private static LinkedList<Node> pathNodes;
    int onFloor = Main.currentFloor;
    int indexOfElevator = 0;
    private String output = "";
    private Tag starttag = null;
    private int startfloor = 0;
    private int destfloor = 0;


    @FXML
    private void initialize()
    {
        TextFields.bindAutoCompletion(TypeDestination,dir.getTags());
        setSpanishText();
        directions.setText(output);
        floorMap.setImage(imgInt.display(floorNum));
        setFloorSliderListener();


        gc = MapCanvas.getGraphicsContext2D();
        if(pathNodes != null) {
            draw();
        }
    }

    private void setFloorSliderListener(){
        floorSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                if (!floorSlider.isValueChanging()) {
                    onFloor = new_val.intValue();
                    floorSlider.setValue(onFloor);
                    //floorMap.setImage(imageHashMap.get(onFloor));
                    floorMap.setImage(imgInt.display(onFloor));
                    gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
                    output = "";
                    directions.setText(output);
                    System.out.println(onFloor);

                    if(pathNodes != null) {
                        draw();
                    }

                }
            }
        });
    }
    //Spanish button to change language to Spanish
    @FXML
    public void onSpanish(ActionEvent actionEvent) throws  IOException{
        super.switchLanguage();
        switchScreen(MMGpane,"/Views/UserScreen.fxml");
        setSpanishText();
    }

    @FXML
    public void onLogin(ActionEvent actionEvent) throws IOException {
        pathNodes=null;
        switchScreen(MMGpane, "/Views/UserScreen.fxml");
        switchScreen(MMGpane, "/Views/LoginScreen.fxml");
    }

    //Spanish translation
    public void setSpanishText(){
        SpanishButton.setText(Main.bundle.getString("spanish"));
        SearchButton.setText(Main.bundle.getString("search"));
        LoginButton.setText(Main.bundle.getString("login"));
        directionLabel.setText(Main.bundle.getString("directions"));
        EnterDest.setText(Main.bundle.getString("enterDes"));
        floor.setText(Main.bundle.getString("floor"));

        if(ApplicationConfiguration.getInstance().getCurrentLanguage()
                == ApplicationConfiguration.Language.SPANISH){
            LoginButton.setFont(Font.font("System",14));
        }
        else{
            LoginButton.setFont(Font.font("System",20));
        }

    }

    //Search button, generates path and directions on submit
    @FXML
    public void onSearch(ActionEvent actionEvent) throws IOException{
        //stores the destination inputted
        Main.DestinationSelected = TypeDestination.getText();
        //Gets nodes and tags from directory
        int tagCount = dir.getTags().size();
        int nodeCount = dir.getNodes().size();
        //Makes a temporary holder for values
        Tag currentTag;

        String startTagString = "Kiosk";
        for(int itr = 0; itr < tagCount; itr++){
            currentTag = dir.getTags().get(itr);
            //If match is found create path to node from start nodes
            if(startTagString.equals(currentTag.getTagName())){
                starttag = currentTag;
            }

        }
        // Do we have a starting tag???
        if(starttag != null) {
            // What floor is the Kiosk on?
            Main.currentFloor = starttag.getNodes().getFirst().getFloor();
            System.out.println("HEre");
            //Iterates through all existing tags
            for (int itr = 0; itr < tagCount; itr++) {

                currentTag = dir.getTags().get(itr);
                // Have we found our destination?
                if (Main.DestinationSelected.equals(currentTag.getTagName())) {
                    double record = 9999999999999999999999.0;
                    Pathfinder bestPath = null;

                    for (Node n : currentTag.getNodes()) {
                        // Let's find the shortest path
                        Pathfinder attempt = new Pathfinder(starttag.getNodes().getFirst(), n);
                        if (attempt.pathLength(attempt.shortestPath()) < record) {
                            bestPath = attempt;
                        }
                    }
                    // Is there a path from the source to the destination?
                    if (bestPath != null) {
                        pathNodes = bestPath.shortestPath();
                    } else {
                        System.out.println("Failed to find best path out of many\n");
                        Pathfinder pathfinder = new Pathfinder(
                                starttag.getNodes().getFirst(),
                                currentTag.getNodes().getFirst()
                        );
                        //use the shortest path
                        pathNodes = pathfinder.shortestPath();
                    }
                }
            }
        }
        // Clear the canvas
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        output = "";
        directions.setText(output);
        System.out.println(onFloor);

        if(pathNodes != null) {
            draw();
        }

    }

    @FXML
    //Starts path displaying process
    private void draw(){
        System.out.println("Begin drawing");
        plotPath(UserScreenController.pathNodes);
    }

    //Converts a node to a point to display on map
    private Point getConvertedPoint(Node node) { //conversion from database to canvas
        int x = node.getX() + USERSCREEN_X_OFFSET;
        int y = node.getY() + USERSCREEN_Y_OFFSET;
        //Point p = new Point((int) ((x-offset_x)/scale), (int) (imageH-(y-offset_y)/scale));
        Point p = new Point(x, y);
        return p;
    }

    //Converts a given path of nodes to a path of points and then draws it
    public void plotPath(LinkedList<Node> path){
        LinkedList<Point> pointsStartFloor = new LinkedList<>();
        LinkedList<Point> pointsEndFloor = new LinkedList<>();
        int index = 0;
        startfloor = starttag.getNodes().getFirst().getFloor();
        destfloor = path.getFirst().getFloor();
        System.out.println(destfloor);
        for (Node node: path) {
            if(node.getFloor() == startfloor) {
                System.out.println("Node.getfloor" + node.getFloor());
                System.out.println("plot"+ startfloor);
                pointsStartFloor.add(getConvertedPoint(node));
                index++;
            }
            else if(node.getFloor() == destfloor){
                System.out.println("Node.getfloor" + node.getFloor());
                pointsEndFloor.add(getConvertedPoint(node));
            }
        }
        indexOfElevator = index;
        if(startfloor == onFloor) {
            System.out.println("startfloor");
            drawPathFromPoints(gc, pointsStartFloor);
        }
        else if(destfloor == onFloor){
            System.out.println("destfloor");
            drawPathFromPoints(gc, pointsEndFloor);
        }
    }

    //Function to actually draw a path
    private void drawPathFromPoints(GraphicsContext gc, LinkedList<Point> path) {
        System.out.println("Drawing");
        //color for start node
        gc.setFill(javafx.scene.paint.Color.GREEN);
        //color for edges
        gc.setStroke(javafx.scene.paint.Color.BLUE);
        //intialize point previous as null due to start not having a previous
        Point previous = null;
        //Create a list to hold text directions
        LinkedList<String> TextDirections = new LinkedList<String>();
        //Set line width
        gc.setLineWidth(3);
        //Saving the length of the path
        int pathlength = path.size();
        //holder for straight indicator
        String curdir = "";
        //Generate placeholders for text directions
        for (int str = 0; str < pathlength; str++){
            TextDirections.add("");
        }
        //Set radius of first node
        int radius = 7;
        TextDirectionGenerator g = new TextDirectionGenerator(
                path,
                (startfloor == onFloor)&&(startfloor != destfloor)
        );
        List<String> directionsArray = g.generateTextDirections();
        for(String token: directionsArray) {
            System.out.println(token);
        }

        //Iterate through the path
        for  (int i = 0; i < pathlength; i++){
            //get the point for current iterations
            Point current = path.get(i);
            //If it is the last point in the path
            if(i == pathlength-1){
                radius = 7;
                gc.setFill(javafx.scene.paint.Color.RED);
            }
            //draw the point
            gc.fillOval(current.getX(), current.getY(), radius*2, radius*2);
            //draw line from previous to current
            if(previous != null){
                gc.strokeLine(previous.getX() + radius, previous.getY() + radius,
                        current.getX() + radius, current.getY() + radius);
            }
            if(i == indexOfElevator-1){
                String temp = "";
                if (ApplicationConfiguration.getInstance().getCurrentLanguage()
                        == ApplicationConfiguration.Language.SPANISH) {
                    temp = "Terminando al ascensor " + "\n";
                } else {
                    temp = "Ending at elevator " + "\n";
                }
                TextDirections.set(i, temp);
            }
            if(i != indexOfElevator-1) {
                //first point directions
                if (i == 0) {
                    String temp = "";
                    if (ApplicationConfiguration.getInstance().getCurrentLanguage()
                            == ApplicationConfiguration.Language.SPANISH) {
                        temp = "Comenzando y mirando hacia el quiosco" + "\n";
                    } else {
                        temp = "Starting at and facing the kiosk " + "\n";
                    }
                    TextDirections.set(i, temp);
                }
                else if(pathlength < 3) {
                    TextDirections.set(i, "");
                }
                // every point between first and second to last
                else if ((i > 0) && ((i + 2) < pathlength)) {
                    //Assign point
                    Point oldnode = path.get(i - 1);
                    Point currnode = path.get(i);
                    Point nextnode = path.get(i + 1);
                    //Run helper functions to update text
                    if (ApplicationConfiguration.getInstance().getCurrentLanguage()
                            == ApplicationConfiguration.Language.SPANISH) {
                        TextDirections = getTextEsp(oldnode, currnode, nextnode, curdir, TextDirections, i);
                    } else {
                        TextDirections = getText(oldnode, currnode, nextnode, curdir, TextDirections, i);
                    }
                    curdir = setCurdir(oldnode, currnode, nextnode, curdir, i);
                }
                // second to last point
                else if (i == pathlength - 2) {
                    //Assign point
                    Point oldnode = path.get(i - 1);
                    Point currnode = path.get(i);
                    Point nextnode = path.get(i + 1);
                    //Run helper functions to update text
                    if (ApplicationConfiguration.getInstance().getCurrentLanguage()
                            == ApplicationConfiguration.Language.SPANISH) {
                        TextDirections = getTextMidHallwayEsp(oldnode, currnode, nextnode, curdir, TextDirections, i);
                    } else {
                        TextDirections = getTextMidHallway(oldnode, currnode, nextnode, curdir, TextDirections, i);
                    }
                    curdir = setCurdir(oldnode, currnode, nextnode, curdir, i);
                    //System.out.println("Second to last" + TextDirections.getLast());
                }
                //last point
                else if (i == pathlength - 1) {
                    String temp = "";
                    if (ApplicationConfiguration.getInstance().getCurrentLanguage()
                            == ApplicationConfiguration.Language.SPANISH) {
                        temp = "Terminando al " + Main.DestinationSelected;
                    } else {
                        temp = "Ending at " + Main.DestinationSelected;
                    }
                    TextDirections.set(i, temp);
                }
            }

            //Update for next loop
            previous = current;
            //Set intermediate points to be smaller and blue
            gc.setFill(javafx.scene.paint.Color.BLUE);
            radius = 5;
        }

        //iterate through to find directions that are not blanks
        for( int stritr = 0; stritr < pathlength; stritr++){
            String tempstr;
            //Make text directions without blanks
            if(stritr == 0){
                tempstr  = TextDirections.get(stritr);
                if(tempstr != "i"){
                    output = output + tempstr;
                }
            }
            if(stritr > 0 && stritr < pathlength - 1) {
                tempstr = TextDirections.get(pathlength - stritr - 1);
                if (tempstr != "i") {
                    output = output + tempstr;
                }
            }
            if(stritr == pathlength-1){
                tempstr  = TextDirections.get(stritr);
                if(tempstr != "i"){
                    output = output + tempstr;
                }
            }
        }
        directions.setText(output);
        // print output to show path directions
        //System.out.println(output);
    }

    //helper function to determine if the change in x direction if more significant than the change in y direction
    public int moreSignificantChange(double x, double y){
        //System.out.println("Xvalue = " + x + " Yvalue = " + y);
        if(x >= 0){
            if(y >= 0){
                if(x >= y){
                    return 1; // X more signifcant
                } else if(y > x){
                    return 2; // Ymore significant
                }
            } else if(y < 0){
                if(x >= y*-1){
                    return 1; // X more signifcant
                } else if(y*-1 > x){
                    return 2; // Y more signifcant
                }
            }
        } else if(x < 0){
            if(y >= 0){
                if(x*-1 >= y){
                    return 1; // X more signifcant
                } else if(y > x*-1){
                    return 2; // Y more signifcant
                }
            }
            if(y < 0){
                if(x*-1 >= y*-1){
                    return 1; // X more signifcant
                } else if(y*-1 > x*-1){
                    return 2; // Y more signifcant
                }
            }
        }
        return 0; // Doesn't occur
    }

    //Helper function to generate text for most cases
    private LinkedList<String> getText(
            Point oldNode,
            Point currentNode,
            Point nextNode,
            String curdir,
            LinkedList<String> TextDirections,
            int i
    ){
        //Get values
        double oldNodeX = oldNode.getX();
        double oldNodeY = oldNode.getY();
        double currentnodex = currentNode.getX();
        double currentnodey = currentNode.getY();
        double nextnodex = nextNode.getX();
        double nextnodey = nextNode.getY();

        //System.out.println(oldnodex + "" + oldnodey);
        //Get transitions
        double oldcurx = currentnodex - oldNodeX;
        double oldcury = currentnodey - oldNodeY;
        double curnextx = nextnodex - currentnodex;
        double curnexty = nextnodey - currentnodey;

        double oldChangesMoreSignificant = 0;
        double currentChangesMoreSignificant = 0;

        //Get orientations
        oldChangesMoreSignificant = moreSignificantChange(oldcurx, oldcury);
        currentChangesMoreSignificant = moreSignificantChange(curnextx, curnexty);

        //System.out.println("Regular getText " + ocmorsig + "" + cnmorsig);

        // ocx more significant than ocy
        if(oldChangesMoreSignificant == 1){
            // cnx more significant than cny
            if(currentChangesMoreSignificant == 1){
                // oc is movement to the right
                if(oldcurx >= 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                            TextDirections.set(i, "Go straight until end of hallway " + "\n");
                        }
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        // turn around case shouldnt activate
                    }
                }
                // oc is movement to the left
                if(oldcurx < 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        //turn around case
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                            TextDirections.set(i, "Go straight until end of hallway " + "\n");
                        }
                    }
                }
            }
            // cny more significant than cnx
            if(currentChangesMoreSignificant == 2){
                // oc is movement to the right
                if(oldcurx >= 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                        TextDirections.set(i, "Turn left " + "\n");
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                        TextDirections.set(i, "Turn right " + "\n");
                    }
                }
                // oc is movement to the left
                if(oldcurx < 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                        TextDirections.set(i, "Turn right " + "\n");
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                        TextDirections.set(i, "Turn left " + "\n");
                    }
                }
            }
        }

        // ocy more significant than ocx
        if(oldChangesMoreSignificant == 2){
            // cnx more significant than cny
            if(currentChangesMoreSignificant == 1){
                // oc is movement downwards
                if(oldcury >= 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                        TextDirections.set(i, "Turn right " + "\n");
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                        TextDirections.set(i, "Turn left " + "\n");
                    }
                }
                // oc is movement upwards
                if(oldcury < 0){
                    // ocn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                        TextDirections.set(i, "Turn left " + "\n");
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                        TextDirections.set(i, "Turn right " + "\n");
                    }
                }
            }
            // cny more significant than cnx
            if(currentChangesMoreSignificant == 2){
                // oc is movement downwards
                if(oldcury >= 0){
                    // cn is movement upwards
                    if(curnexty < 0){
                        // turn around case
                    }
                    // cn is movement downwards
                    if(curnexty >= 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                            TextDirections.set(i, "Go straight until end of hallway " + "\n");
                        }
                    }
                }
                // oc is movement upwards
                if(oldcury < 0){
                    // cn is movement upwards
                    if(curnexty < 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                            TextDirections.set(i, "Go straight until end of hallway " + "\n");
                        }
                    }
                    // cn is movement downwards
                    if(curnexty >= 0){
                        //turn around case
                    }
                }
            }
        }
        //Return the updated text directions
        return TextDirections;
    }

    //Helper function to generate text for case of reaching an elevator or the destination
    private LinkedList<String> getTextMidHallway(Point oldnode, Point currnode, Point nextnode,
                                                 String curdir, LinkedList<String> TextDirections, int i ){
        double oldnodex = oldnode.getX();
        double oldnodey = oldnode.getY();
        double currentnodex = currnode.getX();
        double currentnodey = currnode.getY();
        double nextnodex = nextnode.getX();
        double nextnodey = nextnode.getY();

        double oldcurx = currentnodex - oldnodex;
        double oldcury = currentnodey - oldnodey;
        double curnextx = nextnodex - currentnodex;
        double curnexty = nextnodey - currentnodey;

        double ocmorsig = 0;
        double cnmorsig = 0;

        ocmorsig = moreSignificantChange(oldcurx, oldcury);
        cnmorsig = moreSignificantChange(curnextx, curnexty);

        //System.out.println("Edge getText " + ocmorsig + "" + cnmorsig);

        // ocx more significant than ocy
        if(ocmorsig == 1){
            // cnx more significant than cny
            if(cnmorsig == 1){
                // shouldnt happen in node layout
            }
            // cny more significant than cnx
            if(cnmorsig == 2){
                // oc is movement to the right
                if(oldcurx >= 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                        TextDirections.set(i, "Turn left in current hallway " + "\n");
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                        TextDirections.set(i, "Turn right in current hallway " + "\n");
                    }
                }
                // oc is movement to the left
                if(oldcurx < 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                        TextDirections.set(i, "Turn right in current hallway " + "\n");
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                        TextDirections.set(i, "Turn left in current hallway " + "\n");
                    }
                }
            }
        }

        // ocy more significant than ocx
        if(ocmorsig == 2){
            // cnx more significant than cny
            if(cnmorsig == 1){
                // oc is movement downwards
                if(oldcury >= 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                        TextDirections.set(i, "Turn right in current hallway " + "\n");
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                        TextDirections.set(i, "Turn left in current hallway " + "\n");
                    }
                }
                // oc is movement upwards
                if(oldcury < 0){
                    // ocn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                        TextDirections.set(i, "Turn left in current hallway " + "\n");
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                        TextDirections.set(i, "Turn right in current hallway " + "\n");
                    }
                }
            }
            // cny more significant than cnx
            if(cnmorsig == 2){
                //shouldnt happen due to node layout
            }
        }
        return TextDirections;
    }

    //Helper function to update curdir for other functions
    private String setCurdir(Point oldnode, Point currnode, Point nextnode,
                             String curdir, int i){
        double oldnodex = oldnode.getX();
        double oldnodey = oldnode.getY();
        double currentnodex = currnode.getX();
        double currentnodey = currnode.getY();
        double nextnodex = nextnode.getX();
        double nextnodey = nextnode.getY();

        double oldcurx = currentnodex - oldnodex;
        double oldcury = currentnodey - oldnodey;
        double curnextx = nextnodex - currentnodex;
        double curnexty = nextnodey - currentnodey;

        double ocmorsig = 0;
        double cnmorsig = 0;

        ocmorsig = moreSignificantChange(oldcurx, oldcury);
        cnmorsig = moreSignificantChange(curnextx, curnexty);

        //System.out.println("Curdir getText " + ocmorsig + "" + cnmorsig);

        // ocx more significant than ocy
        if(ocmorsig == 1){
            // cnx more significant than cny
            if(cnmorsig == 1){
                // oc is movement to the right
                if(oldcurx >= 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                        }
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        // turn around case shouldnt activate
                    }
                }
                // oc is movement to the left
                if(oldcurx < 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        //turn around case
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                        }
                    }
                }
            }
            // cny more significant than cnx
            if(cnmorsig == 2){
                // oc is movement to the right
                if(oldcurx >= 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                    }
                }
                // oc is movement to the left
                if(oldcurx < 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                    }
                }
            }
        }

        // ocy more significant than ocx
        if(ocmorsig == 2){
            // cnx more significant than cny
            if(cnmorsig == 1){
                // oc is movement downwards
                if(oldcury >= 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                    }
                }
                // oc is movement upwards
                if(oldcury < 0){
                    // ocn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                    }
                }
            }
            // cny more significant than cnx
            if(cnmorsig == 2){
                // oc is movement downwards
                if(oldcury >= 0){
                    // cn is movement upwards
                    if(curnexty < 0){
                        // turn around case
                    }
                    // cn is movement downwards
                    if(curnexty >= 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                        }
                    }
                }
                // oc is movement upwards
                if(oldcury < 0){
                    // cn is movement upwards
                    if(curnexty < 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                        }
                    }
                    // cn is movement downwards
                    if(curnexty >= 0){
                        //turn around case
                    }
                }
            }
        }
        return curdir;
    }

    //Helper function to generate text for most cases but in spanish
    private LinkedList<String> getTextEsp(Point oldnode, Point currnode, Point nextnode,
                                       String curdir, LinkedList<String> TextDirections, int i ){
        //Get values
        double oldnodex = oldnode.getX();
        double oldnodey = oldnode.getY();
        double currentnodex = currnode.getX();
        double currentnodey = currnode.getY();
        double nextnodex = nextnode.getX();
        double nextnodey = nextnode.getY();

        //System.out.println(oldnodex + "" + oldnodey);
        //Get transitions
        double oldcurx = currentnodex - oldnodex;
        double oldcury = currentnodey - oldnodey;
        double curnextx = nextnodex - currentnodex;
        double curnexty = nextnodey - currentnodey;

        double ocmorsig = 0;
        double cnmorsig = 0;

        //Get orientations
        ocmorsig = moreSignificantChange(oldcurx, oldcury);
        cnmorsig = moreSignificantChange(curnextx, curnexty);

        //System.out.println("Regular getText " + ocmorsig + "" + cnmorsig);

        // ocx more significant than ocy
        if(ocmorsig == 1){
            // cnx more significant than cny
            if(cnmorsig == 1){
                // oc is movement to the right
                if(oldcurx >= 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                            TextDirections.set(i, "ir recto hasta el final del pasillo " + "\n");
                        }
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        // turn around case shouldnt activate
                    }
                }
                // oc is movement to the left
                if(oldcurx < 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        //turn around case
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                            TextDirections.set(i, "ir recto hasta el final del pasillo " + "\n");
                        }
                    }
                }
            }
            // cny more significant than cnx
            if(cnmorsig == 2){
                // oc is movement to the right
                if(oldcurx >= 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la izquierda " + "\n");
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la derecha " + "\n");
                    }
                }
                // oc is movement to the left
                if(oldcurx < 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la derecha " + "\n");
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la izquierda " + "\n");
                    }
                }
            }
        }

        // ocy more significant than ocx
        if(ocmorsig == 2){
            // cnx more significant than cny
            if(cnmorsig == 1){
                // oc is movement downwards
                if(oldcury >= 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la derecha " + "\n");
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la izquierda " + "\n");
                    }
                }
                // oc is movement upwards
                if(oldcury < 0){
                    // ocn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la izquierda " + "\n");
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la derecha " + "\n");
                    }
                }
            }
            // cny more significant than cnx
            if(cnmorsig == 2){
                // oc is movement downwards
                if(oldcury >= 0){
                    // cn is movement upwards
                    if(curnexty < 0){
                        // turn around case
                    }
                    // cn is movement downwards
                    if(curnexty >= 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                            TextDirections.set(i, "ir recto hasta el final del pasillo " + "\n");
                        }
                    }
                }
                // oc is movement upwards
                if(oldcury < 0){
                    // cn is movement upwards
                    if(curnexty < 0){
                        if(curdir != "Straight"){
                            curdir = "Straight";
                            TextDirections.set(i, "ir recto hasta el final del pasillo " + "\n");
                        }
                    }
                    // cn is movement downwards
                    if(curnexty >= 0){
                        //turn around case
                    }
                }
            }
        }
        //Return the updated text directions
        return TextDirections;
    }

    //Helper function to generate text for case of reaching an elevator or the destination
    private LinkedList<String> getTextMidHallwayEsp(Point oldnode, Point currnode, Point nextnode,
                                                 String curdir, LinkedList<String> TextDirections, int i ){
        double oldnodex = oldnode.getX();
        double oldnodey = oldnode.getY();
        double currentnodex = currnode.getX();
        double currentnodey = currnode.getY();
        double nextnodex = nextnode.getX();
        double nextnodey = nextnode.getY();

        double oldcurx = currentnodex - oldnodex;
        double oldcury = currentnodey - oldnodey;
        double curnextx = nextnodex - currentnodex;
        double curnexty = nextnodey - currentnodey;

        double ocmorsig = 0;
        double cnmorsig = 0;

        ocmorsig = moreSignificantChange(oldcurx, oldcury);
        cnmorsig = moreSignificantChange(curnextx, curnexty);

        //System.out.println("Edge getText " + ocmorsig + "" + cnmorsig);

        // ocx more significant than ocy
        if(ocmorsig == 1){
            // cnx more significant than cny
            if(cnmorsig == 1){
                // shouldnt happen in node layout
            }
            // cny more significant than cnx
            if(cnmorsig == 2){
                // oc is movement to the right
                if(oldcurx >= 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la izquierda en el pasillo actual " + "\n");
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la derecha en el pasillo actual " + "\n");
                    }
                }
                // oc is movement to the left
                if(oldcurx < 0){
                    // cn is movement downwards
                    if(curnexty >= 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la derecha en el pasillo actual " + "\n");
                    }
                    // cn is movement upwards
                    if(curnexty < 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la izquierda en el pasillo actual " + "\n");
                    }
                }
            }
        }

        // ocy more significant than ocx
        if(ocmorsig == 2){
            // cnx more significant than cny
            if(cnmorsig == 1){
                // oc is movement downwards
                if(oldcury >= 0){
                    // cn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la derecha en el pasillo actual " + "\n");
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la izquierda en el pasillo actual " + "\n");
                    }
                }
                // oc is movement upwards
                if(oldcury < 0){
                    // ocn is movement to the right
                    if(curnextx >= 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la izquierda en el pasillo actual " + "\n");
                    }
                    // cn is movement to the left
                    if(curnextx < 0){
                        curdir = "";
                        TextDirections.set(i, "dobla a la derecha en el pasillo actual " + "\n");
                    }
                }
            }
            // cny more significant than cnx
            if(cnmorsig == 2){
                //shouldnt happen due to node layout
            }
        }
        return TextDirections;
    }
}
