package server;

import server.level.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class Server {
    public static Level level;

    private static final Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();

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
        PrintWriter out = null;

        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            clients.add(out);

            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(" ");

                if (message.startsWith("BLOCK_BREAK")) {
                    String[] coords = parts[1].split(",");
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());
                    int z = Integer.parseInt(coords[2].trim());

                    level.setTile(x, y, z, 0);
                    broadcast(String.format("BLOCK_BREAK %d,%d,%d", x, y, z));

                } else if (message.startsWith("BLOCK_PLACE")) {
                    String[] coords = parts[1].split(",");
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());
                    int z = Integer.parseInt(coords[2].trim());

                    level.setTile(x, y, z, 1);
                    broadcast(String.format("BLOCK_PLACE %d,%d,%d", x, y, z));

                } else {
                    out.println("[ECHO] " + message);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                clients.remove(out);
            }
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            System.out.println("Client disconnected.");
        }
    }

    private static void broadcast(String message) {
        for (PrintWriter client : clients) {
            client.println(message);
        }
    }
}
