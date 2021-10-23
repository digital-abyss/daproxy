package daproxy.http.parsers;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import daproxy.http.exceptions.IncompleteRequestException;
import daproxy.http.exceptions.InvalidRequestException;
import daproxy.http.request.ConnectRequest;
import daproxy.http.request.Request;
import daproxy.log.LogUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectParser implements RequestParser{

    @Override
    public Request parse(byte[] requestBytes, int length) throws InvalidRequestException, IncompleteRequestException {
        // TODO Auto-generated method stub
        return parseConnectRequest(requestBytes, length);
    }


    private static final String CONNECT_TOKEN = "CONNECT ";
    private static final String HTTP_TOKEN = "HTTP/1.1";
    private static final byte CR = (byte)'\r';
    private static final byte LF = (byte)'\n';

    /**
     * Returns true if a valid HTTP/1.1 CONNECT request.
     * See https://httpwg.org/specs/rfc7231.html#CONNECT, https://datatracker.ietf.org/doc/html/rfc2817, and https://httpwg.org/specs/rfc7231.html#CONNECT
     * for more details.
     * This implementation does not support request pipelining, and will not evaluate any requests if pipelined.
     * Any subsequent data on the socket will be discarded.
     * @param request - a String object containing a sequence of characters.
     * @return true if the sequence contains a valid connect request
     * 
     */
    public static ConnectRequest parseConnectRequest(byte[] buf, int dataReceived) throws InvalidRequestException, IncompleteRequestException {
        log.debug("Parsing Connect Request\n" + new String(buf,0, dataReceived, StandardCharsets.US_ASCII));
        if (dataReceived < CONNECT_TOKEN.length()) {
            throw new IncompleteRequestException("method not finished");
        }

        byte[] connectToken = CONNECT_TOKEN.getBytes(StandardCharsets.US_ASCII);

        int i = 0;

        for( ; i < connectToken.length; ++i) {
           
            if (connectToken[i] != buf[i]) {
                throw new InvalidRequestException("Invalid Request.  Server only handles CONNECT Requests");
            }
        }
        log.debug("Found Connect Token i = " + i);
        
        //parse url
        boolean foundUrlSpace = false;
        for( ; i < dataReceived; ++i) {
            if(buf[i] == ' ') {
                foundUrlSpace = true;
                break;
            }
        }
        //43 4f 4e 4e 45 43 54 20 62 6c 6f 67 2e 64 69 67 69 74 61 6c 61 62 79 73 73 2e 63 61 3a 34 34 33 20 48 54 54 50 2f 31 2e 31 0d 0a 48 6f 73 74 3a 20 62 6c 6f 67 2e 64 69 67 69 74 61 6c 61 62 79 73 73 2e 63 61 3a 34 34 33 0d 0a 55 73 65 72 2d 41 67 65 6e 74 3a 20 63 75 72 6c 2f 37 2e 37 34 2e 30 0d 0a 50 72 6f 78 79 2d 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 4b 65 65 70 2d 41 6c 69 76 65 0d 0a 0d 0a 
        //43 4f 4e 4e 45 43 54 20 62 6c 6f 67 2e 64 69 67 69 74 61 6c 61 62 79 73 73 2e 63 61 3a 34 34 33 20 48 54 54 50 2f 31 2e 31 0d 0a 48 6f 73 74 3a 20 62 6c 6f 67 2e 64 69 67 69 74 61 6c 61 62 79 73 73 2e 63 61 3a 34 34 33 0d 0a 55 73 65 72 2d 41 67 65 6e 74 3a 20 63 75 72 6c 2f 37 2e 37 34 2e 30 0d 0a 50 72 6f 78 79 2d 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 4b 65 65 70 2d 41 6c 69 76 65 0d 0a 0d 0a 
        //0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35
        //CONNECT blog.digitalabyss.ca:443 HTTP/1.1
        //0123456789012345678901234567890123456789
                                        //32

        if(!foundUrlSpace) {
            throw new IncompleteRequestException("url not finished");
        }
        String url = new String(buf, CONNECT_TOKEN.length(), i - CONNECT_TOKEN.length(), StandardCharsets.US_ASCII);
        log.debug("Found URL = " + url + " i = " + i);
        log.debug("buf = " + LogUtils.bytesToHex(buf, 0, dataReceived));
        //validate Protocol
        byte[] httpToken = HTTP_TOKEN.getBytes(StandardCharsets.US_ASCII);
        i++;
        log.debug("httpToken = " + LogUtils.bytesToHex(httpToken, 0, httpToken.length));
        for(int j = 0 ; i < dataReceived && j < HTTP_TOKEN.length(); ++i, ++j) {
            log.debug("i = " + i + " j = " + j + " buf[i] = " + buf[i] + " httpToken[j] = " + httpToken[j]);
            if (httpToken[j] != buf[i]) {
                throw new InvalidRequestException("Invalid HTTP Protocol. Server only handles HTTP/1.1");
            }
        }

        log.debug("Validated Protocol");
        //find first CRLF
        for ( ; i < dataReceived ; ++i) {
            if (buf[i] != ' ' && buf[i] != CR && buf[i] != LF) {
                throw new InvalidRequestException("Invalid characters after Protocol.");
            }

            if (buf[i] == LF && buf[i-1] == CR) {
                break;
            }
        }
        log.debug("Found first CRLF at " + i);
        //if I have back-to-back CRLF's (can have spaces in between), then CONNECT request is finished
        //need to implement a backtracking algorithm to find back-to-back CRLF's
        int headersStart = i++;
        
        boolean allCharsAreSpaces = true;
        for( ; i < dataReceived ; ++i) {
            if (buf[i] != ' ' && buf[i] != LF && buf[i] != CR) {
                allCharsAreSpaces = false;
            }
            if (buf[i] == LF && buf[i-1] == CR) {
                if (allCharsAreSpaces) {
                    break;  //found end of request
                } else {
                    allCharsAreSpaces = true;
                }
            }
        }
        if ( i >= dataReceived ) {
            throw new IncompleteRequestException("Headers are incomplete");
        }
        log.debug("Found complete request i = " + i + " dataReceived = " + dataReceived);
        byte[] remainingBytes = new byte[0];
        if ( i + 1 < dataReceived) {
            remainingBytes = new byte[dataReceived - i];
            for (int j = 9 ; j < dataReceived - i ; j++) {
                remainingBytes[j] = buf[i+j];
            }
        }

        String headersBlob = new String(buf, headersStart, i, StandardCharsets.US_ASCII);
        return new ConnectRequest(url, headersBlob, remainingBytes);
    }


}
