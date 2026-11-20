import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;

/*
 * TP2 - Part 2, Step 6 : datagram size tester
 *
 * This small program sends several datagrams of increasing size to the server,
 * so we can see which sizes go through and what happens at the UDP limit.
 *
 * A UDP datagram carries at most 65535 bytes, including the 8-byte UDP header;
 * over IPv4 the 20-byte IP header also counts, so the biggest PAYLOAD is
 *   65535 - 8 - 20 = 65507 bytes.
 * Trying to send 65508 bytes of payload is therefore too big and must fail.
 *
 * Run the server with a big buffer first, e.g.:
 *   java UDPServer 8080 65507
 * then:
 *   java UDPSizeTester localhost 8080
 */
public class UDPSizeTester {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java UDPSizeTester <host> <port>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        InetAddress serverAddress = InetAddress.getByName(host);

        // The sizes we want to try, in bytes.
        int[] sizes = { 1000, 10000, 65507, 65508 };

        try (DatagramSocket socket = new DatagramSocket()) {
            for (int size : sizes) {
                // Fill a buffer of this size with the letter 'A'.
                byte[] data = new byte[size];
                java.util.Arrays.fill(data, (byte) 'A');

                DatagramPacket packet =
                        new DatagramPacket(data, data.length, serverAddress, port);

                try {
                    socket.send(packet);
                    System.out.println("Sent " + size + " bytes: OK");
                } catch (IOException e) {
                    // 65508 bytes is over the IPv4 limit -> send() fails here.
                    System.out.println("Sent " + size + " bytes: FAILED -> "
                            + e.getMessage());
                }
            }
        }
    }
}
