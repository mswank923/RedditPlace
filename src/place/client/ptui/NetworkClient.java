package place.client.ptui;

import place.PlaceBoard;
import place.PlaceTile;
import place.client.model.PlaceModel;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.net.UnknownHostException;

import static java.lang.Thread.sleep;
import static place.network.PlaceRequest.RequestType.*;

/**
 * Class NetworkClient
 * This class is the network end of the client, which manages client-to-server interactions. Communicates the UI's
 * requests to the server and the server's responses to the UI.
 */
public class NetworkClient {

    private Socket socket;
    private ObjectOutputStream networkOut;
    private ObjectInputStream networkIn;

    private PlaceModel model;

    /**
     * Constructor method. Gets the hostname, port number, username, and model from the UI and initializes I/O
     * streams. Also sends login request to the server and manages its responses. Ends by spawning a thread to send
     * tile change requests from the UI to the server.
     * @param hostName hostname of the server
     * @param portNumber port number to be connected to
     * @param username username of the client
     * @param model the client's copy of the model
     */
    public NetworkClient(String hostName, int portNumber, String username, PlaceModel model) {
        try {
            this.socket = new Socket(hostName, portNumber);
            this.networkOut = new ObjectOutputStream(socket.getOutputStream());
            this.networkIn = new ObjectInputStream(socket.getInputStream());
            this.model = model;
        } catch (UnknownHostException e) {
            System.err.println("Caught UnknownHostException");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Caught IOException");
            e.printStackTrace();
        }

        //send login request to server
        PlaceRequest<String> loginRequest = new PlaceRequest<>(LOGIN, username);
        sendRequest(loginRequest);

        //loop until login and board responses have been processed
        boolean receivedBoard = false;
        while (!receivedBoard) {
            try {
                PlaceRequest<?> response = (PlaceRequest<?>) networkIn.readUnshared();
                //check response type and handle accordingly
                if (response.getType() == LOGIN_SUCCESS) {
                    String message = (String) response.getData();
                    System.out.println(message);
                } else if (response.getType() == BOARD) {
                    this.model.setBoard((PlaceBoard) response.getData());
                    System.out.println("Successfully received board from server.");
                    receivedBoard = true;
                } else if (response.getType() == ERROR) {
                    String errorMessage = (String) response.getData();
                    System.err.println("Error: " + errorMessage);
                    System.exit(1);
                } else {
                    System.err.println("Unexpected Error - Received unexpected response from server.");
                    System.exit(1);
                }
            } catch (StreamCorruptedException e) {
                //expected :P
            } catch (IOException e) {
                System.err.println("Caught IOException");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("Caught ClassNotFoundException");
                e.printStackTrace();
            }

        }

        //start thread that will process tile changes
        Thread netThread = new Thread(this::run);
        netThread.start();
    }

    /**
     * sendRequest
     * Helper method to send a request to the server thread with the client's output stream.
     * @param request the request to be forwarded to the server
     */
    private void sendRequest(PlaceRequest request) {
        try{
            networkOut.writeUnshared(request);
            networkOut.flush();
        } catch (IOException e) {
            System.err.println("Caught IOException");
            e.printStackTrace();
        }
    }

    /**
     * close
     * Simple function that closes the client's socket for shutdown.
     */
    void close() {
        try {
            this.socket.close();
        } catch (IOException e) {
            System.err.println("Caught IOException");
            e.printStackTrace();
        }
    }

    /**
     * run
     * The thread's looping method. Its purpose is to wait for tile change requests from the UI and forward them to
     * the server thread.
     */
    private void run() {
        try {
            //get request from UI
            Object inputObject;
            while ((inputObject = networkIn.readUnshared()) != null) {
                PlaceRequest<?> response = (PlaceRequest<?>) inputObject;
                //check if request is tile change and handle accordingly
                if (response.getType() == TILE_CHANGED) {
                    System.out.println("A user changed a tile.");
                    PlaceTile tile = (PlaceTile) response.getData();
                    model.setTile(tile);
                } else {
                    System.err.println("Unexpected Error - Received unexpected response from server.");
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            System.err.println("Caught IOException");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Caught ClassNotFoundException");
            e.printStackTrace();
        }
        this.close();
    }

    /**
     * tileChange
     * Helper method to forward a tile change request to the server. Called by the UI when the user makes a tile
     * change.
     * @param tile the new tile to be sent
     */
    public void tileChange(PlaceTile tile) {
        PlaceRequest<PlaceTile> tileChangeRequest = new PlaceRequest<>(CHANGE_TILE, tile);
        sendRequest(tileChangeRequest);
        try {
            sleep(500);
        } catch (InterruptedException e) {
            System.err.println("Caught InterruptedException");
            e.printStackTrace();
        }
    }

}
