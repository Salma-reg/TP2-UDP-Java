import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - Part 4, Step 11 : UDP client with sequence numbers
 *
 * Same idea as UDPClient, but each message is now prefixed with a sequence
 * number that grows by one every time: "0:hello", "1:how are you", "2:bye"...
 *
 * The sequence number lets the server tell whether a message is missing or
 * arrives out of order (UDP gives no such guarantee by itself).
 *
 * Run:
 *   java UDPClientSeq localhost 8080
 */
public class UDPClientSeq {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java UDPClientSeq <host> <port>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        InetAddress serverAddress = InetAddress.getByName(host);

        try (DatagramSocket socket = new DatagramSocket()) {

            BufferedReader keyboard = openKeyboard();
            System.out.println("Type your messages (Ctrl+Z then Enter to quit):");

            int seq = 0; // the sequence number, starts at 0

            String line;
            while ((line = keyboard.readLine()) != null) {

                // Build the message "seq:text", e.g. "0:hello".
                String message = seq + ":" + line;

                byte[] data = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet =
                        new DatagramPacket(data, data.length, serverAddress, port);
                socket.send(packet);

                System.out.println("Sent #" + seq);
                seq++; // next message gets the next number
            }
        }

        System.out.println("Client stopped.");
    }

    // Keyboard reader (console in a terminal, System.in otherwise).
    private static BufferedReader openKeyboard() {
        Console console = System.console();
        if (console != null) {
            return new BufferedReader(console.reader());
        }
        return new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }
}
