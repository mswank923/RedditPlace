package place.client.model;

import place.PlaceBoard;
import place.PlaceTile;

import java.util.Observable;

/**
 * Wrapper class for the PlaceBoard. Acts as the model which is observed by the UI. Contains getters and setters for
 * making changes to the board.
 */
public class PlaceModel extends Observable {

    private PlaceBoard board;

    /**
     * getBoard
     * Accessor method for getting the model's board.
     * @return the model's board
     */
    public PlaceBoard getBoard() {
        return board;
    }

    /**
     * setBoard
     * Sets the model's board to the new board. Used by the network client when it receives the board from the server.
     * @param board the new board
     */
    public void setBoard(PlaceBoard board) {
        this.board = board;
    }

    /**
     * setTile
     * Helper method for setting a tile on the board to the given tile. This is where observers are updated/notified.
     * @param tile the new tile
     */
    public void setTile(PlaceTile tile) {
        board.setTile(tile);
        super.setChanged();
        super.notifyObservers(tile);
    }

}
