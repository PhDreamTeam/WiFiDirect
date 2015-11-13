package networkBuilder;

import java.awt.*;

/**
 * NodeAP
 */
public class NodeAP extends NodeAbstractAP {
    private static final long serialVersionUID = 3540731836418017940L;

    /**
     *
     */
    public NodeAP(NetworkBuilder networkBuilder, int id, int x,
                  int y) {
        this(networkBuilder, id, x, y, Color.BLUE);
    }

    /**
     *
     */
    public NodeAP(NetworkBuilder networkBuilder, int id, int x,
                  int y, Color color) {
        super(networkBuilder, id, x, y, color);
    }

    /**
     *
     */
    public NodeAP(NodeAbstract node) {
        super(node.networkBuilder, node.getId(), node.getX(), node
                .getY(), node.getColor());
    }

    /**
     *
     */
    protected String getNamePrefix(){
        return "AP";
    }

    /**
     *
     */
    public void doTimerActions() {
        if(isSelected()) {
            networkBuilder.updateCurrentSelectedNodeInfo(this);
            return;
        }

    }

}