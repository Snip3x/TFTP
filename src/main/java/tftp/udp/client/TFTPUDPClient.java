package tftp.udp.client;

import tftp.shared.Configuration;
import tftp.shared.ErrorType;
import tftp.shared.Mode;
import tftp.shared.TFTPException;
import tftp.shared.packet.ErrorPacket;
import tftp.shared.packet.ReadRequestPacket;
import tftp.shared.packet.WriteRequestPacket;
import tftp.udp.UDPUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

public class TFTPUDPClient extends GenericTFTPClient {

    public TFTPUDPClient(int port) {
        super(port);
    }

    @Override
    public void get(String remoteFile, String localFile) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);
            
            //open an output stream to the local file
            try (FileOutputStream fos = new FileOutputStream(localFile)) {

                //receive the file from the server, specifying the first packet in the 'communication' to be
                // a read request packet
                UDPUtil.FileReceiver.receive(
                        socket,
                        new ReadRequestPacket(remoteFile, Mode.OCTET),
                        remoteAddress,
                        remotePort,
                        fos
                );

            } catch (FileNotFoundException fnfe) {
                //file not found exception occurs "if the file exists but is a directory rather than a regular file,
                // does not exist but cannot be created, or cannot be opened for any other reason"
                System.out.println("unable to write to: " + localFile);
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "unable to write to: " + localFile
                );
                DatagramPacket datagram = UDPUtil.toDatagram(errorPacket, remoteAddress, remotePort);
                //send an error packet to the server if this happens
                socket.send(datagram);
            }

        } catch (SocketException e) {
            System.out.println("error: socket could not be opened");
        } catch (TFTPException e) {
            System.out.println(e.getMessage());
        } catch (IOException ignore) {
            //only reaches here if unable to send error packet, but already printed error message by this time
        }
    }


    @Override
    public void put(String localFile, String remoteFile) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            //open an input stream to read from the given file
            try (FileInputStream fis = new FileInputStream(localFile)) {

                //send the file to the server, specifying the first packet in the 'communication' to be
                // a write request packet
                UDPUtil.FileSender.send(
                        socket,
                        new WriteRequestPacket(remoteFile, Mode.OCTET),
                        remoteAddress,
                        remotePort,
                        fis,
                        (short) 0
                );

            } catch (FileNotFoundException e) {
                System.out.println("file not found: " + localFile);
            } catch (TFTPException e) {
                System.out.println(e.getMessage());
            }

        } catch (SocketException e) {
            System.out.println("error: socket could not be opened");
        } catch (IOException e) {
            System.out.println("error closing file input stream");
        }
    }

    public static void main(String[] args) {
        Thread client = new TFTPUDPClient(Configuration.DEFAULT_SERVER_PORT);
        client.start();
    }

}
