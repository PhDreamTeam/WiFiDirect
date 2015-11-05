package networkBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Node
 */
class NodeClient extends NodeAbstract {

    public NodeClient(NetworkBuilder networkBuilder, String name, int x,
                      int y) {
        super(networkBuilder, name, x, y, 2, Color.BLACK);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintConnections(g);
    }

    public void paintConnections(Graphics g) {
        if (connectedByWF != null) {
            g.setColor(colorWFConnection);
            g.drawLine(getX(), getY(), connectedByWF.getX(),
                    connectedByWF.getY());
        }
        if (connectedByWFD != null) {
            g.setColor(colorWFDConnection);
            g.drawLine(getX(), getY(), connectedByWFD.getX(),
                    connectedByWFD.getY());
        }
    }

    /*
     *
     */
    public void doTimerActions() {
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
                    if(hasNeighbourClientsThatCanBeBridge()){
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
        List<NodeClient> neighbours =  networkBuilder.getClientsListInRange(this);
        for (NodeClient neighbour:neighbours) {
            if(neighbour.connectedByWF == null || neighbour.connectedByWFD == null)
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
        for (NodeAbstractAP ap: aps) {
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
}