import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/*
 * TP2 - Part 5, Step 16 : multicast receiver
 *
 * Joins a multicast group and prints every message sent to that group. A single
 * datagram sent to the group address (239.0.0.0 - 239.255.255.255) is delivered
 * to ALL the hosts that joined the group.
 *
 * The old joinGroup(InetAddress) is deprecated since Java 14, so we use the
 * version with a network interface:
 *     socket.joinGroup(new InetSocketAddress(group, port), netIf);
 *
 * Finding the network interface by name (eth0, wlan0...) is awkward on Windows,
 * so this program picks a suitable interface automatically. You can still force
 * one by giving its name as the 3rd argument.
 *
 * Run:
 *   java MulticastReceiver                       (group 239.0.0.1, port 8888)
 *   java MulticastReceiver 239.0.0.1 8888
 *   java MulticastReceiver 239.0.0.1 8888 eth4   (force the interface)
 */
public class MulticastReceiver {

    private static final String DEFAULT_GROUP = "239.0.0.1";
    private static final int DEFAULT_PORT = 8888;

    public static void main(String[] args) throws IOException {
        String groupName = (args.length >= 1) ? args[0] : DEFAULT_GROUP;
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        String ifName = (args.length >= 3) ? args[2] : null;

        InetAddress group = InetAddress.getByName(groupName);

        // MulticastSocket(port) enables SO_REUSEADDR, so several receivers can
        // listen on the same port on the same machine (needed in Step 17a).
        MulticastSocket socket = new MulticastSocket(port);

        // Choose the network interface: the one given, or an automatic one.
        NetworkInterface netIf = (ifName != null)
                ? NetworkInterface.getByName(ifName)
                : pickInterface();

        if (netIf == null) {
            System.err.println("No suitable network interface found.");
            listInterfaces();
            return;
        }

        // Join the group on that interface (the non-deprecated way).
        socket.joinGroup(new InetSocketAddress(group, port), netIf);
        System.out.println("Joined group " + groupName + ":" + port
                + " on interface " + netIf.getName()
                + " (" + netIf.getDisplayName() + ")");
        System.out.println("Waiting for messages... (Ctrl+C to stop)");

        byte[] buffer = new byte[1024];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(), 0, packet.getLength(),
                                    StandardCharsets.UTF_8);
            System.out.println("From " + packet.getAddress().getHostAddress()
                    + ":" + packet.getPort() + " -> " + msg);
        }
        // (We never leave the loop here; Step 17b shows leaveGroup separately.)
    }

    /*
     * Picks the first network interface that is up, supports multicast and is
     * not the loopback. Falls back to loopback if nothing else is found (useful
     * for a quick local test).
     */
    private static NetworkInterface pickInterface() throws SocketException {
        NetworkInterface loopback = null;
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        while (ifs.hasMoreElements()) {
            NetworkInterface ni = ifs.nextElement();
            if (!ni.isUp() || !ni.supportsMulticast()) {
                continue;
            }
            if (ni.isLoopback()) {
                loopback = ni; // remember it, but prefer a real interface
                continue;
            }
            // Keep a real interface that has an IPv4 address.
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                if (addrs.nextElement().getAddress().length == 4) { // IPv4
                    return ni;
                }
            }
        }
        return loopback;
    }

    // Prints the available interfaces so the user can force one if needed.
    private static void listInterfaces() throws SocketException {
        System.out.println("Available interfaces (name -> description):");
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        while (ifs.hasMoreElements()) {
            NetworkInterface ni = ifs.nextElement();
            System.out.println("  " + ni.getName() + " -> " + ni.getDisplayName()
                    + (ni.isLoopback() ? " [loopback]" : "")
                    + " up=" + ni.isUp() + " multicast=" + ni.supportsMulticast());
        }
    }
}
