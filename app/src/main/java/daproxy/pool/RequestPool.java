package daproxy.pool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import daproxy.http.RequestHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestPool {

    private final int QUEUE_SIZE = 2048;
    private final int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private final int maximumPoolSize = corePoolSize;
    private final int keepAliveTime = 10000;
    private final TimeUnit unit = TimeUnit.MILLISECONDS;
    private final BlockingQueue<Runnable> requestQueue;
    private final ThreadPoolExecutor tp;

    public RequestPool() {
        requestQueue = new ArrayBlockingQueue<Runnable>(QUEUE_SIZE);
        tp = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, requestQueue);
    }

    public void submit(RequestHandler handler) {
        log.info("RequestPool: received requestHandler: {} ", handler);

        // handler.run();
        tp.execute(handler);
    }

}
