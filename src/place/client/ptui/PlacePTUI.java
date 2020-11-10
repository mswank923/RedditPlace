package place.client.ptui;

import place.PlaceColor;
import place.PlaceTile;
import place.client.model.PlaceModel;

import java.io.PrintWriter;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

/**
 * Class PlacePTUI
 * The Plain Text User Interface class that is used as a basis for the GUI. Allows full functionality of the Place
 * Application through console interaction.
 */
public class PlacePTUI extends ConsoleApplication implements Observer {

    private PlaceModel localModel;
    private String username;

    private NetworkClient connection;
    private Scanner userIn;
    private PrintWriter userOut;

    /**
     * init
     * Implementation of ConsoleApplication's init() method. Performs first-time setup similarly to a constructor. Gets
     * arguments from the command line and sets them to variables. Also creates a new NetworkClient to handle
     * client-to-server interaction and begins observing updates to the local copy of the model.
     */
    public void init() {
        List<String> args = super.getArguments();
        String hostName = args.get(0);
        int portNumber = Integer.parseInt(args.get(1));
        username = args.get(2);

        localModel = new PlaceModel();
        this.connection = new NetworkClient(hostName, portNumber, username, this.localModel);
        this.localModel.addObserver(this);
    }

    /**
     * go
     * Thread-safe implementation of ConsoleApplication's go() method. Runs the main loop of the console application,
     * prompting the user for text input and displaying text output.
     * @param userIn Scanner to be used for user input
     * @param userOut Location for the application to output text to
     */
    public synchronized void go(Scanner userIn, PrintWriter userOut) {
        this.userIn = userIn;
        this.userOut = userOut;

        this.userOut.println("You may change tiles by entering three integers: [row] [column] [color]");
        //get user's input
        String inputLine;
        while ((inputLine = userIn.nextLine()) != null) {
            //process user's input
            String[] args = inputLine.split(" ");
            if (args[0].equals("-1")) { //user wishes to shut down
                this.userOut.println("Shutting down...");
                connection.close();
                System.exit(0);
            } else if (args.length == 3) { //possible tile change
                int row = Integer.parseInt(args[0]);
                int col = Integer.parseInt(args[1]);
                int colorNum = Integer.parseInt(args[2]);

                //check if arguments are valid values
                if (0 <= row && row < localModel.getBoard().DIM && 0 <= col && col < localModel.getBoard().DIM) {
                    if (0 <= colorNum && colorNum <= 15) {
                        //perform tile change
                        PlaceColor color = PlaceColor.values()[colorNum];
                        PlaceTile tile = new PlaceTile(row, col, username, color, System.currentTimeMillis());
                        connection.tileChange(tile); //send tile change to the server
                    } else {
                        this.userOut.println("Unrecognizable color. Enter only numbers 0-15.");
                    }
                } else {
                    this.userOut.println("Argument out of bounds. Row and Column can only be 0-"
                            + localModel.getBoard().DIM);
                }
            } else {
                this.userOut.println("Unrecognizable command. Usage: [row] [col] [color]");
            }
        }
        //code should not get here, this is here for bug testing
        this.userOut.println("BROKE OUT OF LOOP!");
    }

    /**
     * update
     * Called when local model is updated since this is an observer. Since the model is only updated when a tile is
     * changed, this method prints the board for the user for every tile change.
     * @param o not used
     * @param arg not used
     */
    public void update(Observable o, Object arg) {
        assert o == this.localModel: "Update from non-model Observable";
        //PlaceTile newTile = (PlaceTile) arg;
        //localModel.getBoard().setTile(newTile);
        this.userOut.println(localModel.getBoard());
    }

    /**
     * Main method. Checks if command line arguments are valid and launches the application.
     * @param args arguments from command line
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage java PlacePTUI host port username");
            System.exit(0);
        } else {
            ConsoleApplication.launch(PlacePTUI.class, args);
        }
    }
}

