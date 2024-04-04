import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private int clientId;
    private int redPixelX;
    private int redPixelY;
    private static int redPixelSize = 50;

    public ClientHandler(Socket clientSocket,  int clientId) {
        this.clientSocket = clientSocket;
        this.clientId = clientId;
        try {
            this.outputStream = clientSocket.getOutputStream();
            this.inputStream = clientSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            sendInitialData(); // Send initial data to the client
            while (true) {
                // Receive movement updates from the client and update the red pixel position
                receiveAndUpdateMovement();
            }
        } catch (IOException e) {
            System.err.println("Client " + clientId + " disconnected");
            ParticleSimulator.removeClient(this); // Remove the disconnected client
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendInitialData() {
        try {
            // Send initial data to client
            outputStream.write(("INITIAL_DATA:" + clientId+ ":" + redPixelX + ":" + redPixelY + ":").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendWhitePixelPosition(int yPos) {
        try {
            // Send the position of the white pixel to the client, not sure if this is the best way for particles
            outputStream.write(("WHITE_PIXEL_Y:" + yPos + ";").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveAndUpdateMovement() throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);
        String movementUpdate = new String(buffer, 0, bytesRead).trim();
        if (movementUpdate.startsWith("MOVE:")) {
            String[] parts = movementUpdate.split(":")[1].split(",");
            int newX = Integer.parseInt(parts[0]);
            int newY = Integer.parseInt(parts[1]);
            redPixelX = newX;
            redPixelY = newY;
            // masterPanel.updateRedPixelPosition(clientId, redPixelX, redPixelY);
            // masterPanel.repaint();
        }
    }

    public int getRedPixelX() {
        return redPixelX;
    }

    public int getRedPixelY() {
        return redPixelY;
    }

    public static int getRedPixelSize() {
        return redPixelSize;
    }

    public int getClientId() {
        return clientId;
    }
    
    public void sendMessage(String message) {
        try {
            outputStream.write(message.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    public void notifyGone(int clientId) {
        try {
            outputStream.write(("DISCONNECTED_CLIENT:" + clientId + ";").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
