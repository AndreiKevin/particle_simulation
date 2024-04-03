import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;

public class ParticleSimulator extends JFrame {

    private static SimulatorPanel simulatorPanel;
    private final ExecutorService executorService;
    private static double lastUpdateTime;
    private boolean zoomed = false;
    private static int fps;
    private static int frames;
    private JTextField wX1, wX2, wY1, wY2, numInputs, n2, n3, xStartField, yStartField, xEndField, yEndField, startAngleField, endAngleField, startVelocityField, endVelocityField, singleX, singleY, singleV, singleA;
    private int spriteX, spriteY;
    private static final int SPRITE_SPEED = 1;
    private JComboBox<String> dropdownBox;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int nextClientId = 1;
    private int particleID = 1;
    ServerSocket serverSocket;

    private static final Object particleLock = new Object(); //Object just for synchronized blocks

    private JPanel inputPanel;

    public ParticleSimulator() {
        super("Particle Simulator");
        final double targetFPS = 80.0;
        final double targetFrameTime = 1.0 / targetFPS;
        //long elapsedTime, sleepTime;
        final double deltaTime = 0.016;
        final double particleSize = 10;
        executorService = Executors.newWorkStealingPool();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        simulatorPanel = new SimulatorPanel(deltaTime, particleSize);
        simulatorPanel.setPreferredSize(new Dimension(1280, 720));
        add(simulatorPanel);
        setupUserInterface();
        pack();
        setVisible(true);
        try {
            serverSocket = new ServerSocket(12345);
            System.out.println("Master Server started.");

            // Start a thread to handle the movement of the white pixel
            new Thread(() -> {
                while (true) {
                    long startTime = System.nanoTime();
                    synchronized (particleLock) {
                        updateParticles(deltaTime, particleSize);
                    }
                    repaint();
            
                    final long elapsedTime = System.nanoTime() - startTime;
                    final long sleepTime = (long) ((targetFrameTime - elapsedTime / 1e9) * 1000);
            
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
            }
                    synchronized (particleLock) {
                        notifyParticlesToClients();
                        notifyPixelPositionsToClients();
                    }
                }
            }).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
            
                // Create a new client handler thread for each client
                ClientHandler clientHandler = new ClientHandler(clientSocket, nextClientId++);
                clients.add(clientHandler);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }

    public SimulatorPanel getSimulatorPanel() {
        return simulatorPanel;
    }

    public static void removeClient(ClientHandler client/* , SimulatorPanel masterPanel*/) {
        clients.remove(client);
        clientGone(client.getClientId());
        //masterPanel.repaint(); // Repaint the master panel to remove the disconnected client's red pixel
    }

    private static void clientGone(int clientId) {
        for (ClientHandler client : clients) {
            client.notifyGone(clientId);
        }
    }


