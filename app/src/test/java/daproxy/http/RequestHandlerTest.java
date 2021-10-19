package daproxy.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.Socket;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RequestHandlerTest {
    @ParameterizedTest 
    @ValueSource( strings = {
        "CONNECT asdf.com:443 HTTP/1.1",
    })
    void evaluateReturnsConnectRequest(String firstLine) {

        Socket socket = mock(Socket.class);

        RequestHandler rh = new RequestHandler(socket);

        assertThat(rh.evaluateRequest(firstLine).getMethod()).isEqualTo(RequestMethod.CONNECT);
     }
}
