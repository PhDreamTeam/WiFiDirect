package networkBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Node
 */
class NodeClient extends NodeAbstract {

    public NodeClient(NetworkBuilder networkBuilder, int indexInBuilder, String name, int x,
                      int y) {
        super(networkBuilder, indexInBuilder, name, x, y, 2, Color.BLACK);
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

    public void doTimerActions() {
        if (connectedByWFD == null) {
            // node is currently disconnected in WFD interface
            List<NodeGO> goList = networkBuilder.getGOList(this);
            if (goList.size() == 0) {
                // node is alone, it must advertise itself becoming a GO
                networkBuilder.transformNodeInGO(this);
            } else {
                // node has GOs around - connect to the better one
                NodeGO betterGO = goList.get(0);
                for (int i = 1; i < goList.size(); i++) {
                    if (compareGOs(this, goList.get(i), betterGO) > 0)
                        betterGO = goList.get(i);
                }

                if (betterGO.getNConnectedNodes() < 8)
                    if (networkBuilder.connectClientByWFD(this, betterGO)) {
                        // connection established
                        if (betterGO.getNConnectedNodes() == 8) {
                            // this node should became an GO, but connected to
                            // the other
                            // TODO

                        }
                        return;
                    }
                // All neighboring GO nodes are full
                // TODO ...
            }
        } else {
            // node is connected by WFD, check to connect by WF
            if (connectedByWF == null) {
                // node is currently disconnected in WFD interface
                // connect to
                List<NodeGO> goList = networkBuilder.getGOList(this);
                if (goList.size() != 0) {
                    // node has GOs around - connect to the one GO that is not
                    // connected to my WFD-GO
                    NodeGO bestGO = getBestGONotConnectedToGO(goList,
                            connectedByWFD);
                    System.out.println("Best GO -> " + bestGO);
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

    /**
     * Get the GO with best properties that is not connected to the received GO
     */
    private NodeGO getBestGONotConnectedToGO(List<NodeGO> goList,
                                             NodeGO connectedGO) {
        NodeGO betterGO = null;

        int i = 0;
        for (; i < goList.size(); i++) {
            NodeGO go = goList.get(i);
            if (isGOAcceptable(this, go)) {
                if (isGOConnectedToGO(go, connectedGO)) {
                    betterGO = go;
                    break;
                }
            }
        }

        if (betterGO == null)
            return null;

        for (; i < goList.size(); i++) {
            NodeGO go = goList.get(i);
            if (compareGOs(this, go, betterGO) > 0)
                if (isGOConnectedToGO(go, connectedGO)) {
                    betterGO = go;
                }
        }

        // TODO vamos aqui - getConnectedGOs ...

        return betterGO;
    }

    /*
     *
     */
    private boolean isGOConnectedToGO(NodeAbstractAP ap1, NodeAbstractAP ap2) {
        List<NodeAbstractAP> aps = ap1.getConnectedAPs();
        for (int i = 0; i < aps.size(); i++) {
            // compare by address - caution with this
            if (aps.get(i) == ap2)
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