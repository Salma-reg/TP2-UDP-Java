# TP2 — UDP client-server with Java sockets

Java for networks — 3RTS, Year 2026-27
Instructor: Linda Senigagliesi

This repository contains the Java code for TP2 (UDP). Each program is a small,
self-contained class. Compile everything with:

```
javac *.java
```

## Part 1 — UDP client-server
| File | Role | Steps |
|------|------|-------|
| `UDPServer.java` | UDP server: receives datagrams and prints them (buffer size selectable) | 1, 2, 5 |
| `UDPClient.java` | UDP client: sends keyboard lines as datagrams | 3, 4 |

Run:
```
java UDPServer 8080
java UDPClient localhost 8080
```

## Part 2 — Datagram analysis
| File | Role | Steps |
|------|------|-------|
| `UDPSizeTester.java` | Sends datagrams of increasing size (1000 … 65508 bytes) | 6 |

The buffer size is the 2nd argument of `UDPServer` (e.g. `java UDPServer 8080 64`).

## Part 4 — Reliability over UDP
| File | Role | Steps |
|------|------|-------|
| `UDPServerSeq.java` / `UDPClientSeq.java` | Sequence numbers, missing / out-of-order detection | 11 |
| `UDPServerAck.java` / `UDPClientAck.java` | Acknowledgments (ACK), timeout and retransmission | 12 |
| `UDPBurstSender.java` / `UDPCountServer.java` | Burst of N datagrams + loss-rate measurement | 13 |
| `UDPServerDrop.java` | Drops 20% of ACKs on purpose, detects duplicates (use with `UDPClientAck`) | 14 |

Examples:
```
java UDPServerAck 8080
java UDPClientAck localhost 8080

java UDPCountServer 8080            (fast)      / java UDPBurstSender localhost 8080 10000 1000
java UDPCountServer 8080 2          (slow, 2ms) / java UDPBurstSender localhost 8080 50000 1000

java UDPServerDrop 8080
java UDPClientAck localhost 8080
```

## Part 5 — Multicast and broadcast
| File | Role | Steps |
|------|------|-------|
| `MulticastReceiver.java` | Joins a multicast group and prints the messages | 16, 17 |
| `MulticastSender.java` | Sends messages to the multicast group | 16, 17 |
| `MulticastReceiverLeave.java` | Leaves the group after 3 messages (shows `leaveGroup`) | 17b |
| `BroadcastSender.java` | Enables broadcast and sends to 255.255.255.255 | 18 |

Examples:
```
java MulticastReceiver 239.0.0.1 8888
java MulticastSender 239.0.0.1 8888

java UDPServer 8888
java BroadcastSender 255.255.255.255 8888
```

## Notes
- All programs auto-detect a suitable network interface (useful on Windows where
  interface names are not `eth0`).
- Written and tested on Windows with JDK 21.
