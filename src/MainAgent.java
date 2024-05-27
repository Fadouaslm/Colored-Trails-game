import jade.core.behaviours.CyclicBehaviour;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import jade.core.Agent;

public class MainAgent extends Agent {

    private static final int GRID_WIDTH = 7;
    private static final int GRID_HEIGHT = 5;
    private int currentPlayerIndex = 0;
    private Timer turnTimer;
    private boolean gameEnded = false;

    protected void setup() {
        System.out.println("Main agent " + getLocalName() + " started.");

        // Create and display the GUI
        GameGUI gui = new GameGUI("Game GUI");

        // Define the target position
        int targetX = GRID_WIDTH / 2;
        int targetY = GRID_HEIGHT / 2;
        Icon targetIcon = new ImageIcon("icons/red-flag.png");
        gui.setIconAtPosition(targetX, targetY, targetIcon, GRID_WIDTH);

        Case targetCase = new Case(targetX, targetY);

        // Calculate equidistant starting positions for each player
        Case[] startingPositions = calculateEquidistantPositions(targetCase, 2);

        // Initialize tokens for each player
        Map<String, Integer>[] playerTokens = new HashMap[2];
        initializeTokens(playerTokens);

        // Launch player agents
        try {
            ContainerController container = getContainerController();
            for (int i = 0; i < 2; i++) {
                Icon icon = new ImageIcon("icons/player" + (i + 1) + "Icon.png"); // Update the path to the icon
                Object[] args = new Object[] { gui, startingPositions[i], icon, targetCase, playerTokens[i] };
                AgentController playerAgent = container.createNewAgent("PlayerAgent" + (i + 1), PlayerAgent.class.getName(), args);
                playerAgent.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Schedule the first player's turn
        scheduleNextPlayerTurn();

        // Start the timer to schedule subsequent turns
        turnTimer = new Timer(3000, e -> scheduleNextPlayerTurn());
        turnTimer.setRepeats(true);
        turnTimer.start();

        // Add behaviour to listen for move results from PlayerAgents
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    String senderName = msg.getSender().getLocalName();
                    handleMoveResult(senderName, content);
                } else {
                    block();
                }
            }
        });
    }

    private void scheduleNextPlayerTurn() {
        if (gameEnded) {
            return; // Do not schedule next turn if the game has ended
        }

        // Send message to the current player indicating their turn
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("Your turn");
        msg.addReceiver(new AID("PlayerAgent" + (currentPlayerIndex + 1), AID.ISLOCALNAME));
        send(msg);

        // Move to the next player
        currentPlayerIndex = (currentPlayerIndex + 1) % 2;
    }

    public void handleMoveResult(String playerName, String moveResult) {
        if (moveResult.equals("blocked")) {
            System.out.println("Player " + playerName + " is blocked.");
            // Handle player being blocked (e.g., end the game)
        } else if (moveResult.equals("reached target")) {
            System.out.println("Player " + playerName + " reached the target!");
            gameEnded = true; // Set gameEnded to true
            if (turnTimer != null) {
                turnTimer.stop(); // Stop the timer when the game ends
            }
            System.out.println("Game stopped.");
        }
    }

    private Case[] calculateEquidistantPositions(Case targetCase, int numPlayers) {
        Case[] positions = new Case[numPlayers];
        positions[0] = new Case(targetCase.x - 2, targetCase.y - 2); // Adjust the position as needed
        positions[1] = new Case(targetCase.x + 2, targetCase.y - 2); // Adjust the position as needed
        // Add more logic here if you have more than 2 players or different equidistant logic
        return positions;
    }

    private void initializeTokens(Map<String, Integer>[] playerTokens) {
        Random random = new Random();
        String[] colors = {"red", "blue", "green", "yellow"};
        for (int i = 0; i < playerTokens.length; i++) {
            playerTokens[i] = new HashMap<>();
            for (String color : colors) {
                playerTokens[i].put(color, random.nextInt(5) + 1); // 1 to 5 tokens of each color
            }
        }
    }
}
