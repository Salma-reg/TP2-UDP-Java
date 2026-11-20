import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/*
 * TP2 - Part 5 : multicast sender (used in Step 17)
 *
 * Reads lines from the keyboard and sends each one to a multicast group. Every
 * receiver that joined the group (even on other machines) gets a copy.
 *
 * We use a MulticastSocket so we can set the TTL (how many routers the datagram
 * may cross); TTL = 1 keeps it on the local network.
 *
 * Run:
 *   java MulticastSender                 (group 239.0.0.1, port 8888)
 *   java MulticastSender 239.0.0.1 8888
 */
public class MulticastSender {

    private static final String DEFAULT_GROUP = "239.0.0.1";
    private static final int DEFAULT_PORT = 8888;

    public static void main(String[] args) throws IOException {
        String groupName = (args.length >= 1) ? args[0] : DEFAULT_GROUP;
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        InetAddress group = InetAddress.getByName(groupName);

        try (MulticastSocket socket = new MulticastSocket()) {
            socket.setTimeToLive(1); // stay on the local network

            // Send on the same kind of interface the receiver auto-picks, so that
            // on a machine with several (virtual) adapters the datagrams go out on
            // the right one and local receivers see them.
            NetworkInterface netIf = pickInterface();
            if (netIf != null) {
                socket.setNetworkInterface(netIf);
                System.out.println("Sending on interface " + netIf.getName()
                        + " (" + netIf.getDisplayName() + ")");
            }

            BufferedReader keyboard = openKeyboard();
            System.out.println("Sending to group " + groupName + ":" + port
                    + " (Ctrl+Z then Enter to quit):");

            String line;
            while ((line = keyboard.readLine()) != null) {
                byte[] data = line.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length, group, port));
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

    // Same choice as the receiver: first real interface that is up and supports
    // multicast, otherwise the loopback.
    private static NetworkInterface pickInterface() throws SocketException {
        NetworkInterface loopback = null;
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        while (ifs.hasMoreElements()) {
            NetworkInterface ni = ifs.nextElement();
            if (!ni.isUp() || !ni.supportsMulticast()) {
                continue;
            }
            if (ni.isLoopback()) {
                loopback = ni;
                continue;
            }
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                if (addrs.nextElement().getAddress().length == 4) {
                    return ni;
                }
            }
        }
        return loopback;
    }
}
