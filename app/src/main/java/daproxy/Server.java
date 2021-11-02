package daproxy;

import java.net.Socket;

import daproxy.conf.Config;
import daproxy.http.RequestHandler;
import daproxy.pool.RequestPool;
import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;

@Slf4j
public class Server {

    private final RequestPool pool = new RequestPool();

    public Server() {

    }

    public void start() {
        try {
            Config conf = Config.getConfig();

            try (ServerSocket sSocket = new ServerSocket(conf.getListenPort())){

                while(true) {
                    Socket aSocket = sSocket.accept();
    
                    pool.submit(new RequestHandler(aSocket));
    
                }
            }
        } catch (Exception ex) {
            log.error("Unable to start the server. Exiting.", ex);
        }   
    }
    
}
