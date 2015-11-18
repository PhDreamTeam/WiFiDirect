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

    public static int MAX_TIME_TO_MOVE_MS = 20 * 1000;
    public static int MIN_TIME_TO_MOVE_MS = 1 * 1000;

    public static int SPEED_MIN_CMs_SEC = (int) (0.5 * 1000);
    public static int SPEED_MAX_CMs_SEC = 5 * 1000;

    //public static Random rg = new Random();

    private static int PRIVILEGED_DURATION_TICKS = 3;

    transient NetworkBuilder networkBuilder;

    // node id
    int id;

    // x, y at the center of the node
    private double xPos;
    private double yPos;

    // Moving fields
    int ticksToWait = (int) (Math.random() * 10 + 2);
    int ticksToMove = 0;
    double xDeltaMovingPxPerTick = Math.random() * 4 - 2; // -N..-1..0..1..N
    double yDeltaMovingPxPerTick = Math.random() * 4 - 2; // -N..-1..0..1..N

    // central radius for simple nodes; coverage radius of APs and GOs
    int radius;

    // color
    Color color;

    private boolean isSelected;

    /*
     * This flag says that the node is privileged when compared to other, s it will not change state
     * The privilege will just remain for some tick (defined in a constant)
     */
    private boolean isPrivilegedNode = false;

    /*
     * Number of ticks to lose privileged behaviour
     */
    private int nPrivilegedTicks = 0;

    Timer timer = null;

    // GO/AP connected by available interfaces
    NodeGO connectedByWFD;
    NodeAbstractAP connectedByWF;

    /**
     *
     */
    public NodeAbstract(NetworkBuilder networkBuilder,
                        int id, int x, int y, int radius, Color color) {
        this.networkBuilder = networkBuilder;
        this.id = id;
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

    /**
     *
     */
    public String getName() {
        return getNamePrefix() + id + (isPrivilegedNode ? "P" : "");
    }


    public boolean getIsPrivilegedNode() {
        return isPrivilegedNode;
    }


    public void setIsPrivilegedNode(boolean isPrivilegedNode) {
        this.isPrivilegedNode = isPrivilegedNode;
        if (isPrivilegedNode)
            nPrivilegedTicks = PRIVILEGED_DURATION_TICKS;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    protected abstract String getNamePrefix();


    /**
     *
     */
    public void setX(int xPos) {
        this.xPos = xPos;
    }

    /**
     *
     */
    public int getX() {
        return (int) xPos;
    }

    /**
     *
     */
    public void setY(int yPos) {
        this.yPos = yPos;
    }

    /**
     *
     */
    public int getY() {
        return (int) yPos;
    }

    /**
     *
     */
    public int getRadius() {
        return radius;
    }

    /**
     *
     */
    public Color getColor() {
        return color;
    }

    /**
     *
     */
    public NodeGO getConnectedByWFD() {
        return connectedByWFD;
    }

    /**
     *
     */
    public void setConnectedByWFD(NodeGO connectedByWFD) {
        this.connectedByWFD = connectedByWFD;
    }

    /**
     *
     */
    public NodeAbstractAP getConnectedByWF() {
        return connectedByWF;
    }

    /**
     *
     */
    public void setConnectedByWF(NodeAbstractAP connectedByWF) {
        this.connectedByWF = connectedByWF;
    }

    /**
     *
     */
    public void setNetworkBuilder(NetworkBuilder networkBuilder) {
        this.networkBuilder = networkBuilder;
    }

    /**
     *
     */
    public boolean equals(Object obj) {
        return obj != null && (obj instanceof NodeAbstract) && getName().equalsIgnoreCase(
                ((NodeAbstract) obj).getName());
    }

    /**
     *
     */
    public String toString() {
        return getName();
    }

    /**
     *
     */
    public void paintComponent(Graphics g) {
        drawCircle(g, 2);
    }

    /**
     *
     */
    public void startTimer(int delay) {
        timer.setDelay(delay);
        timer.start();
    }

    /**
     *
     */
    public void stopTimer() {
        timer.stop();
    }

    /**
     *
     */
    public void drawCircle(Graphics g, int radius) {
        int zoom = networkBuilder.getZoomFactor();
        int xSO = networkBuilder.getXScreenOffset();
        int ySO = networkBuilder.getYScreenOffset();

        // circle inside color
        g.setColor(isSelected ? Color.yellow : this.color);
        g.fillOval((getX() - radius + xSO) * zoom, (getY() - radius + ySO) * zoom, 2 * radius * zoom,
                2 * radius * zoom);
        // circle border line
        g.setColor(Color.BLACK);
        g.drawOval((getX() - radius + xSO) * zoom, (getY() - radius + ySO) * zoom, 2 * radius * zoom,
                2 * radius * zoom);
    }

    public void paintNodeName(Graphics g) {
        int zoom = networkBuilder.getZoomFactor();
        int xSO = networkBuilder.getXScreenOffset();
        int ySO = networkBuilder.getYScreenOffset();

        g.setColor(Color.darkGray);
        g.drawString(getName(), (getX() + xSO) * zoom - 10, (getY() + ySO) * zoom - 3 - 2 * zoom);
    }

    /**
     *
     */
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

    /**
     *
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     *
     */
    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    /**
     *
     */
    public boolean isCoordOnNode(int x2, int y2) {
        int x1 = getX();
        int y1 = getY();

        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) <= radius;
    }

    /**
     *
     */
    public void doTimerActions() {
        if (isPrivilegedNode && --nPrivilegedTicks == 0)
            setIsPrivilegedNode(false);

        if (networkBuilder.isAutoMoveActivated())
            doAutoMoveActions();
    }

    /*
     *
     */
    private void doAutoMoveActions() {
        if (--ticksToWait == 0) {
            // start new movement
            ticksToMove = 10;
        }

        if (--ticksToMove >= 0) {
            moveByTick();
            networkBuilder.repaint();
        }

        if (ticksToMove == 0) {
            // start new stop time
            ticksToWait = 20;
        }
    }

    /**
     *
     */
    public void moveTo(int x, int y) {
        System.out.println("Moving node " + getName() + " to " + x + ", " + y);
        setX(x);
        setY(y);
    }

    /**
     *
     */
    public void moveByTick() {
        // these fields must be accessed directly
        xPos += xDeltaMovingPxPerTick;
        yPos += yDeltaMovingPxPerTick;

        System.out.println("Moving by tick node " + getName() + " to " + (int) xPos + ", " + (int) yPos);
    }

    /**
     *
     */
    public String getNodeInfo() {
        return getName();
    }

    /**
     *
     */
    void addNodesAndDirectConnectionsToStringBuilder(StringBuilder info, String initialMsg, List<? extends NodeAbstract> nodes) {
        info.append(initialMsg);
        info.append("{");

        boolean isFirst = true;
        for (NodeAbstract node : nodes) {
            if (isFirst)
                isFirst = false;
            else info.append(", ");
            addNodeAndDirectConnectionsToStringBuilder(info, node);
        }
        info.append("}");
    }

    /**
     *
     */
    void addNodeAndDirectConnectionsToStringBuilder(StringBuilder info, String initialMsg, NodeAbstract node) {
        info.append(initialMsg);
        addNodeAndDirectConnectionsToStringBuilder(info, node);
    }

    /**
     *
     */
    void addNodeAndDirectConnectionsToStringBuilder(StringBuilder info, NodeAbstract node) {
        info.append(node.getName());
        info.append("[");
        if (node.connectedByWFD != null) {
            info.append("wfd(");
            info.append(node.connectedByWFD.getName());
            info.append(")");
        }
        if (node.connectedByWF != null) {
            if (node.connectedByWFD != null)
                info.append(",");
            info.append("wf(");
            info.append(node.connectedByWF.getName());
            info.append(")");
        }
        info.append("]");
    }

    /**
     * Add list of GOAPS and the GOAPs that they can connect with just one client in the middle
     */
    static void addGOAPNodesToStringBuilder(StringBuilder info, String initialMsg, List<? extends NodeAbstractAP> nodes) {
        info.append(initialMsg);
        info.append("{");

        boolean isFirst = true;
        for (NodeAbstractAP nodeGOAP : nodes) {
            if (isFirst)
                isFirst = false;
            else info.append(", ");
            info.append(nodeGOAP.getName());

            // show GO connected to the current GO
            boolean isFirst2 = true;
            info.append("[");
            List<NodeAbstractAP> conGOAPs = nodeGOAP.getConnectedAPs();
            for (NodeAbstractAP nodeGOAPi : conGOAPs) {
                if (!nodeGOAPi.equals(nodeGOAP)) {
                    if (isFirst2)
                        isFirst2 = false;
                    else info.append(", ");
                    info.append(nodeGOAPi.getName());
                }
            }
            info.append("]");

        }

        info.append("}");
    }

    /**
     *
     */
    public abstract void disconnectAll();
}