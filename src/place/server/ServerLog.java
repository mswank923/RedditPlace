package place.server;

import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Class ServerLog
 * This class is a helper class for the server and its threads. It manages which users are logged in and out and also
 * contains a broadcast function which sends a message (tile updates) to every user.
 */
class ServerLog {
    private ArrayList<String> userList;
    private ArrayList<ObjectOutputStream> userOutputs;

    /**
     * Constructor method. Initializes the two lists which will track users and their output streams.
     */
    ServerLog() {
        userList = new ArrayList<>();
        userOutputs = new ArrayList<>();
    }

    /**
     * logUserIn
     * Checks whether or not the username is valid, then adds the users name and output stream to the lists if so.
     * This method is called from PlaceServerThread when it sends a login request to the server.
     * @param username the user's username
     * @param output the user's output stream (so that they can be broadcast to)
     * @return whether or not the user was successfully logged in
     */
    synchronized boolean logUserIn(String username, ObjectOutputStream output) {
        if (!userList.contains(username)) {
            this.userList.add(username);
            this.userOutputs.add(output);
            return true;
        }
        return false;
    }

    /**
     * broadcast
     * Sends a request to every user connected to the server.
     * @param request the data to be sent
     * @throws IOException
     */
    synchronized void broadcast(PlaceRequest<?> request) throws IOException {
        for (ObjectOutputStream output : userOutputs) {
            output.writeUnshared(request);
            output.flush();
        }
    }
}
