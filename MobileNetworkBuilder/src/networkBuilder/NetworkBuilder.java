package networkBuilder;

import layouts.ProportionalLayout;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Mobile network backbone builder
 * .
 */
public class NetworkBuilder extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final int TIMER_STEP = 1000;

    public static int MAX_WIFI_RANGE_TO_MAKE_CONNECTIONS = 32;

    List<ArrayList<ScenarioNode>> timerJobs = Collections
            .synchronizedList(new ArrayList<ArrayList<ScenarioNode>>());

    JButton btnClearAll = null;

    private JPanel buttonsPanel = null;

    private NodesPanel myPanel = null;

    ArrayList<NodeAbstract> nodes = new ArrayList<>(100);

    Timer timer = null;

    int buttonPressed = -1;

    private int nextNodeID = 1;

    private JButton btnStartTimer;

    private JButton btnStopTimer;

    private JButton btnStepTimer;

    private JFileChooser fc = new JFileChooser();

    private NodeAbstract currentSelectedNode = null;
    private int deltaXSelectedNode;
    private int deltaYSelectedNode;
    private JButton btnStartIndTimers;
    private JButton btnStopIndTimers;

    Random rg = new Random();
    private boolean indidividualTimersActivated;

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

        // Button start individual timers
        btnStartIndTimers = new JButton("Start Individual Timers");
        btnStartIndTimers.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doStartIndividualTimerActions();
            }
        });

        // Button stop individual timers
        btnStopIndTimers = new JButton("Stop Individual Timers");
        btnStopIndTimers.setVisible(false);
        btnStopIndTimers.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doStopIndividualTimerActions();
            }
        });

        // Button step timer
        btnStepTimer = new JButton("Step Timer");
        btnStepTimer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTimerActions();
            }
        });

        // button Clear All
        btnClearAll = new JButton("Clear all");
        btnClearAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });

        buttonsPanel = new JPanel();
        buttonsPanel.add(btnStartTimer);
        buttonsPanel.add(btnStopTimer);
        buttonsPanel.add(btnStartIndTimers);
        buttonsPanel.add(btnStopIndTimers);
        buttonsPanel.add(btnStepTimer);
        buttonsPanel.add(btnClearAll);

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
                //buttonPressed = e.getButton();

                if (e.getButton() == MouseEvent.BUTTON1) {
                    if ((e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
                        // left button - create new NodeClient
                        addNode(new NodeClient(NetworkBuilder.this, "NI-" + nextNodeID++,
                                e.getX(), e.getY()));
                        repaint();
                    } else {
                        // select action
                        selectAction(e.getX(), e.getY());
                        if (currentSelectedNode != null) {
                            deltaXSelectedNode = currentSelectedNode.getX() - e.getX();
                            deltaYSelectedNode = currentSelectedNode.getY() - e.getY();
                        }
                    }
                }

                if (e.getButton() == MouseEvent.BUTTON3) {
                    if ((e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
                        // left button and no CTRL key pressed - create new GO
                        addNode(new NodeGO(NetworkBuilder.this, "NI-" + nextNodeID++,
                                e.getX(), e.getY()));
                    } else {
                        // left button and CTRL key pressed - create new AP
                        addNode(new NodeAP(NetworkBuilder.this, "NI-" + nextNodeID++,
                                e.getX(), e.getY()));
                    }
                    repaint();
                }
            }

            public void mouseReleased(MouseEvent e) {
                //buttonPressed = -1;
            }
        });

        // moving support listener
        myPanel.addMouseMotionListener(
                new MouseMotionAdapter() {
                    public void mouseDragged(MouseEvent e) {
                        //System.out.println("Drag");
                        if (currentSelectedNode != null) {
                            moveNodeTO(currentSelectedNode, deltaXSelectedNode + e.getX(),
                                    deltaYSelectedNode + e.getY());
                        }
                    }
                }
        );


        // timer and timer listener
        timer = new Timer(TIMER_STEP, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTimerActions();
            }
        });

        // set the start directory for scenario loading file chooser
        fc.setCurrentDirectory(new File("MobileNetworkBuilder/src/networkBuilder/scenarios"));

        // create menu and put it on frame
        setJMenuBar(createMenu());

        // puts the frame visible (is not visible at start)
        setVisible(true);
    }

    /*
     *
     */
    private void moveNodeTO(NodeAbstract node, int x, int y) {
        node.moveTo(x, y);
        repaint();
    }

    /*
     *
     */
    private void selectAction(int x, int y) {
        // search first of simple nodes
        for (NodeAbstract node : nodes) {
            if (!(node instanceof NodeAbstractAP)) {
                if (node.isCoordOnNode(x, y)) {
                    setSelectedNode(node);
                    repaint();
                    return;
                }
            }
        }

        // search then on AP nodes
        for (NodeAbstract node : nodes) {
            if (node instanceof NodeAbstractAP) {
                if (node.isCoordOnNode(x, y)) {
                    setSelectedNode(node);
                    repaint();
                    return;
                }
            }
        }

        setSelectedNode(null);
        repaint();
    }

    public void setSelectedNode(NodeAbstract node) {
        if (currentSelectedNode != null)
            currentSelectedNode.setSelected(false);

        if (node != null)
            node.setSelected(true);

        currentSelectedNode = node;
    }

    /**
     *
     */
    private JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Menu");
        menuBar.add(menu);

        ActionListener al = getMenuListener();

        JMenuItem miLoadScenario = new JMenuItem("Load Scenario", KeyEvent.VK_L);
        miLoadScenario.addActionListener(al);
        menu.add(miLoadScenario);

        JMenuItem miSaveScenario = new JMenuItem("Save Scenario", KeyEvent.VK_S);
        miSaveScenario.addActionListener(al);
        menu.add(miSaveScenario);

        menu.addSeparator();

        JMenuItem miHelp = new JMenuItem("Help", KeyEvent.VK_H);
        miHelp.addActionListener(al);
        menu.add(miHelp);

        JMenuItem miAbout = new JMenuItem("About", KeyEvent.VK_A);
        miAbout.addActionListener(al);
        menu.add(miAbout);

        menu.addSeparator();

        JMenuItem miExit = new JMenuItem("Exit", KeyEvent.VK_X);
        miExit.addActionListener(al);
        menu.add(miExit);

        return menuBar;
    }

    ActionListener getMenuListener() {
        // Menu Action Listener
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String menuItemText = ((JMenuItem) (e.getSource())).getText();

                if (menuItemText.equals("Load Scenario"))
                    loadScenarioActions();

                if (menuItemText.equals("Save Scenario"))
                    saveScenarioActions();

                if (menuItemText.equals("Help"))
                    helpActions();

                if (menuItemText.equals("About"))
                    aboutActions();

                if (menuItemText.equals("Exit"))
                    exitActions();
            }
        };
    }

    private void helpActions() {
        String strHelp = "HELP NOTES: ";
        strHelp += "\n\nSave scenario:\n   - scenarios will be saved only as serialized objects. " +
                "They should be saved with an extension different than .txt";
        strHelp += "\nLoad scenario:\n   - it is possible to load serialized or txt scenarios";
        strHelp += "\n\nScenario edition:\n   - add simple node:  left click;" +
                "\n   - add GO:  right click;\n   - add AP:  right click + CTRL";

        JOptionPane.showMessageDialog(this,
                strHelp, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void aboutActions() {
        String strAbout = "Hyrax project - Mobile Network Backbone Builder" +
                "\n\nV0.6" + "\n\nby António Teófilo and Diogo Remédios"
                + "\n\n supervised by  Hervé Paulino and João Lourenço";

        JOptionPane.showMessageDialog(this, strAbout, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exitActions() {
        // stop timer and dispose frame
        doStopTimerActions();
        if(indidividualTimersActivated) {
            doStopIndividualTimerActions();
        }
        dispose();

    }

    /**
     *
     */
    private void loadScenarioActions() {
        // open file chooser
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            String scenarioPathName = file.getPath();
            System.out.println("Loading scenario: " + file.getName());
            if (file.getName().endsWith(".txt"))
                loadScenario(scenarioPathName);
            else
                loadSerializedScenario(scenarioPathName);
        } else {
            System.out.println("Load scenario command cancelled by user");
        }
    }

    /**
     *
     */
    private void saveScenarioActions() {
        // open file chooser
        int returnVal = fc.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            String scenarioPathName = file.getPath();
            System.out.println("Loading scenario: " + file.getName());
            saveSerializedScenario(scenarioPathName);
        } else {
            System.out.println("Load scenario command cancelled by user");
        }
    }

    /*
     *
     */
    private void saveSerializedScenario(String scenarioPathName) {
        try {
            OutputStream file = new FileOutputStream(scenarioPathName);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);
            output.writeObject(nodes);
            output.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadSerializedScenario(String scenarioPathName) {
        // stop the timer, just in case
        doStopTimerActions();

        // clear all existing nodes
        clearAll();

        try {
            InputStream file = new FileInputStream(scenarioPathName);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);

            // deserialize the List
            List<NodeAbstract> loadedNodes = (List<NodeAbstract>) input.readObject();
            input.close();

            // add nodes
            for (NodeAbstract node : loadedNodes) {
                node.setNetworkBuilder(this);
                nodes.add(node);
            }
            repaint();

        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
    *
    */
    private void doStartIndividualTimerActions() {
        for (NodeAbstract node : nodes) {
            node.startTimer(getIndividualTimerDelay());
        }

        indidividualTimersActivated = true;
        btnStartTimer.setVisible(false);
        btnStartIndTimers.setVisible(false);
        btnStopIndTimers.setVisible(true);
        btnStepTimer.setVisible(false);
    }

    /*
     *
     */
    private void doStopIndividualTimerActions() {
        for (NodeAbstract node : nodes) {
            node.stopTimer();
        }

        indidividualTimersActivated = false;
        btnStartTimer.setVisible(true);
        btnStartIndTimers.setVisible(true);
        btnStopIndTimers.setVisible(false);
        btnStepTimer.setVisible(true);
    }


    /**
     *
     */
    private void doStartTimerActions() {
        timer.start();
        btnStartTimer.setVisible(false);
        btnStopTimer.setVisible(true);
        btnStartIndTimers.setVisible(false);
        btnStopIndTimers.setVisible(false);
        btnStepTimer.setVisible(false);
    }

    /**
     *
     */
    private void doStopTimerActions() {
        timer.stop();
        btnStartTimer.setVisible(true);
        btnStopTimer.setVisible(false);
        btnStartIndTimers.setVisible(true);
        btnStopIndTimers.setVisible(false);
        btnStepTimer.setVisible(true);
    }

    /*
     *
     */
    private void doTimerActions() {
        // execute one timer jobs if exist
        if (!timerJobs.isEmpty()) {
            ArrayList<ScenarioNode> scenarioNodes = timerJobs.remove(0);
            for (ScenarioNode scenarioNode : scenarioNodes) {
                addNode(new NodeClient(this, scenarioNode.name, scenarioNode.x,
                        scenarioNode.y));
            }
        }

        // do timer actions on all existing nodes
        for (int i = 0; i < nodes.size(); ++i)
            nodes.get(i).doTimerActions();

        // update GUI
        repaint();
    }

    /*
     *
     */
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
        setSelectedNode(null);
        nodes.add(node);
        if(indidividualTimersActivated) {
            node.startTimer(getIndividualTimerDelay());
        }
        return true;
    }


    /**
     *
     */
    public void clearAll() {
        nodes.clear();
        myPanel.repaint();
    }

    /*
     *
     */
    public NodeAP transformNodeInAP(NodeAbstract node) {
        // get GO node from node
        NodeAP ap = new NodeAP(node);

        // get index of node
        int idx = nodes.indexOf(node);
        if (idx == -1)
            throw new RuntimeException("Node not found is NODES: " + node);

        // remove old node
        nodes.remove(idx);
        // add new node at the same index
        nodes.add(idx, ap);

        repaint();
        return ap;
    }

    /*
     *
     */
    public NodeGO transformNodeInGO(NodeAbstract node) {
        // get GO node from node
        NodeGO go = new NodeGO(node);

        // get index of node
        int idx = nodes.indexOf(node);
        if (idx == -1)
            throw new RuntimeException("Node not found is NODES: " + node);

        // remove old node
        nodes.remove(idx);
        // add new node at the same index
        nodes.add(idx, go);

        repaint();
        return go;
    }

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

    public int getIndividualTimerDelay() {
        return TIMER_STEP + rg.nextInt((int) (TIMER_STEP * 0.2));
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
            for (NodeAbstract node : nodes) {
                if (node instanceof NodeAbstractAP)
                    node.paintComponent(g);
            }

            // then draw clients
            for (NodeAbstract node : nodes) {
                if (!(node instanceof NodeAbstractAP))
                    node.paintComponent(g);
            }
        }
    }

    /**
     *
     */
    public List<NodeGO> getGOListInRange(NodeAbstract node) {
        ArrayList<NodeGO> gos = new ArrayList<NodeGO>();

        for (NodeAbstract n : nodes) {
            // TODO fazer equals pelo nome
            if (n != node && n instanceof NodeGO) {
                if (areInConnectionRange(node, n))
                    gos.add((NodeGO) n);
            }
        }
        return gos;
    }

    /**
     *
     */
    public List<NodeClient> getClientsListInRange(NodeAbstract node) {
        ArrayList<NodeClient> clients = new ArrayList<>();

        for (NodeAbstract n : nodes) {
            if (n != node && n instanceof NodeClient) {
                if (areInConnectionRange(node, n))
                    clients.add((NodeClient) n);
            }
        }
        return clients;
    }

    /**
     *
     */
    public boolean areInConnectionRange(NodeAbstract node1, NodeAbstract node2) {
        int x1 = node1.getX();
        int x2 = node2.getX();
        int y1 = node1.getY();
        int y2 = node2.getY();

        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) <= MAX_WIFI_RANGE_TO_MAKE_CONNECTIONS;
    }

    /**
     *
     */
    public boolean connectClientByWFD(NodeAbstract client, NodeGO go) {
        if (go.getNConnectedNodes() == NodeAP.MAX_CONNECTED_NODES_ON_AP || client.getConnectedByWFD() != null)
            return false;

        go.addConnectedClient(client);
        client.setConnectedByWFD(go);
        repaint();
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
        repaint();
        return true;
    }

    /*
     *
     */
    public void disconnectWFDClient(NodeAbstract nodeClient) {
        NodeGO go = nodeClient.connectedByWFD;
        nodeClient.connectedByWFD = null;
        go.disconnectClient(nodeClient);
        repaint();
    }

    /*
     *
     */
    public void disconnectWFClient(NodeAbstract nodeClient) {
        NodeAbstractAP ap = nodeClient.connectedByWF;
        nodeClient.connectedByWF = null;
        ap.disconnectClient(nodeClient);
        repaint();
    }

    /*
     *
     */
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
