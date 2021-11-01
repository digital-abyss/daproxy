package daproxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

//import daproxy.log.LogUtils;

public class IOHelper {

    private static final int BUFFER_SIZE = 1024;
    private static final int END_OF_STREAM = -1;

    /**
     * A helper method to write bytes from an InputStream to an OutputStream, and throw an exception once detecting the socket is closed.
     * NOTE: using InputStream.transferTo(OutputStream) seems to block indefinitely and hangs the server.
     * @param in
     * @param out
     * @throws IOException
     */
    public static void writeInputToOutput(InputStream in, OutputStream out) throws IOException {
        int recvBytes;
        byte[] buf = new byte[BUFFER_SIZE];

        while ((recvBytes = in.read(buf)) != END_OF_STREAM) {
            // System.out.println("Thread: Writing " + recvBytes + " to socket");
            // System.out.println("HEX Output: " + LogUtils.bytesToHex(buf, 0, recvBytes));
            out.write(buf, 0, recvBytes);
        }

        if (recvBytes == END_OF_STREAM) { // socket is closed
            throw new SocketException("Detected closed socket");
        }
    }

}
