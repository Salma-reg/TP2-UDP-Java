import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - UDP server (Part 1.1, extended in Part 2 Step 5)
 *
 * Goal: receive datagrams from clients and print what they sent.
 * Each datagram holds a text string encoded in UTF-8. For every datagram we
 * receive, we print the client's address and port, the number of bytes, then
 * the message.
 *
 * Part 2 (Step 5): the size of the receive buffer can now be chosen on the
 * command line. This lets us see what happens when the buffer is smaller or
 * larger than the messages.
 *
 * How to run:
 *   java UDPServer                 -> port 8080, buffer 1024 bytes
 *   java UDPServer 8080            -> port 8080, buffer 1024 bytes
 *   java UDPServer 8080 64         -> port 8080, buffer 64 bytes (too small)
 *   java UDPServer 8080 2048       -> port 8080, buffer 2048 bytes
 *   java UDPServer 8080 65507      -> port 8080, biggest UDP payload over IPv4
 */
public class UDPServer {

    // Default buffer size asked by the handout in Part 1 (1024 bytes).
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    // Port used when the user does not give one.
    private static final int DEFAULT_PORT = 8080;

    private final int port;        // the port the server listens on
    private final int bufferSize;  // size of the receive buffer, in bytes
    private boolean running;       // true once launch() is looping

    // Full constructor: choose the port AND the buffer size.
    public UDPServer(int port, int bufferSize) {
        this.port = port;
        this.bufferSize = bufferSize;
        this.running = false;
    }

    // Constructor with only the port: use the default buffer size.
    public UDPServer(int port) {
        this(port, DEFAULT_BUFFER_SIZE);
    }

    // Default constructor: default port and default buffer size.
    public UDPServer() {
        this(DEFAULT_PORT, DEFAULT_BUFFER_SIZE);
    }

    /*
     * Starts the server. It opens the UDP socket, then loops forever:
     * wait for a datagram, read it, print it, and wait for the next one.
     */
    public void launch() throws SocketException {
        // try-with-resources: the socket closes by itself when we leave this block.
        try (DatagramSocket socket = new DatagramSocket(port)) {
            running = true;
            System.out.println("UDP server started: " + this);

            // The buffer where each incoming datagram is stored.
            // IMPORTANT: a datagram bigger than this buffer is TRUNCATED to
            // bufferSize bytes; UDP throws away the extra bytes, and the client
            // is NOT told about it (that is exactly what Step 5 shows).
            byte[] buffer = new byte[bufferSize];

            while (true) {
                // Fresh packet each time so getLength() = size of THIS datagram.
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // receive() blocks until a datagram arrives.
                socket.receive(packet);

                // Decode only the bytes actually received, as UTF-8.
                String msg = new String(packet.getData(), 0, packet.getLength(),
                                        StandardCharsets.UTF_8);

                // Print sender (address:port), number of bytes kept, then message.
                System.out.println(packet.getAddress().getHostAddress()
                        + ":" + packet.getPort()
                        + " (" + packet.getLength() + " bytes) -> " + msg);
            }
        } catch (IOException e) {
            running = false;
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // Short description of the server's state.
    @Override
    public String toString() {
        return "UDPServer{port=" + port + ", bufferSize=" + bufferSize
                + ", running=" + running + "}";
    }

    public static void main(String[] args) {
        // args[0] = port (optional), args[1] = buffer size (optional).
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        int bufferSize = (args.length >= 2) ? Integer.parseInt(args[1])
                                            : DEFAULT_BUFFER_SIZE;

        UDPServer server = new UDPServer(port, bufferSize);

        try {
            server.launch();
        } catch (SocketException e) {
            // Usually means the port is already in use.
            System.err.println("Could not open the socket: " + e.getMessage());
            System.exit(1);
        }
    }
}
