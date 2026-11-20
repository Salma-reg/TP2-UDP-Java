import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - Part 4, Step 13 : burst sender (measures packet loss)
 *
 * Sends N numbered datagrams to the server AS FAST AS POSSIBLE, without waiting
 * for any acknowledgment. Then it sends a few "END:N" markers so the counting
 * server knows the burst is over and can compute the loss rate. The END marker
 * is sent several times because it, too, could be lost.
 *
 * An optional 4th argument sets the size of each datagram in bytes (default
 * 1000). Bigger datagrams fill the receiver's buffer faster, which makes the
 * packet loss of Step 13 (especially case c, the slow server) clearly visible.
 *
 * Run (default N = 10000, size = 1000 bytes):
 *   java UDPBurstSender localhost 8080
 *   java UDPBurstSender localhost 8080 10000
 *   java UDPBurstSender localhost 8080 50000 1000
 */
public class UDPBurstSender {

    private static final int DEFAULT_N = 10000;
    private static final int DEFAULT_SIZE = 1000; // bytes per datagram

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java UDPBurstSender <host> <port> [N] [sizeBytes]");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int n = (args.length >= 3) ? Integer.parseInt(args[2]) : DEFAULT_N;
        int size = (args.length >= 4) ? Integer.parseInt(args[3]) : DEFAULT_SIZE;

        InetAddress serverAddress = InetAddress.getByName(host);

        try (DatagramSocket socket = new DatagramSocket()) {
            System.out.println("Sending " + n + " datagrams of " + size
                    + " bytes to " + host + ":" + port + " ...");

            // Fire the N datagrams, each carrying its number (padded to `size`
            // bytes), with no pause.
            for (int i = 0; i < n; i++) {
                byte[] data = new byte[size];
                byte[] num = Integer.toString(i).getBytes(StandardCharsets.UTF_8);
                // Put the number at the start; the rest stays as padding (zeros).
                System.arraycopy(num, 0, data, 0, Math.min(num.length, size));
                socket.send(new DatagramPacket(data, data.length, serverAddress, port));
            }

            // Tell the server the burst is finished. When the burst caused loss,
            // the receiver's buffer is full, so an END could be dropped too; we
            // send it many times over ~2 seconds so at least one copy gets in as
            // the server drains its buffer.
            byte[] end = ("END:" + n).getBytes(StandardCharsets.UTF_8);
            for (int k = 0; k < 20; k++) {
                socket.send(new DatagramPacket(end, end.length, serverAddress, port));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Done: " + n + " datagrams sent (+ END markers).");
        }
    }
}
