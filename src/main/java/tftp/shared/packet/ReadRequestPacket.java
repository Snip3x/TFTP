package tftp.shared.packet;

import tftp.shared.Mode;
import tftp.shared.TFTPException;

public class ReadRequestPacket extends RequestPacket {

    public ReadRequestPacket(String file, Mode mode) {
        super(file, mode);
    }

    public ReadRequestPacket(byte[] bytes, int length) throws TFTPException{
        super(bytes, length);
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.READ_REQUEST;
    }

}
