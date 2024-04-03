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
import java.awt.image.BufferedImage;

public class ClientServer extends JFrame {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int PIXEL_SIZE = 10;
    private static final Color RED_COLOR = Color.RED;
    private static final Color GRAY_COLOR = Color.GRAY;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private JPanel mainPanel;
    private Rectangle redPixel;
    private int clientId;
    private final int desired_width = 33; // the desired adventure mode in width of the canvas
    private final int desired_height = 19; // the desired aventure mode height of the canvas
    private final double scale_factor_width = WIDTH / desired_width;
    private final double scale_factor_height = HEIGHT / desired_height;
    final double zoom2 = Math.min(scale_factor_width, scale_factor_height); 
    private int offsetX, offsetY;

    private final Object redPixelsLock = new Object();
    private final Object particlesLock = new Object();

    // Map to store positions of red pixels of other clients
    private Map<Integer, Rectangle> redPixels = new HashMap<>();
    private Map<Integer, Rectangle> particles = new HashMap<>();

    public ClientServer() {
        setTitle("Client");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };
        mainPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        mainPanel.setBackground(GRAY_COLOR); // Set background color to gray
        add(mainPanel);

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
        // Create offscreen image
        BufferedImage offscreenImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D offscreenGraphics = (Graphics2D) offscreenImage.getGraphics();
        
        int offsetX = (WIDTH - PIXEL_SIZE) / 2 - redPixel.x;
        int offsetY = (HEIGHT - PIXEL_SIZE) / 2 - redPixel.y;

        // Draw background
        offscreenGraphics.setColor(GRAY_COLOR);
        offscreenGraphics.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw red pixel
        offscreenGraphics.setColor(RED_COLOR);
        offscreenGraphics.fillRect(redPixel.x + offsetX, redPixel.y + offsetY, PIXEL_SIZE, PIXEL_SIZE);
        
        // Draw red pixels for other clients
        Map<Integer, Rectangle> redPixelsCopy;
        synchronized (redPixelsLock) {
            redPixelsCopy = new HashMap<>(redPixels);
        }
        for (Rectangle otherRedPixel : redPixelsCopy.values()) {
            offscreenGraphics.fillRect(otherRedPixel.x + offsetX, otherRedPixel.y + offsetY, PIXEL_SIZE, PIXEL_SIZE);
        }
        
        // Draw white pixels for particles
        Map<Integer, Rectangle> particlesCopy;
        synchronized (particlesLock) {
            particlesCopy = new HashMap<>(particles);
        }
        offscreenGraphics.setColor(Color.WHITE);
        for (Rectangle particle : particlesCopy.values()) {
            offscreenGraphics.fillRect(particle.x + offsetX, particle.y + offsetY, PIXEL_SIZE, PIXEL_SIZE);
        }
        
        // Dispose of offscreen graphics
        offscreenGraphics.dispose();
        
        // Draw offscreen image onto the main panel
        g.drawImage(offscreenImage, 0, 0, mainPanel);
    }
    
    
    

    private void handleMovement(int keyCode) throws IOException {
        int newX = redPixel.x;
        int newY = redPixel.y;
    
        switch (keyCode) {
            case KeyEvent.VK_UP:
                newY = Math.max(0, redPixel.y - 10); // Ensure newY is within bounds
                break;
            case KeyEvent.VK_DOWN:
                newY = Math.min(HEIGHT - PIXEL_SIZE, redPixel.y + 10); // Ensure newY is within bounds
                break;
            case KeyEvent.VK_LEFT:
                newX = Math.max(0, redPixel.x - 10); // Ensure newX is within bounds
                break;
            case KeyEvent.VK_RIGHT:
                newX = Math.min(WIDTH - PIXEL_SIZE, redPixel.x + 10); // Ensure newX is within bounds
                break;
        }
    
        // Update red pixel position
        redPixel.setLocation(newX, newY);
    
        // Send updated position to the server
        outputStream.write(("MOVE:" + newX + "," + newY + "\n").getBytes());
        outputStream.flush();

        int canvasCenterX = getWidth() / 2;
        int canvasCenterY = getHeight() / 2;
        offsetX = canvasCenterX - (int) (redPixel.getX() * zoom2) - WIDTH / 2;
        offsetY = canvasCenterY - (int) (redPixel.getY() * zoom2) - HEIGHT / 2;
    
        mainPanel.repaint(); // Repaint the panel to reflect the updated position
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
            mainPanel.repaint();
        }
    }

    public void receiveAndUpdatePositions(String update) {
        String[] parts = update.split(":");
        if (parts[0].equals("C")) {
            String[] pixelData = parts[1].split(";");
            for (String data : pixelData) {
                String[] info = data.split(",");
                if (info.length >= 3) {
                    int id = Integer.parseInt(info[0]);
                    int newX = Integer.parseInt(info[1]);
                    int newY = Integer.parseInt(info[2]);
                    synchronized (redPixelsLock) {
                        Rectangle otherRed = redPixels.get(id);
                        if (otherRed == null) {
                            otherRed = new Rectangle(newX, newY, PIXEL_SIZE, PIXEL_SIZE);
                            redPixels.put(id, otherRed);
                        } else {
                            otherRed.x = newX;
                            otherRed.y = newY;
                        }
                    }
                }
            }
            mainPanel.repaint();
        }
    }
    
    public void test(String update) {
        String[] parts = update.split(":");
        if (parts.length >= 2 && parts[0].equals("P")) {
            String[] pixelData = parts[1].split(";");
            for (String data : pixelData) {
                String[] info = data.split(",");
                if (info.length >= 3) {
                    int id = Integer.parseInt(info[0]);
                    int newX = Integer.parseInt(info[1]);
                    int newY = Integer.parseInt(info[2]);
                    synchronized (particlesLock) {
                        Rectangle otherParticles = particles.get(id);
                        if (otherParticles == null) {
                            otherParticles = new Rectangle(newX, newY, PIXEL_SIZE, PIXEL_SIZE);
                            particles.put(id, otherParticles);
                        } else {
                            otherParticles.x = newX;
                            otherParticles.y = newY;
                        }
                    }
                }
            }
            mainPanel.repaint();
        }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientServer clientServer = new ClientServer();
            clientServer.connectToServer();
            clientServer.setVisible(true);
        });
    }
}
