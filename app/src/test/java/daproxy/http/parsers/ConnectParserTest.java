package daproxy.http.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import daproxy.http.RequestMethod;
import daproxy.http.exceptions.IncompleteRequestException;
import daproxy.http.exceptions.InvalidRequestException;
import daproxy.http.request.ConnectRequest;
import daproxy.http.request.Request;

public class ConnectParserTest {
    private static Stream<Arguments> provideHttpRequestString() {
        return Stream.of(Arguments.of("CONNECT asdf.com:443 HTTP/1.1\r\n\r\n", "asdf.com", 443), Arguments.of(
                "CONNECT blog.digitalabyss.ca:443 HTTP/1.1\r\nHost: blog.digitalabyss.ca:443\r\nUser-Agent: curl/7.74.0\r\nProxy-Connection: Keep-Alive\r\n\r\n",
                "blog.digitalabyss.ca", 443));
    }

    @ParameterizedTest
    @MethodSource("provideHttpRequestString")
    public void testConnectParserValidScenarios(String connectString, String url, int port)
            throws MalformedURLException, InvalidRequestException, IncompleteRequestException {

        byte[] requestAsBytes = connectString.getBytes(StandardCharsets.US_ASCII);

        ConnectParser cp = new ConnectParser();
        Request req = cp.parse(requestAsBytes, requestAsBytes.length);

        assertThat(req.getMethod()).isEqualTo(RequestMethod.CONNECT);
        ConnectRequest cr = (ConnectRequest) req;
        assertThat(cr.getUrl().getHost()).isEqualTo(url);
        assertThat(cr.getUrl().getPort()).isEqualTo(port);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "C",
        "CO",
        "CONNECT",
        "CONNECT ",
        "CONNECT a",
        "CONNECT asdf.com",
        "CONNECT blog.digitalabyss.ca:443",
        "CONNECT blog.digitalabyss.ca:443 HTTP/1.1",
        "CONNECT blog.digitalabyss.ca:443 HTTP/1.1\r",
        "CONNECT blog.digitalabyss.ca:443 HTTP/1.1\r\n",
        "CONNECT blog.digitalabyss.ca:443 HTTP/1.1\r\n\r",

    })
    public void testIncompleteRequest(String connectString) {
        byte[] requestAsBytes = connectString.getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> {
            ConnectParser cp = new ConnectParser();
            cp.parse(requestAsBytes, requestAsBytes.length);
    
        }).isInstanceOf(IncompleteRequestException.class);
    }

    private static Stream<Arguments> provideInvalidConnectRequests() {
        return Stream.of(
            Arguments.of("CONNECTA asdf.com:443 HTTP/1.1\r\n\r\n", InvalidRequestException.class), 
            Arguments.of("CONNECT blog .digitalabyss.ca HTTP/1.1 \r\n\r\n ", InvalidRequestException.class),
            Arguments.of("CONNECT blog.digitalabyss.ca \r\n\r\n ", InvalidRequestException.class)
            );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidConnectRequests")
    public void testInvalidConnect(String connectString, Class<? extends Exception> exceptionType) {
        byte[] requestAsBytes = connectString.getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> {
            ConnectParser cp = new ConnectParser();
            cp.parse(requestAsBytes, requestAsBytes.length);
    
        }).isInstanceOf(exceptionType);
    }
}
