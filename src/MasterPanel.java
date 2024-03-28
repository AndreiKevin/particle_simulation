import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MasterPanel extends JPanel {
    private Map<Integer, Point> redPixelPositions;

    public MasterPanel() {
        redPixelPositions = new HashMap<>(); // Initialize the redPixelPositions map
    }

    public void updateRedPixelPosition(int clientId, int newX, int newY) {
        redPixelPositions.put(clientId, new Point(newX, newY));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw white pixel
        g.setColor(Color.WHITE);
        g.fillRect(150, MasterServer.whitePixelY, MasterServer.WHITE_PIXEL_SIZE, MasterServer.WHITE_PIXEL_SIZE);

        // Draw red pixels for each client
        g.setColor(Color.RED);
        for (Integer clientId : redPixelPositions.keySet()) {
            Point position = redPixelPositions.get(clientId);
            g.fillRect(position.x, position.y, MasterServer.WHITE_PIXEL_SIZE, MasterServer.WHITE_PIXEL_SIZE);
        }
    }
}
