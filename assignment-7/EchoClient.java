import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A simple client application to connect to the EchoServer,
 * send messages from the console, and print the server's echoed response.
 */
public class EchoClient {

    public static void main(String[] args) {
        String hostname = "localhost"; // Server hostname
        int port = 9999;              // Server port

        // This try-with-resources block ensures all resources are closed automatically.
        try (
            Socket echoSocket = new Socket(hostname, port);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to echo server on " + hostname + ":" + port);
            System.out.println("Type your message and press Enter. Type 'exit' to quit.");

            String userInput;
            // Read from the console until the user types 'exit'
            while ((userInput = stdIn.readLine()) != null) {
                // Send the user's line to the server
                out.println(userInput);

                // If the user types 'exit', break the loop immediately
                if ("exit".equalsIgnoreCase(userInput.trim())) {
                    break;
                }

                // Read the server's echo response and print it
                String serverResponse = in.readLine();
                if (serverResponse == null) {
                    System.out.println("Server disconnected.");
                    break;
                }
                System.out.println(serverResponse);
            }

            System.out.println("Disconnecting from server.");

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostname);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostname + ". Is the server running?");
        }
    }
}