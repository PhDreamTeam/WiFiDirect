package networkBuilder;

import java.awt.*;
import java.util.List;

/**
 * Node
 */
public abstract class NodeAbstract {

	public static Color colorWFDConnection = Color.RED;
	public static Color colorWFConnection = Color.BLUE;

	NetworkBuilder networkBuilder;
	int indexInBuilder;

    String name;

	int xPos;
	int yPos;
	int radius;
	Color color;

	NodeGO connectedByWFD;
	NodeAbstractAP connectedByWF;

	public NodeAbstract(NetworkBuilder networkBuilder, int indexInBuilder,
			String name, int x, int y, int radius, Color color) {
		this.networkBuilder = networkBuilder;
		this.indexInBuilder = indexInBuilder;
        this.name = name;
		this.radius = radius;
		xPos = x;
		yPos = y;
		this.color = color;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setX(int xPos) {
		this.xPos = xPos;
	}

	public int getX() {
		return xPos;
	}

	public void setY(int yPos) {
		this.yPos = yPos;
	}

	public int getY() {
		return yPos;
	}

	public int getRadius() {
		return radius;
	}

	public Color getColor() {
		return color;
	}

	public NodeGO getConnectedByWFD() {
		return connectedByWFD;
	}

	public void setConnectedByWFD(NodeGO connectedByWFD) {
		this.connectedByWFD = connectedByWFD;
	}

	public NodeAbstractAP getConnectedByWF() {
		return connectedByWF;
	}

	public void setConnectedByWF(NodeAbstractAP connectedByWF) {
		this.connectedByWF = connectedByWF;
	}

    public String toString() {
        return name;
    }

    public void paintComponent(Graphics g) {
		drawCircle(g, 2);
	}

	public void drawCircle(Graphics g, int radius) {
		// circle inside color
		g.setColor(color);
		g.fillOval(xPos - radius, yPos - radius, 2 * radius, 2 * radius);
		// circle border line
		g.setColor(Color.BLACK);
		g.drawOval(xPos - radius, yPos - radius, 2 * radius, 2 * radius);
	}

	//public abstract void paintConnections(Graphics g);
	
	public abstract List<NodeAbstractAP> getConnectedAPs() ;

	
	/**
	 * @return > 0 if ap1 is better, < 0 if ap2 is better, 0 if they are equal  
	 */
	public static int compareGOs(NodeClient client, NodeAP ap1, NodeAP ap2) {
		// TODO maybe consider the signal power
		return ap2.getNConnectedNodes() - ap1.getNConnectedNodes();
	}

	/*
	 *
	 */
	public static boolean isGOAcceptable(NodeClient client, NodeAP ap) {
		// TODO maybe consider the signal power
		return ap.getNConnectedNodes() < NodeAbstractAP.MAX_CONNECTED_NODES_ON_AP;
	}

	public void doTimerActions() {
		
	}
}