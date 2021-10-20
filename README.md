# Requirements


## Sample Requests

### Using cUrl

```
curl -v https://blog.digitalabyss.ca --proxy http:/127.0.0.1:8086
```
This will instruct curl to issue an HTTP CONNECT request.

### Using wget

1. edit `/etc/wgetrc` to include `https_proxy` and `http_proxy` to point to your local proxy.
2. Use wget as normal

```
wget https://blog.digitalabyss.ca
```

## Debugging TLS handshakes

One way to debug is to use [wireshark](https://www.wireshark.org/)


# Exploring existing solutions

One approach is to use an existing proxy solution.  For example [mitmproxy](https://mitmproxy.org/) can be configured to operate as a proxy.

1. Download/Install mitmproxy.
2. Start the proxy, with configuration to not "man in the middle" https traffic. This seems to have worked for me:
```
./mitmproxy --listen-port 8086 --tcp ".*" --ignore ".*"
```
3.  Send a proxy request as described in ##SampleRequests


# Resources

* [HTTP Connect on MDM](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/CONNECT)
