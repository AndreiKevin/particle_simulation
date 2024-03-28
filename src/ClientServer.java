import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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
            receiveInitialData();

            redPixel = new Rectangle(0, 0, PIXEL_SIZE, PIXEL_SIZE);

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
        if (parts.length == 3 && parts[0].equals("INITIAL_DATA")) {
            // Update the position of the red pixel
            redPixel.x = Integer.parseInt(parts[1]);
            redPixel.y = Integer.parseInt(parts[2]);
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
                    String[] parts = update.split(":");
                    if (parts.length == 2) {
                        if (parts[0].equals("WHITE_PIXEL_Y")) {
                            whitePixelY = Integer.parseInt(parts[1]);
                            panel.repaint();
                        } else if (parts[0].equals("RED_PIXEL_POSITION")) {
                            String[] coordinates = parts[1].split(",");
                            int redX = Integer.parseInt(coordinates[0]);
                            int redY = Integer.parseInt(coordinates[1]);
                            redPixel = new Rectangle(redX, redY, PIXEL_SIZE, PIXEL_SIZE);
                            panel.repaint();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
