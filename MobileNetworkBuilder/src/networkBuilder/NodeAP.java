package networkBuilder;

import java.awt.*;

/**
 * NodeAP
 */
public class NodeAP extends NodeAbstractAP {
    private static final long serialVersionUID = 3540731836418017940L;

    public NodeAP(NetworkBuilder networkBuilder, int id, int x,
                  int y) {
        this(networkBuilder, id, x, y, Color.BLUE);
    }

    public NodeAP(NetworkBuilder networkBuilder, int id, int x,
                  int y, Color color) {
        super(networkBuilder, id, x, y, color);
    }

    public NodeAP(NodeAbstract node) {
        super(node.networkBuilder, node.getId(), node.getX(), node
                .getY(), node.getColor());
    }

    protected String getNamePrefix(){
        return "AP";
    }

    /*
     *
     */
    public void doTimerActions() {
        if(isSelected()) {
            networkBuilder.updateCurrentSelectedNodeInfo(this);
            return;
        }

    }

    /*
     *
     */
    public String getNodeInfo() {
        StringBuilder info = new StringBuilder(getName());

        if (connectedByWFD != null) {
            info.append(",&nbsp; WFD: ");
            info.append(connectedByWFD.getName());
        }

        addNodeAndDirectConnectionsToStringBuilder(info, ",&nbsp; Connected nodes:", connectedNodes);
        addNodesToStringBuilder(info, ",&nbsp; GOs in range:", networkBuilder.getGOListInRange(this));
        addNodeAndDirectConnectionsToStringBuilder(info, ",&nbsp; Other clients in range:",
                networkBuilder.getClientsListInRange(this));

        return info.toString();
    }
}