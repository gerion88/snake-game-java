import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.Timer;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.prefs.Preferences;

public class GamePanel extends JPanel {
    private static final int CELL_SIZE = 25;
    private final Image background =
            new ImageIcon(getClass().getResource("/background.jpg")).getImage();
    private static final int START_DELAY = 200;
    private static final int MIN_DELAY = 70;
    private static final int SPEED_STEP = 20;
    private final List<Point> snake = new ArrayList<>();
    private final Image snakeHead =
            new ImageIcon(getClass().getResource("/skins/cartoon/head.png")).getImage();
    private final Image snakeBody =
            new ImageIcon(getClass().getResource("/skins/cartoon/body.png")).getImage();
    private final Image snakeTail =
            new ImageIcon(getClass().getResource("/skins/cartoon/tail.png")).getImage();
    private final Image cornerLeftDown =
            new ImageIcon(getClass().getResource("/skins/cartoon/corner-left-down.png"
            )).getImage();
    private final Image cornerLeftUp =
            new ImageIcon(getClass().getResource("/skins/cartoon/corner-left-up.png"
            )).getImage();
    private final Image cornerRightUp =
            new ImageIcon(getClass().getResource("/skins/cartoon/corner-right-up.png"
            )).getImage();
    private final Image cornerRightDown =
            new ImageIcon(getClass().getResource("/skins/cartoon/corner-right-down.png"
            )).getImage();
    private final Image[] foodImages = {
            new ImageIcon(getClass().getResource("/food/apple.png")).getImage(),
            new ImageIcon(getClass().getResource("/food/strawberry.png")).getImage(),
            new ImageIcon(getClass().getResource("/food/grapes.png")).getImage(),
            new ImageIcon(getClass().getResource("/food/pear.png")).getImage(),
            new ImageIcon(getClass().getResource("/food/orange.png")).getImage(),
            new ImageIcon(getClass().getResource("/food/cherries.png")).getImage()
    };
    private static final int SNAKE_DRAW_SIZE =CELL_SIZE + 6;
    private static final int SNAKE_DRAW_OFFSET = 3;
    private static final int FOOD_DRAW_SIZE = 34;
    private static final int FOOD_DRAW_OFFSET = (FOOD_DRAW_SIZE - CELL_SIZE) / 2;
    private final Preferences preferences =
            Preferences.userNodeForPackage(GamePanel.class);
    private int bestScore = preferences.getInt("bestScore", 0);
    private int currentFoodIndex = 0;
    private int foodX = 300;
    private int foodY = 300;
    private boolean gameOver = false;
    private boolean paused = false;
    private Timer timer;
    private int directionX = CELL_SIZE;
    private int directionY = 0;
    private int nextDirectionX = CELL_SIZE;
    private int nextDirectionY = 0;
    private int score = 0;
    private boolean gameStarted = false;
    private boolean recordBeaten = false;
    private boolean soundEnabled = true;

