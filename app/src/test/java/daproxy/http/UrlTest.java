package daproxy.http;

import java.net.MalformedURLException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UrlTest {

    private static Stream<Arguments> provideAuthorityFormURIs() {
        return Stream.of(Arguments.of("asdf.com:443", "asdf.com", 443),
                Arguments.of("blog.digitalabyss.ca:443", "blog.digitalabyss.ca", 443));
    }

    private static Stream<Arguments> provideInvalidURIs() {
        return Stream.of(Arguments.of("blah"), Arguments.of("a@a"));

    }

    @ParameterizedTest
    @MethodSource("provideAuthorityFormURIs")
    public void testGetHostAndPort(String input, String expectedAuthority, int expectedPort)
            throws MalformedURLException {
        Url url = new Url(input);

        assertThat(url.getHost()).isEqualTo(expectedAuthority);
        assertThat(url.getPort()).isEqualTo(expectedPort);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidURIs")
    public void testInvalidURLs(String input) {
        Url url = new Url(input);
        assertThatThrownBy(() -> {
            url.getHost();
            url.getPort();
        }).isInstanceOf(MalformedURLException.class);
    }

}
