package place.network.test;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import static place.network.PlaceRequest.RequestType.*;

public class TestClient {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("incorrect args usage");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String username = args[2];

        try (Socket clientSocket = new Socket(hostName, portNumber)) {
            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());

            //send login request
            System.out.println("Sending login request to server...");
            sendRequest(output, LOGIN, username);

            //retrieve the successful login from server
            String loginMessage = (String) receiveRequest(input);

            //receive and display the initial board
            PlaceBoard board = (PlaceBoard) receiveRequest(input);
            System.out.println(board);

        } catch (Exception e) {}
    }

    private static Object receiveRequest(ObjectInputStream input) {
        //get response object and type from the server
        PlaceRequest<?> response = null;
        PlaceRequest.RequestType responseType = null;
        try {
            response = (PlaceRequest<?>) input.readUnshared();
            responseType = response.getType();
        } catch (IOException e) {
        } catch (ClassNotFoundException e) { }

        //check response type and handle respectively
        Object o = null;
        if (responseType == LOGIN_SUCCESS || responseType == ERROR) {
            String message = (String) response.getData();
            System.out.println(message);
            o = message;
        } else if (responseType == BOARD) {
            PlaceBoard board = (PlaceBoard) response.getData();
            System.out.println("Received board from server.");
            o = board;
        } else if (responseType == TILE_CHANGED) {
            PlaceTile tile = (PlaceTile) response.getData();
            System.out.println("Received tile from server.");
            o = tile;
        } else {
            System.err.println("Unrecognizable response type. Return object is now null.");
        }
        return o;
    }

    private static void sendRequest(ObjectOutputStream output, PlaceRequest.RequestType requestType, Object o) {
        //set the new request to the correct class and request type
        PlaceRequest<?> newRequest;
        if (requestType == CHANGE_TILE) {
            PlaceTile tile = (PlaceTile) o;
            newRequest = new PlaceRequest<>(CHANGE_TILE, tile);
        } else if (requestType == LOGIN) {
            String username = (String) o;
            newRequest = new PlaceRequest<>(LOGIN, username);
        } else {
            System.err.println("Unrecognizable request type found");
            return;
        }

        //send off the newly constructed request
        try {
            output.writeUnshared(newRequest);
        } catch (IOException e) { }
    }

}
