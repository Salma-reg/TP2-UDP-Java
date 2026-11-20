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
 * TP2 - Part 5, Step 17b : a receiver that leaves the group
 *
 * Same as MulticastReceiver, but after receiving a few messages (3 here) it
 * calls leaveGroup(...) and stops receiving. Run it next to a normal
 * MulticastReceiver: after this one leaves, only the normal receiver keeps
 * getting the messages, which shows that leaveGroup really works.
 *
 * Run:
 *   java MulticastReceiverLeave 239.0.0.1 8888
 */
public class MulticastReceiverLeave {

    private static final String DEFAULT_GROUP = "239.0.0.1";
    private static final int DEFAULT_PORT = 8888;
    private static final int LEAVE_AFTER = 3; // leave after this many messages

    public static void main(String[] args) throws IOException {
        String groupName = (args.length >= 1) ? args[0] : DEFAULT_GROUP;
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        InetAddress group = InetAddress.getByName(groupName);
        MulticastSocket socket = new MulticastSocket(port);
        NetworkInterface netIf = pickInterface();

        InetSocketAddress groupAddr = new InetSocketAddress(group, port);
        socket.joinGroup(groupAddr, netIf);
        System.out.println("Joined group " + groupName + ":" + port
                + " on interface " + netIf.getName());
        System.out.println("Will LEAVE after " + LEAVE_AFTER + " messages.");

        byte[] buffer = new byte[1024];
        int count = 0;

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(), 0, packet.getLength(),
                                    StandardCharsets.UTF_8);
            count++;
            System.out.println("[" + count + "] From "
                    + packet.getAddress().getHostAddress() + " -> " + msg);

            if (count == LEAVE_AFTER) {
                // Leave the group: from now on this socket no longer receives the
                // group's datagrams.
                socket.leaveGroup(groupAddr, netIf);
                System.out.println("=== LEFT THE GROUP - will not receive any more ===");
                break;
            }
        }

        // Prove we stopped: keep listening for 10 more seconds; nothing should come.
        socket.setSoTimeout(10000);
        System.out.println("Listening 10 s after leaving (expect nothing)...");
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            System.out.println("Unexpected: still received something?!");
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("Confirmed: received NOTHING after leaving the group.");
        }
        socket.close();
    }

    private static NetworkInterface pickInterface() throws SocketException {
        NetworkInterface loopback = null;
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        while (ifs.hasMoreElements()) {
            NetworkInterface ni = ifs.nextElement();
            if (!ni.isUp() || !ni.supportsMulticast()) continue;
            if (ni.isLoopback()) { loopback = ni; continue; }
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                if (addrs.nextElement().getAddress().length == 4) return ni;
            }
        }
        return loopback;
    }
}
