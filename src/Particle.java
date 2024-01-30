public class Particle {
    private double x, y; // Position of the particle
    private double velocity; // Speed of the particle
    private double angle; // Direction of the particle in degrees

    public Particle(double x, double y, double velocity, double angle) {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
        this.angle = angle;
    }

    // Method to update the particle's position
    public void move(double deltaTime) {
        x += velocity * Math.cos(Math.toRadians(angle)) * deltaTime;
        y += velocity * Math.sin(Math.toRadians(angle)) * deltaTime;
    }

    public void bounceHorizontal() {
        angle = 180 - angle;
    }
    
    public void bounceVertical() {
        angle = -angle;
    }

    public void bounceOffWall(double wallAngle) {
        double angleDiff = wallAngle - angle;
        angle = wallAngle + angleDiff;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocity() { return velocity; }
    public double getVelocityX() { return velocity * Math.cos(Math.toRadians(angle)); }
    public double getVelocityY() { return velocity * Math.sin(Math.toRadians(angle)); }
}
