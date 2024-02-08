import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.util.ArrayList;

public class ParticleSimulator extends JFrame {

    private final SimulatorPanel simulatorPanel;
    private static ExecutorService executorService;
    private static double lastUpdateTime;
    private static int fps;
    private static int frames;
    private JTextField wX1, wX2, wY1, wY2, numInputs, n2, n3, xStartField, yStartField, xEndField, yEndField, startAngleField, endAngleField, startVelocityField, endVelocityField, singleX, singleY, singleV, singleA;

    private JComboBox<String> inputMethodComboBox;
    private JPanel inputPanel;

    public ParticleSimulator() {
        super("Particle Simulator");
        executorService = Executors.newFixedThreadPool(1);
        lastUpdateTime = System.currentTimeMillis();
        setSize(new Dimension(1600, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        simulatorPanel = new SimulatorPanel();
        add(simulatorPanel);

        setupUserInterface();
        setVisible(true);
        gameLoop();
    }

    private void gameLoop() {
        while (true) {
            simulatorPanel.repaint();
        }
    }

    private void setupUserInterface() {
        inputMethodComboBox = new JComboBox<>(new String[]{"Default singular particle", "Constant Velocity and Angle", "Constant Start Point and Varying Angle", "Constant Start Point and Varying Velocity", "Add wall"});
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

        inputPanel = new JPanel(new CardLayout());

        inputPanel.add(singleParticle(), "Default singular particle");
        inputPanel.add(createConstantVelocityAndAnglePanel(), "Constant Velocity and Angle");
        inputPanel.add(createConstantStartPointAndVaryingAnglePanel(), "Constant Start Point and Varying Angle");
        inputPanel.add(createConstantStartPointAndVaryingVelocityPanel(), "Constant Start Point and Varying Velocity");
        inputPanel.add(addWall(), "Add wall");

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(inputMethodComboBox, BorderLayout.NORTH);
        controlPanel.add(inputPanel, BorderLayout.CENTER);
        controlPanel.add(addButton, BorderLayout.SOUTH);

        inputMethodComboBox.addActionListener(new ActionListener() {
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

                String selectedMethod = (String) inputMethodComboBox.getSelectedItem();
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
                        Particle particle = new Particle(X, Y, velocity, angle);
                        simulatorPanel.addParticle(particle);
                    break;
                    case "Constant Velocity and Angle":
                        int n = Integer.parseInt(numInputs.getText());
                        double startX = Double.parseDouble(xStartField.getText());
                        double startY = Double.parseDouble(yStartField.getText());
                        double endX = Double.parseDouble(xEndField.getText());
                        double endY = Double.parseDouble(yEndField.getText());
                        simulatorPanel.addParticlesFixedVelocityAndAngle(n, startX, startY, endX, endY, 50, 45);
                    break;
                    case "Constant Start Point and Varying Angle":
                        n = Integer.parseInt(n2.getText());
                        double startAngle = Double.parseDouble(startAngleField.getText());
                        double endAngle = Double.parseDouble(endAngleField.getText());
                        simulatorPanel.addParticlesFixedStartPointAndVelocity(n, 0, 0, startAngle, endAngle, 50);
                    break;
                    case "Constant Start Point and Varying Velocity":
                        n = Integer.parseInt(n3.getText());
                        double startVelocity = Double.parseDouble(startVelocityField.getText());
                        double endVelocity = Double.parseDouble(endVelocityField.getText());
                        simulatorPanel.addParticlesFixedStartPointAndAngle(n, 0, 0, 45, startVelocity, endVelocity);
                        break;
                }
            }
        });

        add(controlPanel, BorderLayout.EAST);
    }

