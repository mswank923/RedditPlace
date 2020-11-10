package place.server;

import place.PlaceBoard;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Class PlaceServer
 * This class houses the main ServerSocket and waits for connections to it. Once a connection has been established, the
 * server spawns a PlaceServerThread to manage it further.
 */
public class PlaceServer {

    /** Main method
     * This is where everything in the class happens.
     * @param args The arguments taken from the command line.
     */
    public static void main(String[] args) {
        //check command line arguments for proper format and set them to variables
        if (args.length != 2) {
            System.err.println("Usage: java PlaceServer port DIM");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        int DIM = Integer.parseInt(args[1]);

        if (!(DIM >= 1)) {
            System.err.println("DIM must be greater than or equal to 1");
            System.exit(1);
        }

        //create the board and a server log to handle users
        PlaceBoard board = new PlaceBoard(DIM);
        ServerLog log = new ServerLog();

        //create the server's socket and wait for connections
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Now accepting connections...");
            while (true) {
                //send each server thread the board and the server's log class
                new PlaceServerThread(serverSocket.accept(), board, log).start();
                System.out.println("User connected!");
            }
        } catch (IOException e) {
            System.err.println("Caught IOException");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
