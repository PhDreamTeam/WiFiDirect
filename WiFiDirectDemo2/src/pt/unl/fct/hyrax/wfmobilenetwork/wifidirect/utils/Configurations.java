package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils;

import android.util.Log;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Created by AT DR on 23-02-2016
 * .
 */ /*
 *
 */
public class Configurations {

    enum PriorityInterface {WFD, WF, NOTSET}

    ;

    PriorityInterface priorityInterface = PriorityInterface.NOTSET;
    String deviceName = null;

    /*
     * empty and private constructor to avoid been called.
     * To get an instance you should call readFromConfigurationsFile
     */
    private Configurations() {

    }

    /*
     *
     */
    public boolean isPriorityInterfaceWF() {
        return priorityInterface == PriorityInterface.WF;
    }

    /*
     *
     */
    public boolean isPriorityInterfaceWFD() {
        return priorityInterface == PriorityInterface.WFD;
    }

    /*
     *
     */
    public void setPriorityInterfaceWF() {
        priorityInterface = PriorityInterface.WF;
    }

    /*
     *
     */
    public void clearPriorityInterface() {
        priorityInterface = PriorityInterface.NOTSET;
    }

    /*
     *
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /*
     *
     */
    public String getDeviceName() {
        return this.deviceName;
    }


    /*
     *
     */
    public void setPriorityInterfaceWFD() {
        priorityInterface = PriorityInterface.WFD;
    }

    /*
     *
     */
    public void saveToConfigurationsFile() {
        // unsure path existence
        AndroidUtils.buildPath(MainActivity.APP_MAIN_FILES_DIR_PATH);

        File confFile = new File(MainActivity.APP_MAIN_FILES_DIR_PATH, MainActivity.CONFIG_FILENAME);
        try {
            PrintWriter pw = new PrintWriter(confFile);

            // write device name
            if (deviceName != null)
                pw.println("deviceName = " + deviceName);

            // write priority interface
            if (priorityInterface != PriorityInterface.NOTSET)
                pw.println("priorityInterface = " + priorityInterface);

            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /*
     *
     */
    static public Configurations readFromConfigurationsFile() {
        Configurations cnf = new Configurations();

        File confFile = new File(MainActivity.APP_MAIN_FILES_DIR_PATH, MainActivity.CONFIG_FILENAME);
        try {
            // open scanner reader
            Scanner scan = new Scanner(confFile);

            // process lines
            while (scan.hasNextLine()) {
                // read line
                String line = scan.nextLine();

                //ignore empty lines
                if (line.trim().length() == 0)
                    continue;

                // process line
                Scanner tokens = new Scanner(line);

                // first token is guaranteed
                String firstToken = tokens.next();

                // get second token, must be "="
                if (!tokens.hasNext())
                    throw new IllegalStateException("Configurations file: line badly formed: " + line);
                String secondToken = tokens.next();
                if (!secondToken.equals("="))
                    throw new IllegalStateException("Configurations file: line badly formed, missing '=' in second place: " + line);

                // get third token
                if (!tokens.hasNext())
                    throw new IllegalStateException("Configurations file: line badly formed, no third value: " + line);
                String thirdToken = tokens.next();

                // checking for device name configuration item
                if (firstToken.equalsIgnoreCase("deviceName")) {
                    cnf.setDeviceName(thirdToken);
                }

                // checking for Priority Interface configuration item
                if (firstToken.equalsIgnoreCase("priorityInterface")) {
                    if (thirdToken.equalsIgnoreCase("WFD")) {
                        cnf.setPriorityInterfaceWFD();
                    } else if (thirdToken.equalsIgnoreCase("WF")) {
                        cnf.setPriorityInterfaceWF();
                    } else
                        throw new IllegalStateException("Configurations file: line badly formed: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            Log.d(MainActivity.TAG, "Reading configurations file: file not found: " + MainActivity.CONFIG_FILENAME);
        }

        return cnf;
    }
}
