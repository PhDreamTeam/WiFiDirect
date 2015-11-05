package networkBuilder;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * NodeAP
 */
public class NodeAP extends NodeAbstractAP {
	public NodeAP(NetworkBuilder networkBuilder, int indexInBuilder, String name, int x,
			int y) {
		this(networkBuilder, indexInBuilder, name, x, y, Color.BLUE);
	}

	public NodeAP(NetworkBuilder networkBuilder,int indexInBuilder,  String name, int x,
			int y, Color color) {
		super(networkBuilder, indexInBuilder, name, x, y,
				NetworkBuilder.MAXWIFIRANGETOMAKECONNECTION, color);
	}

	public NodeAP(NodeAbstract node) {
		super(node.networkBuilder, node.indexInBuilder, node.getName(), node.getX(), node
				.getY(), node.getRadius(), node.getColor());
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				0.4f));

		// draw inner circle
		super.paintComponent(g);
		// draw coverage circle
		drawCircle(g2, getRadius());
		// draw connections
		// paintConnections(g); //APAGAR

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
	}



	// public void paintConnections(Graphics g) {
	// g.setColor(Color.BLACK);
	// for (int i = 0; i < nConnectedNodes; i++) {
	// Node n = connectedNodes[i];
	// g.drawLine(getX(), getY(), n.getX(), n.getY());
	// }
	// }

}