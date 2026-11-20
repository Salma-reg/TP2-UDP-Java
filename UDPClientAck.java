import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - Part 4, Step 12 : UDP client with acknowledgments and retransmission
 *
 * For each message typed by the user, the client:
 *   1. sends "seq:text" to the server,
 *   2. waits for the matching "ACK:seq" with a TIMEOUT (setSoTimeout),
 *   3. if no ACK arrives in time (SocketTimeoutException), it resends the same
 *      message, up to MAX_ATTEMPTS times,
 *   4. if it still gets no ACK after all attempts, it prints an error.
 *
 * This is the basic reliability idea of TCP, rebuilt by hand on top of UDP.
 *
 * Run:
 *   java UDPClientAck localhost 8080
 */
public class UDPClientAck {

    private static final int TIMEOUT_MS = 2000; // how long we wait for an ACK
    private static final int MAX_ATTEMPTS = 3;  // how many times we try to send

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java UDPClientAck <host> <port>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        InetAddress serverAddress = InetAddress.getByName(host);

        try (DatagramSocket socket = new DatagramSocket()) {
            // The socket will throw SocketTimeoutException if no datagram
            // arrives within TIMEOUT_MS while we wait for an ACK.
            socket.setSoTimeout(TIMEOUT_MS);

            BufferedReader keyboard = openKeyboard();
            System.out.println("Type your messages (Ctrl+Z then Enter to quit):");

            byte[] ackBuffer = new byte[1024];
            int seq = 0;

            String line;
            while ((line = keyboard.readLine()) != null) {

                String message = seq + ":" + line;
                byte[] data = message.getBytes(StandardCharsets.UTF_8);

                boolean acknowledged = false;

                // Try up to MAX_ATTEMPTS times to get the ACK for this message.
                for (int attempt = 1; attempt <= MAX_ATTEMPTS && !acknowledged; attempt++) {

                    // (Re)send the message.
                    DatagramPacket packet = new DatagramPacket(
                            data, data.length, serverAddress, port);
                    socket.send(packet);
                    System.out.println("Sent #" + seq + " (attempt " + attempt + ")");

                    try {
                        // Wait for the ACK.
                        DatagramPacket ackPacket =
                                new DatagramPacket(ackBuffer, ackBuffer.length);
                        socket.receive(ackPacket); // blocks up to TIMEOUT_MS

                        String ack = new String(ackPacket.getData(), 0,
                                ackPacket.getLength(), StandardCharsets.UTF_8);

                        // Accept it only if it is the ACK for THIS message.
                        if (ack.equals("ACK:" + seq)) {
                            System.out.println("  got " + ack);
                            acknowledged = true;
                        } else {
                            System.out.println("  ignored unexpected reply: " + ack);
                        }
                    } catch (SocketTimeoutException e) {
                        // No ACK in time -> the loop will resend (if attempts remain).
                        System.out.println("  timeout, no ACK for #" + seq);
                    } catch (PortUnreachableException e) {
                        // On Windows, sending to a port with no server can make the
                        // OS return an ICMP "port unreachable"; we treat it the same
                        // way as a timeout and retransmit.
                        System.out.println("  no server (port unreachable) for #" + seq);
                    }
                }

                if (!acknowledged) {
                    System.out.println("ERROR: #" + seq
                            + " not acknowledged after " + MAX_ATTEMPTS + " attempts");
                }

                seq++;
            }
        }

        System.out.println("Client stopped.");
    }

    private static BufferedReader openKeyboard() {
        Console console = System.console();
        if (console != null) {
            return new BufferedReader(console.reader());
        }
        return new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }
}
