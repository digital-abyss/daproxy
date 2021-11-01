package daproxy.pool;

import java.util.Map;
import java.util.Map.Entry;

import daproxy.http.RequestHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LongRequestCanceller implements Runnable {

    private static final long MAX_WAIT = 1500;
    private static final long SLEEP_TIME = 150;
    private final Map<RequestHandler, Long> activeRequests;

    public LongRequestCanceller(Map<RequestHandler, Long> activeRequests) {
        this.activeRequests = activeRequests;
    }

    @Override
    public void run() {
        while (true) {
            for (Entry<RequestHandler, Long> entry : activeRequests.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() > MAX_WAIT) {
                    entry.getKey().terminate();
                }
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ex) {
                log.error("LongRequestCanceller interrupted, and ceasing to check for long running threads", ex);
                break;
            }
        }
    }
}
