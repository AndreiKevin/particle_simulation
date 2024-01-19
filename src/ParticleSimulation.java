import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ParticleSimulation {
    JFrame frame;
    CanvasPanel canvas;
    ArrayList<Particle> particles = new ArrayList<>();
    Random random = new Random();
    ScheduledExecutorService executorService;

    public ParticleSimulation() {
        frame = new JFrame("Particle Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        canvas = new CanvasPanel();
        frame.add(canvas, BorderLayout.CENTER);

        JButton addButton = new JButton("Add Particle");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addParticles();
            }
        });
        frame.add(addButton, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Initialize the ScheduledExecutorService
        executorService = Executors.newScheduledThreadPool(4); // 4 threads for load balancing
        executorService.scheduleAtFixedRate(this::updateParticles, 0, 33, TimeUnit.MILLISECONDS); // 30 FPS
    }

    private void addParticle() {
        Particle particle = new Particle(
                random.nextInt(canvas.getWidth()),
                random.nextInt(canvas.getHeight()),
                random.nextDouble() * 10, // random velocity
                random.nextDouble() * 360 // random angle in degrees
        );
        particles.add(particle);
    }

    private void addParticles() {
        for(int i=0; i<5000; i++) {
            Particle particle = new Particle(
                    random.nextInt(canvas.getWidth()),
                    random.nextInt(canvas.getHeight()),
                    random.nextDouble() * 10, // random velocity
                    random.nextDouble() * 360 // random angle in degrees
            );
            particles.add(particle);
        }
    }

    private void updateParticles() {
        for (Particle particle : particles) {
            particle.update(canvas.getWidth(), canvas.getHeight());
        }
        canvas.repaint();
    }

    private class CanvasPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (Particle particle : particles) {
                g.fillOval((int) particle.x, (int) particle.y, 10, 10);
            }
        }
    }

    private class Particle {
        double x, y, velocity, angle;

        public Particle(double x, double y, double velocity, double angle) {
            this.x = x;
            this.y = y;
            this.velocity = velocity;
            this.angle = angle;
        }

        public void update(int width, int height) {
            // Calculate new position
            x += velocity * Math.cos(Math.toRadians(angle));
            y += velocity * Math.sin(Math.toRadians(angle));

            // Check for collision with walls and reflect the angle
            if (x <= 0 || x >= width) {
                angle = 180 - angle;
            }
            if (y <= 0 || y >= height) {
                angle = 360 - angle;
            }

            // Normalize the angle
            angle = (angle + 360) % 360;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ParticleSimulation();
            }
        });
    }
}

