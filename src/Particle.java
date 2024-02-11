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

    public double get_next_x(double deltaTime) {
        return x + velocity * Math.cos(Math.toRadians(angle)) * deltaTime;
    }

    public double get_next_y(double deltaTime) {
        return y + velocity * Math.sin(Math.toRadians(angle)) * deltaTime;
    }

    public void bounceHorizontal() {
        angle = 180 - angle;
    }
    
    public void setAngle(double angle){
        this.angle = angle;
    }
    public void bounceVertical() {
        angle = -angle;
    }

    public void bounceOffWall(double wallAngle) {
        double wallNormalX = Math.cos(wallAngle + Math.PI / 2); 
        double wallNormalY = Math.sin(wallAngle + Math.PI / 2);
    
        double dotProduct = getVelocityX() * wallNormalX + getVelocityY() * wallNormalY;
    
        double reflectX = getVelocityX() - 2 * dotProduct * wallNormalX;
        double reflectY = getVelocityY() - 2 * dotProduct * wallNormalY;
    
        angle = Math.toDegrees(Math.atan2(reflectY, reflectX));
    }
    

    public double getAngle() {
        return angle;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocity() { return velocity; }
    public double getVelocityX() { return velocity * Math.cos(Math.toRadians(angle)); }
    public double getVelocityY() { return velocity * Math.sin(Math.toRadians(angle)); }
}