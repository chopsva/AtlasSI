package com.icr.atlas.Datacap;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class DatacapDeviceDiscovery {

    public static void main(String[] args) {
        final int port = 9001;
        //final String deviceID = "183567303261086003685327";
        final String deviceID = args[0];

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(port);

            // Required to allow broadcasts.
            socket.setBroadcast(true);

            // Stop listening after 5 seconds.
            socket.setSoTimeout(5000);

            // Broadcast
            String request = "Who has " + deviceID;
            byte[] sendData = request.getBytes(StandardCharsets.UTF_8);

            InetAddress broadcastAddress =
                    InetAddress.getByName("255.255.255.255");

            DatagramPacket sendPacket =
                    new DatagramPacket(
                            sendData,
                            sendData.length,
                            broadcastAddress,
                            port);

            socket.send(sendPacket);

            System.out.println("Broadcast sent: " + request);

            // Listen for responses
            long endTime = System.currentTimeMillis() + 5000;

            while (System.currentTimeMillis() < endTime) {
                try {
                    byte[] recvBuffer = new byte[4096];

                    DatagramPacket recvPacket =
                            new DatagramPacket(
                                    recvBuffer,
                                    recvBuffer.length);

                    socket.receive(recvPacket);

                    String resp = new String(
                            recvPacket.getData(),
                            recvPacket.getOffset(),
                            recvPacket.getLength(),
                            StandardCharsets.UTF_8);

                    String expected = deviceID + " is at:";

                    if (resp.contains(expected)) {
                        int colonPos = resp.indexOf(':');

                        if (colonPos >= 0) {
                            System.out.println(
                                    "Found DC Direct at: "
                                            + resp.substring(colonPos + 1).trim());
                        }
                    }

                } catch (SocketTimeoutException e) {
                    // No more responses received within timeout.
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}