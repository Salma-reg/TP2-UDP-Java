import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - Part 1.2 : UDP client
 *
 * Goal: read the lines typed by the user on the keyboard, and send each line
 * to the server in a UDP datagram (the text is encoded in UTF-8).
 *
 * The server address and port are given on the command line, for example:
 *   java UDPClient localhost 8080
 *
 * The client stops when the input is closed (Ctrl+Z).
 */
public class UDPClient {

    public static void main(String[] args) throws IOException {
        // We need exactly two arguments: the server name and its port.
        if (args.length < 2) {
            System.err.println("Usage: java UDPClient <host> <port>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        // Resolve the server name (e.g. "localhost") into an IP address.
        InetAddress serverAddress = InetAddress.getByName(host);

        // One socket is enough to send every datagram. The OS picks a free
        // local port for us (the "ephemeral" port the server will display).
        try (DatagramSocket socket = new DatagramSocket()) {

            BufferedReader keyboard = openKeyboard();
            System.out.println("Type your messages (Ctrl+Z then Enter to quit):");

            String line;
            // readLine() returns null when the input is closed -> we stop.
            while ((line = keyboard.readLine()) != null) {

                // Encode the line in UTF-8: this gives the exact bytes to send.
                byte[] data = line.getBytes(StandardCharsets.UTF_8);

                // Build the datagram: the data, its length, and where it goes.
                DatagramPacket packet =
                        new DatagramPacket(data, data.length, serverAddress, port);

                // Send it. With UDP there is no connection: we just fire it off.
                socket.send(packet);
            }
        }

        System.out.println("Client stopped.");
    }

    /*
     * Returns a reader for the keyboard.
     * In a normal terminal we could use System.console(), but it returns null
     * when the program is launched from an IDE (Eclipse, IntelliJ, VS Code).
     * So we fall back to reading System.in directly, always in UTF-8.
     */
    private static BufferedReader openKeyboard() {
        Console console = System.console();
        if (console != null) {
            return new BufferedReader(console.reader());
        }
        return new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }
}
