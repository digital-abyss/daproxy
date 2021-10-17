package daproxy;

import java.net.Socket;


import daproxy.http.RequestHandler;
import daproxy.pool.RequestPool;


import java.io.IOException;

import java.net.ServerSocket;

public class Server {

    private final RequestPool pool = new RequestPool();

    public Server() {

    }

    public void start() {

        try (ServerSocket sSocket = new ServerSocket(8085)){

            while(true) {
                Socket aSocket = sSocket.accept();

                pool.submit(new RequestHandler(aSocket));

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }   
    }
    
}
