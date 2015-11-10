package networkBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.List;

/**
 * Node
 */
public abstract class NodeAbstract implements Serializable {

    private static final long serialVersionUID = -4367897668221156083L;

    public static Color colorWFDConnection = Color.RED;
    public static Color colorWFConnection = Color.BLUE;


    transient NetworkBuilder networkBuilder;

    // node name
    String name;

    // x, y at the center of the node
    int xPos;
    int yPos;

    // central radius for simple nodes; coverage radius of APs and GOs
    int radius;

    // color
    Color color;

    private boolean isSelected;

    Timer timer = null;

    // GO/AP connected by available interfaces
    NodeGO connectedByWFD;
    NodeAbstractAP connectedByWF;


    public NodeAbstract(NetworkBuilder networkBuilder,
                        String name, int x, int y, int radius, Color color) {
        this.networkBuilder = networkBuilder;
        this.name = name;
        this.radius = radius;
        xPos = x;
        yPos = y;
        this.color = color;

        timer = new Timer(1000, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doTimerActions();
            }
        }
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setX(int xPos) {
        this.xPos = xPos;
    }

    public int getX() {
        return xPos;
    }

    public void setY(int yPos) {
        this.yPos = yPos;
    }

    public int getY() {
        return yPos;
    }

    public int getRadius() {
        return radius;
    }

    public Color getColor() {
        return color;
    }

    public NodeGO getConnectedByWFD() {
        return connectedByWFD;
    }

    public void setConnectedByWFD(NodeGO connectedByWFD) {
        this.connectedByWFD = connectedByWFD;
    }

    public NodeAbstractAP getConnectedByWF() {
        return connectedByWF;
    }

    public void setConnectedByWF(NodeAbstractAP connectedByWF) {
        this.connectedByWF = connectedByWF;
    }

    public void setNetworkBuilder(NetworkBuilder networkBuilder) {
        this.networkBuilder = networkBuilder;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (obj instanceof NodeAbstract) && getName().equalsIgnoreCase(
                ((NodeAbstract) obj).getName());
    }

    public String toString() {
        return name;
    }

    public void paintComponent(Graphics g) {
        drawCircle(g, 2);
    }

    public void startTimer(int delay) {
        timer.setDelay(delay);
        timer.start();
    }


    public void stopTimer() {
        timer.stop();
    }

    public void drawCircle(Graphics g, int radius) {
        // circle inside color
        g.setColor(isSelected ? Color.yellow : this.color);
        g.fillOval(xPos - radius, yPos - radius, 2 * radius, 2 * radius);
        // circle border line
        g.setColor(Color.BLACK);
        g.drawOval(xPos - radius, yPos - radius, 2 * radius, 2 * radius);
    }

    //public abstract void paintConnections(Graphics g);

    public abstract List<NodeAbstractAP> getConnectedAPs();


    /**
     * TODO maybe consider the signal power
     *
     * @return > 0 if ap1 is better, < 0 if ap2 is better, 0 if they are equal
     */
    public static int compareGOs(NodeAbstractAP ap1, NodeAbstractAP ap2) {

        if (ap1 == null) return -1;
        if (ap2 == null) return +1;
        return ap2.getNConnectedNodes() - ap1.getNConnectedNodes();
    }

    /*
     * TODO maybe consider the signal power
     */
    public static boolean isGOAvailable(NodeAbstractAP ap) {
        return ap.getNConnectedNodes() < NodeAbstractAP.MAX_CONNECTED_NODES_ON_AP;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public boolean isCoordOnNode(int x2, int y2) {
        int x1 = getX();
        int y1 = getY();

        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) <= radius;
    }

    public abstract void doTimerActions();

    public void moveTo(int x, int y) {
        System.out.println("Moving node " + getName() + " to " + x + ", " + y);
        setX(x);
        setY(y);
    }
}