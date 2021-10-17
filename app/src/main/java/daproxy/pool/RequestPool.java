package daproxy.pool;

import daproxy.http.RequestHandler;

public class RequestPool {


    public void submit(RequestHandler handler) {
        System.out.println("RequestPool: received requestHandler: " + handler);

        handler.handle();
    }
    
}
