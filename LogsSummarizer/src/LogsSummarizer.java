import sun.rmi.runtime.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by António Teófilo on 04/07/2016
 * .
 */
public class LogsSummarizer extends JFrame {

    private static String START_DIR = "C:\\AT\\PhD\\ADB\\scenarios\\";

    private String workingDirectory;
    private JLabel labelWorkingDirectory;
    private JFileChooser fc;

    private boolean guiExecution = true;


    private void init(String[] args) {
        if (args.length == 0) {
            init();
            return;
        }

        guiExecution = false;
        for (int i = 0; i < args.length; i++) {
            //System.out.println("Processing dir: " + args[i]);
            setWorkingDirectory(args[i]);
            processLogs();
        }
    }

    /**
     *
     */
    private void init() {
        setTitle("LogsSummarizer V1.0");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 300);

        setLocationRelativeTo(null);

        setLayout(new ProportionalLayout(0.3f, 0f, 0f, 0f));

        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        fc.setApproveButtonText("Do Log Summary");

        // intro label
        JLabel labelLogSSummarizer = new JLabel("Logs Summarizer");
        labelLogSSummarizer.setFont(new Font("Comic Sans MS", Font.BOLD, 30));
        labelLogSSummarizer.setOpaque(true);
        labelLogSSummarizer.setBackground(new Color(120, 194, 87));
        labelLogSSummarizer.setHorizontalAlignment(SwingConstants.CENTER);
        add(labelLogSSummarizer, ProportionalLayout.NORTH);

        JPanel auxPanel = new JPanel(new BorderLayout());
        add(auxPanel, ProportionalLayout.CENTER);

        // center panel
        labelWorkingDirectory = new JLabel();
        labelWorkingDirectory.setFont(new Font("Comic Sans MS", Font.BOLD, 20));
        labelWorkingDirectory.setOpaque(true);
        labelWorkingDirectory.setBackground(new Color(194, 134, 44));
        labelWorkingDirectory.setHorizontalAlignment(SwingConstants.CENTER);
        auxPanel.add(labelWorkingDirectory, BorderLayout.CENTER);


        // buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(true);
        buttonsPanel.setBackground(new Color(48, 133, 192));

        JButton btnSelectWorkingDirectory = new JButton("Select working directory");
        buttonsPanel.add(btnSelectWorkingDirectory);

        auxPanel.add(buttonsPanel, BorderLayout.SOUTH);

        File curDir = new File(START_DIR);
        if (!curDir.exists())
            curDir = new File("");

        setWorkingDirectory(curDir.getAbsolutePath());

