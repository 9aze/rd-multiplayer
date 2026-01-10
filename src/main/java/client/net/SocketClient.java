package client.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient implements Runnable {
    private final String host;
    private final int port;
    public static PrintWriter out;

    public SocketClient(String host, int port){
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("This is a test msg");

            new Thread(() -> {
                String response;
                try {
                    while((response = in.readLine()) != null) {
                        System.out.println("Server: " + response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();


        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(String message){
        assert out != null;
        out.println(message);
    }
}
