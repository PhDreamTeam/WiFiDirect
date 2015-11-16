package networkBuilder;

import layouts.CenterLayout;
import layouts.FlowLayoutShowAll;
import layouts.ProportionalLayout;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Mobile network backbone builder
 * <p/>
 * IN Work:
 * DONE: individual timers, mover nós
 * <p/>
 * Scenario doesn't stop
 * Código dos APs
 * Optimizar (reorganizar a rede) / reconfigurar
 * Falha no algoritmo: dois GO com dois clientes estes não se connectam
 * UM GO que se mova e ficar perto de outro GO, nenhum deles volta a ser um Cliente (caso de ego)
 *
 * - alterar o GITHUB para a HEAD
 *
 *  - load scenario and timer (what to do?)
 *  - cliente: um novo GO, que não está ligado a nada, mas o CL vê que esse GO vê outro
 *  GO activo e com espaço e portanto não se liga
 *
 * buu2 Master
 */
public class NetworkBuilder extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final int TIMER_STEP = 1000;

    public static int MAX_WIFI_RANGE_TO_MAKE_CONNECTIONS = 32;

    private int OFFSET_SCREEN_FACTOR = 10;

    List<ArrayList<ScenarioNode>> timerJobs = Collections
            .synchronizedList(new ArrayList<ArrayList<ScenarioNode>>());

    JButton btnClearAll = null;

    private JPanel buttonsPanel = null;

    private NodesPanel myPanel = null;

    ArrayList<NodeAbstract> nodes = new ArrayList<>(100);

    Timer globaltimer = null;

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
    private JLabel nodesLabel;
    private Timer timerInfo;

    // zoom data
    private int zoomFactor = 1;
    private SpinnerNumberModel spinnerZoomModel;
    private JSpinner spinnerZoom;

    private int xScreenOffset = 0, yScreenOffset = 0;
    private JLabel labelCurrentSelectedNode;
    private KeyEventDispatcher ked;

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
                System.out.println("WindowClosed... stopping all timers");

                // stopping all timers
                doStopTimerActions();
                doStopIndividualTimerActions();
                timerInfo.stop();
            }
        };
        addWindowListener(wa);

        // main layout - content pane
        getContentPane().setLayout(new ProportionalLayout(0.1f, 0, 0.05f, 0.05f));

        // Button start globaltimer
        btnStartTimer = new JButton("Start Global Timer");
        btnStartTimer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doStartTimerActions();
            }
        });

        // Button stop globaltimer
        btnStopTimer = new JButton("Stop Global Timer");
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

        // Button step globaltimer
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

        // buttons panel
        buttonsPanel = new JPanel(new FlowLayoutShowAll());
        buttonsPanel.add(btnStartTimer);
        buttonsPanel.add(btnStopTimer);
        buttonsPanel.add(btnStartIndTimers);
        buttonsPanel.add(btnStopIndTimers);
        buttonsPanel.add(btnStepTimer);
        buttonsPanel.add(btnClearAll);

        // Zoom area
        JPanel panelZoom = new JPanel();
        panelZoom.setBorder(BorderFactory.createLineBorder(Color.gray));
        panelZoom.add(new JLabel("Zoom: "));
        // Spinner and SpinnerModel
        spinnerZoomModel = new SpinnerNumberModel(1, 1, 4, 1); // value, min, max, step
        spinnerZoom = new JSpinner(spinnerZoomModel);
        spinnerZoom.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                zoomFactor = spinnerZoomModel.getNumber().intValue();
                repaint();
            }
        });
        spinnerZoom.setEditor(new JSpinner.DefaultEditor(spinnerZoom));
        panelZoom.add(spinnerZoom);
        buttonsPanel.add(panelZoom);

        // upper Panel
        myPanel = new NodesPanel();
        myPanel.setBorder(BorderFactory.createLineBorder(Color.black));

        // bottomPanel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonsPanel, BorderLayout.NORTH);
        labelCurrentSelectedNode = new JLabel("Current node:");
        Border outsideBorder = BorderFactory.createLineBorder(Color.lightGray);
        Border insideBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        Border border = BorderFactory.createCompoundBorder(outsideBorder, insideBorder);
        labelCurrentSelectedNode.setBorder(border);
        labelCurrentSelectedNode.setHorizontalAlignment(SwingConstants.CENTER);
        bottomPanel.add(labelCurrentSelectedNode, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(myPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, ProportionalLayout.CENTER);

        // info panel with one label
        JPanel infoPanel = new JPanel(new CenterLayout());
        nodesLabel = new JLabel();
        infoPanel.add(nodesLabel);
        add(infoPanel, BorderLayout.NORTH);

        System.out.println("Press mouse buttons inside panel");

        // first square
        // addNode(new Node(this, numberOfNodes, 50, 50));

        myPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int xSO = getXScreenOffset();
                int ySO = getYScreenOffset();

                int x = e.getX() / getZoomFactor() - xSO;
                int y = e.getY() / getZoomFactor() - ySO;


                // right mouse button
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if ((e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
                        // left button - create new NodeClient
                        addNode(new NodeClient(NetworkBuilder.this, nextNodeID++,
                                x, y));
                        repaint();
                    } else {
                        // select action
                        selectAction(x, y);
                        if (currentSelectedNode != null) {
                            deltaXSelectedNode = currentSelectedNode.getX() - x;
                            deltaYSelectedNode = currentSelectedNode.getY() - y;
                        }
                    }
                }

                if (e.getButton() == MouseEvent.BUTTON3) {
                    if ((e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
                        // left button and no CTRL key pressed - create new GO
                        addNode(new NodeGO(NetworkBuilder.this, nextNodeID++, x, y));
                    } else {
                        // left button and CTRL key pressed - create new AP
                        addNode(new NodeAP(NetworkBuilder.this, nextNodeID++, x, y));
                    }
                    repaint();
                }
            }
        });

        // moving support listener
        myPanel.addMouseMotionListener(
                new MouseMotionAdapter() {
                    public void mouseDragged(MouseEvent e) {
                        int xSO = getXScreenOffset();
                        int ySO = getYScreenOffset();

                        int x = e.getX() / getZoomFactor() - xSO;
                        int y = e.getY() / getZoomFactor() - ySO;

                        //System.out.println("Drag");
                        if (currentSelectedNode != null) {
                            moveNodeTO(currentSelectedNode, deltaXSelectedNode + x,
                                    deltaYSelectedNode + y);
                        }
                    }
                }
        );


        // globaltimer and globaltimer listener
        globaltimer = new Timer(TIMER_STEP, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTimerActions();
            }
        });

        // timer info and info timer listener
        timerInfo = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateInfoLabel();
            }
        });
        timerInfo.start();

        // set the start directory for scenario loading file chooser
        fc.setCurrentDirectory(new File("MobileNetworkBuilder/src/networkBuilder/scenarios"));

        // create menu and put it on frame
        setJMenuBar(getMenu());

        /**
         * Handle direction keys
         */
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(getKeyEventDispatcher());


        // puts the frame visible (is not visible at start)
        setVisible(true);
    }

    private KeyEventDispatcher getKeyEventDispatcher() {
        if (ked == null) {

            ked = new KeyEventDispatcher() {
                public boolean dispatchKeyEvent(KeyEvent e) {
                    //System.out.println("KD " + e);
                    if (e.getID() == KeyEvent.KEY_RELEASED) {
                        int deltaXOffset = 0;
                        int deltaYOffset = 0;
                        switch (e.getKeyCode()) {
                            case 37: // left
                                deltaXOffset = 1;
                                break;
                            case 38: // up
                                deltaYOffset = 1;
                                break;
                            case 39: // right
                                deltaXOffset = -1;
                                break;
                            case 40: // down
                                deltaYOffset = -1;
                                break;
                        }
                        addScreenOffset(deltaXOffset, deltaYOffset);
                        nodesLabel.requestFocus();
                    }

                    // return true, indicating that this key was processed
                    return e.getKeyCode() >= 37 && e.getKeyCode() <= 40;
                }
            };
        }
        return ked;
    }

    private void addScreenOffset(int deltaXOffset, int deltaYOffset) {
        deltaXOffset *= OFFSET_SCREEN_FACTOR;
        deltaYOffset *= OFFSET_SCREEN_FACTOR;

        xScreenOffset += deltaXOffset;
        yScreenOffset += deltaYOffset;
        repaint();
    }

    /*
     *
     */
    private void moveNodeTO(NodeAbstract node, int x, int y) {
        node.moveTo(x, y);
        repaint();
    }

    public void setTextOnLabelCurrentSelectedNode(String txt) {
        labelCurrentSelectedNode.setText("<html>" + txt + "</html>");
    }

    /*
     *
     */
    public void updateInfoLabel() {
        StringBuilder msgBuilder = new StringBuilder("Nodes: ");
        for (NodeAbstract node : nodes) {
            msgBuilder.append(node);
            msgBuilder.append(" ");
        }
        nodesLabel.setText(msgBuilder.toString());
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
        if (currentSelectedNode != null) {
            currentSelectedNode.setSelected(false);
            updateCurrentSelectedNodeInfo(null);
        }

        if (node != null) {
            node.setSelected(true);
            updateCurrentSelectedNodeInfo(node);
        }

        currentSelectedNode = node;
    }

    public int getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(int zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    /**
     *
     */
    private JMenuBar getMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Menu");
        menuBar.add(menu);

        ActionListener al = getMenuListener();

        JMenuItem miLoadScenario = new JMenuItem("Load Scenario", KeyEvent.VK_L);
        miLoadScenario.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                InputEvent.ALT_MASK));
        miLoadScenario.addActionListener(al);
        menu.add(miLoadScenario);

        JMenuItem miSaveScenario = new JMenuItem("Save Scenario", KeyEvent.VK_S);
        miSaveScenario.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.ALT_MASK));
        miSaveScenario.addActionListener(al);
        menu.add(miSaveScenario);

        menu.addSeparator();

        JMenuItem miDeleteSelectedNodeScenario = new JMenuItem("Delete selected node", KeyEvent.VK_D);
        miDeleteSelectedNodeScenario.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                InputEvent.ALT_MASK));
        miDeleteSelectedNodeScenario.addActionListener(al);
        menu.add(miDeleteSelectedNodeScenario);

        menu.addSeparator();

        JMenuItem miHelp = new JMenuItem("Help", KeyEvent.VK_H);
        miHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,
                InputEvent.ALT_MASK));
        miHelp.addActionListener(al);
        menu.add(miHelp);

        JMenuItem miAbout = new JMenuItem("About", KeyEvent.VK_A);
        miAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                InputEvent.ALT_MASK));
        miAbout.addActionListener(al);
        menu.add(miAbout);

        menu.addSeparator();

        JMenuItem miExit = new JMenuItem("Exit", KeyEvent.VK_X);
        miExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                InputEvent.ALT_MASK));
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

                if (menuItemText.equals("Delete selected node"))
                    deleteSelectedNodeActions();

                if (menuItemText.equals("Help"))
                    helpActions();

                if (menuItemText.equals("About"))
                    aboutActions();

                if (menuItemText.equals("Exit"))
                    exitActions();
            }
        };
    }

    /*
     *
     */
    private void deleteSelectedNodeActions() {
        NodeAbstract node = currentSelectedNode;

        setSelectedNode(null);

        node.stopTimer();

        disconnectNode(node);

        nodes.remove(node);

        repaint();
    }

    /*
     * help actions
     */
    private void helpActions() {
        String strHelp = "HELP NOTES: ";
        strHelp += "\n\nSave scenario:\n   - scenarios will be saved only as serialized objects. " +
                "They should be saved with an extension different than .txt";
        strHelp += "\nLoad scenario:\n   - it is possible to load serialized or txt scenarios";
        strHelp += "\n\nScenario edition:\n   - add simple node:  left click;" +
                "\n   - add GO:  right click;\n   - add AP:  CTRL + right click" +
                "\n   - select node:  CTRL + left clickL on node (on empty area to unselect)";
        strHelp += "\n\nChanging Scenario offset:\n   - with keys: Up, Down, Left and Right";

        JOptionPane.showMessageDialog(this,
                strHelp, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    /*
     * about actions
     */
    private void aboutActions() {
        String strAbout = "Hyrax project - Mobile Network Backbone Builder" +
                "\n\nV0.6" +
                "\n\nNOVA LINCS, DI-FCT-UNL" +
                "\n\nby António Teófilo and Diogo Remédios" +
                "\nsupervised by  Hervé Paulino and João Lourenço";

        JOptionPane.showMessageDialog(this, strAbout, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exitActions() {
        // stop timers and dispose frame
        doStopTimerActions();
        if (indidividualTimersActivated) {
            doStopIndividualTimerActions();
        }
        timerInfo.stop();
        dispose();

    }

    /**
     *
     */
    private void loadScenarioActions() {
        // remove keyboard focus manager that suppress arrow keys
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(getKeyEventDispatcher());

        // open file chooser
        int returnVal = fc.showOpenDialog(this);

        // set again the keyboard focus manager
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(getKeyEventDispatcher());

        // process the result
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
        // remove keyboard focus manager that suppress arrow keys
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(getKeyEventDispatcher());

        // open file chooser
        int returnVal = fc.showSaveDialog(this);

        // set again the keyboard focus manager
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(getKeyEventDispatcher());

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

        // save current selected node - to avoid save scenario to have it
        NodeAbstract currentSelectedNode = this.currentSelectedNode;
        setSelectedNode(null);

        try {
            OutputStream file = new FileOutputStream(scenarioPathName);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);
            output.writeObject(nodes);
            output.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // get back old selected node
        setSelectedNode(currentSelectedNode);
    }

    /**
     * Load a serialized scenario
     */
    private void loadSerializedScenario(String scenarioPathName) {
        // stop the timers, just in case
        doStopTimerActions();
        doStopIndividualTimerActions();

        // clear all existing nodes
        clearAll();

        try {
            InputStream file = new FileInputStream(scenarioPathName);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);

            // deserialize the List
            @SuppressWarnings("unchecked")
            List<NodeAbstract> loadedNodes = (List<NodeAbstract>) input.readObject();
            input.close();

            // add nodes
            for (NodeAbstract node : loadedNodes) {
                node.setNetworkBuilder(this);
                addNode(node);
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
        globaltimer.start();
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
        globaltimer.stop();
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
                NodeClient newNode = new NodeClient(this, scenarioNode.id, scenarioNode.x,
                        scenarioNode.y);
                addNode(newNode);
            }
        }

        // do globaltimer actions on all existing nodes
        for (int i = 0; i < nodes.size(); ++i)
            nodes.get(i).doTimerActions();

        // update GUI
        repaint();
    }

    /*
     * Load txt scenario
     */
    protected void loadScenario(String fileName) {
        try {
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
                    int id = lineContents.nextInt();
                    int x = lineContents.nextInt();
                    int y = lineContents.nextInt();
                    System.out.print(" " + id + "(" + x + ", " + y + ")");
                    // create new node and add it to lineNodes arrayList
                    lineNodes.add(new ScenarioNode(id, x, y));
                }
                System.out.println();
                // submit lineNodes as a timer job
                timerJobs.add(lineNodes);
                timerSlot++;
                lineContents.close();
            }
            scan.close();
            // start the globaltimer to process its jobs
            doStartTimerActions();
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName);
        }
    }

    /**
     * Add a node to scenario
     */
    boolean addNode(NodeAbstract node) {
        if (nodes.contains(node))
            throw new RuntimeException("Global nodes add node: error duplicate IDs: " + node);

        setSelectedNode(null);

        nodes.add(node);

        if (node.getId() >= nextNodeID)
            nextNodeID = node.getId() + 1;

        if (indidividualTimersActivated) {
            node.startTimer(getIndividualTimerDelay());
        }
        return true;
    }


    /**
     *
     */
    public void clearAll() {
        nodes.clear();
        nextNodeID = 1;
        myPanel.repaint();
    }

    /*
     *
     */
    public NodeAP transformNodeInAP(NodeAbstract node) {
        node.stopTimer();

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

        if (indidividualTimersActivated)
            ap.startTimer(getIndividualTimerDelay());

        repaint();
        return ap;
    }

    /*
     *
     */
    public NodeGO transformNodeInGO(NodeAbstract node) {
        node.stopTimer();

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

        if (indidividualTimersActivated)
            go.startTimer(getIndividualTimerDelay());

        repaint();
        return go;
    }

    /*
     *
     */
    public NodeClient transformNodeGOAPInNodeClient(NodeAbstractAP nodeGO) {
        nodeGO.stopTimer();

        // get client node from GO node
        NodeClient nodeCli = new NodeClient(nodeGO);

        // get index of node
        int idx = nodes.indexOf(nodeGO);
        if (idx == -1)
            throw new RuntimeException("Node not found is NODES: " + nodeGO);

        // remove old node
        nodes.remove(idx);
        // add new node at the same index
        nodes.add(idx, nodeCli);

        if (indidividualTimersActivated)
            nodeCli.startTimer(getIndividualTimerDelay());

        repaint();
        return nodeCli;
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

    public int getXScreenOffset() {
        return xScreenOffset;
    }

    public int getYScreenOffset() {
        return yScreenOffset;
    }

    public void updateCurrentSelectedNodeInfo(NodeAbstract node) {
        setTextOnLabelCurrentSelectedNode(node == null ? "" : node.getNodeInfo());
    }

    Font nodesFont = new Font("Courier", Font.PLAIN, 10);


    /**
     * Network nodes panel
     */
    class NodesPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private AlphaComposite alphaGOAPS = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
        private AlphaComposite alphaNoAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);


        private RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        /**
         * paint method
         */
        public void paintComponent(Graphics g) {
            // System.out.println("paintComponent called");
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHints(rh);

            // first draw APs and GOs
            g2.setComposite(alphaGOAPS);
            for (NodeAbstract node : nodes) {
                if (node instanceof NodeAbstractAP)
                    node.paintComponent(g);
            }
            g2.setComposite(alphaNoAlpha);

            // then draw clients
            for (NodeAbstract node : nodes) {
                if (!(node instanceof NodeAbstractAP))
                    node.paintComponent(g);
            }


            g.setFont(nodesFont);

            // then draw node names
            for (NodeAbstract node : nodes) {
                node.paintNodeName(g);
            }

        }
    }

    /**
     *
     */
    public List<NodeGO> getGOListInRange(NodeAbstract node) {
        ArrayList<NodeGO> gos = new ArrayList<>();

        for (NodeAbstract n : nodes) {
            if (!n.equals(node) && n instanceof NodeGO) {
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

    /**
     *
     */
    public void disconnectWFDClient(NodeAbstract nodeClient) {
        NodeGO go = nodeClient.connectedByWFD;
        nodeClient.connectedByWFD = null;
        go.disconnectClient(nodeClient);
        repaint();
    }

    /**
     *
     */
    public void disconnectWFClient(NodeAbstract nodeClient) {
        NodeAbstractAP ap = nodeClient.connectedByWF;
        nodeClient.connectedByWF = null;
        ap.disconnectClient(nodeClient);
        repaint();
    }

    /**
     *
     */
    public void disconnectNode(NodeAbstract node) {
        node.disconnectAll();
    }

    /**
     *
     */
    public void disconnectClient(NodeAbstractAP nodeAbstractAP, NodeAbstract nodeToDisconnect) {
        if (nodeToDisconnect.connectedByWFD != null && nodeToDisconnect.connectedByWFD.equals(nodeAbstractAP))
            disconnectWFDClient(nodeToDisconnect);

        if (nodeToDisconnect.connectedByWF != null && nodeToDisconnect.connectedByWF.equals(nodeAbstractAP))
            disconnectWFClient(nodeToDisconnect);
    }

    /**
     *
     */
    class ScenarioNode {
        int id, x, y;

        public ScenarioNode(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }
}