        btnSelectWorkingDirectory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectWorkingDirectory_actionListener();
            }
        });

        setVisible(true);
    }

    /**
     * Each log file can only contain one log session
     */
    private void processLogs() {

        // get all log file
        FilenameFilter fnf = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("log_") && name.endsWith(".txt");
            }
        };

        File workDir = new File(workingDirectory);
        String[] logFiles = workDir.list(fnf);

        Arrays.sort(logFiles);

        // get log summary resulting fle
        try {
            PrintWriter pw = new PrintWriter(workingDirectory + "//logSummary.txt");

            // process all files
            for (String logFile : logFiles) {
                Scanner fileScan = new Scanner(new File(workingDirectory + "//" + logFile));
                pw.print(logFile + " ");
                while (fileScan.hasNextLine()) {
                    String line = fileScan.nextLine();

                    if (line.contains("ClientSendDataThreadTCP") || line.contains("ClientSendDataThreadUDP")) {
                        doSenderTCPUDPSummary(fileScan, pw);
                    }

                    if (line.contains("ClientDataReceiverThreadTCP")) {
                        doReceiverTCPSummary(fileScan, pw);
                    }

                    if (line.contains("ClientDataReceiverServerSocketThreadUDP")) {
                        doReceiverUDPSummary(fileScan, pw);
                    }


                    if (line.contains("CrForwardThreadTCP")) {
                        doTCPRelaySummary(fileScan, pw);
                    }
                }
                fileScan.close();
            }

            pw.close();
            if (guiExecution)
                JOptionPane.showMessageDialog(this, "Working directory precessed: " + workingDirectory);
            else System.out.println("Directory: " + workingDirectory + ", processed");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void doSenderTCPUDPSummary(Scanner fileScan, PrintWriter pw) {
        double maxSendSpeed = 0, avgSendSpeed = 0, joules = 0;

        while (fileScan.hasNextLine()) {
            String line2 = fileScan.nextLine();

            if (line2.contains("Max speed")) {
                maxSendSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("Data sent speed")) {
                avgSendSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("joules")) {
                joules = Double.parseDouble(getLastToken(line2));
            }
        }
        pw.println(maxSendSpeed + " " + avgSendSpeed + " " + joules);
    }

    /**
     *
     */
    private void doReceiverTCPSummary(Scanner fileScan, PrintWriter pw) {
        double maxReceiveSpeed = 0, avgReceiveSpeed = 0, joules = 0;

        while (fileScan.hasNextLine()) {
            String line2 = fileScan.nextLine();

            if (line2.contains("Max rcv speed")) {
                maxReceiveSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("Global average speed")) {
                avgReceiveSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("joules")) {
                joules = Double.parseDouble(getLastToken(line2));
            }
        }
        pw.println(maxReceiveSpeed + " " + avgReceiveSpeed + " " + joules);
    }

    /**
     *
     */
    private void doReceiverUDPSummary(Scanner fileScan, PrintWriter pw) {
        double maxReceiveSpeed = 0, avgReceiveSpeed = 0, joules = 0;
        long receivedBytes = 0;

        while (fileScan.hasNextLine()) {
            String line2 = fileScan.nextLine();

            if (line2.contains("Receive max speed")) {
                maxReceiveSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("Receive global speed")) {
                avgReceiveSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("Received bytes")) {
                receivedBytes = Long.parseLong(getLastToken(line2));
            }

            if (line2.contains("joules")) {
                joules = Double.parseDouble(getLastToken(line2));
            }
        }
        pw.println(maxReceiveSpeed + " " + avgReceiveSpeed + " " + joules + " " + receivedBytes);
    }

    /**
     *
     */
    private void doTCPRelaySummary(Scanner fileScan, PrintWriter pw) {
        double fwdMaxReceiveSpeed = 0, fwdAvgSpeed = 0, bckMaxReceiveSpeed = 0, bckAvgSpeed = 0, joules = 0;

        while (fileScan.hasNextLine()) {
            String line2 = fileScan.nextLine();

            if (line2.contains("FWD Data forwarded Max speed")) {
                fwdMaxReceiveSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("FWD Data forwarded speed")) {
                fwdAvgSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("BCK Data forwarded Max speed")) {
                bckMaxReceiveSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("BCK Data forwarded speed")) {
                bckAvgSpeed = Double.parseDouble(getLastToken(line2));
            }

            if (line2.contains("joules")) {
                joules = Double.parseDouble(getLastToken(line2));
            }
        }
        pw.println(fwdMaxReceiveSpeed + " " + fwdAvgSpeed + " " +
                bckMaxReceiveSpeed + " " + bckAvgSpeed + " " + joules);
    }


    /**
     *
     */
    private String getLastToken(String str) {
        String[] tokens = str.trim().split("\\s+");
        return tokens[tokens.length - 1];
    }

    /**
     *
     */
    private void selectWorkingDirectory_actionListener() {
        fc.setCurrentDirectory(new File(workingDirectory));
        //fc.setCurrentDirectory(new File(workingDirectory).getParentFile());

        int returnVal = fc.showOpenDialog(LogsSummarizer.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFiles();
            for (File file: files) {
                if (!file.isDirectory()) {
                    file = file.getParentFile();
                }
                // This is where a real application would open the file.
                System.out.println("FileChooser, selected as working directory: " + file.getAbsolutePath());
                setWorkingDirectory(file.getAbsolutePath());
                processLogs();
            }
        } else {
            System.out.println("FileChooser, cancelled by user.");
        }
    }

    /**
     *
     */
    void setWorkingDirectory(String workDir) {
        workingDirectory = workDir;
        if (labelWorkingDirectory != null)
            labelWorkingDirectory.setText("<html><center>Working dir:<br>" + workDir + "<center><html>");
    }

    /**
     *
     */
    public static void main(final String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LogsSummarizer().init(args);
            }
        });
    }

}
