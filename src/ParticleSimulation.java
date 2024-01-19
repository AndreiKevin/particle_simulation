import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSimulation {
    JFrame frame;
    CanvasPanel canvas;
    List<Particle> particles = new ArrayList<>();
    Random random = new Random();
    List<WorkerThread> workerThreads;
    final int THREAD_COUNT = 4; // Number of threads in the custom thread pool

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
                addParticle();
            }
        });
        frame.add(addButton, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Initialize worker threads
        workerThreads = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            WorkerThread worker = new WorkerThread();
            workerThreads.add(worker);
            worker.start();
        }

        // Timer for the update loop
        new Timer(33, e -> updateParticles()).start(); // 30 FPS
    }

    private void addParticle() {
        synchronized (particles) {
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
        synchronized (particles) {
            // Divide the task of updating particles among threads
            int partitionSize = particles.size() / THREAD_COUNT;
            for (int i = 0; i < THREAD_COUNT; i++) {
                int start = i * partitionSize;
                int end = (i == THREAD_COUNT - 1) ? particles.size() : (start + partitionSize);
                workerThreads.get(i).enqueueTask(() -> {
                    for (int j = start; j < end; j++) {
                        particles.get(j).update(canvas.getWidth(), canvas.getHeight());
                    }
                });
            }
        }
        canvas.repaint();
    }

    private class CanvasPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            synchronized (particles) {
                for (Particle particle : particles) {
                    g.fillOval((int) particle.x, (int) particle.y, 10, 10);
                }
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
            // Movement and collision logic 
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

    private class WorkerThread extends Thread {
        private final List<Runnable> tasks = new ArrayList<>();

        public void enqueueTask(Runnable task) {
            synchronized (tasks) {
                tasks.add(task);
                tasks.notify(); // Notify the thread that a new task is available
            }
        }

        @Override
        public void run() {
            while (true) {
                Runnable task;
                synchronized (tasks) {
                    while (tasks.isEmpty()) {
                        try {
                            tasks.wait(); // Wait until a task is available
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    task = tasks.remove(0); // Get the first task from the queue
                }
                task.run();
            }
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
