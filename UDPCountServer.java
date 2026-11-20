import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - Part 4, Step 13 : counting server (measures packet loss)
 *
 * Works with UDPBurstSender. The sender fires N numbered datagrams as fast as
 * it can, then sends a few "END:N" markers. This server counts how many data
 * datagrams it actually received, and when it sees "END:N" it prints how many
 * were lost and the loss rate.
 *
 * An optional second argument makes the server wait a few milliseconds after
 * each datagram (Thread.sleep). This slows it down on purpose: while it sleeps,
 * the OS receive buffer fills up and extra datagrams are dropped -> this is
 * case (c) of the handout, which shows a much higher loss rate.
 *
 * Run:
 *   java UDPCountServer 8080        (no sleep  -> case a)
 *   java UDPCountServer 8080 5      (5 ms sleep -> case c)
 */
public class UDPCountServer {

    private static final int DEFAULT_PORT = 8080;
    // Big buffer so the datagrams themselves are never truncated.
    private static final int BUFFER_SIZE = 65507;

    public static void main(String[] args) throws SocketException {
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        int sleepMs = (args.length >= 2) ? Integer.parseInt(args[1]) : 0;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            // Ask the OS for a large receive buffer so that, on localhost with no
            // sleep (case a), the burst can be held while we drain it and the loss
            // stays low. With the sleep (case c) the buffer still overflows.
            socket.setReceiveBufferSize(8 * 1024 * 1024); // 8 MB (the OS may cap it)

            System.out.println("Counting server on port " + port
                    + " (recv buffer = " + socket.getReceiveBufferSize() + " bytes)"
                    + (sleepMs > 0 ? ", sleep " + sleepMs + " ms per datagram" : ""));

            byte[] buffer = new byte[BUFFER_SIZE];
            int received = 0;          // how many DATA datagrams counted so far
            boolean reported = false;  // did we already print the stats for this burst?

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength(),
                                        StandardCharsets.UTF_8);

                if (msg.startsWith("END:")) {
                    // The sender repeats END several times; print the stats only
                    // for the first one we see.
                    if (!reported) {
                        int n = Integer.parseInt(msg.substring(4));
                        int lost = n - received;
                        double rate = 100.0 * lost / n;
                        System.out.printf("Expected %d, received %d, lost %d (%.2f%% loss)%n",
                                n, received, lost, rate);
                        reported = true;
                    }
                } else {
                    // A new data datagram. If a new burst starts after a previous
                    // report, reset the counters.
                    if (reported) {
                        received = 0;
                        reported = false;
                    }
                    received++;
                    if (sleepMs > 0) {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
