package networkBuilder;

import layouts.ProportionalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Painting - several squares, all of them share the same rhythm and timer. All
 * the information is on main class.
 *
 * @author Ant�nio Te�filo
 */
public class NetworkBuilder extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final int TIMER_STEP = 1000;

    public static int MAXWIFIRANGETOMAKECONNECTION = 32;

    List<ArrayList<ScenarioNode>> timerJobs = Collections
            .synchronizedList(new ArrayList<ArrayList<ScenarioNode>>());

    JButton btnClearAll = null;

    private JPanel buttonsPanel = null;

    private NodesPanel myPanel = null;

    // TODO passar isto para um ArrayList
    NodeAbstract[] nodes = new NodeAbstract[100];

    Timer timer = null;

    int numberOfNodes = 0;

    NodeAbstract currentNode = null;

    int buttonPressed = -1;

    int deltaX = 0;
    int deltaY = 1;

    int width = 0;
    int height = 0;

    private int nextNodeID = 1;

    private JButton btnStartTimer;

    private JButton btnStopTimer;

    private JButton btnLoadScenario;

    private JButton btnStepTimer;

    private JFileChooser fc = new JFileChooser();

    /**
     * Este método cria toda a frame e coloca-a visível
     */
    public void init() {
        // set title
        setTitle("...: Mobile Network Backbone Builder :...");
        // set size
        setSize(800, 600);
        // set location
        setLocationRelativeTo(null); // to center a frame
        // set what happens when close button is pressed
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        WindowAdapter wa = new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                System.out.println(" windowClosed...");
            }
        };
        addWindowListener(wa);

        // button Clear All
        btnClearAll = new JButton("Clear all");
        btnClearAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });

        // Button start timer
        btnStartTimer = new JButton("Start Timer");
        btnStartTimer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doStartTimerActions();
            }
        });

        // Button stop timer
        btnStopTimer = new JButton("Stop Timer");
        btnStopTimer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doStopTimerActions();
            }
        });
        btnStopTimer.setVisible(false);

        // Button step timer
        btnStepTimer = new JButton("Step Timer");
        btnStepTimer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTimerActions();
            }
        });

        // Button stop timer
        btnLoadScenario = new JButton("Load Scenario");
        btnLoadScenario.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadScenarioActions();
            }
        });

        buttonsPanel = new JPanel();
        buttonsPanel.add(btnStartTimer);
        buttonsPanel.add(btnStopTimer);
        buttonsPanel.add(btnStepTimer);
        buttonsPanel.add(btnClearAll);
        buttonsPanel.add(btnLoadScenario);

        getContentPane().setLayout(new ProportionalLayout(0.1f));

        myPanel = new NodesPanel();
        myPanel.setBorder(BorderFactory.createLineBorder(Color.black));

        add(myPanel, ProportionalLayout.CENTER);
        add(buttonsPanel, ProportionalLayout.SOUTH);

        System.out.println("Press mouse buttons inside panel");

        // first square
        // addNode(new Node(this, numberOfNodes, 50, 50));

        myPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // right mouse button
                buttonPressed = e.getButton();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // left button - create new NodeClient
                    if (numberOfNodes < nodes.length) {
                        addNode(new NodeClient(NetworkBuilder.this, numberOfNodes, "NI-" + nextNodeID++,
                                e.getX(), e.getY()));
                        currentNode = nodes[numberOfNodes - 1];
                        repaint();
                    }
                }

                if (e.getButton() == MouseEvent.BUTTON3) {
                    // left button - create new square
                    if (numberOfNodes < nodes.length) {
                        addNode(new NodeGO(NetworkBuilder.this, numberOfNodes, "NI-" + nextNodeID++,
                                e.getX(), e.getY()));
                        currentNode = nodes[numberOfNodes - 1];
                        repaint();
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                currentNode = null;
                buttonPressed = -1;
            }
        });

        myPanel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                //System.out.println("Label resized...");
                myPanelResized();
            }
        });

        // timer and timer listener
        timer = new Timer(TIMER_STEP, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTimerActions();
            }
        });

        // set the start directory for scenario loading file chooser
        fc.setCurrentDirectory(new File("MobileNetworkBuilder/src/networkBuilder/scenarios"));

        // puts the frame visible (is not visible at start)
        setVisible(true);
    }


    private void loadScenarioActions() {
        // open file chooser
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            String scenarioPathName = file.getPath();
            System.out.println("Loading scenario: " + file.getName());
            loadScenario(scenarioPathName);
        } else {
            System.out.println("Load scenario command cancelled by user");
        }
    }

    /**
     *
     */
    private void doStartTimerActions() {
        timer.start();
        btnStartTimer.setVisible(false);
        btnStopTimer.setVisible(true);
    }

    /**
     *
     */
    private void doStopTimerActions() {
        timer.stop();
        btnStartTimer.setVisible(true);
        btnStopTimer.setVisible(false);
    }

    private void doTimerActions() {
        // execute one timer jobs if exist
        if (!timerJobs.isEmpty()) {
            ArrayList<ScenarioNode> scenarioNodes = timerJobs.remove(0);
            for (ScenarioNode scenarioNode : scenarioNodes) {
                addNode(new NodeClient(this, numberOfNodes, scenarioNode.name, scenarioNode.x,
                        scenarioNode.y));
            }
        }

        // do timer actions on all existing nodes
        for (int i = 0; i < numberOfNodes; i++) {
            nodes[i].doTimerActions();
        }

        // update GUI
        repaint();
    }

    protected void loadScenario(String fileName) {
        try {
            //new File("d.txt").createNewFile();
            Scanner scan = new Scanner(new File(fileName));
            int timerSlot = 0;
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                if (line.trim().isEmpty())
                    continue;
                // we have a line with contents
                Scanner lineContents = new Scanner(line);
                ArrayList<ScenarioNode> lineNodes = new ArrayList<>();
                System.out.print("Nodes of time slot " + timerSlot + " :");
                // read line contents
                while (lineContents.hasNext()) {
                    String name = lineContents.next();
                    int x = lineContents.nextInt();
                    int y = lineContents.nextInt();
                    System.out.print(" " + name + "(" + x + ", " + y + ")");
                    // create new node and add it to lineNodes arrayList
                    lineNodes.add(new ScenarioNode(name, x, y));
                }
                System.out.println();
                // submit lineNodes as a timer job
                timerJobs.add(lineNodes);
                timerSlot++;
                lineContents.close();
            }
            scan.close();
            // start the timer to process its jobs
            doStartTimerActions();
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName);
        }
    }

    /**
     *
     */
    boolean addNode(NodeAbstract node) {
        if (numberOfNodes == nodes.length)
            return false;
        nodes[numberOfNodes++] = node;
        return true;
    }

    /**
     *
     */
    protected void myPanelResized() {
        width = myPanel.getWidth();
        height = myPanel.getHeight();
        // System.out.println("width -> " + width + " height -> " + height);
    }

    /**
     *
     */
    public void clearAll() {
        for (int i = 0; i < numberOfNodes; i++) {
            nodes[i] = null;
        }
        numberOfNodes = 0;
        myPanel.repaint();
    }

    public NodeAP transformNodeInAP(NodeAbstract node) {
        NodeAP ap = new NodeAP(node);
        nodes[ap.indexInBuilder] = ap;
        return ap;
    }

    public NodeGO transformNodeInGO(NodeAbstract node) {
        NodeGO go = new NodeGO(node);
        nodes[go.indexInBuilder] = go;
        repaint();
        return go;
    }

    // private void moveSquare(AbstractAP rs, int xx, int yy) {
    // int x = rs.getX();
    // int y = rs.getY();
    // int width = rs.getWidth();
    // int height = rs.getHeight();
    // int OFFSET = 1;
    //
    // // The square is moving, repaint background
    // // over the old square location.
    // myPanel.repaint(x, y, width + OFFSET, height + OFFSET);
    //
    // // Update coordinates.
    // x = xx - deltaX;
    // rs.setX(x);
    // y = yy - deltaY;
    // rs.setY(y);
    //
    // // Repaint the square at the new location.
    // myPanel.repaint(x, y, width + OFFSET, height + OFFSET);
    // // myPanel.repaint();
    // }

    /**
     * Main
     */
    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                NetworkBuilder myFrame = new NetworkBuilder();
                myFrame.init();
                // life goes on
                System.out.println("Frame created...");
            }
        });
        System.out.println("End of main...");
    }

    /**
     *
     */
    class NodesPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        public void paintComponent(Graphics g) {
            // System.out.println("paintComponent called");
            super.paintComponent(g);

            ((Graphics2D) g).setRenderingHints(rh);

            // first draw APs and GOs
            for (int i = 0; i < numberOfNodes; i++) {
                if (nodes[i] instanceof NodeAP)
                    nodes[i].paintComponent(g);
            }

            // then draw clients
            for (int i = 0; i < numberOfNodes; i++) {
                if (!(nodes[i] instanceof NodeAP))
                    nodes[i].paintComponent(g);
            }
        }
    }

    /**
     *
     */
    public List<NodeGO> getGOList(NodeAbstract node) {
        ArrayList<NodeGO> gos = new ArrayList<NodeGO>();

        for (int i = 0; i < numberOfNodes; i++) {
            if (nodes[i] instanceof NodeGO) {
                if (areInConnectionRange(node, nodes[i]))
                    gos.add((NodeGO) nodes[i]);
            }

        }
        return gos;
    }

    /**
     *
     */
    private boolean areInConnectionRange(NodeAbstract node1, NodeAbstract node2) {
        int x1 = node1.getX();
        int x2 = node2.getX();
        int y1 = node1.getY();
        int y2 = node2.getY();

        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) <= MAXWIFIRANGETOMAKECONNECTION;
    }

    /**
     *
     */
    public boolean connectClientByWFD(NodeAbstract client, NodeGO go) {
        if (go.getNConnectedNodes() == NodeAP.MAX_CONNECTED_NODES_ON_AP || client.getConnectedByWFD() != null)
            return false;

        go.addConnectedClient(client);
        client.setConnectedByWFD(go);
        return true;
    }

    /**
     *
     */
    public boolean connectClientByWF(NodeAbstract client, NodeAbstractAP apgo) {
        if (apgo.getNConnectedNodes() == NodeAP.MAX_CONNECTED_NODES_ON_AP || client.getConnectedByWF() != null)
            return false;

        apgo.addConnectedClient(client);
        client.setConnectedByWF(apgo);
        return true;
    }

    class ScenarioNode {
        String name;
        int x, y;

        public ScenarioNode(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }
}
