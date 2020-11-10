package place.client.ptui;

import place.PlaceColor;
import place.PlaceTile;
import place.client.model.PlaceModel;

import java.util.ArrayList;

/**
 * Class DickBot
 * Bot that draws a dick in the middle of the board and prevents users from editing it.
 */
public class DickBot {

    private static PlaceModel localModel;

    /**
     * Main method. Takes program arguments, then draws and protects the dick.
     * @param args arguments from the command line
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage java DickBot host port color");
            System.exit(0);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        int colorNum = Integer.parseInt(args[2]);
        if (!(0 <= colorNum && colorNum <= 15)) {
            System.err.println("Invalid Color. Enter numbers from 0-15");
        }

        localModel = new PlaceModel();
        NetworkClient connection = new NetworkClient(hostName, portNumber, "DickBot", localModel);

        //make tile that dick will be made from
        PlaceColor tileColor = PlaceColor.values()[colorNum];

        //draw the dick
        ArrayList<Coordinate> coords = draw(localModel.getBoard().DIM, colorNum, connection);

        //protect the dick
        protect(coords, tileColor, connection);
    }

    /**
     * draw
     * Helper method to calculate the coordinate of the center of the board and then draw the dick there.
     * @param DIM dimension of the board
     * @param colorNum number of the color to draw in
     * @param connection connection to the server for forwarding changes
     * @return an ArrayList of Coordinates which is the instructions of where to draw the dick.
     */
    private static ArrayList<Coordinate> draw(int DIM, int colorNum, NetworkClient connection) {
        //check if board is large enough for the dick to be drawn
        if (DIM < 4) {
            System.err.println("Board can't handle the dick.");
            System.exit(1);
        }

        //set a starting coordinate in the middle of the board
        Coordinate start;
        if (DIM % 2 == 0) { //if DIM is even
            int half = DIM / 2;
            start = new Coordinate(half - 1, half - 1);
        } else { //if DIM is odd
            int half = (DIM - 1) / 2 - 1;
            start = new Coordinate(half, half);
        }

        //generate dick coordinates
        ArrayList<Coordinate> dickCoords = new ArrayList<>();
        dickCoords.add(new Coordinate(start.row - 1, start.col - 1));
        dickCoords.add(new Coordinate(start.row - 1, start.col + 1));
        dickCoords.add(start);
        dickCoords.add(new Coordinate(start.row + 1, start.col));
        dickCoords.add(new Coordinate(start.row + 2, start.col));

        //loop through coords and send new dick tiles to server
        for (Coordinate coord : dickCoords) {
            //perform tile change
            PlaceColor color = PlaceColor.values()[colorNum];
            PlaceTile tile =
                    new PlaceTile(coord.row, coord.col, "DickBot", color, System.currentTimeMillis());
            connection.tileChange(tile); //send tile change to the server
            localModel.setTile(tile);
        }
        return dickCoords;
    }

    /**
     * protect
     * Method that loops infinitely and re-draws the dick if it has been altered.
     * @param coords location of the dick tiles
     * @param tileColor color that the dick is supposed to be
     * @param connection connection to the server for forwarding changes
     */
    private static void protect(ArrayList<Coordinate> coords, PlaceColor tileColor, NetworkClient connection) {
        while (true) {
            for (Coordinate coord : coords) {
                if (localModel.getBoard().getTile(coord.row, coord.col).getColor() != tileColor) {
                    PlaceTile tile =
                            new PlaceTile(coord.row, coord.row, "DickBot", tileColor, System.currentTimeMillis());
                    connection.tileChange(tile); //send tile change to the server
                }
            }
        }
    }
}

/**
 * Class Coordinate
 * Helper class to store a row and column of something.
 */
class Coordinate {
    int row;
    int col;

    /**
     * Constructor method. Sets row and column.
     * @param row the row
     * @param col the column
     */
    Coordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }
}
