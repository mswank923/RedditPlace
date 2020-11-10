package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static place.network.PlaceRequest.RequestType.*;

/**
 * Class PlaceServerThread
 * A thread class that spawns to handle server-to-client interactions whenever a user connects. Receives login request,
 * then sends the board. After setup, waits for tile change requests from the client.
 */
public class PlaceServerThread extends Thread {

    private Socket socket;
    private ServerLog log;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    private PlaceBoard board;
    private String username;

    /**
     * Constructor method. Gets the socket, server log, and board from the server.
     * @param socket the client's socket
     * @param board the board from the server
     * @param log the server's log
     */
    PlaceServerThread(Socket socket, PlaceBoard board, ServerLog log) {
        super("PlaceServerThread");
        this.socket = socket;
        this.log = log;
        this.board = board;
        this.username = "";
    }

    /**
     * run
     * The thread's looping run method. Manages login request from user and then waits for tile change requests.
     */
    public void run() {
        try {
            //---------------------------SETUP---------------------------
            //Establish I/O streams
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            //receive login response from user
            PlaceRequest<?> loginRequest = (PlaceRequest<?>) input.readUnshared();

            if (loginRequest.getType() == LOGIN) {
                //handle login
                username = (String) loginRequest.getData();
                if (log.logUserIn(username, output)) {
                    System.out.println("User " + username + " successfully logged in.");
                } else {
                    System.out.println("Attempted login with duplicate username: " + username);
                    sendError("Username \"" + username + "\" already taken.");
                }
            } else {
                System.out.println("Unexpected Error - Received non-LOGIN request from user.");
                sendError("Received non-login request");
            }

            //tell client that login was successful
            PlaceRequest<String> loginResponse =
                    new PlaceRequest<>(LOGIN_SUCCESS, "User: " + username + " successfully logged in.");
            sendResponse(loginResponse);

            //send the client the board
            PlaceRequest<PlaceBoard> boardResponse = new PlaceRequest<>(BOARD, board);
            sendResponse(boardResponse);

            //--------------------------MAINLOOP--------------------------
            //accept tile changes
            Object inputObject;
            while ((inputObject = input.readUnshared()) != null) { //should loop here for the rest of execution
                PlaceRequest<?> request = (PlaceRequest<?>) inputObject;
                if (request.getType() == CHANGE_TILE) {
                    //perform tile change
                    PlaceTile tile = (PlaceTile) request.getData();
                    board.setTile(tile);
                    PlaceRequest<PlaceTile> tileResponse = new PlaceRequest<>(TILE_CHANGED, tile);
                    log.broadcast(tileResponse);

                    try {
                        sleep(500); //sleep for 500ms to prevent spam
                    } catch (InterruptedException e) {
                        System.err.println("Caught InterruptedException");
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Unexpected Error: Received non-CHANGE_TILE request from user.");
                }
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("Caught IOException");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Caught ClassNotFoundException");
            e.printStackTrace();
        }
    }

    /**
     * sendResponse
     * Helper method to simply send a response using the thread's output stream.
     * @param response the response to be sent
     */
    private void sendResponse(PlaceRequest response) {
        try {
            output.writeUnshared(response);
            output.flush();
        } catch (IOException e) {
            System.err.println("Caught IOException");
            e.printStackTrace();
        }
    }

    /**
     * errorMessage
     * Helper method to send an error message to the client.
     * @param errorMessage the message to be sent
     */
    private void sendError (String errorMessage){
        PlaceRequest<String> errorResponse = new PlaceRequest<>(ERROR, errorMessage);
        sendResponse(errorResponse);
    }
}
