package client.net;

import client.Minecraft;

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

            new Thread(() -> {
                String response;
                try {
                    while((response = in.readLine()) != null) {
                        String[] parts = response.split(" ");

                        if(response.startsWith("BLOCK_BREAK")) {
                            String args = parts[1];

                            String[] coords = args.split(",");
                            int x = Integer.parseInt(coords[0].trim());
                            int y = Integer.parseInt(coords[1].trim());
                            int z = Integer.parseInt(coords[2].trim());
                            Minecraft.mc.getLevel().setTile(x, y, z, 0);
                        } else if(response.startsWith("BLOCK_PLACE")) {
                            String args = parts[1];

                            String[] coords = args.split(",");
                            int x = Integer.parseInt(coords[0].trim());
                            int y = Integer.parseInt(coords[1].trim());
                            int z = Integer.parseInt(coords[2].trim());
                            Minecraft.mc.getLevel().setTile(x, y, z, 1);
                        }
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
        try {
            out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
