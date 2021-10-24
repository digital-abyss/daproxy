package daproxy.http;

import java.net.MalformedURLException;

/**
 * A class for parsing URLs. The default java.net.url class throws exceptions if
 * things like the protocol aren't included Implemented a quick and dirty Url
 * parser for handling specifically CONNECT requests. For connect request, the
 * URL must be in authority-form:
 * https://httpwg.org/specs/rfc7230.html#authority-form About the authority
 * form: https://datatracker.ietf.org/doc/html/rfc3986#section-3.2 TODO:
 * consider replacing with: either https://github.com/smola/galimatias or
 * https://github.com/anthonynsimon/jurl
 */
public class Url {

    private final String url;

    public Url(String url) {
        this.url = url;
    }

    /**
     * 
     * @return The host section of the url
     */
    public String getHost() throws MalformedURLException {
        try {
            return url.split(":")[0];
        } catch (Exception ex) {
            throw new MalformedURLException("Invalid URL: " + url);
        }

    }

    /**
     * 
     * @return The port to connect to.
     */
    public int getPort() throws MalformedURLException {
        try {
            return Integer.parseInt(url.split(":")[1]);
        } catch (Exception ex) {
            throw new MalformedURLException("Invalid URL:" + url);
        }
    }
}
