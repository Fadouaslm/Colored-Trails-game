import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameGUI extends JFrame {
    private JTextArea textArea;
    private JButton[] buttons;
    private String[] colors = {"red", "blue", "green", "yellow"};
    private Random random = new Random();
    private Map<String, Color> colorMap;

    public GameGUI(String title) {
        super(title);

        // Initialize the color map
        colorMap = new HashMap<>();
        colorMap.put("red", Color.RED);
        colorMap.put("blue", Color.BLUE);
        colorMap.put("green", Color.GREEN);
        colorMap.put("yellow", Color.YELLOW);

        // Initialize the window
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        // Text area for logs
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(200, 0));
        add(scrollPane, BorderLayout.EAST);

        // Panel for buttons
        // Panel for buttons
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 7)); // Adjust the layout to match grid dimensions
        buttons = new JButton[35];
        for (int i = 0; i < 35; i++) {
            buttons[i] = new JButton();
            String colorName = colors[random.nextInt(colors.length)];
            buttons[i].setBackground(colorMap.get(colorName));
            buttons[i].setOpaque(true);
            buttons[i].setBorderPainted(false);
            buttons[i].setLayout(new GridBagLayout()); // Set layout to GridBagLayout for centering
            int index = i % 7; // Calculate the x coordinate based on the column
            int y = i / 7; // Calculate the y coordinate based on the row
            int reverseY = 4 - y; // Reverse the y coordinate to match the game logic
            int adjustedIndex = index * 5 + reverseY; // Calculate the adjusted index
            buttons[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    log("Button " + adjustedIndex + " clicked. Color: " + colorName);
                }
            });
            panel.add(buttons[i]);
        }
        add(panel, BorderLayout.CENTER);


        setVisible(true);
    }

    public void log(String message) {
        textArea.append(message + "\n");
    }

    public void updateButton(int index, String colorName) {
        buttons[index].setBackground(colorMap.get(colorName));
    }

    // Method to set icon for button at a specified index
    public void setButtonIcon(int index, Icon icon) {
        if (icon == null) {
            buttons[index].setIcon(null);
            return;
        }

        // Resize the icon
        if (icon instanceof ImageIcon) {
            ImageIcon imageIcon = (ImageIcon) icon;
            Image image = imageIcon.getImage();
            Image scaledImage = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            buttons[index].setIcon(scaledIcon);
        } else {
            buttons[index].setIcon(icon);
        }
    }

    public String getColorNameAtIndex(int index) {
        Color color = buttons[index].getBackground();
        for (Map.Entry<String, Color> entry : colorMap.entrySet()) {
            if (entry.getValue().equals(color)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void setIconAtPosition(int x, int y, Icon icon, int width) {
        int index = y * width + x;
        int buttonWidth = 30;
        int buttonHeight = 30;
        buttons[index].setIcon(resizeIcon(icon, buttonWidth, buttonHeight));

        // Center the icon in the button
        buttons[index].setHorizontalTextPosition(SwingConstants.CENTER);
        buttons[index].setVerticalTextPosition(SwingConstants.CENTER);
    }

    private ImageIcon resizeIcon(Icon icon, int width, int height) {
        if (icon instanceof ImageIcon) {
            Image image = ((ImageIcon) icon).getImage();
            Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(resizedImage);
        } else {
            // If the icon is not an instance of ImageIcon, return it as is
            return (ImageIcon) icon;
        }
    }

    public static void main(String[] args) {
        new GameGUI("Colored Trails Game");
    }
}
