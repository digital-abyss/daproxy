package daproxy.http.parsers;

import java.nio.charset.StandardCharsets;

import daproxy.http.exceptions.IncompleteRequestException;
import daproxy.http.exceptions.InvalidRequestException;
import daproxy.http.request.ConnectRequest;
import daproxy.http.request.Request;
import daproxy.log.LogUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectParser implements RequestParser {

    @Override
    public Request parse(byte[] requestBytes, int length) throws InvalidRequestException, IncompleteRequestException {
        return parseConnectRequest(requestBytes, length);
    }

    private static final String CONNECT_TOKEN = "CONNECT ";
    private static final String HTTP_TOKEN = "HTTP/1.1";
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';

    /**
     * Returns ConnectRequest if input is a valid HTTP/1.1 CONNECT request,
     * otherwise throw either InvalidRequestException or IncompleteRequestException
     * See https://httpwg.org/specs/rfc7231.html#CONNECT,
     * https://datatracker.ietf.org/doc/html/rfc2817, and for more details. This
     * implementation does not support request pipelining, and will not evaluate any
     * requests if pipelined. Any subsequent data on the socket will be discarded.
     * Example CONNECT Request: CONNECT blog.digitalabyss.ca:443 HTTP/1.1
     * 
     * @param request - a String object containing a sequence of characters.
     * @return valid ConnectRequest if the sequence contains a valid connect request
     * 
     */
    private static ConnectRequest parseConnectRequest(byte[] buf, int dataReceived)
            throws InvalidRequestException, IncompleteRequestException {
        log.debug("Parsing Connect Request\n" + new String(buf, 0, dataReceived, StandardCharsets.US_ASCII));

        int i = checkForConnectToken(buf, dataReceived);

        i = parseUrl(buf, dataReceived, i);
        String url = new String(buf, CONNECT_TOKEN.length(), i - CONNECT_TOKEN.length(), StandardCharsets.US_ASCII);
        log.debug("Found URL = " + url + " i = " + i);

        i = validateProtocol(buf, dataReceived, i);

        i = findEndOfFirstLine(buf, dataReceived, i);

        // if there are back-to-back CRLF's (can have spaces in between), then CONNECT
        // request is finished
        int headersStart = i++;

        i = findEndOfRequest(buf, dataReceived, i);

        byte[] remainingBytes = keepRemainingBytesForWriting(buf, dataReceived, i);

        String headersBlob = new String(buf, headersStart, i - headersStart, StandardCharsets.US_ASCII);
        return new ConnectRequest(url, headersBlob, remainingBytes);
    }

    /**
     * It is (theoretically?) possible for the first bytes of data that's supposed
     * to be transmitted to the CONNECT host to be on the wire before the proxy
     * returns 200 OK to the client. This method extracts these bytes to be written
     * to the downstream host.
     * 
     * @param buf
     * @param dataReceived
     * @param i
     * @return
     */
    private static byte[] keepRemainingBytesForWriting(byte[] buf, int dataReceived, int i) {
        byte[] remainingBytes = new byte[0];
        if (i + 1 < dataReceived) {
            remainingBytes = new byte[dataReceived - i];
            for (int j = 9; j < dataReceived - i; j++) {
                remainingBytes[j] = buf[i + j];
            }
        }
        return remainingBytes;
    }

    private static int findEndOfRequest(byte[] buf, int dataReceived, int i) throws IncompleteRequestException {
        boolean allCharsAreSpaces = true;
        for (; i < dataReceived; ++i) {
            if (buf[i] != ' ' && buf[i] != LF && buf[i] != CR) {
                allCharsAreSpaces = false;
            }
            if (buf[i] == LF && buf[i - 1] == CR) {
                if (allCharsAreSpaces) {
                    break; // found end of request
                } else {
                    allCharsAreSpaces = true;
                }
            }
        }
        if (i >= dataReceived) {
            throw new IncompleteRequestException("Headers are incomplete");
        }
        log.debug("Found complete request i = " + i + " dataReceived = " + dataReceived);
        return i;
    }

    /**
     * This method seeks to find the CRLF token indicating the end of the first line
     * in a CONNECT request.
     * 
     * @param buf
     * @param dataReceived
     * @param i
     * @return the index of the CRLF token.
     * @throws InvalidRequestException
     */
    private static int findEndOfFirstLine(byte[] buf, int dataReceived, int i) throws InvalidRequestException {
        // find first CRLF
        for (; i < dataReceived; ++i) {
            if (buf[i] != ' ' && buf[i] != CR && buf[i] != LF) {
                throw new InvalidRequestException("Invalid characters after Protocol.");
            }

            if (buf[i] == LF && buf[i - 1] == CR) {
                break;
            }
        }
        log.debug("Found first CRLF at " + i);
        return i;
    }

    private static int validateProtocol(byte[] buf, int dataReceived, int i) throws InvalidRequestException {
        byte[] httpToken = HTTP_TOKEN.getBytes(StandardCharsets.US_ASCII);
        i++;
        log.debug("httpToken = " + LogUtils.bytesToHex(httpToken, 0, httpToken.length));
        for (int j = 0; i < dataReceived && j < HTTP_TOKEN.length(); ++i, ++j) {
            // log.debug("i = " + i + " j = " + j + " buf[i] = " + buf[i] + " httpToken[j] =
            // " + httpToken[j]);
            if (httpToken[j] != buf[i]) {
                throw new InvalidRequestException("Invalid HTTP Protocol. Server only handles HTTP/1.1");
            }
        }
        log.debug("Validated Protocol");
        return i;
    }

    private static int parseUrl(byte[] buf, int dataReceived, int i) throws IncompleteRequestException {
        // parse url
        boolean foundUrlSpace = false;
        for (; i < dataReceived; ++i) {
            if (buf[i] == ' ') {
                foundUrlSpace = true;
                break;
            }
        }

        if (!foundUrlSpace) {
            throw new IncompleteRequestException("url not finished");
        }
        return i;
    }

    private static int checkForConnectToken(byte[] buf, int dataReceived)
            throws IncompleteRequestException, InvalidRequestException {
        if (dataReceived < CONNECT_TOKEN.length()) {
            throw new IncompleteRequestException("method not finished");
        }

        byte[] connectToken = CONNECT_TOKEN.getBytes(StandardCharsets.US_ASCII);

        int i = 0;

        for (; i < connectToken.length; ++i) {

            if (connectToken[i] != buf[i]) {
                throw new InvalidRequestException("Invalid Request.  Server only handles CONNECT Requests");
            }
        }
        log.debug("Found Connect Token i = " + i);
        return i;
    }
}
