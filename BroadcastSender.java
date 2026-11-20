import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/*
 * TP2 - Part 5, Step 18 : broadcast sender
 *
 * Sends each line typed by the user to the broadcast address of the local
 * network (255.255.255.255 by default). Broadcast must be enabled on the socket
 * with setBroadcast(true), otherwise the OS refuses to send it.
 *
 * Every host on the SAME local network (same subnet) receives the datagram,
 * whether it asked for it or not. A broadcast cannot cross a router, so it never
 * reaches another network.
 *
 * Any plain UDP server bound to the port receives it, e.g. our Part 1 server:
 *   java UDPServer 8888
 * then:
 *   java BroadcastSender 255.255.255.255 8888
 */
public class BroadcastSender {

    private static final String DEFAULT_ADDR = "255.255.255.255";
    private static final int DEFAULT_PORT = 8888;

    public static void main(String[] args) throws IOException {
        String addr = (args.length >= 1) ? args[0] : DEFAULT_ADDR;
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        InetAddress target = InetAddress.getByName(addr);

        try (DatagramSocket socket = new DatagramSocket()) {
            // Without this, sending to a broadcast address throws an exception.
            socket.setBroadcast(true);

            BufferedReader keyboard = openKeyboard();
            System.out.println("Broadcasting to " + addr + ":" + port
                    + " (Ctrl+Z then Enter to quit):");

            String line;
            while ((line = keyboard.readLine()) != null) {
                byte[] data = line.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length, target, port));
            }
        }

        System.out.println("Sender stopped.");
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
