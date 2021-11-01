package daproxy.pool;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import daproxy.http.RequestHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * A basic Thread pool implementation with two protections: 1) Bounded incoming
 * requests based on QUEUE_SIZE 2) Background thread to kill long running
 * requests to prevent a Denial-of-Service
 */

@Slf4j
public class RequestPool {

    private final int QUEUE_SIZE = 2048;
    private final int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private final BlockingQueue<RequestHandler> requestQueue;
    private final Map<RequestHandler, Long> activeRequests;
    private final Set<Thread> threadPool;
    private final LongRequestCanceller canceller;

    public RequestPool() {

        requestQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        activeRequests = new ConcurrentHashMap<>();
        threadPool = new HashSet<>();
        canceller = new LongRequestCanceller(activeRequests);
        new Thread(canceller).start();

        for (int i = 0; i < corePoolSize; i++) {
            Thread thread = new Thread(new SocketClosingThread(requestQueue, activeRequests), "RequestPoolThread-" + i);
            thread.start();
            threadPool.add(thread);
        }
    }

    public void submit(RequestHandler handler) {
        if (!requestQueue.offer(handler)) { // System is at capacity.
            handler.reject();
        }
    }

}
