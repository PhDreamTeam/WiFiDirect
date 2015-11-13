package networkBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class NodeAbstractAP extends NodeAbstract {
    private static final long serialVersionUID = 7228806338473490783L;

    public static int MAX_CONNECTED_NODES_ON_AP = 8;
    protected ArrayList<NodeAbstract> connectedNodes = new ArrayList<NodeAbstract>();

    /*
     *
     */
    public NodeAbstractAP(NetworkBuilder networkBuilder, int id,
                          int x, int y, Color color) {
        super(networkBuilder, id, x, y, NetworkBuilder.MAX_WIFI_RANGE_TO_MAKE_CONNECTIONS, color);
    }

    /*
     *
     */
    public int getNConnectedNodes() {
        return connectedNodes.size();
    }

    /*
     *
     */
    public void moveTo(int x, int y) {
        // register new x and y
        super.moveTo(x, y);

        // a new list to avoid the side effects of original list changes
        ArrayList<NodeAbstract> nodes = new ArrayList<>(connectedNodes);
        for (NodeAbstract nodeClient : nodes) {
            if (!networkBuilder.areInConnectionRange(this, nodeClient)) {
                if(nodeClient.getConnectedByWFD()!= null && nodeClient.getConnectedByWFD().equals(this))
                    networkBuilder.disconnectWFDClient(nodeClient);
                if(nodeClient.getConnectedByWF()!= null && nodeClient.getConnectedByWF().equals(this))
                    networkBuilder.disconnectWFClient(nodeClient);
            }
        }

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
        // draw inner circle
        super.paintComponent(g);
        // draw coverage circle
        drawCircle(g, getRadius());
    }

}