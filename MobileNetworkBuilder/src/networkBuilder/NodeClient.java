package networkBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Node
 */
public class NodeClient extends NodeAbstract {
    private static final long serialVersionUID = 6522880377285017785L;

    /**
     *
     */
    public NodeClient(NetworkBuilder networkBuilder, int id, int x,
                      int y) {
        super(networkBuilder, id, x, y, 2, Color.BLACK);
    }

    /**
     *
     */
    public NodeClient(NodeAbstractAP nodeGOAP) {
        this(nodeGOAP.networkBuilder, nodeGOAP.getId(), nodeGOAP.getX(), nodeGOAP.getY());
    }

    protected String getNamePrefix(){
        return "CL";
    }

    /**
     *
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintConnections(g);
    }

    /**
     *
     */
    public void paintConnections(Graphics g) {
        int zoom = networkBuilder.getZoomFactor();
        int xSO = networkBuilder.getXScreenOffset();
        int ySO = networkBuilder.getYScreenOffset();

        if (connectedByWF != null) {
            g.setColor(colorWFConnection);
            g.drawLine((getX() + xSO) * zoom, (getY() + ySO) * zoom, (connectedByWF.getX() + xSO) * zoom,
                    (connectedByWF.getY() + ySO) * zoom);
        }
        if (connectedByWFD != null) {
            g.setColor(colorWFDConnection);
            g.drawLine((getX() + xSO) * zoom, (getY() + ySO) * zoom, (connectedByWFD.getX() + xSO) * zoom,
                    (connectedByWFD.getY() + ySO) * zoom);
        }
    }


    /**
     *
     */
    public String getNodeInfo() {
        StringBuilder info = new StringBuilder();

        // this node info
        addNodeAndDirectConnectionsToStringBuilder(info, this);

        // GOs in range
        addGOAPNodesToStringBuilder(info, ":&nbsp; GOs in range:",
                networkBuilder.getGOListInRange(this));

        // other clients in range
        addNodesAndDirectConnectionsToStringBuilder(info, ",&nbsp; Other clients in range:",
                networkBuilder.getClientsListInRange(this));

        return info.toString();
    }


    /*
     *
     */
    public void doTimerActions() {
        // TODO think is is to avoid selection or moving
        if (isSelected()) {
            networkBuilder.updateCurrentSelectedNodeInfo(this);
            return;
        }

        if (connectedByWFD == null) {
            // node is currently disconnected in WFD interface
            List<NodeGO> goList = networkBuilder.getGOListInRange(this);
            if (goList.size() == 0) {
                // node is alone, it must advertise itself becoming a GO
                networkBuilder.transformNodeInGO(this);
            } else {
                // node has GOs around - connect to the better one
                NodeGO betterGO = null;
                for (NodeGO GOi : goList) {
                    if (isGOAvailable(GOi) && compareGOs(GOi, betterGO) > 0)
                        betterGO = GOi;
                }

                if (betterGO != null) {
                    if (networkBuilder.connectClientByWFD(this, betterGO)) {
                        // connection established
                        return;
                    }
                    return;
                } else {
                    // All neighboring GO nodes are full
                    // if this node have one neighborhood client with just only one interface used
                    // just promote this node to GO
                    // TODO ...
                    if (hasNeighbourClientsThatCanBeBridge()) {
                        networkBuilder.transformNodeInGO(this);
                    } else {
                        // Nothing to do, the GO has to free one connection
                    }
                }
            }
        } else {
            // node is connected by WFD, check to connect by WF
            if (connectedByWF == null) {
                // node is currently disconnected in WFD interface
                // connect to
                List<NodeGO> goList = networkBuilder.getGOListInRange(this);
                if (goList.size() != 0) {
                    // node has GOs around - connect to the one GO that is not
                    // connected to my WFD-GO
                    NodeGO bestGO = getBestGONotConnectedToGO(goList,
                            connectedByWFD);
                    //System.out.println("Best GO -> " + bestGO);
                    if (bestGO != null) {
                        System.out.println("Node " + this + " connecting to " + bestGO);
                        networkBuilder.connectClientByWF(this, bestGO);
                    }
                }
            } else {
                // node with both interfaces connected
                // nothing to do
            }
        }
    }

    /*
     * if it have at least one neighbour client tha is available to act as a bridge
     */
    private boolean hasNeighbourClientsThatCanBeBridge() {
        List<NodeClient> neighbours = networkBuilder.getClientsListInRange(this);
        for (NodeClient neighbour : neighbours) {
            if (neighbour.connectedByWF == null || neighbour.connectedByWFD == null)
                return true;
        }
        return false;
    }

    /**
     * Get the GO with best properties that is not connected to the received GO
     */
    private NodeGO getBestGONotConnectedToGO(List<NodeGO> goList,
                                             NodeGO connectedGO) {
        NodeGO betterGO = null;
        for (NodeGO go : goList) {
            if (isGOAvailable(go) && go != connectedGO) {
                if (!isGOConnectedToGO(go, connectedGO) && compareGOs(go, betterGO) > 0) {
                    betterGO = go;
                }
            }
        }
        return betterGO;
    }

    /*
     *
     */
    private boolean isGOConnectedToGO(NodeAbstractAP ap1, NodeAbstractAP ap2) {
        List<NodeAbstractAP> aps = ap1.getConnectedAPs();
        for (NodeAbstractAP ap : aps) {
            // compare by address - caution with this
            if (ap == ap2)
                return true;
        }
        return false;
    }

    /**
     *
     */
    public ArrayList<NodeAbstractAP> getConnectedAPs() {
        ArrayList<NodeAbstractAP> aps = new ArrayList<>();
        if (connectedByWF != null)
            aps.add(connectedByWF);
        if (connectedByWFD != null)
            aps.add(connectedByWFD);
        return aps;
    }

    /**
     *
     */
    public void moveTo(int x, int y) {
        // register new x and y
        super.moveTo(x, y);

        // update connections
        if (connectedByWFD != null) {
            if (!networkBuilder.areInConnectionRange(connectedByWFD, this)) {
                networkBuilder.disconnectWFDClient(this);
            }
        }
        if (connectedByWF != null) {
            if (!networkBuilder.areInConnectionRange(connectedByWF, this)) {
                networkBuilder.disconnectWFClient(this);
            }
        }
    }

    /**
     *
     */
    public void disconnectAll() {
        if(connectedByWFD != null)
            networkBuilder.disconnectWFDClient(this);
        if(connectedByWF != null)
            networkBuilder.disconnectWFClient(this);

    }
}