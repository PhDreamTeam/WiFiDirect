package networkBuilder;

import java.awt.*;

/**
 * NodeAP
 */
public class NodeAP extends NodeAbstractAP {
    public NodeAP(NetworkBuilder networkBuilder, String name, int x,
                  int y) {
        this(networkBuilder, name, x, y, Color.BLUE);
    }

    public NodeAP(NetworkBuilder networkBuilder, String name, int x,
                  int y, Color color) {
        super(networkBuilder, name, x, y, color);
    }

    public NodeAP(NodeAbstract node) {
        super(node.networkBuilder, node.getName(), node.getX(), node
                .getY(), node.getColor());
    }

    /*
     *
     */
    public void doTimerActions() {

    }
}