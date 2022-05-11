package tftp.udp.server;

import tftp.shared.Configuration;
import tftp.shared.ErrorType;
import tftp.shared.Mode;
import tftp.shared.TFTPException;
import tftp.shared.packet.*;
import tftp.udp.UDPUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TFTPUDPServer extends Thread {

    private final int port;
    private final ExecutorService executor;

    public TFTPUDPServer(int port) {
        this.port = port;
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        try {
            //create a new datagram socket and bind to the given port
            DatagramSocket socket = new DatagramSocket(port);

            //allocate a buffer for holding received datagrams
            byte[] buffer = new byte[Configuration.MAX_PACKET_LENGTH];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            //loop forever until forcibly stopped
            while (true) {

                try {
                    //receive a datagram packet from the network - this method blocks
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    System.out.println("error receiving packet: " + e);
                    continue;
                }

                try {
                    //extract the TFTP packet from the datagram
                    TFTPPacket packet = UDPUtil.fromDatagram(receivePacket);

                    //if the packet is a RRQ or WRQ, submit a job to the executor
                    // to respond to the client, otherwise ignore.
                    switch (packet.getPacketType()) {
                        case READ_REQUEST:
                            executor.submit(new ServerRRQHandler(
                                    receivePacket.getAddress(),
                                    receivePacket.getPort(),
                                    (ReadRequestPacket) packet
                            ));
                            break;
                        case WRITE_REQUEST:
                            executor.submit(new ServerWRQHandler(
                                    receivePacket.getAddress(),
                                    receivePacket.getPort(),
                                    (WriteRequestPacket) packet
                            ));
                            break;
                        default:
                            System.out.println("received packet " + packet + ", ignoring");
                            break;
                    }

                } catch (TFTPException e) {
                    System.out.println("error parsing received packet: " + e);
                }
            }

        } catch (SocketException e) {
            System.out.println("failed to start server: " + e);
        }
    }

    public static void main(String[] args) {
        int port = Configuration.DEFAULT_SERVER_PORT;

        //parse the optional arguments
        for (int i = 0; i < args.length - 1; ++i) {
            if (args[i].equals("-port")) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException nfe) {
                    System.out.println("invalid port: " + args[i + 1]);
                    return;
                }
            } else if (args[i].equals("-timeout")) {
                try {
                    Configuration.TIMEOUT = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException nfe) {
                    System.out.println("invalid timeout: " + args[i + 1]);
                    return;
                }
            }
        }

        //run the server, passing the port as an argument
        TFTPUDPServer server = new TFTPUDPServer(port);
        server.start();
    }

    public static class ServerWRQHandler implements Runnable {

        private InetAddress clientAddress;
        private int clientPort;
        private final WriteRequestPacket wrq;


        public ServerWRQHandler(InetAddress clientAddress, int clientPort, WriteRequestPacket wrq) {
            this.clientAddress = clientAddress;
            this.clientPort = clientPort;
            this.wrq = wrq;
        }

        @Override
        public void run() {
            System.out.println("responding to request: " + wrq + " from client: " + clientAddress + ":" + clientPort);

            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(Configuration.TIMEOUT);

                if (wrq.getMode() != Mode.OCTET) {
                    ErrorPacket error = new ErrorPacket(ErrorType.UNDEFINED, "unsupported mode: " + wrq.getMode());
                    socket.send(UDPUtil.toDatagram(error, clientAddress, clientPort));
                    System.out.println("unsupported mode: " + wrq.getMode());
                    return;
                }

                //open output stream to the file specified in the write request
                try (FileOutputStream fos = new FileOutputStream(wrq.getFileName())) {

                    //receive the file from the client, specifying the first packet to be
                    // acknowledging packet 0 as specified in the RFC
                    UDPUtil.FileReceiver.receive(
                            socket,
                            new AcknowledgementPacket((short) 0),
                            clientAddress,
                            clientPort,
                            fos
                    );

                } catch (FileNotFoundException fnfe) {
                    //some sort of error occurred in writing to the file, print a message and send that
                    // same message to the client in an error packet
                    System.out.println("unable to write to: " + wrq.getFileName());
                    ErrorPacket errorPacket = new ErrorPacket(
                            ErrorType.FILE_NOT_FOUND,
                            "unable to write to: " + wrq.getFileName()
                    );
                    DatagramPacket datagram = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                    socket.send(datagram);
                } catch (TFTPException e) {
                    //an error occurred in receiving the file, just print an error and end this handler
                    System.out.println(e.getMessage());
                }

            } catch (IOException e) {
                //couldn't even open a socket - give up
                // also could happen if the output stream failed to close, but that doesn't really matter
                System.out.println("failed to receive: " + e.getMessage());
            }
        }

    }

    public static class ServerRRQHandler implements Runnable {

        private final InetAddress clientAddress;
        private final int clientPort;
        private final ReadRequestPacket rrq;

        public ServerRRQHandler(InetAddress clientAddress, int clientPort, ReadRequestPacket rrq) {
            this.clientAddress = clientAddress;
            this.clientPort = clientPort;
            this.rrq = rrq;
        }

        @Override
        public void run() {
            System.out.println("responding to request: " + rrq + " from client: " + clientAddress + ":" + clientPort);

            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(Configuration.TIMEOUT);

                if (rrq.getMode() != Mode.OCTET) {
                    ErrorPacket error = new ErrorPacket(ErrorType.UNDEFINED, "unsupported mode: " + rrq.getMode());
                    socket.send(UDPUtil.toDatagram(error, clientAddress, clientPort));
                    System.out.println("unsupported mode: " + rrq.getMode());
                    return;
                }

                try (FileInputStream fis = new FileInputStream(rrq.getFileName())) {

                    byte[] first = new byte[Configuration.MAX_DATA_LENGTH];
                    int read = fis.read(first);
                    if (read == -1) read = 0;
                    DataPacket data = new DataPacket((short) 1, first, read);

                    UDPUtil.FileSender.send(socket, data, clientAddress, clientPort, fis, (short) 1);

                } catch (FileNotFoundException e) {
                    ErrorPacket errorPacket = new ErrorPacket(
                            ErrorType.FILE_NOT_FOUND,
                            "file not found: " + rrq.getFileName()
                    );
                    DatagramPacket sendPacket = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                    socket.send(sendPacket);
                } catch (TFTPException e) {
                    System.out.println(e.getMessage());
                }

            } catch (IOException e) {
                System.out.println("error: " + e.getMessage());
            }
        }

    }
}
