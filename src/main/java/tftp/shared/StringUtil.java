package tftp.shared;

import java.nio.charset.StandardCharsets;

/**
 * Provides string-related utilities for parsing TFTP packets according to the TFTP RFC.
 */
public class StringUtil {

    //String to byte array
    public static byte[] getBytes(String string) {
        //get bytes with correct character format
        byte[] bytes = string.getBytes(StandardCharsets.US_ASCII);
        byte[] addNull = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, addNull, 0, bytes.length);
        //add a null character at the end
        addNull[addNull.length - 1] = 0;
        return addNull;
    }

    //Byte array to String
    public static String getString(byte[] bytes, int offset) {
        //first, find the null byte position
        int nullPos = offset;
        while (nullPos < bytes.length && bytes[nullPos] != 0) {
            ++nullPos;
        }
        //given the null byte position, calculate the length of the string
        int length = nullPos - offset;
        //return a new string with the correct character format
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

}
