package daproxy;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

public class App {
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        System.out.println(new App().getGreeting());

        try {

            ServerSocket sSocket = new ServerSocket(8085);

            while(true) {
                Socket aSocket = sSocket.accept();

                SocketAddress remoteAddr = aSocket.getRemoteSocketAddress();
                SocketAddress localAddr = aSocket.getLocalSocketAddress();
                System.out.println("accepted connection from " + remoteAddr + " with connection " + localAddr);
                
                String text = new BufferedReader(new InputStreamReader(aSocket.getInputStream())).lines().collect(Collectors.joining("\n"));
                
                System.out.println("Text = " + text);
                
                aSocket.close();

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
