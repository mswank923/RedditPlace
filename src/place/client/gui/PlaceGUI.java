package place.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import place.PlaceColor;
import place.PlaceTile;
import place.client.model.PlaceModel;
import place.client.ptui.NetworkClient;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Class PlaceGUI
 * GUI version of PlacePTUI. Allows full functionality of the Place Application through a Graphic User Interface window.
 */
public class PlaceGUI extends Application implements Observer {

    private NetworkClient connection;

    private PlaceModel localModel;

    private String username;
    private Label label = new Label();
    private int colorNum;
    private Button[][] buttonGrid;

    /**
     * init
     * Initialization method for the application. Performs first-time setup similarly to a constructor. Gets arguments
     * from the command line and sets them to variables. Also creates a new NetworkClient to handle client-to-server
     * interaction and begins observing updates to the local copy of the model.
     */
    public void init() {
        List<String> args = getParameters().getRaw();

        String hostname = args.get(0);
        int portNumber = Integer.parseInt(args.get(1));
        username = args.get(2);

        localModel = new PlaceModel();
        connection = new NetworkClient(hostname, portNumber, username, localModel);
        localModel.addObserver(this);
    }

    /**
     * start
     * Builds entire GUI stage.
     * @param mainStage the stage
     */
    public void start(Stage mainStage) {
        //initialize empty panes
        GridPane gridPane = new GridPane();
        BorderPane borderPane = new BorderPane();
        VBox colorPanel = new VBox();

        //dimension is used a lot so set it to a variable
        int DIM = this.localModel.getBoard().DIM;
        buttonGrid = new Button[DIM][DIM];

        //build the blank starting grid of buttons
        for (int row = 0; row < DIM; row++) {
            for (int col = 0; col < DIM; col++) {
                //make a new button and set its size
                Button button = new Button("");
                button.setPadding(Insets.EMPTY);
                button.setMinSize(60, 60);
                button.setPrefSize(40, 40);

                //get background color of the button from the tile in the model
                PlaceTile tile = localModel.getBoard().getTile(row, col);
                PlaceColor tileColor = tile.getColor();
                Color color = Color.rgb(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue(), 1);
                button.setBackground(new Background(new BackgroundFill(color, new CornerRadii(0), new Insets(0))));

                //generate default tooltip
                String tileInfo = "(" + tile.getRow() + ", " + tile.getCol() + ") " + tile.getColor().getName()
                        + "Has not been changed";
                //add tooltip to button
                Tooltip tool = new Tooltip(tileInfo);
                button.setTooltip(tool);
                //add button press funcionality
                button.setOnAction(new ButtonEventHandler(row, col));
                //add button to the GUI
                gridPane.add(button, row, col);
                buttonGrid[row][col] = button;
            }
        }

        //build panel of colors for color selection
        for (PlaceColor color : PlaceColor.values()) {
            //create the button and set its size
            ToggleButton toggleButton = new ToggleButton();
            toggleButton.setMinSize(40, 40);
            toggleButton.setMaxSize(50, 50);

            //set background color of button
            Color color2 = Color.rgb(color.getRed(), color.getGreen(), color.getBlue(), 1);
            toggleButton.setBackground(new Background(new BackgroundFill(color2, new CornerRadii(0), new Insets(0))));

            //add button press functionality
            int colorNum = color.getNumber();
            toggleButton.setOnAction(new ToggleButtonEventHandler(colorNum));

            //add scaling to the button and add it to the panel
            VBox.setVgrow(toggleButton, Priority.ALWAYS);
            colorPanel.getChildren().add(toggleButton);
        }

        //configure the grid and add all components to the border pane
        gridPane.setGridLinesVisible(true);
        borderPane.setTop(label);
        borderPane.setCenter(gridPane);
        borderPane.setLeft(colorPanel);

        //finalize the scene and display it
        mainStage.setScene(new Scene(borderPane));
        mainStage.setTitle("Place: " + username);
        mainStage.show();
    }

    /**
     * Class ButtonEventHandler
     * Handler class to recognize clicks on the grid buttons and change the tile accordingly.
     */
    public class ButtonEventHandler implements EventHandler<ActionEvent> {
        public final int row;
        public final int col;

        /**
         * Constructor method. Sets up some variables.
         * @param row the row of the clicked button
         * @param col the column of the clicked button
         */
        public ButtonEventHandler(int row, int col) {
            this.row = row;
            this.col = col;
        }

        /**
         * handle
         * Implementation of parent class's handle() method. Sends tile change request to the network client when user
         * clicks on a tile.
         * @param event not used
         */
        public void handle(ActionEvent event) {
            PlaceTile tile = new PlaceTile(row, col, username, PlaceColor.values()[colorNum]);
            connection.tileChange(tile);
        }
    }

    /**
     * Class ToggleButtonEventHandler
     * Handler class to recognize clicks on the color panel buttons and set color accordingly.
     */
    public class ToggleButtonEventHandler implements EventHandler<ActionEvent> {
        public final int selectColor;

        /**
         * Constructor method.
         * @param colorNum the number of the color of the button
         */
        public ToggleButtonEventHandler(int colorNum) {
            selectColor = colorNum;
        }

        /**
         * handle
         * Implementation of parent class's handle() method. Changes the user's selected color and the label when the
         * user picks a new color.
         * @param event not used
         */
        public void handle(ActionEvent event) {
            colorNum = selectColor;
            label.setText("Color: " + PlaceColor.values()[colorNum].getName());
        }
    }

    /**
     * refresh
     * Updates the GUI with a new tile. Called from update whenever an update is observed on the model.
     * @param tile the new tile
     */
    private void refresh(PlaceTile tile) {
        //make a new tile with the selected color
        PlaceColor tileColor = tile.getColor();
        Color color = Color.rgb(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue(), 1);
        buttonGrid[tile.getRow()][tile.getCol()].setBackground(
                new Background(new BackgroundFill(color, new CornerRadii(0), new Insets(0))));

        //generate a new tooltip for the tile
        String tileInfo = "(" + tile.getRow() + ", " + tile.getCol() + ") " + tile.getColor().getName()
                + "\nUpdated by " + tile.getOwner() + " at " + tile.getTime();
        Tooltip toolTip = new Tooltip(tileInfo);
        buttonGrid[tile.getRow()][tile.getCol()].setTooltip(toolTip);
    }

    /**
     * update
     * Called when local model is updated since this is an observer. Calls refresh with the new tile when an update is
     * recognized on the model.
     * @param o not used
     * @param arg
     */
    public void update(Observable o, Object arg) {
        assert this.localModel == o : "Update from non-model Observable";
        PlaceTile updatedTile = (PlaceTile) arg;
        if (Platform.isFxApplicationThread()) {
            this.refresh(updatedTile);
        } else {
            Platform.runLater(() -> this.refresh(updatedTile));
        }
    }

    /**
     * Main method.
     * Checks if command line arguments are valid and launches the application.
     * @param args arguments from the command line
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceGUI host port username");
            System.exit(0);
        } else {
            Application.launch(args);
        }
    }
}
