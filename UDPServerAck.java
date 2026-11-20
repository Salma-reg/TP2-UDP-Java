import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - Part 4, Step 12 : UDP server with acknowledgments (ACK)
 *
 * The client sends "seq:text" (as in Step 11). For every message it receives,
 * this server:
 *   1. prints the message with its sequence number,
 *   2. sends back an acknowledgment "ACK:seq" to the SAME address and port the
 *      datagram came from, so the client knows the message arrived.
 *
 * It also keeps the Step 11 checks (missing / out-of-order / duplicate).
 *
 * Run:
 *   java UDPServerAck 8080
 */
public class UDPServerAck {

    private static final int BUFFER_SIZE = 1024;
    private static final int DEFAULT_PORT = 8080;

    private final int port;

    public UDPServerAck(int port) {
        this.port = port;
    }

    public UDPServerAck() {
        this(DEFAULT_PORT);
    }

    public void launch() throws SocketException {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP server (ACK) started on port " + port);

            byte[] buffer = new byte[BUFFER_SIZE];
            int expected = 0; // next sequence number we expect

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength(),
                                             StandardCharsets.UTF_8);

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

                System.out.println("#" + seq + " from "
                        + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                        + " -> " + text);

                // Order check (same as Step 11).
                if (seq == expected) {
                    expected = seq + 1;
                } else if (seq > expected) {
                    System.out.println("  >> MISSING: " + expected + " to " + (seq - 1));
                    expected = seq + 1;
                } else {
                    System.out.println("  >> OUT OF ORDER or DUPLICATE: #" + seq);
                }

                // Send the acknowledgment "ACK:seq" back to the sender.
                String ack = "ACK:" + seq;
                byte[] ackData = ack.getBytes(StandardCharsets.UTF_8);
                DatagramPacket ackPacket = new DatagramPacket(
                        ackData, ackData.length,
                        packet.getAddress(), packet.getPort());
                socket.send(ackPacket);
                System.out.println("  << sent " + ack);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        try {
            new UDPServerAck(port).launch();
        } catch (SocketException e) {
            System.err.println("Could not open the socket: " + e.getMessage());
            System.exit(1);
        }
    }
}
