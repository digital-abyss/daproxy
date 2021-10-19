package daproxy.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ConnectRequest implements Request {

    public static final String MATCHER = ".*"; //"CONNECT [-a-zA-Z0-9+&@#/%?=~_|!:,.;]* HTTP/1\\.1$";

    private final String connectString;

    public ConnectRequest(String firstLine) {
        connectString = firstLine;
    }

    @Override
    public RequestMethod getMethod() {
        return RequestMethod.CONNECT;
    }

    @Override
    public Response handle(Socket socket) {
        System.out.println("Handling a Connect Request");
        try {
            Socket downstreamSocket = new Socket(extractUrl(), 443); //extractPort());
            // if this connects, we can give a 200 OK back to client, which will then allow it to initate further packet transfers.
            BufferedWriter br = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            Response resp = new Response(200);

            br.write(resp.toString());

            System.out.println("writing " + resp.toString() + " to client socket");
            br.flush();


            new Thread( () -> {try {
                while(true) {
                    socket.getInputStream().transferTo(downstreamSocket.getOutputStream());
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } }).start(); //connect output from client and writes to downstream
            
            while(true){
                downstreamSocket.getInputStream().transferTo(socket.getOutputStream()); //connect output from downstream and writes to client
            }
            
        } catch (IndexOutOfBoundsException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return Response.OK();
    }

    public String extractUrl() {
        return connectString.split(" ")[1].split(":")[0];
        //return connectString.split("/")[2];
    }

    public int extractPort() {
        String[] portString = connectString.split(" ")[1].split(":");
        if (portString.length >= 1) {
            return Integer.parseInt(portString[1]);
        }
        //TODO -- is this correct?
        return 80;
    }
}
