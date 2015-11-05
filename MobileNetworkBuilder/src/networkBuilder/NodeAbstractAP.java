package networkBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class NodeAbstractAP extends NodeAbstract {

    public static int MAX_CONNECTED_NODES_ON_AP = 8;
    protected NodeAbstract[] connectedNodes = new NodeAbstract[MAX_CONNECTED_NODES_ON_AP];
    protected int nConnectedNodes = 0;

    public NodeAbstractAP(NetworkBuilder networkBuilder, int indexInBuilder, String name,
                          int x, int y, int radius, Color color) {
        super(networkBuilder, indexInBuilder, name, x, y, radius, color);
    }

    public int getNConnectedNodes() {
        return nConnectedNodes;
    }


    /**
     *
     */
    public List<NodeAbstractAP> getConnectedAPs() {
        ArrayList<NodeAbstractAP> aps = new ArrayList<>();
        for (int i = 0; i < nConnectedNodes; i++) {
            List<NodeAbstractAP> clientAPs = connectedNodes[i].getConnectedAPs();
            for (int j = 0; j < clientAPs.size(); j++) {
                NodeAbstractAP ap = clientAPs.get(j);
                if (!aps.contains(ap))
                    aps.add(ap);
            }
        }
        return aps;
    }

    /**
     *
     */
    protected boolean addConnectedClient(NodeAbstract n) {
        if (nConnectedNodes == connectedNodes.length)
            return false;

        connectedNodes[nConnectedNodes++] = n;
        return true;
    }

}