import java.util.List;
import java.util.ArrayList;

import java.awt.Graphics;
import java.awt.Dimension;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class ParticleSimulator extends JFrame {
    /* UI Structure:
    JFrame (the main window, represented by the ParticleSimulator class)
        SimulatorPanel: JPanel
        controlPanel: JPanel
            JLabel ("X:")
            JTextField (xPosField)
            JLabel ("Y:")
            JTextField (yPosField)
            JLabel ("Angle:")
            JTextField (angleField)
            JLabel ("Velocity:")
            JTextField (velocityField)
            JButton (addParticleButton)
            JLabel ("Wall X1:")
            JTextField (x1Field)
            JLabel ("Y1:")
            JTextField (y1Field)
            JLabel ("X2:")
            JTextField (x2Field)
            JLabel ("Y2:")
            JTextField (y2Field)
     */

    private final SimulatorPanel simulatorPanel;
    private static ExecutorService executorService;
    private static double lastUpdateTime;
    private static int fps;
    private static int frames;

    public ParticleSimulator() {
        super("Particle Simulator");

        // Maximum threads will blow up your computer
        //executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        // Do 2 threads only
        executorService = Executors.newFixedThreadPool(1);
        lastUpdateTime = System.currentTimeMillis();

        setSize(new Dimension(1280, 800)); // slightly bigger height for inputs
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // SimulatorPanel is a JPanel that shows the particles and walls
        simulatorPanel = new SimulatorPanel();
        add(simulatorPanel);

        // setupUserINterface creates a JPanel with the UI components
        setupUserInterface();

        // Make sure all components have been added before we render the UI
        setVisible(true); 

        // Start the game loop
        gameLoop();
    }

    private void gameLoop() {
        while (true) {
            simulatorPanel.repaint();
        }
    }

    private void setupUserInterface() {
        // Particle UI components
        JTextField xPosField = new JTextField(5);
        JTextField yPosField = new JTextField(5);
        JTextField angleField = new JTextField(5);
        JTextField velocityField = new JTextField(5);
        JButton addParticleButton = new JButton("Add Particle");

        // Wall UI components
        JTextField x1Field = new JTextField(5);
        JTextField y1Field = new JTextField(5);
        JTextField x2Field = new JTextField(5);
        JTextField y2Field = new JTextField(5);
        JButton addWallButton = new JButton("Add Wall");

        // Event listener for add particle button
        addParticleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double x = Double.parseDouble(xPosField.getText());
                double y = Double.parseDouble(yPosField.getText());
                double angle = Double.parseDouble(angleField.getText());
                double velocity = Double.parseDouble(velocityField.getText());
                Particle particle = new Particle(x, y, velocity, angle);
                simulatorPanel.addParticle(particle);
            }
        });

        // Event listener for add wall button
        addWallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double x1 = Double.parseDouble(x1Field.getText());
                double y1 = Double.parseDouble(y1Field.getText());
                double x2 = Double.parseDouble(x2Field.getText());
                double y2 = Double.parseDouble(y2Field.getText());
                Wall wall = new Wall(x1, y1, x2, y2);
                simulatorPanel.addWall(wall);
            }
        });

        // Add particle components to the frame
        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel("X:"));
        controlPanel.add(xPosField);
        controlPanel.add(new JLabel("Y:"));
        controlPanel.add(yPosField);
        controlPanel.add(new JLabel("Angle:"));
        controlPanel.add(angleField);
        controlPanel.add(new JLabel("Velocity:"));
        controlPanel.add(velocityField);
        controlPanel.add(addParticleButton);

        // Add wall components to the frame
        controlPanel.add(new JLabel("Wall X1:"));
        controlPanel.add(x1Field);
        controlPanel.add(new JLabel("Y1:"));
        controlPanel.add(y1Field);
        controlPanel.add(new JLabel("X2:"));
        controlPanel.add(x2Field);
        controlPanel.add(new JLabel("Y2:"));
        controlPanel.add(y2Field);
        controlPanel.add(addWallButton);
        
        // Add controlPanel to the frame
        add(controlPanel, BorderLayout.SOUTH); 
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
            addParticle(new Particle(100, 100, 20, 45));
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

            // Draw particles
            for (Particle particle : particles) {
                g.drawOval((int) particle.getX(), (int) particle.getY(), 10, 10);
            }

            // Draw walls
            for (Wall wall : walls) {
                g.drawLine((int) wall.getX1(), (int) wall.getY1(), (int) wall.getX2(), (int) wall.getY2());
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

