package networkBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class NodeAbstractAP extends NodeAbstract {

    public static int MAX_CONNECTED_NODES_ON_AP = 8;
    protected ArrayList<NodeAbstract> connectedNodes = new ArrayList<NodeAbstract>();

    /*
     *
     */
    public NodeAbstractAP(NetworkBuilder networkBuilder, String name,
                          int x, int y, Color color) {
        super(networkBuilder, name, x, y, NetworkBuilder.MAX_WIFI_RANGE_TO_MAKE_CONNECTIONS, color);
    }

    /*
     *
     */
    public int getNConnectedNodes() {
        return connectedNodes.size();
    }


    /**
     *
     */
    public List<NodeAbstractAP> getConnectedAPs() {
        ArrayList<NodeAbstractAP> aps = new ArrayList<>();
        for (NodeAbstract client: connectedNodes) {
            List<NodeAbstractAP> clientAPs = client.getConnectedAPs();
            for (NodeAbstractAP ap: clientAPs) {
                if (!aps.contains(ap))
                    aps.add(ap);
            }
        }
        return aps;
    }

    /**
     *
     */
    protected boolean addConnectedClient(NodeAbstract node) {
        if (connectedNodes.size() == MAX_CONNECTED_NODES_ON_AP)
            return false;

        connectedNodes.add(node);
        return true;
    }

    /**
     *
     */
    public void disconnectClient(NodeAbstract node) {
        connectedNodes.remove(node);
    }

    /*
     *
     */
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                0.4f));

        // draw inner circle
        super.paintComponent(g);
        // draw coverage circle
        drawCircle(g2, getRadius());

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

}