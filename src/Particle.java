public class Particle {
    private double x, y, vx, vy;
	//x, y = world coordinates
	//vx, vy = movement vector
    private double velocity; 
    private double angle; 

    public Particle(double x, double y, double velocity, double angle) {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
		this.vx = velocity * Math.cos(Math.toRadians(angle));
		this.vy = velocity * Math.sin(Math.toRadians(angle));
        this.angle = angle;
    }

    public void move(double deltaTime) {
        x += vx * deltaTime; 
        y += vy * deltaTime;
    }

    public double get_next_x(double deltaTime) {
        return x + vx * deltaTime;
    }

    public double get_next_y(double deltaTime) {
        return y + vy * deltaTime;
    }

    public void bounceHorizontal() {
        angle = 180 - angle;
		recalculateV();
    }
    
    public void setAngle(double angle){
        this.angle = angle;
		recalculateV();
    }
    public void bounceVertical() {
        angle = -angle;
		recalculateV();
    }

    public void bounceOffWall(double wallAngle) {
        double wallNormalX = Math.cos(wallAngle + Math.PI / 2); 
        double wallNormalY = Math.sin(wallAngle + Math.PI / 2);
    
        double dotProduct = vx * wallNormalX + vy * wallNormalY;
    
        double reflectX = vx - 2 * dotProduct * wallNormalX;
        double reflectY = vy - 2 * dotProduct * wallNormalY;
    
        angle = Math.toDegrees(Math.atan2(reflectY, reflectX));
		recalculateV();
    }
    
	public void recalculateV() {
		vx = velocity * Math.cos(Math.toRadians(angle));
		vy = velocity * Math.sin(Math.toRadians(angle));
	}

    public double getAngle() {
        return angle;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocity() { return velocity; }
    public double getVelocityX() { return vx; }
    public double getVelocityY() { return vy; }
}
