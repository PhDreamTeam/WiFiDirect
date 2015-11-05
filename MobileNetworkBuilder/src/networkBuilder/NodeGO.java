package networkBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * NodeGO
 */
class NodeGO extends NodeAP {

	public NodeGO(NetworkBuilder networkBuilder, int indexInBuilder, String name, int x,
			int y) {
		super(networkBuilder, indexInBuilder, name, x, y, Color.RED);
	}

	public NodeGO(NodeAbstract node) {
		super(node.networkBuilder, node.indexInBuilder, node.getName(), node.getX(), node
				.getY(), Color.RED);
	}

	public void paintConnections(Graphics g) {
		g.setColor(Color.RED);
		for (int i = 0; i < nConnectedNodes; i++) {
			NodeAbstract n = connectedNodes[i];
			g.drawLine(getX(), getY(), n.getX(), n.getY());
		}
	}

}