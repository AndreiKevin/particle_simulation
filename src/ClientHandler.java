import java.awt.Color;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private int clientId;
    private int x;
    private int y;
    private Color color;
    private boolean active;
    private Semaphore activeSem = new Semaphore(1);
    private static int redPixelSize = 50;
    private List<Particle> particles;
    private Object particleLock;
    private List<ClientHandler> clients;

    public ClientHandler(Color color, boolean active, List<Particle> particles, Object particleLock, List<ClientHandler> clients) {
        this.color = color;
        this.active = active;
        this.particles = particles;
        this.particleLock = particleLock;
        this.clients = clients;
    }

    @Override
    public void run() {
        while(true) {
            try {
                activeSem.acquire();
                if (active) {
                    activeSem.release();
                    try {
                        sendInitialData(); // Send initial data to the client
                        while (active) {
                            // Receive movement updates from the client and update the red pixel position
                            receiveAndUpdateMovement();
                        }
                    } catch (IOException e) {
                        System.err.println("Client " + clientId + " disconnected");
                    } finally {
                        try {
                            System.out.println("Closing socket of client: " + clientId);
                            clientSocket.close();
                            ParticleSimulator.removeClient(this); // Remove the disconnected client
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    activeSem.release();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendParticleMessage(Particle particle) {
        StringBuilder message = new StringBuilder("P:");
        message.append(particle.getID())
            .append(",")
            .append((int) particle.getX())
            .append(",")
            .append((int) particle.getY())
            .append(",")
            .append((double) particle.getVelocity())
            .append(",")
            .append((double) particle.getAngle())
            .append(";");
        this.sendMessage(message.toString());
    }

    public void sendSpriteMessageToOtherClient(ClientHandler clientToSendTo, int clientIdThatMoved, int x, int y) {
        StringBuilder message = new StringBuilder("C:");
        message.append(this.clientId)
                .append(",")
                .append(this.getX())
                .append(",")
                .append(this.getY())
                .append(";");
        // using the ClientHandler of another client, we send the message (so that we send it to the other client and not to ourselves)
        clientToSendTo.sendMessage(message.toString());
    }

    public void sendSpriteMessageToThisClient(int clientIdThatMoved, int x, int y) {
        StringBuilder message = new StringBuilder("C:");
        message.append(clientIdThatMoved)
                .append(",")
                .append(this.getX())
                .append(",")
                .append(this.getY())
                .append(";");
        // using the ClientHandler of another client, we send the message (so that we send it to the other client and not to ourselves)
        for (ClientHandler client : clients) {
            if (client.getClientId() == clientIdThatMoved) {
                client.sendMessage(message.toString());
            }
        }
    }

    // This function sends all the current particles to the client
    public void sendInitialData() {
        synchronized (particleLock) {
            for (Particle particle : particles) {
                sendParticleMessage(particle);
            }
        }
        // send sprite positions of other clients to this client
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getClientId() != this.clientId) {
                    sendSpriteMessageToThisClient(this.clientId, client.getX(), client.getY());
                }
            }
        }
    }

    public void receiveAndUpdateMovement() throws IOException {
        byte[] buffer = new byte[1024];
        System.out.println("Waiting for client " + clientId + " to move");
        int bytesRead = inputStream.read(buffer);
        if (bytesRead == -1) {
            // The client has disconnected 
            // if the bytesRead is -1 
            // This is a signal by TCP that the socket connection if finished
            active = false;
            return;
        }
        String movementUpdate = new String(buffer, 0, bytesRead).trim();
        System.out.println("From client " + clientId + ": " + movementUpdate);
        if (movementUpdate.startsWith("MOVE:")) {
            String[] parts = movementUpdate.split(":")[1].split(",");
            int newX = Integer.parseInt(parts[0]);
            int newY = Integer.parseInt(parts[1]);
            this.x = newX;
            this.y = newY;
            
            // notify the other clients about the change
            notifySpritePositionsChangedToClients(this.clientId, newX, newY);
        }
    }

    private void notifySpritePositionsChangedToClients(int clientId, int x, int y) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getClientId() != this.clientId && client.isActive()) {
                    sendSpriteMessageToOtherClient(client, clientId, x, y);
                }
            }
        }
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public static int getSize() {
        return redPixelSize;
    }

    public int getClientId() {
        return this.clientId;
    }

    public Color getColor() {
        return this.color;
    }

    public boolean isActive() {
        boolean temp = false;
        try {
            activeSem.acquire();
            temp = this.active;
            activeSem.release();
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return temp;
    }

    public void setActive(int clientId, Socket clientSocket) {
        // Change to a new clientid but keep the color and position of the old client
        this.clientId = clientId;
        // Continue receiving position updates from the new client
        this.clientSocket = clientSocket;
        try {
            this.outputStream = clientSocket.getOutputStream();
            this.inputStream = clientSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            activeSem.acquire();
            this.active = true;
            activeSem.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            if (clientId == this.clientId) {
                System.out.println("Client " + clientId + " disconnected");
            } else {
                outputStream.write(("DISCONNECTED_CLIENT:" + clientId + ";").getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
