package tftp.shared;

public class Configuration {

    public static final int MAX_DATA_LENGTH = 512;
    public static final int MAX_PACKET_LENGTH = MAX_DATA_LENGTH + 4;
    public static final int DEFAULT_SERVER_PORT = 6009;
    public static final int MAX_TIMEOUTS = 5;
    public static final int MAX_INVALIDS = 5;
    public static int TIMEOUT = 3000;

}
