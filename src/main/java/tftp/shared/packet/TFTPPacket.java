package tftp.shared.packet;

import tftp.shared.TFTPException;

import java.nio.ByteBuffer;


public abstract class TFTPPacket {

    public abstract byte[] getPacketBytes();

    public abstract PacketType getPacketType();

    public static TFTPPacket fromByteArray(byte[] buffer, int length) throws TFTPException {
        short opcode = ByteBuffer.wrap(buffer).getShort();
        PacketType type = PacketType.fromOpcode(opcode);

        switch (type) {
            case ACKNOWLEDGEMENT:
                return new AcknowledgementPacket(buffer, length);
            case DATA:
                return new DataPacket(buffer, length);
            case ERROR:
                return new ErrorPacket(buffer, length);
            case READ_REQUEST:
                return new ReadRequestPacket(buffer, length);
            case WRITE_REQUEST:
                return new WriteRequestPacket(buffer, length);
            default:
                throw new TFTPException("unknown packet type: " + type);
        }

    }

}
