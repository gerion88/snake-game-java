import javax.swing.JFrame;

public class SnakeGame {
    public static void main(String[] args) {
        JFrame window = new JFrame("Snake Game");

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);

        window.pack();
        window.setResizable(false);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
