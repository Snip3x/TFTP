package tftp.shared.packet;

import tftp.shared.Mode;
import tftp.shared.TFTPException;

public class WriteRequestPacket extends RequestPacket {

    public WriteRequestPacket(String file, Mode mode) {
        super(file, mode);
    }

    public WriteRequestPacket(byte[] bytes, int length) throws TFTPException{
        super(bytes, length);
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.WRITE_REQUEST;
    }

}
