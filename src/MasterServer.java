import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MasterServer {
    public static int WHITE_PIXEL_SIZE = 50;

    private static List<ClientHandler> clients = new ArrayList<>();
    private static int nextClientId = 1; // Unique ID for each client
    public static int whitePixelY = 0;
    private static boolean whitePixelMovingUp = true;

    public static void main(String[] args) {
        JFrame masterFrame = new JFrame("Master Server");
        masterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        masterFrame.setSize(300, 300);
        MasterPanel masterPanel = new MasterPanel();
        masterFrame.add(masterPanel);
        masterFrame.setVisible(true);

        try {
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Master Server started.");

            // Start a thread to handle the movement of the white pixel
            new Thread(() -> {
                while (true) {
                    moveWhitePixel();
                    masterPanel.repaint();
                    notifyWhitePixelToClients();
                    notifyPixelPositionsToClients();
                    try {
                        Thread.sleep(100); // Adjust the speed of the movement here
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
            
                // Create a new client handler thread for each client
                ClientHandler clientHandler = new ClientHandler(clientSocket, masterPanel, nextClientId++);
                clients.add(clientHandler);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void moveWhitePixel() {
        // Move the white pixel up and down just to make sure nothing hangs
        if (whitePixelMovingUp) {
            whitePixelY -= 5;
            if (whitePixelY < 0) {
                whitePixelY = 0;
                whitePixelMovingUp = false;
            }
        } else {
            whitePixelY += 5;
            if (whitePixelY > 350 - WHITE_PIXEL_SIZE) {
                whitePixelY = 350 - WHITE_PIXEL_SIZE;
                whitePixelMovingUp = true;
            }
        }
    }

    private static void notifyWhitePixelToClients() {
        for (ClientHandler client : clients) {
            StringBuilder message = new StringBuilder("WHITE_PIXEL_Y:" + MasterServer.whitePixelY + "\n");
            client.sendMessage(message.toString());
        }
    }
    
    private static void notifyPixelPositionsToClients() {
        StringBuilder message = new StringBuilder("PIXEL_POSITIONS:");
        for (ClientHandler client : clients) {
            message.append(client.getClientId())
                   .append(",")
                   .append(client.getRedPixelX())
                   .append(",")
                   .append(client.getRedPixelY())
                   .append(";");
        }
        message.append("\n");
        for (ClientHandler client : clients) {
            client.sendMessage(message.toString());
        }
    }
    

    public static List<ClientHandler> getClients() {
        return clients;
    }

    public static void removeClient(ClientHandler client, MasterPanel masterPanel) {
        clients.remove(client);
        clientGone(client.getClientId());
        masterPanel.repaint(); // Repaint the master panel to remove the disconnected client's red pixel
    }
    
    
    private static void clientGone(int clientId) {
        for (ClientHandler client : clients) {
            client.notifyGone(clientId);
        }
    }
    
}
