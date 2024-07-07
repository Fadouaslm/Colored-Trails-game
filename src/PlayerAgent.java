import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.util.*;

public class PlayerAgent extends Agent {

    private Map<String, Integer> tokens;
    private final Random random = new Random();
    private GameGUI gui;
    private Case startingPosition;
    private Icon icon;
    private Case current;
    private Case target;
    private static final int GRID_WIDTH = 7;
    private static final int GRID_HEIGHT = 5;
    private boolean isBlocked = false;
    private PlayBehaviour playBehaviour;

    // Inner class to represent color and index
    private static class ColorIndex {
        String color;
        int index;

        ColorIndex(String color, int index) {
            this.color = color;
            this.index = index;
        }
    }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 4) {
            gui = (GameGUI) args[0];
            startingPosition = (Case) args[1];
            current = startingPosition;
            icon = (Icon) args[2];
            target = (Case) args[3];
            tokens = (Map<String, Integer>) args[4];
            System.out.println("Icon: " + icon);
        }

        System.out.println("Player agent " + getLocalName() + " started at position: " + startingPosition);

        if (gui != null) {
            SwingUtilities.invokeLater(() -> {
                gui.log("Agent " + getLocalName() + " started with tokens: " + tokens + " at position: " + startingPosition);
                int index = startingPosition.y * GRID_WIDTH + startingPosition.x; // Calculate index based on position
                gui.setButtonIcon(index, icon);
            });
        }

        playBehaviour = new PlayBehaviour();
        addBehaviour(playBehaviour);
        addBehaviour(new TokenRequestBehaviour()); // Add new behavior for token requests
    }

    private class PlayBehaviour extends CyclicBehaviour {

        private boolean gameStopped = false;

        @Override
        public void action() {
            while (!gameStopped) { // Continue the game until it's stopped
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println(getLocalName() + " received message: " + msg.getContent());
                    if (gui != null) {
                        SwingUtilities.invokeLater(() -> gui.log("Received message: " + msg.getContent()));
                    }
                    // Process the message and game logic
                    if (msg.getContent().equals("Your turn")) {
                        move();
                    } else if (msg.getContent().equals("reached target")) {
                        // If a player reached the target, stop the game
                        gameStopped = true;
                        System.out.println(getLocalName() + " reached the target! Game stopped.");
                        if (gui != null) {
                            SwingUtilities.invokeLater(() -> gui.log("Agent " + getLocalName() + " reached the target! Game stopped."));
                        }
                    } else if (msg.getContent().startsWith("RequestToken")) {
                        String[] parts = msg.getContent().split(":");
                        String requestedColor = parts[0];
                        handleTokenRequest(msg.getSender().getLocalName(), requestedColor);
                    } else if (msg.getContent().startsWith("SendToken")) {
                        String[] parts = msg.getContent().split(":");
                        String tokenColor = parts[1];
                        handleTokenReception(tokenColor);
                    }
                } else {
                    block();
                }
            }
        }

        private void move() {
            if (!isBlocked && !hasReachedTarget()) {
                ColorIndex[] neighbors = getNeighborColors();
                Arrays.sort(neighbors, new Comparator<ColorIndex>() {
                    @Override
                    public int compare(ColorIndex c1, ColorIndex c2) {
                        if (c1 == null && c2 == null) {
                            return 0;
                        }
                        if (c1 == null) {
                            return 1; // c1 is considered greater than c2
                        }
                        if (c2 == null) {
                            return -1; // c1 is considered smaller than c2
                        }
                        int dist1 = Math.abs(c1.index % GRID_WIDTH - target.x) + Math.abs(c1.index / GRID_WIDTH - target.y);
                        int dist2 = Math.abs(c2.index % GRID_WIDTH - target.x) + Math.abs(c2.index / GRID_HEIGHT - target.y);
                        return Integer.compare(dist1, dist2);
                    }
                });

                boolean moved = false;
                for (ColorIndex neighbor : neighbors) {
                    if (neighbor != null && tokens.containsKey(neighbor.color) && tokens.get(neighbor.color) > 0) {
                        // Remove icon from current position
                        int currentIndex = current.y * GRID_WIDTH + current.x;
                        int neighborIndex = neighbor.index;

                        SwingUtilities.invokeLater(() -> {
                            gui.setButtonIcon(currentIndex, null); // Remove icon from current position
                            gui.setButtonIcon(neighborIndex, icon); // Set icon to the new position
                        });

                        // Update current position
                        current = new Case(neighbor.index % GRID_WIDTH, neighbor.index / GRID_WIDTH);

                        // Remove one token
                        tokens.put(neighbor.color, tokens.get(neighbor.color) - 1);

                        gui.log("Agent " + getLocalName() + " moved to position: " + current + " with color: " + neighbor.color);

                        // Check if the player has reached the target
                        if (hasReachedTarget()) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.setContent("reached target");
                            msg.addReceiver(new jade.core.AID("MainAgent", jade.core.AID.ISLOCALNAME));
                            send(msg);
                            return;
                        }

                        moved = true;
                        break; // Exit the loop after making one move
                    }
                }

                if (!moved) {
                    // If no valid move, send a message to the other player requesting a token
                    isBlocked = true;
                    StringBuilder neighborsInfo = new StringBuilder();
                    for (ColorIndex neighbor : neighbors) {
                        if (neighbor != null) {
                            neighborsInfo.append(neighbor.color).append(",");
                        }
                    }
                    if (neighborsInfo.length() > 0) {
                        neighborsInfo.setLength(neighborsInfo.length() - 1); // Remove trailing comma
                    }
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent("RequestToken:" + neighborsInfo.toString());
                    msg.addReceiver(new jade.core.AID(getOtherPlayerName(), jade.core.AID.ISLOCALNAME));
                    send(msg);
                    gui.log("Agent " + getLocalName() + " is blocked and requests tokens from " + getOtherPlayerName());
                }
            }
        }

        private String getOtherPlayerName() {
            return getLocalName().equals("PlayerAgent1") ? "PlayerAgent2" : "PlayerAgent1";
        }

        private boolean hasReachedTarget() {
            return current.x == target.x && current.y == target.y;
        }

        private ColorIndex[] getNeighborColors() {
            List<ColorIndex> neighborsList = new ArrayList<>();

            // Check the top neighbor
            if (current.y > 0) {
                ColorIndex topNeighbor = getColorAtPosition(current.x, current.y - 1);
                if (topNeighbor != null && tokens.containsKey(topNeighbor.color) && tokens.get(topNeighbor.color) > 0) {
                    neighborsList.add(topNeighbor);
                }
            }

            // Check the bottom neighbor
            if (current.y < GRID_HEIGHT - 1) {
                ColorIndex bottomNeighbor = getColorAtPosition(current.x, current.y + 1);
                if (bottomNeighbor != null && tokens.containsKey(bottomNeighbor.color) && tokens.get(bottomNeighbor.color) > 0) {
                    neighborsList.add(bottomNeighbor);
                }
            }

            // Check the left neighbor
            if (current.x > 0) {
                ColorIndex leftNeighbor = getColorAtPosition(current.x - 1, current.y);
                if (leftNeighbor != null && tokens.containsKey(leftNeighbor.color) && tokens.get(leftNeighbor.color) > 0) {
                    neighborsList.add(leftNeighbor);
                }
            }

            // Check the right neighbor
            if (current.x < GRID_WIDTH - 1) {
                ColorIndex rightNeighbor = getColorAtPosition(current.x + 1, current.y);
                if (rightNeighbor != null && tokens.containsKey(rightNeighbor.color) && tokens.get(rightNeighbor.color) > 0) {
                    neighborsList.add(rightNeighbor);
                }
            }

            // Convert the list to an array
            return neighborsList.toArray(new ColorIndex[0]);
        }

        // Method to get the color of a cell at a specified position
        private ColorIndex getColorAtPosition(int x, int y) {
            if (x < 0 || y < 0 || x >= GRID_WIDTH || y >= GRID_HEIGHT) {
                return null; // Out of bounds
            }
            int index = y * GRID_WIDTH + x;
            String color = gui.getColorNameAtIndex(index);
            return new ColorIndex(color, index);
        }
    }

    // Methods moved to the PlayerAgent class
    private void handleTokenRequest(String requesterName, String requestedColor) {
        if (tokens.containsKey(requestedColor) && tokens.get(requestedColor) > 0) {
            tokens.put(requestedColor, tokens.get(requestedColor) - 1);
            ACLMessage response = new ACLMessage(ACLMessage.INFORM);
            response.setContent("SendToken:" + requestedColor);
            response.addReceiver(new AID(requesterName, AID.ISLOCALNAME));
            send(response);
            gui.log("Agent " + getLocalName() + " sent a " + requestedColor + " token to " + requesterName);
        } else {
            gui.log("Agent " + getLocalName() + " does not have a " + requestedColor + " token for " + requesterName);
        }
    }
    private void handleTokenReception(String tokenColor) {
        tokens.put(tokenColor, tokens.getOrDefault(tokenColor, 0) + 1);
        gui.log("Agent " + getLocalName() + " received a " + tokenColor + " token");
        isBlocked = false; // Unblock the agent
        playBehaviour.move(); // Attempt to move again
    }

    private class TokenRequestBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getContent().startsWith("RequestToken")) {
                    String[] parts = msg.getContent().split(":");
                    String[] requestedColors = parts[1].split(",");
                    for (String requestedColor : requestedColors) {
                        handleTokenRequest(msg.getSender().getLocalName(), requestedColor);
                    }
                } else if (msg.getContent().startsWith("SendToken")) {
                    String[] parts = msg.getContent().split(":");
                    String tokenColor = parts[1];
                    handleTokenReception(tokenColor);
                }
            } else {
                block();
            }
        }
    }

}