    public GamePanel() {
        snake.add(new Point(100,100));
        snake.add(new Point(75,100));
        snake.add(new Point(50,100));
        setFocusable(true);
        setPreferredSize(new Dimension(750, 750));

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                int key = event.getKeyCode();

                if (key == KeyEvent.VK_M) {
                    soundEnabled = !soundEnabled;
                    repaint();
                    return;
                }

                if (key == KeyEvent.VK_ENTER && !gameStarted) {
                    gameStarted = true;
                    placeFood();
                    timer.start();
                    repaint();
                    recordBeaten = false;
                    return;
                }
                if (!gameStarted) {
                    return;
                }

                if (key == KeyEvent.VK_SPACE && !gameOver) {
                    paused = !paused;

                    if (paused) {
                        timer.stop();
                        playSound("/sounds/pause.wav");
                    } else {
                        timer.start();
                        playSound("/sounds/pause.wav");
                    }
                    repaint();
                    return;
                }

                if (key == KeyEvent.VK_R && (gameOver || paused)) {
                    restartGame();
                    recordBeaten = false;
                    return;
                }

                if ((key == KeyEvent.VK_UP || key == KeyEvent.VK_W)
                && directionY == 0) {
                    nextDirectionX = 0;
                    nextDirectionY = -CELL_SIZE;
                }
                if ((key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S)
                        && directionY == 0) {
                    nextDirectionX = 0;
                    nextDirectionY = CELL_SIZE;
                }
                if ((key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A)
                && directionX == 0) {
                    nextDirectionX = -CELL_SIZE;
                    nextDirectionY = 0;
                }
                if ((key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D)
                        && directionX == 0){
                    nextDirectionX = CELL_SIZE;
                    nextDirectionY = 0;
                }

            }
        });

        timer = new Timer(START_DELAY, event -> {
            directionX = nextDirectionX;
            directionY = nextDirectionY;

            Point oldTail = new Point(snake.get(snake.size() - 1));

            for (int i = snake.size() - 1; i > 0; i--) {
                snake.get(i).setLocation(snake.get(i - 1));
            }
            Point head = snake.get(0);
            head.translate(directionX, directionY);

            if (head.x == foodX && head.y == foodY) {
                snake.add(oldTail);
                score++;

                if (score > bestScore) {
                    bestScore = score;
                    preferences.putInt("bestScore", bestScore);

                    if (!recordBeaten) {
                        recordBeaten = true;
                        playSound("/sounds/new-record.wav");
                    } else {
                        playSound("/sounds/eat.wav");
                    }
                } else {
                    playSound("/sounds/eat.wav");
                }


                if (score % 5 == 0) {
                    int newDelay = Math.max(MIN_DELAY, timer.getDelay() - SPEED_STEP);
                    timer.setDelay(newDelay);
                }
                placeFood();
            }

            if (head.x < 0 ||
                    head.y < 0 ||
                    head.x + CELL_SIZE > getWidth() ||
                    head.y + CELL_SIZE > getHeight()) {
                endGame();
            }

            for (int i = 1; i < snake.size(); i++) {
                if (head.equals(snake.get(i))) {
                    endGame();
                    break;
                }
            }
            repaint();
        });

        // Timer will start after "Enter" pressed.
    }



    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D graphics2D = (Graphics2D) graphics;

        graphics2D.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics2D.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        graphics.drawImage(background,0, 0, getWidth(), getHeight(), this);

        for (int i = 0; i < snake.size(); i++) {
            Point part = snake.get(i);

            if (i == 0) {
                double angle = getRotation(directionX,directionY);
                drawRotatedImage(graphics, snakeHead, part.x, part.y, angle);
            } else if (i == snake.size() - 1) {
                Point previousPart = snake.get(i - 1);
                int tailDirectionX = part.x - previousPart.x;
                int tailDirectionY = part.y - previousPart.y;
                double angle = getRotation(tailDirectionX,tailDirectionY);
                drawRotatedImage(graphics, snakeTail,part.x, part.y, angle);
            } else {
                Point previousPart = snake.get(i - 1);
                Point nextPart = snake.get(i + 1);
                boolean horizontal =
                        previousPart.y == part.y &&
                                nextPart.y == part.y;
                boolean vertical =
                        previousPart.x == part.x &&
                                nextPart.x == part.x;
                if (horizontal) {
                    drawRotatedImage(
                            graphics,
                            snakeBody,
                            part.x,
                            part.y,
                            Math.PI / 2
                    );
                } else if (vertical) {
                    drawRotatedImage(
                            graphics,
                            snakeBody,
                            part.x,
                            part.y,
                            0
                    );
                } else {
                    boolean connectsUp =
                            previousPart.y < part.y || nextPart.y < part.y;
                    boolean connectsDown =
                            previousPart.y > part.y || nextPart.y > part.y;
                    boolean connectsLeft =
                            previousPart.x < part.x || nextPart.x < part.x;
                    boolean connectsRight =
                            previousPart.x > part.x || nextPart.x > part.x;
                    Image cornerTexture;
                    if (connectsLeft && connectsDown) {
                        cornerTexture = cornerLeftDown;
                    }else if (connectsLeft && connectsUp) {
                        cornerTexture = cornerLeftUp;
                    } else if (connectsRight && connectsUp) {
                        cornerTexture = cornerRightUp;
                    } else {
                        cornerTexture = cornerRightDown;
                    }
                    graphics.drawImage(
                            cornerTexture,
                            part.x - SNAKE_DRAW_OFFSET,
                            part.y - SNAKE_DRAW_OFFSET,
                            SNAKE_DRAW_SIZE,
                            SNAKE_DRAW_SIZE,
                            this
                    );
                }
            }
        }

        graphics.drawImage(
                foodImages[currentFoodIndex],
                foodX - FOOD_DRAW_OFFSET,
                foodY - FOOD_DRAW_OFFSET,
                FOOD_DRAW_SIZE,
                FOOD_DRAW_SIZE,
                this
        );

        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial", Font.BOLD, 20));
        graphics.drawString("Score: " + score, 10, 25);
        graphics.drawString("Best: " + bestScore, 15, 58);

        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        String soundText;
        if (soundEnabled) {
            soundText = "Sound: ON (M)";
        }else {
            soundText = "Sound: OFF (M)";
        }
        FontMetrics soundMetrics = graphics.getFontMetrics();
        int soundX = getWidth() - soundMetrics.stringWidth(soundText) - 15;
        graphics.drawString(soundText, soundX, 30);

        if (!gameStarted) {
            int boxWidth = 560;
            int boxHeight = 250;
            int boxX = (getWidth() - boxWidth) / 2;
            int boxY = (getHeight() - boxHeight) / 2;

            graphics.setColor(new Color(0, 0, 0, 180));
            graphics.fillRoundRect(boxX,
                    boxY,
                    boxWidth,
                    boxHeight,
                    30,
                    30);

            drawCenteredText(
                    graphics,
                    "SNAKE GAME",
                    new Font("Arial", Font.BOLD, 48),
                    Color.WHITE,
                    boxY + 75
            );
            drawCenteredText(
                    graphics,
                    "Press ENTER to start",
                    new Font("Arial", Font.BOLD, 26),
                    Color.WHITE,
                    boxY + 135
            );
            drawCenteredText(
                    graphics,
                    "Use WASD or Arrow Keys to move",
                    new Font("Arial", Font.PLAIN, 18),
                    Color.WHITE,
                    boxY + 185
            );

            drawCenteredText(
                    graphics,
                    "SPACE - pause | M - sound",
                    new Font("Arial", Font.BOLD, 16),
                    Color.WHITE,
                    boxY + 215
            );
        }

        if (gameOver) {
            int centerY = getHeight() / 2;
            drawCenteredText(graphics,
                    "GAME OVER",
                    new Font("Monospaced", Font.BOLD, 50),
                    Color.BLACK,
                    centerY
            );
        drawCenteredText(
                graphics,
                "Press R to restart the game",
                new Font("Arial", Font.BOLD,22),
                Color.BLACK,
                centerY + 45
        );
        drawCenteredText(
                graphics,
                "Score: " + score + " Best score: " + bestScore,
                new Font("Arial", Font.BOLD,20),
                Color.BLACK,
                centerY + 80
        );
        }

        if (paused) {
            drawCenteredText(
                    graphics,
                    "PAUSE",
                    new Font("Arial", Font.BOLD, 48),
                    Color.BLACK,
                    getHeight() / 2
            );
        }
    }

    private void placeFood() {
        boolean foodOnSnake;
        do {
            foodX = (int) (Math.random() * (getWidth() / CELL_SIZE)) * CELL_SIZE;
            foodY = (int) (Math.random() * (getHeight() / CELL_SIZE)) * CELL_SIZE;
            foodOnSnake = false;

            for (Point part : snake) {
                if (part.x == foodX && part.y == foodY) {
                    foodOnSnake = true;
                    break;
                }
            }

        } while (foodOnSnake);
        currentFoodIndex = (int) (Math.random() * foodImages.length);
    }

    private void drawCenteredText(
            Graphics graphics,
            String text,
            Font font,
            Color color,
            int y
    ) {
        graphics.setFont(font);
        graphics.setColor(color);

        FontMetrics metrics = graphics.getFontMetrics(font);
        int x = (getWidth() - metrics.stringWidth(text)) / 2;

        graphics.drawString(text, x, y);
    }

    private void restartGame() {
        paused = false;
        snake.clear();
        recordBeaten = false;

        timer.setDelay(START_DELAY);

        snake.add(new Point(100, 100));
        snake.add(new Point(75, 100));
        snake.add(new Point(50, 100));

        directionX = CELL_SIZE;
        directionY = 0;
        nextDirectionX = CELL_SIZE;
        nextDirectionY = 0;
        placeFood();
        score = 0;
        gameOver = false;

        timer.start();
        repaint();
    }

    private void drawRotatedImage (Graphics graphics,
                                   Image image,
                                   int x,
                                   int y,
                                   double angle) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();

        graphics2D.rotate(
                angle,
                x + CELL_SIZE / 2.0,
                y +CELL_SIZE / 2.0
        );
        graphics2D.drawImage(
                image,
                x - SNAKE_DRAW_OFFSET,
                y - SNAKE_DRAW_OFFSET,
                SNAKE_DRAW_SIZE,
                SNAKE_DRAW_SIZE,
                this
        );
        graphics2D.dispose();
    }

    private double getRotation(int directionX, int directionY) {
        if (directionY > 0) {
            return 0;
        }
        if (directionX > 0) {
            return -Math.PI / 2;
        }
        if (directionY < 0) {
            return Math.PI;
        }
        return Math.PI / 2;
    }

    private void playSound(String soundPath) {
        if (!soundEnabled) {
            return;
        }
        try {
            AudioInputStream audioStream =
                    AudioSystem.getAudioInputStream(
                            getClass().getResource(soundPath)
                    );
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception exception) {
            System.out.println("Exception in play sound: " + soundPath);
        }
    }
    private void endGame() {
        if (!gameOver) {
            gameOver = true;
            timer.stop();
            playSound("/sounds/game-over.wav");
        }
    }
}

























