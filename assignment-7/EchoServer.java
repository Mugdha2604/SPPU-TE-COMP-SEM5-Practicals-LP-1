import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A multithreaded echo server that listens on a port and echoes back any
 * message it receives from a client. Each client connection is handled
 * in a separate thread.
 */
public class EchoServer {

    public static void main(String[] args) {
        int port = 9999; // Port to listen on

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Echo Server started. Listening on port " + port);

            // The main server loop. It waits for new connections indefinitely.
            while (true) {
                try {
                    // accept() blocks until a client connects.
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                    // Create a new thread to handle this client's communication
                    // This allows the server to immediately go back to waiting (accept())
                    // for new clients.
                    ClientHandler clientThread = new ClientHandler(clientSocket);
                    new Thread(clientThread).start();

                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port + ": " + e.getMessage());
        }
    }

    /**
     * ClientHandler is a Runnable class that handles communication
     * with a single client in its own thread.
     */
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            // We use try-with-resources to automatically close the streams and socket
            // when the block is exited (either normally or via an exception).
            try (
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                String inputLine;
                // Read lines from the client until the client disconnects (in.readLine() == null)
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from " + clientSocket.getInetAddress().getHostAddress() + ": " + inputLine);

                    // Check for a quit command
                    if ("exit".equalsIgnoreCase(inputLine.trim())) {
                        out.println("Goodbye!");
                        break;
                    }

                    // Echo the received message back to the client
                    out.println("Server echoes: " + inputLine);
                }
            } catch (IOException e) {
                System.err.println("Error in client handler: " + e.getMessage());
            } finally {
                // Ensure the socket is closed when the thread finishes.
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
                System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
            }
        }
    }
}