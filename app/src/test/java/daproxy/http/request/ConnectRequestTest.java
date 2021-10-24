package daproxy.http.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


public class ConnectRequestTest {
    
    // private static Stream<Arguments> provideHttpHeaderString() {
    //     return Stream.of( 
    //             Arguments.of("CONNECT asdf.com:443 HTTP/1.1", "asdf.com", 443)
    //     );
    // }

    // @ParameterizedTest 
    // @MethodSource("provideHttpHeaderString")
    // public void testExractionMethods(String connectString, String url, int port ) {
    //     ConnectRequest cr = new ConnectRequest(connectString);
    //     assertThat(cr.extractUrl()).isEqualTo(url);
    //     assertThat(cr.extractPort()).isEqualTo(port);
    // }
}
