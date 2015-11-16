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

    protected String getNamePrefix() {
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
        // Do not execute actions when selected, to avoid node transformation (in GO/AP)
        if (isSelected()) {
            networkBuilder.updateCurrentSelectedNodeInfo(this);
            return;
        }

        if (connectedByWFD == null && connectedByWF == null) {
            // node is currently disconnected in WFD interface
            List<NodeGO> goList = networkBuilder.getGOListInRange(this);
            if (goList.size() == 0) {
                // node is alone, it must advertise itself becoming a GO
                networkBuilder.transformNodeInGO(this);
            } else {
                // node has GOs around - connect to the better one
                NodeGO betterGO = null;
//                for (NodeGO GOi : goList) {
//                    if (isGOAvailable(GOi) && compareGOs(GOi, betterGO) > 0)
//                        betterGO = GOi;
//                }
                betterGO = getBestGONotConnectedToGO(goList,
                        connectedByWF);

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
                        return;
                    } else {
                        // Nothing to do, the GO has to free one connection
                    }
                }
            }
        }

        // node is connected by WFD, checking to connect by WF
        if (connectedByWFD != null && connectedByWF == null) {
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
                    return;
                }
            }
        }

        // node is connected by WF, checking to connect by WFD
        if (connectedByWFD == null && connectedByWF != null) {
            // node is currently disconnected in WFD interface
            // connect to
            List<NodeGO> goList = networkBuilder.getGOListInRange(this);
            if (goList.size() != 0) {
                // node has GOs around - connect to the one GO that is not
                // connected to my WFD-GO
                NodeGO bestGO = getBestGONotConnectedToGO(goList,
                        connectedByWF);
                //System.out.println("Best GO -> " + bestGO);
                if (bestGO != null) {
                    System.out.println("Node " + this + " connecting to " + bestGO);
                    networkBuilder.connectClientByWFD(this, bestGO);
                    return;
                }
            }
        }

        if (caseTwoNotConnectedGOSWithClientsInRange())
            return;
    }

    /**
     * handle anomaly case of Two Not Connected GOS With Clients In Range
     */
    boolean caseTwoNotConnectedGOSWithClientsInRange() {
        //  1) um nó cliente CL1 ligado a GO5 vê outro cliente CL2 ligado a GO6
        //  2)   e GO5 não está ligado a GO6
        //  3)   e CL1 é o melhor PGO
        //          - PGO (potencial GO:
        //                um cliente (ao alcance a CL2) que tem um outro cliente CL3 do mesmo GO ao seu alcance
        //          - Melhor PGO: o que tiver maior ID.
        //  4) THEN Converter em GO
        //  5) TODO: Caso não haja nem um PGO: entre CL1 e CL2 o com maior ID; ex: Cl1, passar CL1 a GO, passar o seu GO a client
        //        GO5 passa a cliente (levar consigo a informação de alteração)

        // 1,2) checking if this node client is in range of another client connected to a GO that is not
        //    connected to the GO of the current node
        NodeClient cliOutsider = getFirstClientInRangeConnectedToUnknownGO();
        if (cliOutsider == null)
            return false;

        NodeGO outsiderGO = (NodeGO) cliOutsider.getConnectedAPs().get(0);
        // 3) Am I the best PGO to connect to outsiderGO
        if (!isBestPGO(outsiderGO))
            return false;

        // 4) THEN convert myself to GO
        System.out.println("Case Two Not Connected GOS With Clients In Range occurred....");
        System.out.println(this + " is best PGO to make the connection to " + outsiderGO);

        disconnectAll();
        networkBuilder.transformNodeInGO(this);
        return true;
    }

    /**
     * Checking if this client is the best potential GO to enable a connection between
     * this node GO to the outsider GO  (to convert myself to GO).
     * <p/>
     * Best PGO: the one with higher ID
     *
     * @param outsiderGO
     * @return
     */
    private boolean isBestPGO(NodeGO outsiderGO) {
        List<NodeClient> clientsListInRange = networkBuilder.getClientsListInRange(this);

        NodeAbstractAP myGO = getConnectedByWFD() != null ? getConnectedByWFD() : getConnectedByWF();

        NodeClient bestPGO = this;

        for (NodeClient nc : clientsListInRange) {
            if (nc.getConnectedAPs().size() == 2)
                continue;
            NodeAbstractAP go = nc.getConnectedByWFD() != null ? nc.getConnectedByWFD() : nc.getConnectedByWF();
            if (go == null)
                continue;

            // connected to outsider GO or this GO
            if (go.equals(outsiderGO) || go.equals(myGO)) {
                if (comparePGOs(bestPGO, nc) < 0)
                    bestPGO = nc;
            }
        }

        return bestPGO.equals(this);
    }

    /*
     * One client node c1 is best PGO than another client node c2
     * if c1
     */
    private static int comparePGOs(NodeClient c1, NodeClient c2) {
        int diff = c1.getNBrothersPotentialBridgesInRange() - c2.getNBrothersPotentialBridgesInRange();
        if (diff != 0)
            return diff;
        return c1.getId() - c2.getId();
    }

    /**
     * @return
     */
    private int getNBrothersPotentialBridgesInRange() {
        NodeAbstractAP myGO = getConnectedByWFD() != null ? getConnectedByWFD() : getConnectedByWF();

        List<NodeClient> clientsListInRange = networkBuilder.getClientsListInRange(this);
        int n = 0;
        for (NodeClient nc : clientsListInRange) {
            if (nc.getConnectedAPs().size() == 1 && nc.getConnectedAPs().get(0).equals(myGO))
                n++;
        }
        return n;
    }

    /**
     * Return the first client in range of this, that is only connected to one GO (unknown GO)
     */
    private NodeClient getFirstClientInRangeConnectedToUnknownGO() {
        // this client cannot have two GOAPs
        if (getConnectedAPs().size() != 1)
            return null;

        NodeAbstractAP myGO = getConnectedByWFD() != null ? getConnectedByWFD() : getConnectedByWF();

        List<NodeClient> clientsListInRange = networkBuilder.getClientsListInRange(this);
        for (NodeClient nc : clientsListInRange) {
            if (nc.getConnectedAPs().size() == 2)
                continue;
            NodeAbstractAP go = nc.getConnectedByWFD() != null ? nc.getConnectedByWFD() : nc.getConnectedByWF();
            if (go == null)
                continue;
            if (myGO.equals(go) || isGOConnectedToGO(myGO, go))
                continue;
            // we get a client in range that has as unconnected GO to his GO
            return nc;
        }
        return null;
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
                                             NodeAbstractAP connectedGO) {
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
        if (connectedByWFD != null)
            networkBuilder.disconnectWFDClient(this);
        if (connectedByWF != null)
            networkBuilder.disconnectWFClient(this);

    }
}