import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientServer extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PIXEL_SIZE = 50;
    private static final Color RED_COLOR = Color.RED;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private JPanel panel;
    private Rectangle redPixel;
    private int whitePixelY = 0;
    private int clientId;

    // Map to store positions of red pixels of other clients
    private Map<Integer, Rectangle> redPixels = new HashMap<>();

    public ClientServer() {
        setTitle("Client");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };
        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(panel);

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    handleMovement(e.getKeyCode());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        requestFocus();
    }

    private void draw(Graphics g) {
        g.setColor(RED_COLOR);
        g.fillRect(redPixel.x, redPixel.y, PIXEL_SIZE, PIXEL_SIZE);

        // Draw red pixels for other clients
        g.setColor(RED_COLOR);
        for (Rectangle otherRedPixel : redPixels.values()) {
            g.fillRect(otherRedPixel.x, otherRedPixel.y, PIXEL_SIZE, PIXEL_SIZE);
        }

        // Draw the white pixel
        g.setColor(Color.WHITE);
        g.fillRect((WIDTH - PIXEL_SIZE) / 2, whitePixelY, PIXEL_SIZE, PIXEL_SIZE);
    }

    public void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // Receive initial data from the server (e.g., position of the red pixel)
            redPixel = new Rectangle(0, 0, PIXEL_SIZE, PIXEL_SIZE);
            receiveInitialData();

            

            new Thread(this::receiveUpdates).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveInitialData() throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);
        String initialData = new String(buffer, 0, bytesRead);
        String[] parts = initialData.split(":");
        if (parts.length >= 0 && parts[0].equals("INITIAL_DATA")) {
            // Update the position of the red pixel
            clientId = Integer.parseInt(parts[1]);
            redPixel.x = Integer.parseInt(parts[2]);
            redPixel.y = Integer.parseInt(parts[3]);
            System.out.println("CLIENTID: " + clientId);
            panel.repaint();
        }
    }

    private void handleMovement(int keyCode) throws IOException {
        switch (keyCode) {
            case KeyEvent.VK_UP:
                redPixel.y -= 10;
                break;
            case KeyEvent.VK_DOWN:
                redPixel.y += 10;
                break;
            case KeyEvent.VK_LEFT:
                redPixel.x -= 10;
                break;
            case KeyEvent.VK_RIGHT:
                redPixel.x += 10;
                break;
        }
        outputStream.write(("MOVE:" + redPixel.x + "," + redPixel.y + "\n").getBytes());
        outputStream.flush();
        panel.repaint();
    }

    private void receiveUpdates() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead != -1) {
                    String update = new String(buffer, 0, bytesRead).trim();
                    receiveAndUpdatePositions(update);
                    test(update);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveAndUpdatePositions(String update) {
        String[] parts = update.split(":");
        // for(int i = 0; i < parts.length; i++){
        //     System.out.println("Parts: " + parts[i]);
        // }
        if (parts.length >= 1) {
            if (parts[0].equals("WHITE_PIXEL_Y")) {
                //System.out.println("Parts0: " + parts[0]);
                whitePixelY = Integer.parseInt(parts[1]);
                panel.repaint();
                //System.out.println("Parts2: " + parts[2]);
            } 
        }
    }

    public void test(String update){
        String[] parts = update.split(":");
        if (parts[0].equals("PIXEL_POSITIONS")) {
            String[] pixelData = parts[1].split(";");
            for (String data : pixelData) {
                String[] info = data.split(",");
                if (info.length >= 3) {
                    int id = Integer.parseInt(info[0]);
                    int newX = Integer.parseInt(info[1]);
                    int newY = Integer.parseInt(info[2]);
                        Rectangle otherRedPixel = redPixels.get(id);
                        if (otherRedPixel == null) {
                            // Create a new red pixel rectangle for this client
                            otherRedPixel = new Rectangle(newX, newY, PIXEL_SIZE, PIXEL_SIZE);
                            redPixels.put(id, otherRedPixel);
                        } else {
                            otherRedPixel.x = newX;
                            otherRedPixel.y = newY;
                        }
                    
                }
            }
            panel.repaint();
        }
    }
    
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientServer clientServer = new ClientServer();
            clientServer.connectToServer();
            clientServer.setVisible(true);
        });
    }
}
