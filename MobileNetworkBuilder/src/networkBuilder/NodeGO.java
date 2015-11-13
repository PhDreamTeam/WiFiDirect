package networkBuilder;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * NodeGO
 */
class NodeGO extends NodeAbstractAP {
    private static final long serialVersionUID = -7183337219303624907L;

    /**
     *
     */
    public NodeGO(NetworkBuilder networkBuilder, int id, int x,
                  int y) {
        super(networkBuilder, id, x, y, Color.RED);
    }

    /**
     *
     */
    public NodeGO(NodeAbstract node) {
        super(node.networkBuilder, node.getId(), node.getX(), node
                .getY(), Color.RED);
    }

    /**
     *
     */
    protected String getNamePrefix(){
        return "GO";
    }

    ArrayList<NodeClient> waitingClients = new ArrayList<>();


    /*
     * If I'm full of clients and there is one clients that is not connected to me (TO MY NETWORK - TODO):
     * I need to send a message to one of my clients to transform itself in a GO.
     * The client node should be: one that have less GOs in the range; and have many potential clients
     * as neighbours (nodes with just one interface in use)
     */
    public void doTimerActions() {
        // System.out.println("Timer actions " + getName());
        if (isSelected()) {
            networkBuilder.updateCurrentSelectedNodeInfo(this);
            return;
        }

        if (connectedNodes.size() == 0) {
            List<NodeGO> gosInRange = networkBuilder.getGOListInRange(this);
            if (gosInRange.size() > 0) {
                networkBuilder.transformNodeGOAPInNodeClient(this);
                return;
            }
        }

        if (connectedNodes.size() == NodeAbstractAP.MAX_CONNECTED_NODES_ON_AP) {
            if (existDisconnectedClientsAndAloneInMyRange()) {
                // promote one of my clients, with just WFD interface used, to GO
                // one that has less GOs as neighbours ( and that has bigger clients like him - NOT DONE)
                NodeAbstract bestNode = null;
                for (NodeAbstract node : connectedNodes) {
                    if (node.connectedByWF == null && node instanceof NodeClient) {
                        bestNode = getNodeWithBiggerGONeighboursNumber(bestNode, node);
                    }
                }
                // promote best node to GO
                networkBuilder.disconnectWFDClient(bestNode);
                networkBuilder.transformNodeInGO(bestNode);
            }
        }
    }



    /**
     *
     */
    private NodeAbstract getNodeWithBiggerGONeighboursNumber(NodeAbstract node1, NodeAbstract node2) {
        if (node1 == null) return node2;
        if (node2 == null) return node1;

        if (networkBuilder.getGOListInRange(node1).size() > networkBuilder.getGOListInRange(node1).size())
            return node1;
        return node2;
    }


    /*
     * there are clients that didn't have any interface use and only have me as neighbour GO
     */
    private boolean existDisconnectedClientsAndAloneInMyRange() {
        // TODO ....
        List<NodeClient> clients = networkBuilder.getClientsListInRange(this);
        for (NodeClient client : clients) {
            if (client.connectedByWF == null && client.connectedByWFD == null &&
                    imTheOnlyReachableGo(client, this)) {
                return true;
            }
        }
        return false;
    }


    /**
     *
     */
    private boolean imTheOnlyReachableGo(NodeClient client, NodeGO nodeGO) {
        List<NodeGO> gos = networkBuilder.getGOListInRange(client);
        return gos.size() == 1;
    }

}