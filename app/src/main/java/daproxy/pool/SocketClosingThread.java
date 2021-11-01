package daproxy.pool;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import daproxy.http.RequestHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocketClosingThread implements Runnable {

    private final BlockingQueue<RequestHandler> taskQueue;
    private final Map<RequestHandler, Long> activeRequests;

    public SocketClosingThread(BlockingQueue<RequestHandler> taskQueue, Map<RequestHandler, Long> activeRequests) {
        this.taskQueue = taskQueue;
        this.activeRequests = activeRequests;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            RequestHandler request = null;
            try {
                request = taskQueue.take();
                activeRequests.put(request, System.currentTimeMillis());
                request.run();
            } catch (InterruptedException ex) {
                log.debug("Thread is interrupted - will stop taking tasks", ex);
                break;
            } finally {
                if (request != null) {
                    activeRequests.remove(request);
                }
            }
        }
    }
}