package place.network.test;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static place.network.PlaceRequest.RequestType.*;

public class TestServer {

    private static PlaceBoard board;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("improper usage of args");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);  //set port and dimension to variables
        int DIM = Integer.parseInt(args[1]);

        if (!(DIM >= 1)) {  //check if DIM is greater than or equal to 1
            System.err.println("DIM must be greater than or equal to 1");
            System.exit(1);
        }

        ServerSocket serverSocket;
        Socket clientSocket;
        try {
            //set up server socket
            serverSocket = new ServerSocket(portNumber);

            //wait for the client to connect
            System.out.println("Waiting for client to connect...");
            clientSocket = serverSocket.accept();

            //initialize I/O streams
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());

            //receive the login request from the client
            receiveRequest(input);

            //send back that the login was successful, followed by the board
            sendRequest(output, LOGIN_SUCCESS, null);
            sendRequest(output, BOARD, null);

        } catch (IOException e) {
        }
    }

    private static void sendRequest(ObjectOutputStream output, PlaceRequest.RequestType requestType, Object o) {
        //set the new request to the correct class and request type
        PlaceRequest<?> newRequest;
        if (requestType == LOGIN_SUCCESS) {
            String message = "Server: Login Succeeded.";
            newRequest = new PlaceRequest<>(LOGIN_SUCCESS, message);
        } else if (requestType == BOARD) {
            newRequest = new PlaceRequest<>(BOARD, board);
        } else if (requestType == TILE_CHANGED) {
            PlaceTile tile = (PlaceTile) o;
            newRequest = new PlaceRequest<>(TILE_CHANGED, tile);
        } else if (requestType == ERROR) {
            String errorMessage = (String) o;
            newRequest = new PlaceRequest<>(ERROR, errorMessage);
        } else {
            System.err.println("Unrecognizable request type found");
            return;
        }

        //send off the newly constructed request
        try {
            output.writeUnshared(newRequest);
            output.flush();
        } catch (IOException e) { }
    }

    private static Object receiveRequest(ObjectInputStream input) {
        //get response object and type from the client
        PlaceRequest<?> response = null;
        PlaceRequest.RequestType responseType = null;
        try {
            response = (PlaceRequest<?>) input.readUnshared();
            responseType = response.getType();
        } catch (IOException e) {
        } catch (ClassNotFoundException e) { }

        //check response type and handle respectively
        Object o = null;
        if (responseType == CHANGE_TILE) {
            PlaceTile newTile = (PlaceTile) response.getData();
            o = newTile;
            board.setTile((PlaceTile) o);
        } else if (responseType == LOGIN) {
            String username = (String) response.getData();
            System.out.println("User logging in with username: " + username);
            o = username;
        } else {
            System.err.println("Unrecognizable response type. Return object is now null.");
        }
        return o;
    }
}
