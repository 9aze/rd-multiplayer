package server;

import server.level.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static Level level;

    public static void main(String args[]) throws IOException {
        level = new Level(256, 256, 64);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Saving level...");
            level.save();
        }));

        ServerSocket serverSocket = new ServerSocket(9090);
        System.out.println("server started...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println(String.format("client connected from: %s", clientSocket.getInetAddress().getHostAddress()));

            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String message;
            while((message = in.readLine()) != null) {
                String[] parts = message.split(" ");

                if(message.startsWith("BLOCK_BREAK")) {
                    String args = parts[1];

                    String[] coords = args.split(",");
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());
                    int z = Integer.parseInt(coords[2].trim());
                    level.setTile(x, y, z, 0);
                    out.println(String.format("BLOCK_BREAK %s,%s,%s", x, y, z));
                } else if(message.startsWith("BLOCK_PLACE")) {
                    String args = parts[1];

                    String[] coords = args.split(",");
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());
                    int z = Integer.parseInt(coords[2].trim());
                    level.setTile(x, y, z, 1);
                    out.println(String.format("BLOCK_PLACE %s,%s,%s", x, y, z));
                }


                out.println("(Echo) " + message);
            }

            System.out.println("Client disconnected.");
            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
