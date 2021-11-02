
# Exploring existing solutions

One approach is to use an existing proxy solution.  For example [mitmproxy](https://mitmproxy.org/) can be configured to operate as a proxy.

1. Download/Install mitmproxy.
2. Start the proxy, with configuration to not "man in the middle" https traffic. This seems to have worked for me:
```
./mitmproxy --listen-port 8086 --tcp ".*" --ignore ".*"
```
3.  Send a proxy request as described in the [Readme](src/../README.md##Using DAProxy)
