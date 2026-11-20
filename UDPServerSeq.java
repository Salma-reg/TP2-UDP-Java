import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - Part 4, Step 11 : UDP server that reads sequence numbers
 *
 * The client now sends messages shaped like "seq:text" (e.g. "3:hello").
 * This server splits each message at the first ':', reads the sequence number,
 * and checks the order:
 *   - if the number is the one expected  -> normal
 *   - if some numbers were skipped        -> reports the MISSING ones
 *   - if the number is smaller than expected -> reports OUT OF ORDER (or duplicate)
 *
 * Run:
 *   java UDPServerSeq 8080
 */
public class UDPServerSeq {

    private static final int BUFFER_SIZE = 1024;
    private static final int DEFAULT_PORT = 8080;

    private final int port;
    private boolean running;

    public UDPServerSeq(int port) {
        this.port = port;
        this.running = false;
    }

    public UDPServerSeq() {
        this(DEFAULT_PORT);
    }

    public void launch() throws SocketException {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            running = true;
            System.out.println("UDP server (seq) started on port " + port);

            byte[] buffer = new byte[BUFFER_SIZE];

            // The sequence number we expect next (the first one should be 0).
            int expected = 0;

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength(),
                                             StandardCharsets.UTF_8);

                // Split "seq:text" at the FIRST ':' only (the text may contain ':').
                int colon = received.indexOf(':');
                if (colon < 0) {
                    System.out.println("Ignored (no sequence number): " + received);
                    continue;
                }

                int seq;
                try {
                    seq = Integer.parseInt(received.substring(0, colon));
                } catch (NumberFormatException e) {
                    System.out.println("Ignored (bad sequence number): " + received);
                    continue;
                }
                String text = received.substring(colon + 1);

                // Print the message with its number and sender.
                System.out.println("#" + seq + " from "
                        + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                        + " -> " + text);

                // Check the order against what we expected.
                if (seq == expected) {
                    // Perfect, in order.
                    expected = seq + 1;
                } else if (seq > expected) {
                    // Some numbers were skipped: expected .. seq-1 are missing.
                    System.out.println("  >> MISSING: " + expected
                            + " to " + (seq - 1));
                    expected = seq + 1;
                } else {
                    // seq < expected: an old number came back.
                    System.out.println("  >> OUT OF ORDER or DUPLICATE: #" + seq
                            + " (already expected #" + expected + ")");
                }
            }
        } catch (IOException e) {
            running = false;
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        UDPServerSeq server = new UDPServerSeq(port);
        try {
            server.launch();
        } catch (SocketException e) {
            System.err.println("Could not open the socket: " + e.getMessage());
            System.exit(1);
        }
    }
}
