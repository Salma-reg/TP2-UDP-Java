import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/*
 * TP2 - Part 4, Step 14 : server that loses packets on purpose + detects duplicates
 *
 * To test the acknowledgment mechanism of Step 12 even on localhost (where
 * nothing is really lost), this server simulates loss: for each message it
 * receives, with probability 20% it DROPS the acknowledgment (it does not send
 * ACK:seq back). The client then gets no ACK, times out, and retransmits.
 *
 * The retransmitted message has the SAME sequence number, so when it arrives
 * again the server recognises it as a DUPLICATE (it already delivered that
 * number). This is exactly the case described in the handout: "a message
 * received twice because its acknowledgment was lost". The server detects
 * duplicates with a set of the sequence numbers it has already delivered.
 *
 * Works with the Step 12 client UDPClientAck (unchanged).
 *
 * Run:
 *   java UDPServerDrop 8080
 */
public class UDPServerDrop {

    private static final int BUFFER_SIZE = 1024;
    private static final int DEFAULT_PORT = 8080;
    private static final double DROP_PROBABILITY = 0.2; // 20% of ACKs are dropped

    public static void main(String[] args) throws SocketException {
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP server (drop " + (int)(DROP_PROBABILITY * 100)
                    + "% of ACKs) started on port " + port);

            byte[] buffer = new byte[BUFFER_SIZE];

            // The sequence numbers we have already DELIVERED (to spot duplicates).
            Set<Integer> delivered = new HashSet<>();

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength(),
                                             StandardCharsets.UTF_8);

                int colon = received.indexOf(':');
                if (colon < 0) continue;

                int seq;
                try {
                    seq = Integer.parseInt(received.substring(0, colon));
                } catch (NumberFormatException e) {
                    continue;
                }
                String text = received.substring(colon + 1);

                // Is this sequence number one we have already delivered?
                if (delivered.contains(seq)) {
                    // Duplicate: the client resent it because our earlier ACK was
                    // lost. We do NOT deliver it a second time.
                    System.out.println("#" + seq + " DUPLICATE (already delivered) -> "
                            + text);
                } else {
                    // New message: deliver it (print it) and remember its number.
                    System.out.println("#" + seq + " -> " + text);
                    delivered.add(seq);
                }

                // Simulate loss: 20% of the time we do NOT send the ACK.
                if (Math.random() < DROP_PROBABILITY) {
                    System.out.println("  xx ACK for #" + seq + " dropped (simulated loss)");
                } else {
                    String ack = "ACK:" + seq;
                    byte[] ackData = ack.getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(ackData, ackData.length,
                            packet.getAddress(), packet.getPort()));
                    System.out.println("  << sent " + ack);
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