    private static void notifyParticlesToClients() {
        synchronized (clients) {
            StringBuilder message = new StringBuilder("C:");
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
    }

    private static void notifyPixelPositionsToClients() {
        synchronized (clients) {
            synchronized (simulatorPanel.getParticles()) {
                for (ClientHandler client : clients) {
                    for(Particle particle : simulatorPanel.getParticles()){
                        StringBuilder message = new StringBuilder("P:");
                        message.append(particle.getID())
                            .append(",")
                            .append((int) particle.getX())
                            .append(",")
                            .append((int) particle.getY())
                            .append(";")
                            .append("\n");
                        client.sendMessage(message.toString());
                    }
                }
            }
        }
    }
    
    

    private void updateParticles(double deltaTime, double particleSize) {
        CountDownLatch latch = new CountDownLatch(simulatorPanel.getParticles().size());

        for (Particle particle : simulatorPanel.getParticles()) {
            executorService.submit(() -> {
                particle.move(deltaTime);
                simulatorPanel.checkWallCollision(particle, deltaTime, particleSize);
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void setupUserInterface() {
        //dropdownBox = new JComboBox<>(new String[]{"Default singular particle", "Const Velocity + Angle", "Const Start + Varying Angle", "Const Start + Varying Velocity", "Add wall"});
        dropdownBox = new JComboBox<>(new String[]{"Default singular particle", "Const Velocity + Angle", "Const Start + Varying Angle", "Const Start + Varying Velocity"});
        numInputs = new JTextField();
        wX1 = new JTextField();
        wX2 = new JTextField();
        wY1 = new JTextField();
        wY2 = new JTextField();
        n2 = new JTextField();
        n3 = new JTextField();
        xStartField = new JTextField();
        yStartField = new JTextField();
        xEndField = new JTextField();
        yEndField = new JTextField();
        singleX = new JTextField();
        singleY = new JTextField();
        singleV = new JTextField();
        singleA = new JTextField();
        startAngleField = new JTextField();
        endAngleField = new JTextField();
        startVelocityField = new JTextField();
        endVelocityField = new JTextField();
        JButton addButton = new JButton("Add");
        AtomicBoolean inAdventure = new AtomicBoolean(false);

        inputPanel = new JPanel(new CardLayout());
        inputPanel.add(singleParticle(), "Default singular particle");
        inputPanel.add(createConstantVelocityAndAnglePanel(), "Const Velocity + Angle");
        inputPanel.add(createConstantStartPointAndVaryingAnglePanel(), "Const Start + Varying Angle");
        inputPanel.add(createConstantStartPointAndVaryingVelocityPanel(), "Const Start + Varying Velocity");
        //inputPanel.add(addWall(), "Add wall");
    
        JButton clearParticlesButton = new JButton("Clear Particles");
        clearParticlesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearParticles();
            }
        });

        JButton adventureModeButton = new JButton("Adventure Mode");
        adventureModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addButton.setEnabled(inAdventure.getAndSet(!inAdventure.get()));
                simulatorPanel.requestFocus();
                simulatorPanel.requestFocusInWindow();
                simulatorPanel.toggleZoom();
            }
        });
        adventureModeButton.addActionListener(e -> {
            zoomed = !zoomed;
        });

        //JButton clearWalls = new JButton("Clear Walls");
        /*
         * clearWalls.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearWalls();
            }
        });
         */
        
    
        JPanel addPanel = new JPanel();
        addPanel.add(addButton);
        addPanel.add(adventureModeButton);

        JPanel clearPanel = new JPanel();
        clearPanel.setLayout(new GridLayout(1, 2));
        clearPanel.add(clearParticlesButton);
        //clearPanel.add(clearWalls);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 2));
        buttonPanel.add(addPanel);
        buttonPanel.add(clearPanel);
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0; 
        controlPanel.add(dropdownBox, gbc);
    
        gbc.gridy = 1;
        controlPanel.add(inputPanel, gbc);
    
        gbc.gridy = 2;
        controlPanel.add(buttonPanel, gbc);
    
        dropdownBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIComponents();
                revalidate();
                repaint();
            }
        });
    
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String selectedMethod = (String) dropdownBox.getSelectedItem();
                //Because I'm lazy
                synchronized (particleLock) {
                switch (selectedMethod) {
                    case "Add wall":
                        int x1 = Integer.parseInt(wX1.getText());
                        int x2 = Integer.parseInt(wX2.getText());
                        int y1 = Integer.parseInt(wY1.getText());
                        int y2 = Integer.parseInt(wY2.getText());
                        Wall wall = new Wall(x1, y1, x2, y2);
                        simulatorPanel.addWall(wall);
                        break;
                    case "Default singular particle":
                        double X = Double.parseDouble(singleX.getText());
                        double Y = Double.parseDouble(singleY.getText());
                        double angle = Double.parseDouble(singleA.getText());
                        double velocity = Double.parseDouble(singleV.getText());
                        Particle particle = new Particle(particleID++, X, Y, velocity, angle);
                        simulatorPanel.addParticle(particle);
                        break;
                    case "Const Velocity + Angle":
                        int n = Integer.parseInt(numInputs.getText());
                        double startX = Double.parseDouble(xStartField.getText());
                        double startY = Double.parseDouble(yStartField.getText());
                        double endX = Double.parseDouble(xEndField.getText());
                        double endY = Double.parseDouble(yEndField.getText());
                        simulatorPanel.addParticlesFixedVelocityAndAngle(n, startX, startY, endX, endY, 50, 45);
                        break;
                    case "Const Start + Varying Angle":
                        n = Integer.parseInt(n2.getText());
                        double startAngle = Double.parseDouble(startAngleField.getText());
                        double endAngle = Double.parseDouble(endAngleField.getText());
                        simulatorPanel.addParticlesFixedStartPointAndVelocity(n, 800, 300, startAngle, endAngle, 50);
                        break;
                    case "Const Start + Varying Velocity":
                        n = Integer.parseInt(n3.getText());
                        double startVelocity = Double.parseDouble(startVelocityField.getText());
                        double endVelocity = Double.parseDouble(endVelocityField.getText());
                        simulatorPanel.addParticlesFixedStartPointAndAngle(n, 0, 0, 45, startVelocity, endVelocity);
                        break;
                }
                }
            }
        });

        JFrame controlFrame = new JFrame("Control Panel");
        controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        controlFrame.add(controlPanel);
        controlFrame.setPreferredSize(new Dimension(285, 280));
        controlFrame.pack();
    
        controlFrame.setLocation(1290, 300);
    
        controlFrame.setVisible(true);
    }

    private void clearParticles() {
        synchronized (simulatorPanel) {
            simulatorPanel.getParticles().clear();
        }
        simulatorPanel.repaint();
    }

    private void clearWalls() {
        synchronized (simulatorPanel) {
            simulatorPanel.getWalls().clear();
        }
        simulatorPanel.repaint();
    }

    private JPanel addWall() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("X1:"));
        panel.add(wX1);
        panel.add(new JLabel("Y1:"));
        panel.add(wY1);
        panel.add(new JLabel("X2:"));
        panel.add(wX2);
        panel.add(new JLabel("Y2:"));
        panel.add(wY2);
        return panel;
    }

    private JPanel singleParticle() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("X:"));
        panel.add(singleX);
        panel.add(new JLabel("Y:"));
        panel.add(singleY);
        panel.add(new JLabel("Velocity:"));
        panel.add(singleV);
        panel.add(new JLabel("Angle:"));
        panel.add(singleA);

        return panel;
    }

    private JPanel createConstantVelocityAndAnglePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Number of Particles:"));
        panel.add(numInputs);
        panel.add(new JLabel("Start X:"));
        panel.add(xStartField);
        panel.add(new JLabel("Start Y:"));
        panel.add(yStartField);
        panel.add(new JLabel("End X:"));
        panel.add(xEndField);
        panel.add(new JLabel("End Y:"));
        panel.add(yEndField);
        return panel;
    }

    private JPanel createConstantStartPointAndVaryingAnglePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Number of Particles:"));
        panel.add(n2);
        panel.add(new JLabel("Start angle:"));
        panel.add(startAngleField);
        panel.add(new JLabel("End angle:"));
        panel.add(endAngleField);
        return panel;
    }

    private JPanel createConstantStartPointAndVaryingVelocityPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Number of Particles:"));
        panel.add(n3);
        panel.add(new JLabel("Start velocity:"));
        panel.add(startVelocityField);
        panel.add(new JLabel("End velocity:"));
        panel.add(endVelocityField);
        return panel;
    }

    private void updateUIComponents() {
        String selectedMethod = (String) dropdownBox.getSelectedItem();
        CardLayout cardLayout = (CardLayout) inputPanel.getLayout();
        cardLayout.show(inputPanel, selectedMethod);
        revalidate();
        repaint();
    }

    private class SimulatorPanel extends JPanel {
        private List<Particle> particles;
        private List<Wall> walls;
        private final int canvasWidth = 1280;
        private final int canvasHeight = 720;
        private BufferedImage offScreenBuffer;
        private double particleSize;
        private BufferedImage redPixelSprite;
        private boolean zoomed = false;
        private final int desired_width = 33; // the desired adventure mode in width of the canvas
        private final int desired_height = 19; // the desired aventure mode height of the canvas
        private final double scale_factor_width = canvasWidth / desired_width;
        private final double scale_factor_height = canvasHeight / desired_height;
        private double zoomFactor = 1;
        final double zoom2 = Math.min(scale_factor_width, scale_factor_height); 
        private int offsetX = 0;
        private int offsetY = 0;
        private InputStream inputStream;
        private int redPixelX;
        private int redPixelY;
        private Map<Integer, Point> clientSprite;
    
        public SimulatorPanel(double deltaTime, double particleSize) {
            this.particleSize = particleSize;
            particles = Collections.synchronizedList(new ArrayList<Particle>());
            walls = new ArrayList<>();
            spriteX = (canvasWidth) / 2;
            spriteY = (canvasHeight) / 2;
            setFocusable(true);
            addKeyListener(new ArrowKeyListener());
            offScreenBuffer = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            redPixelSprite = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            redPixelSprite.setRGB(0, 0, Color.RED.getRGB());
            setBackground(Color.gray);

			//Set up border walls
			walls.add(new Wall(-10, -10, 1290, -10));
			walls.add(new Wall(-10, -10, -10, 730));
			walls.add(new Wall(1290, -10, 1290, 730));
			walls.add(new Wall(-10, 730, 1290, 730));
        }

        
        private class ArrowKeyListener implements KeyListener {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if(zoomed){
                    switch (keyCode) {
                case KeyEvent.VK_UP:
                    if(spriteY > 0)
                        spriteY -= SPRITE_SPEED;
                    break;
                case KeyEvent.VK_DOWN:
                    if(spriteY < canvasHeight - 1)
                        spriteY += SPRITE_SPEED;
                    break;
                case KeyEvent.VK_LEFT:
                    if(spriteX > 0)
                        spriteX -= SPRITE_SPEED;
                    break;
                case KeyEvent.VK_RIGHT:
                    if(spriteX < canvasWidth - 1)
                        spriteX += SPRITE_SPEED;
                    break;
            }

                int canvasCenterX = getWidth() / 2;
                int canvasCenterY = getHeight() / 2;
                offsetX = canvasCenterX - (int) (spriteX * zoomFactor) - canvasWidth / 2;
                offsetY = canvasCenterY - (int) (spriteY * zoomFactor) - canvasHeight / 2;
                
                repaint();
                }
            }
        
            @Override
            public void keyTyped(KeyEvent e) {
        
            }
        
            @Override
            public void keyReleased(KeyEvent e) {
        
            }
        }

        // public void receiveAndUpdateMovement() throws IOException {
        //     byte[] buffer = new byte[1024];
        //     int bytesRead = inputStream.read(buffer);
        //     String movementUpdate = new String(buffer, 0, bytesRead).trim();
        //     if (movementUpdate.startsWith("MOVE:")) {
        //         String[] parts = movementUpdate.split(":")[1].split(",");
        //         int clientId = Integer.parseInt(parts[0]);
        //         int newX = Integer.parseInt(parts[1]);
        //         int newY = Integer.parseInt(parts[2]);
        //         redPixelX = newX;
        //         redPixelY = newY;
        //         updateRedPixelPosition(clientId, redPixelX, redPixelY);
        //         repaint();
        //     }
        // }

        // public void updateRedPixelPosition(int clientId, int newX, int newY) {
        //     clientSprite.put(clientId, new Point(newX, newY));
        //     repaint();
        // }
    
        public List<Particle> getParticles() {
            return particles;
        }
    
        public void addParticle(Particle particle) {
            particles.add(particle);
        }
    
        public void addWall(Wall wall) {
            walls.add(wall);
        }

        public List<Wall> getWalls() {
            return walls;
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D offScreenGraphics = (Graphics2D) offScreenBuffer.getGraphics();
            super.paintComponent(offScreenGraphics);

            // int scaledWidth = (int) (canvasWidth * zoomFactor);
            // int scaledHeight = (int) (canvasHeight * zoomFactor);
            // offsetX = (getWidth() - scaledWidth) / 2;
            // offsetY = (getHeight() - scaledHeight) / 2;
            //System.out.println(offsetX + " " + offsetY);
            if(zoomed)
                offScreenGraphics.translate(offsetX+640, offsetY+360);
            else
                offScreenGraphics.translate(offsetX, offsetY);
            
            offScreenGraphics.scale(zoomFactor, zoomFactor);

            drawParticles(offScreenGraphics, particleSize);
            drawWalls(offScreenGraphics);

            offScreenGraphics.setColor(Color.RED);
            offScreenGraphics.fillRect(spriteX, spriteY, 1, 1);

            //offScreenGraphics.scale(1.0 / zoomFactor, 1.0 / zoomFactor);
            offScreenGraphics.translate(-offsetX, -offsetY);

            g.drawImage(offScreenBuffer, 0, 0, this);
    
            frames++;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime >= 500) {
                fps = (int) (frames / ((currentTime - lastUpdateTime) / 1000.0));
                frames = 0;
                lastUpdateTime = currentTime;
            }
            g.drawString("FPS: " + fps, 10, 20);
        }

        private void updateZoom() {
            if (zoomed) {
                zoomFactor = zoom2; 
                int canvasCenterX = getWidth() / 2;
                int canvasCenterY = getHeight() / 2;

                offsetX = canvasCenterX - (int) (spriteX * zoomFactor) - canvasWidth / 2;
                offsetY = canvasCenterY - (int) (spriteY * zoomFactor) - canvasHeight / 2;
            } else {
                zoomFactor = 1.0; 
                offsetX = 0;
                offsetY = 0;
            }
        }
    
        public void toggleZoom() {
            zoomed = !zoomed;
            updateZoom();
        }
    
        private void drawParticles(Graphics g, double particleSize) {
            for (Particle particle : particles) {
                int x = (int) particle.getX();
                int y = getHeight() - (int) particle.getY() - (int) particleSize;
                g.drawOval(x, y, (int)particleSize, (int)particleSize);
            }
        }
    
        private void drawWalls(Graphics g) {
            for (Wall wall : walls) {
				Graphics2D g2 = (Graphics2D) g;
                int x1 = (int) wall.getX1();
                int y1 = getHeight() - (int) wall.getY1();
                int x2 = (int) wall.getX2();
                int y2 = getHeight() - (int) wall.getY2();
				g2.setStroke(new BasicStroke(20));
				g2.setPaint(Color.BLACK);
                g2.drawLine(x1, y1, x2, y2);
            }
        }

        private void checkWallCollision(Particle particle, double deltaTime, double particleSize) {
            if (particle.get_next_x(deltaTime) <= 0 || particle.get_next_x(deltaTime) + particleSize >= canvasWidth) {
                particle.bounceHorizontal();
            }
            if (particle.getY() <= 0 || particle.getY() + particleSize >= canvasHeight) {
                particle.bounceVertical();
            }

            for (Wall wall : walls) {
                if (isParticleCollidingWithWall(particle, wall, deltaTime, particleSize)) {
                    double wallAngle = Math.atan2(wall.getY2() - wall.getY1(), wall.getX2() - wall.getX1());
                    particle.bounceOffWall(wallAngle);
                }
            }
        }

        private boolean isParticleCollidingWithWall(Particle particle, Wall wall, double deltaTime, double particleSize) {
            double x3 = wall.getX1();
            double y3 = wall.getY1();
            double x4 = wall.getX2();
            double y4 = wall.getY2();
        
            double x1 = particle.getX();
            double y1 = particle.getY();
            double x2 = particle.get_next_x(deltaTime);
            double y2 = particle.get_next_y(deltaTime);
        
            double temp = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
            if (temp == 0) return false;
        
            double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / temp;
            double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / temp;
        
            boolean isColliding = t >= 0 && t <= 1 && u >= 0 && u <= 1;

            return isColliding;
        }

        public void addParticlesFixedVelocityAndAngle(int n, double startX, double startY, double endX, double endY, double velocity, double angle) {
            double deltaX = (endX - startX) / (n - 1);
            double deltaY = (endY - startY) / (n - 1);
            for (int i = 0; i < n; i++) {
                double x = startX + i * deltaX;
                double y = startY + i * deltaY;
                addParticle(new Particle(particleID++, x, y, velocity, angle));
            }
        }

        public void addParticlesFixedStartPointAndVelocity(int n, double startX, double startY, double startAngle, double endAngle, double velocity) {
            double deltaAngle = (endAngle - startAngle) / (n - 1);
            for (int i = 0; i < n; i++) {
                double angle = startAngle + i * deltaAngle;
                addParticle(new Particle(particleID++, startX, startY, velocity, angle));
            }
        }

        public void addParticlesFixedStartPointAndAngle(int n, double startX, double startY, double angle, double startVelocity, double endVelocity) {
            double deltaVelocity = (endVelocity - startVelocity) / (n - 1);
            for (int i = 0; i < n; i++) {
                double velocity = startVelocity + i * deltaVelocity;
                addParticle(new Particle(particleID++, startX, startY, velocity, angle));
            }
        }
    }

    public static void main(String[] args) {
        new ParticleSimulator();
    }
}
