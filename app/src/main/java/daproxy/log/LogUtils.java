package daproxy.log;

public class LogUtils {


    /**
     * Converts part of a byte array to a hex string
     * @param bytes
     * @param start - index to start at
     * @param finish - index to finish at
     * @return - a hex string
     */
    public static String bytesToHex(byte[] bytes, int start, int finish) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < finish; i++) {
            byte aByte = bytes[i];
            result.append(String.format("%02x ", aByte));
        }
        return result.toString();
    }
}
