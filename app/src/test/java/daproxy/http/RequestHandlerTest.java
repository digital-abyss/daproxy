package daproxy.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import daproxy.http.exceptions.InvalidRequestException;

public class RequestHandlerTest {

    private static Stream<Arguments> provideHttpHeaderString() {
        return Stream.of(Arguments.of("CONNECT asdf.com:443 HTTP/1.1", RequestMethod.CONNECT),
                Arguments.of("GET / HTTP/1.1", RequestMethod.GET));
    }

    @ParameterizedTest
    @MethodSource("provideHttpHeaderString")
    void testParseHTTPMethod(String httpText, RequestMethod expectedMethod) throws InvalidRequestException {

        Socket socket = mock(Socket.class);

        RequestHandler rh = new RequestHandler(socket);

        byte[] input = httpText.getBytes(StandardCharsets.US_ASCII);

        RequestMethod rm = rh.parseHTTPMethod(input, input.length);

        assertThat(rm).isEqualTo(expectedMethod);

    }

}