    private JPanel addWall(){
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

    private JPanel singleParticle(){
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
        String selectedMethod = (String) inputMethodComboBox.getSelectedItem();
        CardLayout cardLayout = (CardLayout) inputPanel.getLayout();
        cardLayout.show(inputPanel, selectedMethod);
        revalidate();
        repaint();
    }

    private static class SimulatorPanel extends JPanel {
        private List<Particle> particles;
        private List<Wall> walls;
        private final int canvasWidth = 1280;
        private final int canvasHeight = 720;

        public SimulatorPanel() {
            particles = new ArrayList<>();
            walls = new ArrayList<>();
            // Test Particle
            addParticle(new Particle(0, 0, 75, 45));
            // Test wall
            addWall(new Wall(0, 0, 500, 500));
            setBackground(Color.gray);
        }

        public void addParticle(Particle particle) {
            particles.add(particle);
        }

        public void addWall(Wall wall) {
            walls.add(wall);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            CountDownLatch latch = new CountDownLatch(particles.size());

            // Update and draw particles using thread pool
            for (Particle particle : particles) {
                executorService.submit(() -> {
                    particle.move(0.016); // Assuming 60 FPS, deltaTime ~ 1/60
                    // Update angle based on collision
                    checkWallCollision(particle);
                    latch.countDown();
                });
            }
            
            // Drawing of graphics should be done in the main thread because of Swing limitations
            try {
                latch.await(); // Wait till all threads are done before drawing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // Preserve interrupt status
            }

//TODO: I changed this part so that 0,0, starts at bottom left, not 100% if correct
            // Draw particles
            for (Particle particle : particles) {
                int particleSize = 10;
                int x = (int) particle.getX();
                int y = getHeight() - (int) particle.getY() - particleSize; // Adjust for the bottom left corner
                g.drawOval(x, y, particleSize, particleSize);
            }

            // Draw walls
            for (Wall wall : walls) {
                int x1 = (int) wall.getX1();
                int y1 = getHeight() - (int) wall.getY1();  // Adjust Y coordinate
                int x2 = (int) wall.getX2();
                int y2 = getHeight() - (int) wall.getY2();  // Adjust Y coordinate
                g.drawLine(x1, y1, x2, y2);
            }

            
            // Every render is one frame (FPS) so we add one to the frame counter
            frames++;
            // Get current time to compare how long it's been since the last update
            double currentTime = System.currentTimeMillis();
            // Update the displayed FPS every 0.5 seconds (500 milliseconds)
            if (currentTime - lastUpdateTime >= 500) {
                fps = (int) (frames / ((currentTime - lastUpdateTime) / 1000));
                frames = 0;
                lastUpdateTime = currentTime;
            }
            g.drawString("FPS: " + fps, 10, 20);
            
        } 

        private void checkWallCollision(Particle particle) {
            if (particle.getX() <= 0 || particle.getX() >= canvasWidth) {
                particle.bounceHorizontal();
            }
            if (particle.getY() <= 0 || particle.getY() >= canvasHeight) {
                particle.bounceVertical();
            }

            // Check collision with user made walls
            for (Wall wall : walls) {
                if (isParticleCollidingWithWall(particle, wall)) {
                    System.out.print("True");
                    double wallAngle = Math.atan2(wall.getY2() - wall.getY1(), wall.getX2() - wall.getX1());
                    particle.bounceOffWall(wallAngle);
                }
            }
        }

        private boolean isParticleCollidingWithWall(Particle particle, Wall wall) {
            double x3 = wall.getX1();
            double y3 = wall.getY1();
            double x4 = wall.getX2();
            double y4 = wall.getY2();
        
            double x1 = particle.getX();
            double y1 = particle.getY();
            double x2 = x1 + particle.getVelocityX();
            double y2 = y1 + particle.getVelocityY();
        
            double den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
            if (den == 0) return false; // Lines are parallel
        
            double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den;
            double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / den;
        
            return t >= 0 && t <= 1 && u >= 0 && u <= 1;
        }

        public void addParticlesFixedVelocityAndAngle(int n, double startX, double startY, double endX, double endY, double velocity, double angle) {
            double deltaX = (endX - startX) / (n - 1);
            double deltaY = (endY - startY) / (n - 1);
            for (int i = 0; i < n; i++) {
                double x = startX + i * deltaX;
                double y = startY + i * deltaY;
                addParticle(new Particle(x, y, velocity, angle));
            }
        }

        public void addParticlesFixedStartPointAndVelocity(int n, double startX, double startY, double startAngle, double endAngle, double velocity) {
            double deltaAngle = (endAngle - startAngle) / (n - 1);
            for (int i = 0; i < n; i++) {
                double angle = startAngle + i * deltaAngle;
                addParticle(new Particle(startX, startY, velocity, angle));
            }
        }

        public void addParticlesFixedStartPointAndAngle(int n, double startX, double startY, double angle, double startVelocity, double endVelocity) {
            double deltaVelocity = (endVelocity - startVelocity) / (n - 1);
            for (int i = 0; i < n; i++) {
                double velocity = startVelocity + i * deltaVelocity;
                addParticle(new Particle(startX, startY, velocity, angle));
            }
        }

    }

    public static void main(String[] args) {
        new ParticleSimulator();
    }
}
