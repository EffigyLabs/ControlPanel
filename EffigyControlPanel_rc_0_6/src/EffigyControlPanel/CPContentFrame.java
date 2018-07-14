/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EffigyControlPanel;

//import static com.sun.awt.AWTUtilities.*;
import java.awt.GridBagLayout;
import java.awt.MenuBar;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import static java.util.Objects.nonNull;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author jody This frame contains the main panel for the preset generator. The
 * data structures are contained in the Preset, Mode, Position, and Slot
 * classes.
 *
 */
public class CPContentFrame extends javax.swing.JFrame implements DropTargetListener {

    String srcDir; // user.dir root dir from app pov
    String dataDir; // where all dynamic data is stored, the config and the banks
    String fileSeparator; // OS's notion of a file separator, e.g. a slash on Windows
    String extDir = ""; // persisted external load/save dir for convenience
    String lastBank = ""; // last bank loaded persisted
    boolean loadLastBank = false; // flag to load last bank on startup

    public Pedal pedal; // Effigy Control Pedal R4c v14 and higher.  pedal will make it's own pedal bank to store pedal data and communicate with the pedal.
    public List<Bank> bankList; // list of banks found
    public Bank mainBank; // working bank displayed in the UI.  The bank is able to save adn load itself etc.
    boolean autoSync = true; // if the pedal is connected, automatically send all changes to pedal if this is true
    boolean canClose = true; // the master changed-from-ui-flag so a new load, close, or get, will get the chance to confirm before overwriting
    int numericPreset[] = new int[1440]; // 1 fully loaded bank, maximum size of anything
    int numericPresetPtr = 0; // "pointer" for the numeric version of the bank   
    Preset bufferPreset; // this is the behind-the-scene preset that holds the master preset that is read from, written to, send, received, updated, etc.

    // sysex message stuff
    String sysexHeaderTxt = "F0 00 02 21 "; // F0=sysex start, 00 02 21 = MMA vendor ID assigned to Effigy Labs
    String sysexDelimiterTxt = " F7";
    byte[] sysexHeader = {(byte) 0xF7, (byte) 0x00, (byte) 0x02, (byte) 0x21};
    byte[] sysexDelimiter = {(byte) 0xF7};
    boolean isPresetLoaded = false;
    String txtSlotData, arg1, arg2, txtAttr, attributeVal = ""; // internalizing the old ui txt boxes
    int indAttr = 0;
    String txtBuffer;
    boolean pedalQuiet = false; // avoids attribute-by-attribute update to pedal during bank load when autosync is on
    boolean modelQuiet = true; // avoids setting mainbank values to zeroes when the UI controls are initializing
    Property config;
    // 1 bank = 5 presets = 3 modes per preset = 4 positions per mode = 3 slots per position = 8 attributes per slot
    //presetArray = new int[3][4][3][8]; // space for 5 presets all fully loaded just using ints since we are not short on space at this time
    //Preset bufferPreset = new Preset();
    //char[] charBuffer;
    DropTarget dt;

    // constants for slot attributes' names
    int SLOT_COMMAND = 0;
    int SLOT_CHANNEL = 1;
    int SLOT_SUBCOMMAND = 2;
    int SLOT_CURVETYPE = 3;
    int SLOT_CURVEDIRECTION = 4;
    int SLOT_MIN = 5;
    int SLOT_MAX = 6;
    int SLOT_LATCHING = 7;

    /**
     * 
     *  built-in initialization
     * Initialization
     *   Detect&Connect to Pedal
     *   Detect&Load Bank files list
     *   Get/Save pedal bank in hidden bank zero
     *   User sees bank 0 as “Pedal”
     *   User can then sync any other bank or preset to pedal, or save, etc.
     *   Autosync turned off at installation, position is persisted
     *   Autosync sends edits to pedal, not loads
     *   Defense to be tested
     *   Get bank from pedal via button/canClose/txtChg
     */
    public CPContentFrame() {

        super("ShapedWindow");

        setLayout(new GridBagLayout());

        //setUndecorated(false); // menu?? 6/4/2018 **** TAG
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();  // you can't do anything with the controls until this runs!  Put nothing above here that references a UI control.

        dt = new DropTarget(txtBanks, this); // for drag and drop to bank list
        //setMenuBar((MenuBar) jMenuBar1);

        // folder where we are running
        System.out.println("Effigy Control Panel Debug Console v1.0 July 2018");

        // Obtain our relative location and get basic system info
        fileSeparator = System.getProperty("file.separator");
        srcDir = System.getProperty("user.dir") + fileSeparator; // add the file separator for everyone
        System.out.println("srcDir="+srcDir);
        dataDir = srcDir + "data" + fileSeparator; // add the file separator for everyone
        System.out.println("dataDir="+dataDir);
        
        // create the data folder if necessary
        File dataDirFile = new File(dataDir);
        dataDir = srcDir + "data" + fileSeparator;
        File dataFolder = new File(dataDir);
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
            System.out.println("Creating data folder as "+dataDir);
        }

        // get config properties
        config = new Property(dataDir + "config.txt");
        autoSync = config.getBool("autoSync");
        System.out.println("autoSync="+autoSync);

        // load last bank
        loadLastBank = config.getBool("loadLastBank");
        if (loadLastBank) {
            lastBank = config.getString("lastBank");
            System.out.println("will load last bank "+lastBank);
        } 

        extDir = config.getString("extDir");
        txtFileName.setText(extDir);
        System.out.println("extDir="+extDir);

        //instantiate main Bank
        mainBank = new Bank();

        // set autoSync from config
        if (autoSync) {
            chkAutoSync.setSelected(autoSync);
            //chkAutoSync.setEnabled(true);
        } else {
            chkAutoSync.setSelected(false);
            //chkAutoSync.setEnabled(false);
        }
        chkAutoSync.setSelected(autoSync);

        // instantiate the bank list
        bankList = new ArrayList<>();

        // 1. check and make "history" and "data" folders
        // 2. get file list of # of files
        // 3. move pedal bank.ecp if found to history with historical name
        // delete pedal bank file for now since it is temporary, but archive it first in the history folder
        deletePedalBankFile();
        pedal = new Pedal();  // automatically detects & connects to pedal if possible and loads up it's bank into the pedal's Bank object.

        refreshBankList();

        boolean autoConnect = config.getBool("autoConnect");
        
        // refresh the bank the first time to include the pedal bank if it's connected
        pickAndLoadBank();

        // update status in ui
        if(pedal.api.connected) {
            txtStatus.setText("connected");
        } else {
            txtStatus.setText("not connected");        
        }

        // put the saved external file directory in the file name for export and import
        String tFn = config.getString("extDir");
        txtFileName.setText(tFn);

        //highlight the top entry in the bank list

        //now, we have a mainBank populated if there is a pedal connected or if a bank file is found.
        //txtChg.setText("");
        //canClose = true;
    }

    public void pickAndLoadBank() {
        if (pedal.api.connected) {

            try {
                txtSerNum.setText(Integer.toString(pedal.sysblk.serialNumber));// serial num
                String ver = pedal.api.getSwVer();// software version
                txtSwVer.setText(pedal.api.swVersion);
                mainBank = pedal.pedalBank; // copy the pedal bank to the main bank; 
                bankSwitch("Pedal Bank");
                refreshBankList();
                this.setSelectedBankName("Pedal Bank");
            } catch (Exception e) {
                System.err.println("pickAndLoadBank(): error picking and loading bank - pedal may be still disconnected:"+e);
            }
        } else { // pedal is not connected  

            //load a bank if possible
            if (loadLastBank) {
                System.out.println("switching to last bank "+lastBank);
                bankSwitch(lastBank);
            } else {
                System.out.println("loading first bank in list");
                loadFirstBankInList();
            }
        }

    }

    public void loadFirstBankInList() {
        ListModel mod = txtBanks.getModel();
        if (mod.getSize() > 0) {
            String fb = (String) mod.getElementAt(0);
            bankSwitch(fb); // load pedal bank zero.  If pedal isn't connected and loadLastBank isn't set, just load the first bank            
        }
    }

    /**
     * refresh the list of banks in the UI. Get the actual list of bank files in
     * the data directory and load their names as the list.
     */
    public void refreshBankList() {
        System.out.println("refreshBankList()");
        // populate bank list
        DefaultListModel model;
        model = new DefaultListModel();
        Bank workBank = new Bank();
        File[] file = workBank.getBankList(dataDir);

        // go through each file, fake-load it to parse it and get the bank name out of it, and verify the contents
        for (int f = 0; f < file.length; f++) {
            //System.out.println("opening "+file[f].getAbsolutePath());            
            workBank.loadBank(file[f], false); //the false is COMMIT.  a commit of false does not commit the loaded bank to the pedal.  Done as a kind of static method to parse but not update the bank data.
            //bankList.add(workBank.bankName);
            //System.out.println(workBank.bankName);
            if (workBank.bankName.trim().equalsIgnoreCase("pedal bank")) {
                model.insertElementAt("Pedal Bank", 0);
            } else {
                model.addElement(workBank.bankName);
            }
        }
        txtBanks.setModel(model);
        System.out.println("bank list refreshed.");
    }

    public void updateIcon() {
         if (pedal.api.connected) {
            String imageName = "icon_blue16.png";
            ImageIcon icon = new ImageIcon(imageName);
            icon.getImage().flush();
            lblBlueLED.setIcon(icon);
            }
         else { // pedal is not connected  
            // LED status          
            String imageName = "icon_gray16.png";
            ImageIcon icon = new ImageIcon(imageName);
            icon.getImage().flush();
            lblBlueLED.setIcon(icon);
        }
    }

    
    // run this after running a discoverPedal, initialization
    public void pedalConnect() {
        System.out.println("pedalConnect()");
        pedal.api.discoverPedal();

        updateIcon();
        
        if (pedal.api.connected) {
            txtStatus.setText("connected");
            System.out.println("found pedal");
            // get info

            // serial number
            txtSerNum.setText(Integer.toString(pedal.api.sysblk.serialNumber));

            // software version
            String ver = pedal.api.getSwVer();
            txtSwVer.setText(ver);
            mainBank = pedal.pedalBank; // copy the pedal bank to the main bank;  
            bankSwitch("Pedal Bank");
            refreshBankList();
        } else {
            txtStatus.setText("not connected");
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();
        buttonGroupSlot1 = new javax.swing.ButtonGroup();
        buttonGroupSlot2 = new javax.swing.ButtonGroup();
        buttonGroupSlot3 = new javax.swing.ButtonGroup();
        buttonGroupPosition = new javax.swing.ButtonGroup();
        butGrpSelectedSlot = new javax.swing.ButtonGroup();
        jDesktopPane1 = new javax.swing.JDesktopPane();
        butGrpPos1 = new javax.swing.ButtonGroup();
        butGrpPos2 = new javax.swing.ButtonGroup();
        butGrpPos3 = new javax.swing.ButtonGroup();
        butGrpPos4 = new javax.swing.ButtonGroup();
        jLabel7 = new javax.swing.JLabel();
        mainPanel = new javax.swing.JPanel();
        jPanel6 = new TransparentPanel();
        radCurvePosPos1 = new javax.swing.JRadioButton();
        spnMaxRangePos1 = new javax.swing.JSpinner();
        radCurveNegPos1 = new javax.swing.JRadioButton();
        latchingPos1 = new javax.swing.JCheckBox();
        spnMidiChannelPos1 = new javax.swing.JSpinner();
        comMidiSubCommandPos1 = new javax.swing.JComboBox<>();
        comCurveTypePos1 = new javax.swing.JComboBox<>();
        spnMinRangePos1 = new javax.swing.JSpinner();
        comMidiCmdPos1 = new javax.swing.JComboBox<>();
        radPos1Slot1 = new javax.swing.JRadioButton();
        radPos1Slot2 = new javax.swing.JRadioButton();
        radPos1Slot3 = new javax.swing.JRadioButton();
        jLabel15 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        jPanel5 = new TransparentPanel();
        comMidiSubCommandPos2 = new javax.swing.JComboBox<>();
        comCurveTypePos2 = new javax.swing.JComboBox<>();
        radCurvePosPos2 = new javax.swing.JRadioButton();
        spnMidiChannelPos2 = new javax.swing.JSpinner();
        spnMinRangePos2 = new javax.swing.JSpinner();
        latchingPos2 = new javax.swing.JCheckBox();
        spnMaxRangePos2 = new javax.swing.JSpinner();
        comMidiCmdPos2 = new javax.swing.JComboBox<>();
        radCurveNegPos2 = new javax.swing.JRadioButton();
        radPos2Slot1 = new javax.swing.JRadioButton();
        radPos2Slot2 = new javax.swing.JRadioButton();
        radPos2Slot3 = new javax.swing.JRadioButton();
        jLabel20 = new javax.swing.JLabel();
        jLabel59 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        jLabel64 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        txtStatus = new javax.swing.JLabel();
        butRefresh1 = new javax.swing.JButton();
        jPanel8 = new TransparentPanel();
        spnMinRangePos4 = new javax.swing.JSpinner();
        comMidiSubCommandPos4 = new javax.swing.JComboBox<>();
        radCurveNegPos4 = new javax.swing.JRadioButton();
        radCurvePosPos4 = new javax.swing.JRadioButton();
        spnMaxRangePos4 = new javax.swing.JSpinner();
        comMidiCmdPos4 = new javax.swing.JComboBox<>();
        spnMidiChannelPos4 = new javax.swing.JSpinner();
        latchingPos4 = new javax.swing.JCheckBox();
        comCurveTypePos4 = new javax.swing.JComboBox<>();
        radPos4Slot3 = new javax.swing.JRadioButton();
        radPos4Slot2 = new javax.swing.JRadioButton();
        radPos4Slot1 = new javax.swing.JRadioButton();
        jLabel24 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jPanel4 = new TransparentPanel();
        spnMinRangePos3 = new javax.swing.JSpinner();
        comMidiSubCommandPos3 = new javax.swing.JComboBox<>();
        radCurveNegPos3 = new javax.swing.JRadioButton();
        radCurvePosPos3 = new javax.swing.JRadioButton();
        spnMaxRangePos3 = new javax.swing.JSpinner();
        comMidiCmdPos3 = new javax.swing.JComboBox<>();
        spnMidiChannelPos3 = new javax.swing.JSpinner();
        latchingPos3 = new javax.swing.JCheckBox();
        comCurveTypePos3 = new javax.swing.JComboBox<>();
        radPos3Slot1 = new javax.swing.JRadioButton();
        radPos3Slot2 = new javax.swing.JRadioButton();
        radPos3Slot3 = new javax.swing.JRadioButton();
        jLabel21 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lblBlueLED = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        txtActionStatus = new javax.swing.JLabel();
        butApiSend = new javax.swing.JButton();
        chkAutoSync = new javax.swing.JCheckBox();
        butApiSend1 = new javax.swing.JButton();
        jLabel26 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        butAddBank = new javax.swing.JButton();
        butDeleteBank = new javax.swing.JButton();
        butEditBank = new javax.swing.JButton();
        butLoad = new javax.swing.JButton();
        butSave = new javax.swing.JButton();
        txtFileName = new javax.swing.JTextField();
        butChooseFile = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtBanks = new javax.swing.JList<>();
        txtChg = new javax.swing.JLabel();
        lblBankName = new javax.swing.JLabel();
        butRefreshBankList = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        txtSerNum = new javax.swing.JLabel();
        txtSwVer = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        comMode = new javax.swing.JSpinner();
        comPreset = new javax.swing.JSpinner();
        jPanel1 = new javax.swing.JPanel();
        sl00 = new javax.swing.JLabel();
        sl01 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        sl02 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        sl12 = new javax.swing.JLabel();
        sl11 = new javax.swing.JLabel();
        sl10 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        sl22 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        sl20 = new javax.swing.JLabel();
        sl21 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        sl32 = new javax.swing.JLabel();
        sl30 = new javax.swing.JLabel();
        sl31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();

        javax.swing.GroupLayout jDesktopPane1Layout = new javax.swing.GroupLayout(jDesktopPane1);
        jDesktopPane1.setLayout(jDesktopPane1Layout);
        jDesktopPane1Layout.setHorizontalGroup(
            jDesktopPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jDesktopPane1Layout.setVerticalGroup(
            jDesktopPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Effigy Labs Control Pedal Control Panel");
        setBackground(new java.awt.Color(0, 0, 0));
        setLocation(new java.awt.Point(200, 100));
        setName("CPContentFrame"); // NOI18N
        setSize(new java.awt.Dimension(865, 794));

        mainPanel.setBackground(new java.awt.Color(51, 51, 51));
        mainPanel.setName("MainPanel"); // NOI18N
        mainPanel.setOpaque(false);
        mainPanel.setPreferredSize(new java.awt.Dimension(865, 794));

        jPanel6.setBackground(new java.awt.Color(0, 0, 0, 0.45f));
        jPanel6.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jPanel6.setToolTipText("Slots for Position 1");

        buttonGroupSlot1.add(radCurvePosPos1);
        radCurvePosPos1.setText("+");
        radCurvePosPos1.setToolTipText("Curve Direction: Positive selector for this slot/position");
        radCurvePosPos1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radCurvePosPos1ActionPerformed(evt);
            }
        });
        radCurvePosPos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                radCurvePosPos1PropertyChange(evt);
            }
        });

        spnMaxRangePos1.setModel(new javax.swing.SpinnerNumberModel(100, 1, 100, 1));
        spnMaxRangePos1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMaxRangePos1StateChanged(evt);
            }
        });
        spnMaxRangePos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMaxRangePos1PropertyChange(evt);
            }
        });

        buttonGroupSlot1.add(radCurveNegPos1);
        radCurveNegPos1.setText("-");
        radCurveNegPos1.setToolTipText("Curve Direction: Negative selector for this slot/position");
        radCurveNegPos1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radCurveNegPos1ActionPerformed(evt);
            }
        });
        radCurveNegPos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                radCurveNegPos1PropertyChange(evt);
            }
        });

        latchingPos1.setToolTipText("Latching selector for this slot/position");
        latchingPos1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                latchingPos1ActionPerformed(evt);
            }
        });
        latchingPos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                latchingPos1PropertyChange(evt);
            }
        });

        spnMidiChannelPos1.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        spnMidiChannelPos1.setToolTipText("MIDI Channel selector for this slot");
        spnMidiChannelPos1.setFocusCycleRoot(true);
        spnMidiChannelPos1.setFocusTraversalPolicyProvider(true);
        spnMidiChannelPos1.setName("spnPos1MidiChanel"); // NOI18N
        spnMidiChannelPos1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMidiChannelPos1StateChanged(evt);
            }
        });
        spnMidiChannelPos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMidiChannelPos1PropertyChange(evt);
            }
        });

        comMidiSubCommandPos1.setToolTipText("MIDI Sub Command Selector for this slot/position");
        comMidiSubCommandPos1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comMidiSubCommandPos1ActionPerformed(evt);
            }
        });
        comMidiSubCommandPos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comMidiSubCommandPos1PropertyChange(evt);
            }
        });

        comCurveTypePos1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Linear", "Audio", "Log", "Off-On", " " }));
        comCurveTypePos1.setToolTipText("Curve Type for this slot/position");
        comCurveTypePos1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comCurveTypePos1ActionPerformed(evt);
            }
        });
        comCurveTypePos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comCurveTypePos1PropertyChange(evt);
            }
        });

        spnMinRangePos1.setModel(new javax.swing.SpinnerNumberModel(0, 0, 99, 1));
        spnMinRangePos1.setToolTipText("Min % selector for this slot/position");
        spnMinRangePos1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMinRangePos1StateChanged(evt);
            }
        });
        spnMinRangePos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMinRangePos1PropertyChange(evt);
            }
        });

        comMidiCmdPos1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0 - Empty", "8 - Note Off", "9 - Note On", "A - Aftertouch (poly)", "B - Control Chg", "C - Program Chg", "D - Aftertouch (chn)", "E - Pitch Bend" }));
        comMidiCmdPos1.setToolTipText("MIDI Command Selector for this slot/position");
        comMidiCmdPos1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comMidiCmdPos1ItemStateChanged(evt);
            }
        });
        comMidiCmdPos1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comMidiCmdPos1ActionPerformed(evt);
            }
        });
        comMidiCmdPos1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comMidiCmdPos1PropertyChange(evt);
            }
        });

        butGrpPos1.add(radPos1Slot1);
        radPos1Slot1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos1Slot1.setSelected(true);
        radPos1Slot1.setText("Slot");
        radPos1Slot1.setToolTipText("Slot 1 selector for this position");
        radPos1Slot1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos1Slot1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos1Slot1ActionPerformed(evt);
            }
        });

        butGrpPos1.add(radPos1Slot2);
        radPos1Slot2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos1Slot2.setToolTipText("Slot 2 selector for this position");
        radPos1Slot2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos1Slot2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos1Slot2ActionPerformed(evt);
            }
        });

        butGrpPos1.add(radPos1Slot3);
        radPos1Slot3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos1Slot3.setToolTipText("Slot 3 selector for this position");
        radPos1Slot3.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos1Slot3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos1Slot3ActionPerformed(evt);
            }
        });

        jLabel15.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("Position 1 Slots");

        jLabel52.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel52.setForeground(new java.awt.Color(255, 255, 255));
        jLabel52.setText("MAX");

        jLabel53.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel53.setForeground(new java.awt.Color(255, 255, 255));
        jLabel53.setText("LCH");

        jLabel54.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel54.setForeground(new java.awt.Color(255, 255, 255));
        jLabel54.setText("CRV");

        jLabel55.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel55.setForeground(new java.awt.Color(255, 255, 255));
        jLabel55.setText("MIN");

        jLabel56.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel56.setForeground(new java.awt.Color(255, 255, 255));
        jLabel56.setText("SUB");

        jLabel57.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel57.setForeground(new java.awt.Color(255, 255, 255));
        jLabel57.setText("CMD");

        jLabel58.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel58.setForeground(new java.awt.Color(255, 255, 255));
        jLabel58.setText("CHN");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel58)
                            .addComponent(jLabel57)
                            .addComponent(jLabel56)
                            .addComponent(jLabel55)
                            .addComponent(jLabel54))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(comMidiCmdPos1, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comMidiSubCommandPos1, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(spnMinRangePos1, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel52)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnMaxRangePos1, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(comCurveTypePos1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radCurvePosPos1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radCurveNegPos1))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(spnMidiChannelPos1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radPos1Slot1)
                                .addGap(1, 1, 1)
                                .addComponent(radPos1Slot2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radPos1Slot3))
                            .addComponent(jLabel15))
                        .addGap(304, 304, 304))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel53)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(latchingPos1)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spnMidiChannelPos1)
                            .addComponent(jLabel58)
                            .addComponent(radPos1Slot1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(5, 5, 5))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(radPos1Slot3)
                            .addComponent(radPos1Slot2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel57))
                    .addComponent(comMidiCmdPos1, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE))
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel56)
                        .addGap(3, 3, 3))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comMidiSubCommandPos1, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spnMinRangePos1)
                    .addComponent(jLabel55)
                    .addComponent(jLabel52)
                    .addComponent(spnMaxRangePos1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radCurveNegPos1)
                    .addComponent(radCurvePosPos1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comCurveTypePos1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel54))
                .addGap(1, 1, 1)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel53)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(latchingPos1, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel5.setBackground(new java.awt.Color(0, 0, 0, 0.45f));
        jPanel5.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jPanel5.setToolTipText("Slots for Position 2");

        comMidiSubCommandPos2.setToolTipText("MIDI Sub Command Selector for this slot/position");
        comMidiSubCommandPos2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comMidiSubCommandPos2ActionPerformed(evt);
            }
        });
        comMidiSubCommandPos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comMidiSubCommandPos2PropertyChange(evt);
            }
        });

        comCurveTypePos2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Linear", "Audio", "Log", "Off-On", " " }));
        comCurveTypePos2.setToolTipText("Curve Type for this slot/position");
        comCurveTypePos2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comCurveTypePos2ActionPerformed(evt);
            }
        });
        comCurveTypePos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comCurveTypePos2PropertyChange(evt);
            }
        });

        buttonGroupSlot2.add(radCurvePosPos2);
        radCurvePosPos2.setText("+");
        radCurvePosPos2.setToolTipText("Curve Direction: Positive selector for this slot/position");
        radCurvePosPos2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radCurvePosPos2ActionPerformed(evt);
            }
        });
        radCurvePosPos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                radCurvePosPos2PropertyChange(evt);
            }
        });

        spnMidiChannelPos2.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        spnMidiChannelPos2.setToolTipText("MIDI Channel selector for this slot");
        spnMidiChannelPos2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMidiChannelPos2StateChanged(evt);
            }
        });
        spnMidiChannelPos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMidiChannelPos2PropertyChange(evt);
            }
        });

        spnMinRangePos2.setModel(new javax.swing.SpinnerNumberModel(0, 0, 99, 1));
        spnMinRangePos2.setToolTipText("Min % selector for this slot/position");
        spnMinRangePos2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMinRangePos2StateChanged(evt);
            }
        });
        spnMinRangePos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMinRangePos2PropertyChange(evt);
            }
        });

        latchingPos2.setToolTipText("Latching selector for this slot/position");
        latchingPos2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                latchingPos2StateChanged(evt);
            }
        });
        latchingPos2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                latchingPos2ActionPerformed(evt);
            }
        });
        latchingPos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                latchingPos2PropertyChange(evt);
            }
        });

        spnMaxRangePos2.setModel(new javax.swing.SpinnerNumberModel(100, 1, 100, 1));
        spnMaxRangePos2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMaxRangePos2StateChanged(evt);
            }
        });
        spnMaxRangePos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMaxRangePos2PropertyChange(evt);
            }
        });

        comMidiCmdPos2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0 - Empty", "8 - Note Off", "9 - Note On", "A - Aftertouch (polyphonic)", "B - Control Change", "C - Program Change", "D - Aftertouch (channel)", "E - Pitch Bend" }));
        comMidiCmdPos2.setToolTipText("MIDI Command Selector for this slot/position");
        comMidiCmdPos2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comMidiCmdPos2ActionPerformed(evt);
            }
        });
        comMidiCmdPos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comMidiCmdPos2PropertyChange(evt);
            }
        });

        buttonGroupSlot2.add(radCurveNegPos2);
        radCurveNegPos2.setText("-");
        radCurveNegPos2.setToolTipText("Curve Direction: Negative selector for this slot/position");
        radCurveNegPos2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radCurveNegPos2ActionPerformed(evt);
            }
        });
        radCurveNegPos2.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                radCurveNegPos2PropertyChange(evt);
            }
        });

        butGrpPos2.add(radPos2Slot1);
        radPos2Slot1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos2Slot1.setSelected(true);
        radPos2Slot1.setText("Slot");
        radPos2Slot1.setToolTipText("Slot 1 selector for this position");
        radPos2Slot1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos2Slot1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos2Slot1ActionPerformed(evt);
            }
        });

        butGrpPos2.add(radPos2Slot2);
        radPos2Slot2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos2Slot2.setToolTipText("Slot 2 selector for this position");
        radPos2Slot2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos2Slot2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos2Slot2ActionPerformed(evt);
            }
        });

        butGrpPos2.add(radPos2Slot3);
        radPos2Slot3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos2Slot3.setToolTipText("Slot 3 selector for this position");
        radPos2Slot3.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos2Slot3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos2Slot3ActionPerformed(evt);
            }
        });

        jLabel20.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(255, 255, 255));
        jLabel20.setText("Position 2 Slots");

        jLabel59.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel59.setForeground(new java.awt.Color(255, 255, 255));
        jLabel59.setText("MAX");

        jLabel60.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel60.setForeground(new java.awt.Color(255, 255, 255));
        jLabel60.setText("LCH");

        jLabel61.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel61.setForeground(new java.awt.Color(255, 255, 255));
        jLabel61.setText("CRV");

        jLabel62.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel62.setForeground(new java.awt.Color(255, 255, 255));
        jLabel62.setText("MIN");

        jLabel63.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel63.setForeground(new java.awt.Color(255, 255, 255));
        jLabel63.setText("SUB");

        jLabel64.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel64.setForeground(new java.awt.Color(255, 255, 255));
        jLabel64.setText("CMD");

        jLabel65.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel65.setForeground(new java.awt.Color(255, 255, 255));
        jLabel65.setText("CHN");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel65)
                            .addComponent(jLabel64)
                            .addComponent(jLabel63)
                            .addComponent(jLabel62)
                            .addComponent(jLabel61))
                        .addGap(5, 5, 5))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel60)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(comMidiSubCommandPos2, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(comMidiCmdPos2, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(spnMinRangePos2, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel59)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spnMaxRangePos2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(comCurveTypePos2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(latchingPos2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radCurvePosPos2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radCurveNegPos2))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(jLabel20))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(spnMidiChannelPos2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(radPos2Slot1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radPos2Slot2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radPos2Slot3)))
                .addGap(138, 138, 138))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(32, 32, 32)
                                .addComponent(jLabel65))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel20)
                                .addGap(10, 10, 10)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(radPos2Slot3, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(radPos2Slot2, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(radPos2Slot1)
                                        .addComponent(spnMidiChannelPos2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel64)
                            .addComponent(comMidiCmdPos2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel63)
                            .addComponent(comMidiSubCommandPos2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel62)
                            .addComponent(spnMinRangePos2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel59)
                            .addComponent(spnMaxRangePos2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel61)
                            .addComponent(comCurveTypePos2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(radCurvePosPos2)
                            .addComponent(radCurveNegPos2))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel60)
                            .addComponent(latchingPos2))))
                .addGap(8, 8, 8))
        );

        txtStatus.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtStatus.setText("Not Connected");
        txtStatus.setToolTipText("Pedal Status text");

        butRefresh1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/refresh-tiny.gif"))); // NOI18N
        butRefresh1.setToolTipText("Reconnect to Pedal");
        butRefresh1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRefresh1ActionPerformed(evt);
            }
        });

        jPanel8.setBackground(new java.awt.Color(0, 0, 0, 0.45f));
        jPanel8.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jPanel8.setToolTipText("Slots for Knob/Position 4");

        spnMinRangePos4.setModel(new javax.swing.SpinnerNumberModel(0, 0, 99, 1));
        spnMinRangePos4.setToolTipText("Min % selector for this slot/position");
        spnMinRangePos4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMinRangePos4StateChanged(evt);
            }
        });
        spnMinRangePos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMinRangePos4PropertyChange(evt);
            }
        });

        comMidiSubCommandPos4.setToolTipText("MIDI Sub Command Selector for this slot/position");
        comMidiSubCommandPos4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comMidiSubCommandPos4ActionPerformed(evt);
            }
        });
        comMidiSubCommandPos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comMidiSubCommandPos4PropertyChange(evt);
            }
        });

        buttonGroupSlot3.add(radCurveNegPos4);
        radCurveNegPos4.setText("-");
        radCurveNegPos4.setToolTipText("Curve Direction: Negative selector for this slot/position");
        radCurveNegPos4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radCurveNegPos4ActionPerformed(evt);
            }
        });
        radCurveNegPos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                radCurveNegPos4PropertyChange(evt);
            }
        });

        buttonGroupSlot3.add(radCurvePosPos4);
        radCurvePosPos4.setText("+");
        radCurvePosPos4.setToolTipText("Curve Direction: Positive selector for this slot/position");
        radCurvePosPos4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radCurvePosPos4ActionPerformed(evt);
            }
        });
        radCurvePosPos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                radCurvePosPos4PropertyChange(evt);
            }
        });

        spnMaxRangePos4.setModel(new javax.swing.SpinnerNumberModel(100, 1, 100, 1));
        spnMaxRangePos4.setToolTipText("Max % selector for this slot/position");
        spnMaxRangePos4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMaxRangePos4StateChanged(evt);
            }
        });
        spnMaxRangePos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMaxRangePos4PropertyChange(evt);
            }
        });

        comMidiCmdPos4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0 - Empty", "8 - Note Off", "9 - Note On", "A - Aftertouch (polyphonic)", "B - Control Change", "C - Program Change", "D - Aftertouch (channel)", "E - Pitch Bend" }));
        comMidiCmdPos4.setToolTipText("MIDI Command Selector for this slot/position");
        comMidiCmdPos4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comMidiCmdPos4ActionPerformed(evt);
            }
        });
        comMidiCmdPos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comMidiCmdPos4PropertyChange(evt);
            }
        });

        spnMidiChannelPos4.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        spnMidiChannelPos4.setToolTipText("MIDI Channel selector for this slot");
        spnMidiChannelPos4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMidiChannelPos4StateChanged(evt);
            }
        });
        spnMidiChannelPos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMidiChannelPos4PropertyChange(evt);
            }
        });

        latchingPos4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                latchingPos4StateChanged(evt);
            }
        });
        latchingPos4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                latchingPos4ActionPerformed(evt);
            }
        });
        latchingPos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                latchingPos4PropertyChange(evt);
            }
        });

        comCurveTypePos4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Linear", "Audio", "Log", "Off-On", " " }));
        comCurveTypePos4.setToolTipText("Curve Type for this slot/position");
        comCurveTypePos4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comCurveTypePos4ActionPerformed(evt);
            }
        });
        comCurveTypePos4.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comCurveTypePos4PropertyChange(evt);
            }
        });

        butGrpPos4.add(radPos4Slot3);
        radPos4Slot3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos4Slot3.setToolTipText("Slot 3 selector for this position");
        radPos4Slot3.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos4Slot3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos4Slot3ActionPerformed(evt);
            }
        });

        butGrpPos4.add(radPos4Slot2);
        radPos4Slot2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos4Slot2.setToolTipText("Slot 2 selector for this position");
        radPos4Slot2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos4Slot2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos4Slot2ActionPerformed(evt);
            }
        });

        butGrpPos4.add(radPos4Slot1);
        radPos4Slot1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos4Slot1.setSelected(true);
        radPos4Slot1.setText("SLOT");
        radPos4Slot1.setToolTipText("Slot 1 selector for this position");
        radPos4Slot1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos4Slot1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos4Slot1ActionPerformed(evt);
            }
        });

        jLabel24.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setText("Knob/Position 4 Slots");

        jLabel38.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel38.setForeground(new java.awt.Color(255, 255, 255));
        jLabel38.setText("CHN");

        jLabel39.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel39.setForeground(new java.awt.Color(255, 255, 255));
        jLabel39.setText("CMD");

        jLabel40.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel40.setForeground(new java.awt.Color(255, 255, 255));
        jLabel40.setText("SUB");

        jLabel41.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel41.setForeground(new java.awt.Color(255, 255, 255));
        jLabel41.setText("MIN");

        jLabel42.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel42.setForeground(new java.awt.Color(255, 255, 255));
        jLabel42.setText("CRV");

        jLabel43.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel43.setForeground(new java.awt.Color(255, 255, 255));
        jLabel43.setText("MAX");

        jLabel44.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel44.setForeground(new java.awt.Color(255, 255, 255));
        jLabel44.setText("LCH");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(0, 5, Short.MAX_VALUE)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel38)
                                    .addComponent(jLabel42)
                                    .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jLabel41)
                                        .addComponent(jLabel40))
                                    .addComponent(jLabel44))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel39)))
                        .addGap(10, 10, 10)
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addComponent(comCurveTypePos4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radCurvePosPos4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radCurveNegPos4))
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addComponent(spnMinRangePos4, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel43)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnMaxRangePos4, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(comMidiSubCommandPos4, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(comMidiCmdPos4, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel8Layout.createSequentialGroup()
                                    .addComponent(spnMidiChannelPos4, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(radPos4Slot1)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(radPos4Slot2)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(radPos4Slot3)))
                            .addComponent(latchingPos4))
                        .addGap(8, 8, 8))))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addComponent(jLabel24)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel38, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(spnMidiChannelPos4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(radPos4Slot1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(radPos4Slot2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(radPos4Slot3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel39)
                    .addComponent(comMidiCmdPos4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comMidiSubCommandPos4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel40))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spnMinRangePos4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel43)
                    .addComponent(spnMaxRangePos4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel41))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comCurveTypePos4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel42)
                    .addComponent(radCurvePosPos4)
                    .addComponent(radCurveNegPos4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel44)
                    .addComponent(latchingPos4))
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(0, 0, 0, 0.45f));
        jPanel4.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jPanel4.setToolTipText("Slots for Position 3");

        spnMinRangePos3.setModel(new javax.swing.SpinnerNumberModel(0, 0, 99, 1));
        spnMinRangePos3.setToolTipText("Min % selector for this slot/position");
        spnMinRangePos3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMinRangePos3StateChanged(evt);
            }
        });
        spnMinRangePos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMinRangePos3PropertyChange(evt);
            }
        });

        comMidiSubCommandPos3.setToolTipText("MIDI Sub Command Selector for this slot/position");
        comMidiSubCommandPos3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comMidiSubCommandPos3ActionPerformed(evt);
            }
        });
        comMidiSubCommandPos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comMidiSubCommandPos3PropertyChange(evt);
            }
        });

        buttonGroupSlot3.add(radCurveNegPos3);
        radCurveNegPos3.setText("-");
        radCurveNegPos3.setToolTipText("Curve Direction: Negative selector for this slot/position");
        radCurveNegPos3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radCurveNegPos3ActionPerformed(evt);
            }
        });
        radCurveNegPos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                radCurveNegPos3PropertyChange(evt);
            }
        });

        buttonGroupSlot3.add(radCurvePosPos3);
        radCurvePosPos3.setText("+");
        radCurvePosPos3.setToolTipText("Curve Direction: Positive selector for this slot/position");
        radCurvePosPos3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radCurvePosPos3ActionPerformed(evt);
            }
        });
        radCurvePosPos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                radCurvePosPos3PropertyChange(evt);
            }
        });

        spnMaxRangePos3.setModel(new javax.swing.SpinnerNumberModel(100, 1, 100, 1));
        spnMaxRangePos3.setToolTipText("Max % selector for this slot/position");
        spnMaxRangePos3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMaxRangePos3StateChanged(evt);
            }
        });
        spnMaxRangePos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMaxRangePos3PropertyChange(evt);
            }
        });

        comMidiCmdPos3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0 - Empty", "8 - Note Off", "9 - Note On", "A - Aftertouch (polyphonic)", "B - Control Change", "C - Program Change", "D - Aftertouch (channel)", "E - Pitch Bend" }));
        comMidiCmdPos3.setToolTipText("MIDI Command Selector for this slot/position");
        comMidiCmdPos3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comMidiCmdPos3ActionPerformed(evt);
            }
        });
        comMidiCmdPos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comMidiCmdPos3PropertyChange(evt);
            }
        });

        spnMidiChannelPos3.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        spnMidiChannelPos3.setToolTipText("MIDI Channel selector for this slot");
        spnMidiChannelPos3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMidiChannelPos3StateChanged(evt);
            }
        });
        spnMidiChannelPos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                spnMidiChannelPos3PropertyChange(evt);
            }
        });

        latchingPos3.setToolTipText("Latching selector for this slot/position");
        latchingPos3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                latchingPos3StateChanged(evt);
            }
        });
        latchingPos3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                latchingPos3ActionPerformed(evt);
            }
        });
        latchingPos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                latchingPos3PropertyChange(evt);
            }
        });

        comCurveTypePos3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Linear", "Audio", "Log", "Off-On", " " }));
        comCurveTypePos3.setToolTipText("Curve Type for this slot/position");
        comCurveTypePos3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comCurveTypePos3ActionPerformed(evt);
            }
        });
        comCurveTypePos3.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                comCurveTypePos3PropertyChange(evt);
            }
        });

        butGrpPos3.add(radPos3Slot1);
        radPos3Slot1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos3Slot1.setSelected(true);
        radPos3Slot1.setText("Slot");
        radPos3Slot1.setToolTipText("Slot 1 selector for this position");
        radPos3Slot1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos3Slot1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos3Slot1ActionPerformed(evt);
            }
        });

        butGrpPos3.add(radPos3Slot2);
        radPos3Slot2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos3Slot2.setToolTipText("Slot 2 selector for this position");
        radPos3Slot2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos3Slot2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos3Slot2ActionPerformed(evt);
            }
        });

        butGrpPos3.add(radPos3Slot3);
        radPos3Slot3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        radPos3Slot3.setToolTipText("Slot 3 selector for this position");
        radPos3Slot3.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        radPos3Slot3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPos3Slot3ActionPerformed(evt);
            }
        });

        jLabel21.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(255, 255, 255));
        jLabel21.setText("Position 3 Slots");

        jLabel45.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel45.setForeground(new java.awt.Color(255, 255, 255));
        jLabel45.setText("LCH");

        jLabel46.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel46.setForeground(new java.awt.Color(255, 255, 255));
        jLabel46.setText("CRV");

        jLabel47.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel47.setForeground(new java.awt.Color(255, 255, 255));
        jLabel47.setText("MIN");

        jLabel48.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel48.setForeground(new java.awt.Color(255, 255, 255));
        jLabel48.setText("SUB");

        jLabel49.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel49.setForeground(new java.awt.Color(255, 255, 255));
        jLabel49.setText("CMD");

        jLabel50.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel50.setForeground(new java.awt.Color(255, 255, 255));
        jLabel50.setText("CHN");

        jLabel51.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel51.setForeground(new java.awt.Color(255, 255, 255));
        jLabel51.setText("MAX");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel50)
                    .addComponent(jLabel49)
                    .addComponent(jLabel48)
                    .addComponent(jLabel47)
                    .addComponent(jLabel46))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(spnMinRangePos3, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jLabel51)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(spnMaxRangePos3, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(comMidiSubCommandPos3, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(comMidiCmdPos3, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(comCurveTypePos3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(radCurvePosPos3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radCurveNegPos3))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(spnMidiChannelPos3, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(radPos3Slot1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radPos3Slot2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radPos3Slot3)))
                .addGap(0, 9, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel45)
                        .addGap(18, 18, 18)
                        .addComponent(latchingPos3))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(38, 38, 38)
                        .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(radPos3Slot1)
                        .addComponent(radPos3Slot2, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(radPos3Slot3, javax.swing.GroupLayout.Alignment.TRAILING))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addComponent(jLabel50))
                    .addComponent(spnMidiChannelPos3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel49)
                    .addComponent(comMidiCmdPos3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comMidiSubCommandPos3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel48))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel51)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(spnMinRangePos3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(spnMaxRangePos3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel47)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comCurveTypePos3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(radCurvePosPos3)
                    .addComponent(radCurveNegPos3)
                    .addComponent(jLabel46))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel45)
                    .addComponent(latchingPos3))
                .addGap(52, 52, 52))
        );

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel2.setText("Preset");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel3.setText("Mode");

        lblBlueLED.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_gray16.png"))); // NOI18N

        jLabel30.setBackground(new java.awt.Color(0, 0, 0));
        jLabel30.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel30.setText("Action:");

        txtActionStatus.setBackground(new java.awt.Color(0, 0, 0));
        txtActionStatus.setText("no action performed");

        butApiSend.setBackground(new java.awt.Color(204, 0, 0));
        butApiSend.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        butApiSend.setForeground(new java.awt.Color(255, 255, 255));
        butApiSend.setText("Sync");
        butApiSend.setToolTipText("Sync data to pedal");
        butApiSend.setEnabled(false);
        butApiSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butApiSendActionPerformed(evt);
            }
        });

        chkAutoSync.setBackground(new java.awt.Color(204, 0, 0));
        chkAutoSync.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        chkAutoSync.setForeground(new java.awt.Color(255, 255, 255));
        chkAutoSync.setSelected(true);
        chkAutoSync.setText("Auto");
        chkAutoSync.setToolTipText("toggle Pedal auto-update, when checked, any UI changes immediately updates in the pedal");
        chkAutoSync.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkAutoSyncActionPerformed(evt);
            }
        });

        butApiSend1.setBackground(new java.awt.Color(220, 0, 0));
        butApiSend1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        butApiSend1.setForeground(new java.awt.Color(255, 255, 255));
        butApiSend1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/sprocket8-32-red.png"))); // NOI18N
        butApiSend1.setToolTipText("Open Tools and Settings Window");
        butApiSend1.setBorder(null);
        butApiSend1.setMaximumSize(new java.awt.Dimension(32, 32));
        butApiSend1.setMinimumSize(new java.awt.Dimension(32, 32));
        butApiSend1.setOpaque(false);
        butApiSend1.setPreferredSize(new java.awt.Dimension(32, 32));
        butApiSend1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butApiSend1ActionPerformed(evt);
            }
        });

        jLabel26.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel26.setText("Pedal Sync");

        jLabel28.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel28.setText("Tools+Settings");

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel8.setText("SLOT  ADDRESS");

        butAddBank.setFont(new java.awt.Font("Tahoma", 1, 36)); // NOI18N
        butAddBank.setForeground(new java.awt.Color(0, 180, 0));
        butAddBank.setText("+");
        butAddBank.setToolTipText("New Bank");
        butAddBank.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butAddBankActionPerformed(evt);
            }
        });

        butDeleteBank.setFont(new java.awt.Font("Tahoma", 1, 36)); // NOI18N
        butDeleteBank.setForeground(new java.awt.Color(220, 0, 0));
        butDeleteBank.setText("X");
        butDeleteBank.setToolTipText("Delete Bank");
        butDeleteBank.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butDeleteBankActionPerformed(evt);
            }
        });

        butEditBank.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        butEditBank.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/edit_icon2.png"))); // NOI18N
        butEditBank.setToolTipText("Edit Bank");
        butEditBank.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butEditBankActionPerformed(evt);
            }
        });

        butLoad.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        butLoad.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_import.png"))); // NOI18N
        butLoad.setText("Import");
        butLoad.setToolTipText("Import External Bank Data");
        butLoad.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        butLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butLoadActionPerformed(evt);
            }
        });

        butSave.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        butSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_export.png"))); // NOI18N
        butSave.setText("Copy");
        butSave.setToolTipText("Save Bank as External File");
        butSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSaveActionPerformed(evt);
            }
        });

        txtFileName.setText("Exported_Bank1.ecp");
        txtFileName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFileNameActionPerformed(evt);
            }
        });

        butChooseFile.setText("...");
        butChooseFile.setToolTipText("File Name Chooser");
        butChooseFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butChooseFileActionPerformed(evt);
            }
        });

        txtBanks.setBackground(new java.awt.Color(51, 51, 51));
        txtBanks.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 0), 1, true));
        txtBanks.setFont(new java.awt.Font("Tahoma", 1, 16)); // NOI18N
        txtBanks.setForeground(new java.awt.Color(255, 255, 255));
        txtBanks.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        txtBanks.setToolTipText("");
        txtBanks.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        txtBanks.setDragEnabled(true);
        txtBanks.setMinimumSize(new java.awt.Dimension(80, 0));
        txtBanks.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                txtBanksMouseDragged(evt);
            }
        });
        txtBanks.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtBanksMouseClicked(evt);
            }
        });
        txtBanks.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                txtBanksValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(txtBanks);

        txtChg.setFont(new java.awt.Font("Tahoma", 1, 36)); // NOI18N
        txtChg.setForeground(new java.awt.Color(220, 0, 0));
        txtChg.setText("*");
        txtChg.setToolTipText("Unsaved Changes Exist Now");
        txtChg.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                txtChgPropertyChange(evt);
            }
        });

        lblBankName.setBackground(new java.awt.Color(51, 51, 51));
        lblBankName.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lblBankName.setForeground(new java.awt.Color(255, 255, 255));
        lblBankName.setText("None");
        lblBankName.setToolTipText("Current Bank Name");
        lblBankName.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 0), 1, true));
        lblBankName.setOpaque(true);

        butRefreshBankList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_refresh32.png"))); // NOI18N
        butRefreshBankList.setToolTipText("Refresh Bank List");
        butRefreshBankList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRefreshBankListActionPerformed(evt);
            }
        });

        jLabel5.setText("S/N:");
        jLabel5.setToolTipText("Serial Number of connected pedal");

        txtSerNum.setText("-");

        txtSwVer.setText("-");

        jLabel6.setText("SW:");
        jLabel6.setToolTipText("Software version in connected pedal");

        jLabel27.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel27.setText("Compare");
        jLabel27.setToolTipText("Unsaved Changes Indicator");

        comMode.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        comMode.setModel(new javax.swing.SpinnerNumberModel(1, 1, 3, 1));
        comMode.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                comModeStateChanged(evt);
            }
        });

        comPreset.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        comPreset.setModel(new javax.swing.SpinnerNumberModel(0, 0, 4, 1));
        comPreset.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                comPresetStateChanged(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(51, 51, 51));
        jPanel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        jPanel1.setToolTipText("Currently shown slot in this position");

        sl00.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl00.setForeground(new java.awt.Color(255, 255, 255));
        sl00.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl00.setText("1");

        sl01.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl01.setForeground(new java.awt.Color(255, 255, 255));
        sl01.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl01.setText("2");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("Slot");

        sl02.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl02.setForeground(new java.awt.Color(255, 255, 255));
        sl02.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl02.setText("3");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sl00)
                    .addComponent(sl01)
                    .addComponent(sl02)
                    .addComponent(jLabel9))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl00)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl01)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl02)
                .addGap(13, 13, 13))
        );

        jPanel2.setBackground(new java.awt.Color(51, 51, 51));
        jPanel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        jPanel2.setToolTipText("Currently shown slot in this position");

        sl12.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl12.setForeground(new java.awt.Color(255, 255, 255));
        sl12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl12.setText("3");

        sl11.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl11.setForeground(new java.awt.Color(255, 255, 255));
        sl11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl11.setText("2");

        sl10.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl10.setForeground(new java.awt.Color(255, 255, 255));
        sl10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl10.setText("1");

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("Slot");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sl10)
                    .addComponent(sl11)
                    .addComponent(sl12)
                    .addComponent(jLabel10))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl12)
                .addGap(13, 13, 13))
        );

        jPanel3.setBackground(new java.awt.Color(51, 51, 51));
        jPanel3.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        jPanel3.setToolTipText("Currently shown slot in this position");

        sl22.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl22.setForeground(new java.awt.Color(255, 255, 255));
        sl22.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl22.setText("3");

        jLabel11.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("Slot");

        sl20.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl20.setForeground(new java.awt.Color(255, 255, 255));
        sl20.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl20.setText("1");

        sl21.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl21.setForeground(new java.awt.Color(255, 255, 255));
        sl21.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl21.setText("2");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sl20)
                    .addComponent(sl21)
                    .addComponent(sl22)
                    .addComponent(jLabel11))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl22)
                .addGap(13, 13, 13))
        );

        jPanel7.setBackground(new java.awt.Color(51, 51, 51));
        jPanel7.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        jPanel7.setToolTipText("Currently shown slot in this position");

        jLabel12.setBackground(new java.awt.Color(0, 0, 0));
        jLabel12.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Slot");

        sl32.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl32.setForeground(new java.awt.Color(255, 255, 255));
        sl32.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl32.setText("3");

        sl30.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl30.setForeground(new java.awt.Color(255, 255, 255));
        sl30.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl30.setText("1");

        sl31.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        sl31.setForeground(new java.awt.Color(255, 255, 255));
        sl31.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/icon_yellow16.png"))); // NOI18N
        sl31.setText("2");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sl30)
                    .addComponent(sl31)
                    .addComponent(sl32)
                    .addComponent(jLabel12))
                .addContainerGap(15, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl30)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl31)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sl32)
                .addGap(13, 13, 13))
        );

        jLabel32.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel32.setText("NOW:");
        jLabel32.setOpaque(true);

        jLabel33.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel33.setText("File");
        jLabel33.setOpaque(true);

        jLabel31.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel31.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel31.setText("Banks");
        jLabel31.setOpaque(true);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addGap(21, 21, 21)
                                        .addComponent(jLabel32))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel30)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(txtActionStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                                                        .addGap(50, 50, 50)
                                                        .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(lblBlueLED)
                                                        .addGap(56, 56, 56))
                                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                                .addGap(251, 251, 251)
                                                                .addComponent(butRefresh1, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                                .addGap(36, 36, 36)
                                                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                    .addComponent(jLabel5)
                                                                    .addComponent(jLabel6))
                                                                .addGap(12, 12, 12)
                                                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                    .addComponent(txtSerNum, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                    .addComponent(txtSwVer, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                                                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGap(1, 1, 1)
                                        .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(lblBankName, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                                        .addComponent(butLoad, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(butSave, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGap(18, 18, 18))
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel8)
                                                .addGap(68, 68, 68))
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                                        .addComponent(butAddBank)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(butEditBank, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(butDeleteBank)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(butRefreshBankList, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)))
                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addGap(28, 28, 28)
                                                .addComponent(jLabel27)
                                                .addGap(27, 27, 27)
                                                .addComponent(jLabel26)
                                                .addGap(26, 26, 26)
                                                .addComponent(jLabel28))
                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addComponent(butChooseFile, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(txtFileName, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(111, 111, 111)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comPreset, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comMode, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(31, 31, 31)
                                .addComponent(txtChg, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(butApiSend)
                                .addGap(18, 18, 18)
                                .addComponent(chkAutoSync)
                                .addGap(23, 23, 23)
                                .addComponent(butApiSend1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 254, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(74, 74, 74)
                                .addComponent(butRefresh1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(lblBlueLED, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(txtStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 76, Short.MAX_VALUE)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel5)
                                    .addComponent(txtSerNum))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6)
                                    .addComponent(txtSwVer)))
                            .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(5, 5, 5)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtActionStatus)
                            .addComponent(jLabel30)))
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(jLabel26)
                    .addComponent(jLabel28)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(butApiSend, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(butApiSend1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(comPreset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtChg, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblBankName, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel32)))
                    .addComponent(chkAutoSync, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(75, 75, 75)
                        .addComponent(jLabel31))
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(butEditBank, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(butAddBank, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(butDeleteBank, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(butRefreshBankList))
                            .addComponent(jLabel33))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(butLoad, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(butSave, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(butChooseFile)
                                .addComponent(txtFileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 18, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(641, Short.MAX_VALUE)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 684, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 840, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public int getSelectedSlot(int pos) { // returns the radiobutton selected in that slot

        int slotn = 0; // default to slot 1
        // determine the given position's slot to look at, then return that position's selected slot
        switch (pos) {
            case 0:
                if (radPos1Slot1.isSelected()) {
                    slotn = 0;
                    setSlotLights(0, 0);
                }
                if (radPos1Slot2.isSelected()) {
                    slotn = 1;
                    setSlotLights(0, 1);
                }
                if (radPos1Slot3.isSelected()) {
                    slotn = 2;
                    setSlotLights(0, 2);
                }
                break;
            case 1:
                if (radPos2Slot1.isSelected()) {
                    slotn = 0;
                    setSlotLights(1, 0);
                }
                if (radPos2Slot2.isSelected()) {
                    slotn = 1;
                    setSlotLights(1, 1);
                }
                if (radPos2Slot3.isSelected()) {
                    slotn = 2;
                    setSlotLights(1, 2);
                }
                break;
            case 2:
                if (radPos3Slot1.isSelected()) {
                    slotn = 0;
                    setSlotLights(2, 0);
                }
                if (radPos3Slot2.isSelected()) {
                    slotn = 1;
                    setSlotLights(2, 1);
                }
                if (radPos3Slot3.isSelected()) {
                    slotn = 2;
                    setSlotLights(2, 2);
                }
                break;
            case 3:
                if (radPos4Slot1.isSelected()) {
                    slotn = 0;
                    setSlotLights(3, 0);
                }
                if (radPos4Slot2.isSelected()) {
                    slotn = 1;
                    setSlotLights(3, 1);
                }
                if (radPos4Slot3.isSelected()) {
                    slotn = 2;
                    setSlotLights(3, 2);
                }

                break;
            default:
                slotn = 0; // when in doubt, return slot                 
                break; // pro forma break
        }
        //System.out.println("returning "+slotn);
        return slotn;

    }

    public void setSlotLights(int posn, int slotn) { // returns the radiobutton selected in that slot

        // determine the given position's slot to look at, then return that position's selected slot
        switch (posn) {
            case 0:
                sl00.setVisible(false);
                sl01.setVisible(false);
                sl02.setVisible(false);
                switch (slotn) {
                    case 0:
                        sl00.setVisible(true);
                        break;
                    case 1:
                        sl01.setVisible(true);
                        break;
                    case 2:
                        sl02.setVisible(true);
                        break;
                }
                break;
            case 1:
                sl10.setVisible(false);
                sl11.setVisible(false);
                sl12.setVisible(false);
                switch (slotn) {
                    case 0:
                        sl10.setVisible(true);
                        break;
                    case 1:
                        sl11.setVisible(true);
                        break;
                    case 2:
                        sl12.setVisible(true);
                        break;
                }
                break;
            case 2:
                sl20.setVisible(false);
                sl21.setVisible(false);
                sl22.setVisible(false);
                switch (slotn) {
                    case 0:
                        sl20.setVisible(true);
                        break;
                    case 1:
                        sl21.setVisible(true);
                        break;
                    case 2:
                        sl22.setVisible(true);
                        break;
                }
                break;
            case 3:
                sl30.setVisible(false);
                sl31.setVisible(false);
                sl32.setVisible(false);
                switch (slotn) {
                    case 0:
                        sl30.setVisible(true);
                        break;
                    case 1:
                        sl31.setVisible(true);
                        break;
                    case 2:
                        sl32.setVisible(true);
                        break;
                }
                break;
        }
    }

    public void setChangeFlagOld() {
        //System.out.println("setchangeFlag(), pedalQuiet=" + pedalQuiet + ",modelQuiet=" + modelQuiet + ",autoSync=" + autoSync);
        if ((!pedalQuiet) && (!modelQuiet)) {
            mainBank.saveBank(); //
            if (!pedal.api.connected) {  // if no pedal is connected, this is all we're gonna do so don't make the change flag light up.
                canClose = false;
                txtChg.setText("*"); // maybe do this only when autosync is off

            }
        } // save the bank with every edit

        // sync with the pedal if autoSync is turned on
        if (autoSync) {

            // send changes to pedal!
            if (pedal.api.connected) {
                pedal.pedalBank = mainBank;
                // where do the changes get sent?
            } else {
                // do nothing if we can't sync
                //System.out.println("no pedal connected, autosync cannot occur");
                //txtActionStatus.setText("no pedal connected, autosync cannot occur");
            }

            //do this just int each controls method
//            byte[] res = pedal.sendReceive("F0 00 02 21 xx"+  slot address + attribute + data    +" F0");
        } else {
            canClose = false;
            txtChg.setText("*"); // maybe do this only when autosync is off

        }
    }
    public void setChangeFlag() {
        /**
         *  in all cases, UI changes are saved to bank files in real time.
         *  if the pedal is connected and autosync is on, changes are also saved (earlier, by caller)
         *  only set the compare light on if the pedal is connected and autosync is off.
         */
        //System.out.println("setchangeFlag(), pedalQuiet=" + pedalQuiet + ",modelQuiet=" + modelQuiet + ",autoSync=" + autoSync);
        if(!modelQuiet) mainBank.saveBank();        
        if ((!pedalQuiet)) {
            if (!pedal.api.connected) {  // if no pedal is connected, this is all we're gonna do so don't make the change flag light up.
                pedal.pedalBank = mainBank;
                if(!autoSync) {
                    canClose = false;
                    txtChg.setText("*"); // maybe do this only when autosync is of            
                    System.out.println("*");
                }
            }
        } 
    }
    
    void pedalSync() {
        System.out.println("pedalSync()");
        if (pedal.api.connected) {
            // send the whole bank in preset chunks, rather than each attribute
            pedal.pedalBank = mainBank;
            // send the bank to the pedal by sending each preset in turn
            for (int pre = 0; pre < 5; pre++) {
                pedal.api.setPreset(pre, pedal.pedalBank.preset[pre].toByteArr());
            }
            txtChg.setText(""); // saved
        }
    }

    //  file filter to look for bank files.  'ecp' = Effigy Control Pedal extension
    FilenameFilter bankFilesMatcher = new FilenameFilter() {
        public boolean accept(File file, String name) {
            if (name.toLowerCase().endsWith(".ecp")) {
                //System.out.println("match");
                // filters files whose extension is .mp3
                return true;
            } else {
                //System.out.println("no match");
                return false;
            }
        }
    };

    public File[] getBankList() {
        String[] model;
        File dir = new File(dataDir);
        System.out.println("absolutePath=" + dir.getAbsolutePath());
        System.out.println("path=" + dir.getPath());

        File[] files = dir.listFiles(bankFilesMatcher);
        if (files.length == 0) {
            System.out.println("No Banks found.");
            // maybe create factory bank here?
        } else {
            for (File aFile : files) {
                System.out.println(aFile.getName() + " - " + aFile.length());
                //bankList.add(new Bank(aFile.getName()));                
                // implement the use of the container for banks List<Bank> banklist
            }
        }
        return files;
    }

    public void findFile(String name, File file) {
        File[] list = file.listFiles();
        if (list != null) {
            for (File fil : list) {
                if (fil.isDirectory()) {
                    findFile(name, fil);
                } else if (name.equalsIgnoreCase(fil.getName())) {
                    System.out.println(fil.getParentFile());
                }
            }
        }
    }

    /**
     * load the file of the bank into the main bank, and update the UI controls
     *
     * @param bn
     */
    private void bankSwitch(String bn) {
        System.out.print("bankSwitch(" + bn + ")");

        if (!checkCanClose()) {
            return;
        }

        String saveCurrentBankName = lblBankName.getText();

        // do the load the bank thing
        String fn = dataDir + bn + ".ecp";

        System.out.println("Bank " + bn + " selected.");
        try {
            mainBank.loadBank(bn, true);
            mainBank.bankName = bn;
            config.setString("lastBank",bn);
            config.save();

            popUiFromBank(); // update controls
            txtActionStatus.setText("bankSwitch:loaded bank " + bn);
            setSelectedBankName(bn);
            lblBankName.setText(bn); // ui "current bank"
            txtChg.setText("");
            canClose = true;
        } // load and commit
        catch (Exception e) {
            System.err.println("bankSwitch: Exception switching to bank " + bn + ": " + e);
        }
    }

    private void comPresetStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_comPresetStateChanged
        popUiFromBank();
    }//GEN-LAST:event_comPresetStateChanged

    private void comModeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_comModeStateChanged
        popUiFromBank();
    }//GEN-LAST:event_comModeStateChanged

    private void butRefreshBankListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRefreshBankListActionPerformed
        refreshBankList();
    }//GEN-LAST:event_butRefreshBankListActionPerformed

    private void txtChgPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_txtChgPropertyChange
        /*
        if (txtChg.getText().isEmpty()) {
            System.out.println("Change Flag reset");
        } else {
            System.out.println("Change Flag set");
        }
        */
    }//GEN-LAST:event_txtChgPropertyChange

    private void txtBanksValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_txtBanksValueChanged
        // don't do anything if they just scroll, they need to click edit or double click to activeate (load) a bank
    }//GEN-LAST:event_txtBanksValueChanged

    private void txtBanksMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtBanksMouseClicked
        int nc = evt.getClickCount();
        if (nc == 2) { // double-click
            System.out.println("double-click!");
            bankSwitch(txtBanks.getSelectedValue());
        }
    }//GEN-LAST:event_txtBanksMouseClicked

    private void txtBanksMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtBanksMouseDragged
        // TODO add your handling code here:
        //evt.
    }//GEN-LAST:event_txtBanksMouseDragged

    private void butChooseFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butChooseFileActionPerformed

        JFileChooser chooser = new JFileChooser(dataDir);
        File configExtDir = new File(config.getString("extDir"));
        chooser.setCurrentDirectory(configExtDir);
        //chooser.setSize(500, 500);
        //chooser.setSize(800, 600);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Effigy Control Pedal Bank file", "ecp");
        chooser.setFileFilter(filter);

        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("opened file "
                    + chooser.getSelectedFile().getName());
            txtFileName.setText(chooser.getSelectedFile().getPath());
            config.setString("extDir", txtFileName.getText());
            config.save();
        }
    }//GEN-LAST:event_butChooseFileActionPerformed

    private void txtFileNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFileNameActionPerformed
        config.setString("extDir", txtFileName.getText());
        config.save();
    }//GEN-LAST:event_txtFileNameActionPerformed

    private void butSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSaveActionPerformed
        if (txtFileName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "A file name must be supplied.");
            return;
        }
        String aName = txtFileName.getText();
        if (!aName.contains(fileSeparator) && (!aName.contains("."))) { // it's a name with no path, so save it as a bank
            Bank nb = mainBank;
            nb.bankName = aName;
            nb.fileName = dataDir + aName + ".ecp";
            nb.saveBank();
            bankList.add(nb);
            refreshBankList();
            txtActionStatus.setText("created new Bank " + nb.bankName);

        } else { // it's an external file, save the external bank using the file name as the bank name
            // make sure it has an extension
            if (!aName.contains(".")) {
                aName = aName + ".ecp"; // add extension, because it needs one
            }
            mainBank.saveBank(aName); // saving this way doesn't permanently alter the bank's formal file or bbank name
            txtActionStatus.setText("exported file " + aName);
        }

        //saveFile();
        canClose = true;
        txtChg.setText("");
    }//GEN-LAST:event_butSaveActionPerformed

    private void butLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butLoadActionPerformed
        if (!checkCanClose()) {
            return;
        }

        if (txtFileName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "A file name must be supplied.");
            return;
        }

        if (!txtFileName.getText().endsWith(".ecp")) {
            JOptionPane.showMessageDialog(null, "Bank file names must end with '.ecp'");
        }

        //String fn = makeFileName(txtFileName.getText());
        File loadName = new File(txtFileName.getText()); // new file from full path and name
        String tfn = loadName.getName();

        String banknm = tfn;

        if (tfn.contains(".")) {
            String tbn[] = tfn.split("\\."); // find extension
            banknm = tbn[0];    // get bank name from file name
        }

        Bank nb = new Bank(banknm); // create new bank
        nb.loadBank(loadName, true); // get bank data
        System.out.println("verifying import bank...");
        int verBank = nb.verifyBank(banknm);
        System.out.println("bank verify result:" + verBank);
        if (verBank > 0) {
            System.err.println("Bank is not valid, cannot load " + loadName);
            return;
        }

        nb.saveBank();
        bankSwitch(banknm); // make mainBank this bank
        refreshBankList();
        setSelectedBankName(banknm);
        popUiFromBank(); // update controls
        txtChg.setText("");

    }//GEN-LAST:event_butLoadActionPerformed

    private void butEditBankActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butEditBankActionPerformed
        // TODO add your handling code here:
        // what is edit?  it already edits once selected?  Does this obviate the  old sync issue?
        bankSwitch(txtBanks.getSelectedValue());

    }//GEN-LAST:event_butEditBankActionPerformed

    private void butDeleteBankActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butDeleteBankActionPerformed

        // confirm, then delete bank, then refresh bank list
        // bank 0 cannot be deleted
        String selBank = txtBanks.getSelectedValue();
        if (selBank.toLowerCase().trim().equals("pedal bank")) {
            System.err.println("Cannot delete Pedal Bank");
            return;
        }

        Object[] options = {"Delete", "Cancel"};
        int n = JOptionPane.showOptionDialog(new JOptionPane(),
                "Confirm Delete Bank",
                "Delete Bank",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, //do not use a custom Icon
                options, //the titles of buttons
                options[0]); //default button title

        switch (n) {
            case 0: // delete,
                canClose = true;
                Bank workBank = new Bank(selBank);
                workBank.deleteBank();
                refreshBankList();
                if (workBank.bankName.equalsIgnoreCase(lblBankName.getText())) { // we deleted the current bank so load another one
                    pickAndLoadBank();
                }
                break;
            case 1: // cancel
                //txtActionStatus.setText("Action halted, data not overwritten.  Maybe save now.");
                break;
            default:
        }

        //     int bankNum = bn;

    }//GEN-LAST:event_butDeleteBankActionPerformed

    private void butAddBankActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butAddBankActionPerformed
        // add a bank
        // the bank list should be generated from the files selector every time it's generated, like in genpos1data().
        // the model is regenerated by the editing functions on the panel, add, edit, delete
        int bankCt = txtBanks.getModel().getSize() + 1;
        String bcs = String.valueOf(bankCt);
        // display and do system settings on a different window...
        // display the about box
        String bankName = JOptionPane.showInputDialog("New Bank Name");
        String bankNameTxt = bankName;

        //txtBanks.getModel().getElementAt(bankCt);
        //        final ListModel<String> model = txtBanks.getModel();
        //      DefaultListModel dlm = new DefaultListModel();
        Bank X = new Bank(bankName);
        X.clear();
        bankList.add(X);
        X.bankName = bankName;
        X.saveBank();
        bankSwitch(bankName);
        refreshBankList();
        popUiFromBank(); // update controls
        txtChg.setText("");
        setSelectedBankName(bankName);
    }//GEN-LAST:event_butAddBankActionPerformed

    private void butApiSend1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butApiSend1ActionPerformed
        // display and do system settings on a different window...
        // display the about box, give it the pedal bank to use
        //      if(pedal.api.connected) {
        ConfigDialog cd = new ConfigDialog(this, true, mainBank, pedal); // modal
        cd.setVisible(true);
        //      } else {
        //        JOptionPane.showMessageDialog(null, "No pedal connected.");  
        //      }

    }//GEN-LAST:event_butApiSend1ActionPerformed

    /**
     * when the autosync is toggled, there must be a check of if the pedal is
     * connected, and attempt to connect, or wave off, or something...********
     * tag ******
     *
     * @param evt
     */
    private void chkAutoSyncActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkAutoSyncActionPerformed
        if (chkAutoSync.isSelected()) {
            butApiSend.setEnabled(false);
            autoSync = true;
            pedalSync();
            if (pedal.api.connected) {
                // send the whole bank in preset chunks, rather than each attribute
                pedal.pedalBank = mainBank;

                // send the bank to the pedal
                for (int pre = 0; pre < 5; pre++) {
                    pedal.api.setPreset(pre, pedal.pedalBank.preset[pre].toByteArr());
                    // send tmpr to pedal as a preset
                }

                txtChg.setText(""); // saved
                //pedal.api.setBank(convertTextToByteArr(pedal.pedalBank.toString()));
            }

        } else {
            butApiSend.setEnabled(true);
            autoSync = false;
        }
        config.setBool("autoSync", autoSync);
        config.save();
    }//GEN-LAST:event_chkAutoSyncActionPerformed

    private void butApiSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butApiSendActionPerformed
        pedalSync();
    }//GEN-LAST:event_butApiSendActionPerformed

    private void radPos3Slot3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos3Slot3ActionPerformed
        popUiFromBank(2);
        //comPos.setSelectedIndex(2);
        //comSlot.setSelectedIndex(2);                // TODO add your handling code here:
        //byte[] slot3Data = genPos3Data();
        txtActionStatus.setText("Pos 3 Slot 3 data selected.");
    }//GEN-LAST:event_radPos3Slot3ActionPerformed

    private void radPos3Slot2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos3Slot2ActionPerformed
        popUiFromBank(2);
        //comPos.setSelectedIndex(2);
        //comSlot.setSelectedIndex(1);                // TODO add your handling code here:
        //byte[] slot2Data = genPos3Data();
        txtActionStatus.setText("Pos 3 Slot 2 data selected.");
    }//GEN-LAST:event_radPos3Slot2ActionPerformed

    private void radPos3Slot1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos3Slot1ActionPerformed
        popUiFromBank(2);
        //comPos.setSelectedIndex(2);
        //comSlot.setSelectedIndex(0);                // TODO add your handling code here:
        // byte[] slot1Data = genPos3Data();
        txtActionStatus.setText("Pos 3 Slot 1 data selected.");
    }//GEN-LAST:event_radPos3Slot1ActionPerformed

    private void comCurveTypePos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comCurveTypePos3PropertyChange

    }//GEN-LAST:event_comCurveTypePos3PropertyChange

    private void comCurveTypePos3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comCurveTypePos3ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 2;
        int slot = getSelectedSlot(position);
        int val = comCurveTypePos3.getSelectedIndex();
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvetype = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVETYPE, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comCurveTypePos3ActionPerformed

    private void latchingPos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_latchingPos3PropertyChange

    }//GEN-LAST:event_latchingPos3PropertyChange

    private void latchingPos3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_latchingPos3ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) (int) comMode.getValue();
        int position = 2;
        int slot = getSelectedSlot(position);
        int val = latchingPos3.isSelected() ? 1 : 0;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].latching = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_LATCHING, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_latchingPos3ActionPerformed

    private void latchingPos3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_latchingPos3StateChanged

    }//GEN-LAST:event_latchingPos3StateChanged

    private void spnMidiChannelPos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMidiChannelPos3PropertyChange

    }//GEN-LAST:event_spnMidiChannelPos3PropertyChange

    private void spnMidiChannelPos3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMidiChannelPos3StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 2;
        int slot = getSelectedSlot(position);
        int val = (int) spnMidiChannelPos3.getValue() - 1;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiChannel = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CHANNEL, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            setChangeFlag();
        }

    }//GEN-LAST:event_spnMidiChannelPos3StateChanged

    private void comMidiCmdPos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comMidiCmdPos3PropertyChange

    }//GEN-LAST:event_comMidiCmdPos3PropertyChange

    private void comMidiCmdPos3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comMidiCmdPos3ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag
        System.out.println("comMidiCmdPos3ActionPerformed");
        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 2;
        int slot = getSelectedSlot(position);
        String MidiCmdDigit[] = comMidiCmdPos3.getSelectedItem().toString().split(" ");

        int val = Integer.parseInt(MidiCmdDigit[0], 16); // ***** see if handles ok if empty?  if empty, there won't be an action performed e.g. a state change

        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiCommand = (int) val;
        }
        // populate subcommand with matching list
        int subVal = mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiSubCommand;
        popSubCmd(val, comMidiSubCommandPos3); // do not set the sub val!
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_COMMAND, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }

    }//GEN-LAST:event_comMidiCmdPos3ActionPerformed

    private void spnMaxRangePos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMaxRangePos3PropertyChange

    }//GEN-LAST:event_spnMaxRangePos3PropertyChange

    private void spnMaxRangePos3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMaxRangePos3StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 2;
        int slot = getSelectedSlot(position);
        int val = Integer.parseInt(String.valueOf(spnMaxRangePos3.getValue()));
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].max = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_MAX, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMaxRangePos3StateChanged

    private void radCurvePosPos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_radCurvePosPos3PropertyChange

    }//GEN-LAST:event_radCurvePosPos3PropertyChange

    private void radCurvePosPos3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radCurvePosPos3ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 2;
        int slot = getSelectedSlot(position);
        int val = radCurvePosPos3.isSelected() ? 1 : 0;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvedirection = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVEDIRECTION, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_radCurvePosPos3ActionPerformed

    private void radCurveNegPos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_radCurveNegPos3PropertyChange

    }//GEN-LAST:event_radCurveNegPos3PropertyChange

    private void radCurveNegPos3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radCurveNegPos3ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 2;
        int slot = getSelectedSlot(position);
        int val = radCurveNegPos3.isSelected() ? 0 : 1; // reversed for pos dir
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvedirection = (int) val; // save val as pos dir updated
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVEDIRECTION, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_radCurveNegPos3ActionPerformed

    private void comMidiSubCommandPos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comMidiSubCommandPos3PropertyChange

    }//GEN-LAST:event_comMidiSubCommandPos3PropertyChange

    private void comMidiSubCommandPos3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comMidiSubCommandPos3ActionPerformed
        System.out.println("comMidiSubCommandPos3ActionPerformed");
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 2;
        int slot = getSelectedSlot(position);
        int val = comMidiSubCommandPos3.getSelectedIndex(); // ***** see if handles ok if empty?  if empty, there won't be an action performed e.g. a state change
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiSubCommand = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_SUBCOMMAND, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comMidiSubCommandPos3ActionPerformed

    private void spnMinRangePos3PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMinRangePos3PropertyChange

    }//GEN-LAST:event_spnMinRangePos3PropertyChange

    private void spnMinRangePos3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMinRangePos3StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 2;
        int slot = getSelectedSlot(position);
        int val = Integer.parseInt(String.valueOf(spnMinRangePos3.getValue()));
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].min = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_MIN, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMinRangePos3StateChanged

    private void radPos4Slot1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos4Slot1ActionPerformed
        popUiFromBank(3);
        //comPos.setSelectedIndex(3);
        //comSlot.setSelectedIndex(0);
        //byte[] slot1Data = genPos4Data();
        txtActionStatus.setText("Pos 4 Slot 1 data selected.");
    }//GEN-LAST:event_radPos4Slot1ActionPerformed

    private void radPos4Slot2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos4Slot2ActionPerformed
        popUiFromBank(3);
        //comPos.setSelectedIndex(3);
        //comSlot.setSelectedIndex(1);
        //byte[] slot2Data = genPos4Data();
        txtActionStatus.setText("Pos 4 Slot 2 data selected.");
    }//GEN-LAST:event_radPos4Slot2ActionPerformed

    private void radPos4Slot3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos4Slot3ActionPerformed
        popUiFromBank(3);
        //comPos.setSelectedIndex(3);
        //comSlot.setSelectedIndex(2);
        //byte[] slot3Data = genPos4Data();
        txtActionStatus.setText("Pos 4 Slot 3 data selected.");
    }//GEN-LAST:event_radPos4Slot3ActionPerformed

    private void comCurveTypePos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comCurveTypePos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_comCurveTypePos4PropertyChange

    private void comCurveTypePos4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comCurveTypePos4ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);
        int val = comCurveTypePos4.getSelectedIndex();
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvetype = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVETYPE, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comCurveTypePos4ActionPerformed

    private void latchingPos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_latchingPos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_latchingPos4PropertyChange

    private void latchingPos4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_latchingPos4ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);
        int val = latchingPos4.isSelected() ? 1 : 0;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].latching = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_LATCHING, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_latchingPos4ActionPerformed

    private void latchingPos4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_latchingPos4StateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_latchingPos4StateChanged

    private void spnMidiChannelPos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMidiChannelPos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_spnMidiChannelPos4PropertyChange

    private void spnMidiChannelPos4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMidiChannelPos4StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);
        int val = (int) spnMidiChannelPos3.getValue() - 1;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiChannel = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CHANNEL, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            setChangeFlag();
        }

    }//GEN-LAST:event_spnMidiChannelPos4StateChanged

    private void comMidiCmdPos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comMidiCmdPos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_comMidiCmdPos4PropertyChange

    private void comMidiCmdPos4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comMidiCmdPos4ActionPerformed
        System.out.println("comMidiCmdPos4ActionPerformed");
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);
        String MidiCmdDigit[] = comMidiCmdPos4.getSelectedItem().toString().split(" ");

        int val = Integer.parseInt(MidiCmdDigit[0], 16); // ***** see if handles ok if empty?  if empty, there won't be an action performed e.g. a state change

        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiCommand = (int) val;
        }
        // populate subcommand with matching list
        int subVal = mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiSubCommand;
        popSubCmd(val, comMidiSubCommandPos4); // do not set the sub val!
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_COMMAND, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comMidiCmdPos4ActionPerformed

    private void spnMaxRangePos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMaxRangePos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_spnMaxRangePos4PropertyChange

    private void spnMaxRangePos4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMaxRangePos4StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);
        int val = Integer.parseInt(String.valueOf(spnMaxRangePos4.getValue()));
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].max = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_MAX, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMaxRangePos4StateChanged

    private void radCurvePosPos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_radCurvePosPos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_radCurvePosPos4PropertyChange

    private void radCurvePosPos4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radCurvePosPos4ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);
        int val = radCurvePosPos4.isSelected() ? 1 : 0;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvedirection = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVEDIRECTION, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_radCurvePosPos4ActionPerformed

    private void radCurveNegPos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_radCurveNegPos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_radCurveNegPos4PropertyChange

    private void radCurveNegPos4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radCurveNegPos4ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);
        int val = radCurveNegPos4.isSelected() ? 0 : 1; // reversed for pos dir
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvedirection = (int) val; // save val as pos dir updated
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVEDIRECTION, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_radCurveNegPos4ActionPerformed

    private void comMidiSubCommandPos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comMidiSubCommandPos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_comMidiSubCommandPos4PropertyChange

    private void comMidiSubCommandPos4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comMidiSubCommandPos4ActionPerformed
        System.out.println("comMidiSubCommandPos4ActionPerformed");
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);

        int val = comMidiSubCommandPos4.getSelectedIndex(); 
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiSubCommand = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_SUBCOMMAND, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comMidiSubCommandPos4ActionPerformed

    private void spnMinRangePos4PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMinRangePos4PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_spnMinRangePos4PropertyChange

    private void spnMinRangePos4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMinRangePos4StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 3;
        int slot = getSelectedSlot(position);
        int val = Integer.parseInt(String.valueOf(spnMinRangePos4.getValue()));
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].min = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_MIN, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMinRangePos4StateChanged

    private void butRefresh1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRefresh1ActionPerformed
        // reconnnect with pedal
        pedalConnect();
        txtActionStatus.setText(pedal.status);
        // should there be more here as in when we initialize, refresh the bank list, switch to the pedal bank?? ************
    }//GEN-LAST:event_butRefresh1ActionPerformed

    private void radPos2Slot3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos2Slot3ActionPerformed
        popUiFromBank(1);
        //comPos.setSelectedIndex(1);
        //comSlot.setSelectedIndex(2);
        //  byte[] slot3Data = genPos2Data();
        txtActionStatus.setText("Pos 2 Slot 3 data selected.");
    }//GEN-LAST:event_radPos2Slot3ActionPerformed

    private void radPos2Slot2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos2Slot2ActionPerformed
        popUiFromBank(1);
        //comPos.setSelectedIndex(1);
        //comSlot.setSelectedIndex(1);
        // byte[] slot2Data = //genPos2Data();
        txtActionStatus.setText("Pos 2 Slot 2 data selected.");
    }//GEN-LAST:event_radPos2Slot2ActionPerformed

    private void radPos2Slot1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos2Slot1ActionPerformed
        popUiFromBank(1);
        //comPos.setSelectedIndex(1);
        //comSlot.setSelectedIndex(0);
        // byte[] slot2Data = //genPos2Data();
        txtActionStatus.setText("Pos 2 Slot 1 data selected.");
    }//GEN-LAST:event_radPos2Slot1ActionPerformed

    private void radCurveNegPos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_radCurveNegPos2PropertyChange

    }//GEN-LAST:event_radCurveNegPos2PropertyChange

    private void radCurveNegPos2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radCurveNegPos2ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        int val = radCurveNegPos2.isSelected() ? 0 : 1; // reversed for pos dir
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvedirection = (int) val; // save val as pos dir updated
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVEDIRECTION, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_radCurveNegPos2ActionPerformed

    private void comMidiCmdPos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comMidiCmdPos2PropertyChange

    }//GEN-LAST:event_comMidiCmdPos2PropertyChange

    private void comMidiCmdPos2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comMidiCmdPos2ActionPerformed
        System.out.println("comMidiCmdPos2ActionPerformed(" + evt + ")");
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        String MidiCmdDigit[] = comMidiCmdPos2.getSelectedItem().toString().split(" ");

        int val = Integer.parseInt(MidiCmdDigit[0], 16); // ***** see if handles ok if empty?  if empty, there won't be an action performed e.g. a state change

        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiCommand = (int) val;
        }
        // populate subcommand with matching list
        int subVal = mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiSubCommand;
        popSubCmd(val, comMidiSubCommandPos2); // do not set the sub val!
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_COMMAND, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comMidiCmdPos2ActionPerformed

    private void spnMaxRangePos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMaxRangePos2PropertyChange

    }//GEN-LAST:event_spnMaxRangePos2PropertyChange

    private void spnMaxRangePos2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMaxRangePos2StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        int val = Integer.parseInt(String.valueOf(spnMaxRangePos2.getValue()));
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].max = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_MAX, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMaxRangePos2StateChanged

    private void latchingPos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_latchingPos2PropertyChange

    }//GEN-LAST:event_latchingPos2PropertyChange

    private void latchingPos2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_latchingPos2ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        int val = latchingPos2.isSelected() ? 1 : 0;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].latching = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_LATCHING, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_latchingPos2ActionPerformed

    private void latchingPos2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_latchingPos2StateChanged

    }//GEN-LAST:event_latchingPos2StateChanged

    private void spnMinRangePos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMinRangePos2PropertyChange

    }//GEN-LAST:event_spnMinRangePos2PropertyChange

    private void spnMinRangePos2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMinRangePos2StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        int val = Integer.parseInt(String.valueOf(spnMinRangePos2.getValue()));
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].min = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_MIN, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMinRangePos2StateChanged

    private void spnMidiChannelPos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMidiChannelPos2PropertyChange

    }//GEN-LAST:event_spnMidiChannelPos2PropertyChange

    private void spnMidiChannelPos2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMidiChannelPos2StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        int val = (int) spnMidiChannelPos2.getValue() - 1;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiChannel = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CHANNEL, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMidiChannelPos2StateChanged

    private void radCurvePosPos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_radCurvePosPos2PropertyChange

    }//GEN-LAST:event_radCurvePosPos2PropertyChange

    private void radCurvePosPos2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radCurvePosPos2ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        int val = radCurvePosPos2.isSelected() ? 1 : 0;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvedirection = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVEDIRECTION, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_radCurvePosPos2ActionPerformed

    private void comCurveTypePos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comCurveTypePos2PropertyChange

    }//GEN-LAST:event_comCurveTypePos2PropertyChange

    private void comCurveTypePos2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comCurveTypePos2ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        int val = comCurveTypePos2.getSelectedIndex();
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvetype = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVETYPE, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comCurveTypePos2ActionPerformed

    private void comMidiSubCommandPos2PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comMidiSubCommandPos2PropertyChange
        //System.out.println("comMidiSubCommandPos2PropertyChange("+evt);
    }//GEN-LAST:event_comMidiSubCommandPos2PropertyChange

    private void comMidiSubCommandPos2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comMidiSubCommandPos2ActionPerformed
        //System.out.println("comMidiSubCommandPos2ActionPerformed("+evt+")");
        //System.out.println("val="+comMidiSubCommandPos2.getSelectedIndex());
        //System.out.println("modelQuiet="+modelQuiet);
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag
        // we neeed to not do this when the combo model lsit is being populated, only when it is being changed.
        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 1;
        int slot = getSelectedSlot(position);
        int val = comMidiSubCommandPos2.getSelectedIndex(); // ***** see if handles ok if empty?  if empty, there won't be an action performed e.g. a state change
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiSubCommand = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_SUBCOMMAND, (byte) val);
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comMidiSubCommandPos2ActionPerformed

    private void radPos1Slot3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos1Slot3ActionPerformed
        popUiFromBank(0);
        //comPos.setSelectedIndex(0);
        //comSlot.setSelectedIndex(2);
        //byte[] slot3Data = genPos1Data();
        txtActionStatus.setText("Pos 1 Slot 3 data selected.");
    }//GEN-LAST:event_radPos1Slot3ActionPerformed

    private void radPos1Slot2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos1Slot2ActionPerformed
        popUiFromBank(0);
        //comPos.setSelectedIndex(0);
        //comSlot.setSelectedIndex(1);
        //byte[] slot2Data = genPos1Data();
        txtActionStatus.setText("Pos 1 Slot 2 data selected.");
    }//GEN-LAST:event_radPos1Slot2ActionPerformed

    private void radPos1Slot1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPos1Slot1ActionPerformed
        popUiFromBank(0);
        //comPos.setSelectedIndex(0);
        //comSlot.setSelectedIndex(0);
        //byte[] slot1Data = genPos1Data();
        txtActionStatus.setText("Pos 1 Slot 1 data selected.");
    }//GEN-LAST:event_radPos1Slot1ActionPerformed

    private void comMidiCmdPos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comMidiCmdPos1PropertyChange
        //        System.out.println("comMidiCmdPos1 prop chg "+evt);
    }//GEN-LAST:event_comMidiCmdPos1PropertyChange

    private void comMidiCmdPos1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comMidiCmdPos1ActionPerformed
        //System.out.println("comMidiCmdPos1ActionPerformed "+evt);
        //System.out.println("modelQuiet="+modelQuiet);
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        String MidiCmdDigit[] = comMidiCmdPos1.getSelectedItem().toString().split(" ");
        int val = Integer.parseInt(MidiCmdDigit[0], 16); // ***** see if handles ok if empty?  if empty, there won't be an action performed e.g. a state change
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiCommand = (int) val;
        }
        // populate subcommand with matching list
        int subVal = mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiSubCommand;
        popSubCmd(val, comMidiSubCommandPos1); // do not set the sub val!

        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_COMMAND, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }

    }//GEN-LAST:event_comMidiCmdPos1ActionPerformed

    private void comMidiCmdPos1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comMidiCmdPos1ItemStateChanged
        // only if ?quiet = true? ****** tag
        //        canClose = false;
        //        txtChg.setText("*");  // turn on "compare" light
        //        System.out.println("comMidiPos1 state chg: "+evt.toString());
        // do nothing here

        // maybe popsubcmd here, whenever this happens *******
    }//GEN-LAST:event_comMidiCmdPos1ItemStateChanged

    private void spnMinRangePos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMinRangePos1PropertyChange

    }//GEN-LAST:event_spnMinRangePos1PropertyChange

    private void spnMinRangePos1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMinRangePos1StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        int val = Integer.parseInt(String.valueOf(spnMinRangePos1.getValue()));
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].min = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_MIN, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMinRangePos1StateChanged

    private void comCurveTypePos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comCurveTypePos1PropertyChange

    }//GEN-LAST:event_comCurveTypePos1PropertyChange

    private void comCurveTypePos1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comCurveTypePos1ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        int val = comCurveTypePos1.getSelectedIndex();
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvetype = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVETYPE, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comCurveTypePos1ActionPerformed

    private void comMidiSubCommandPos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_comMidiSubCommandPos1PropertyChange

    }//GEN-LAST:event_comMidiSubCommandPos1PropertyChange

    private void comMidiSubCommandPos1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comMidiSubCommandPos1ActionPerformed
        //System.out.println("comMidiSubCommandPos1ActionPerformed "+evt);
        //System.out.println("val="+comMidiSubCommandPos1.getSelectedIndex());
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        int val = 0;
        try {
            val = comMidiSubCommandPos1.getSelectedIndex(); // ***** see if handles ok if empty?  if empty, there won't be an action performed e.g. a state change
        } catch (Exception e) {
            System.err.println("caught exception in setting combo value " + e);
        }
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiSubCommand = (int) val;             // this should just set the same value back at it if being populated by machine
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_SUBCOMMAND, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_comMidiSubCommandPos1ActionPerformed

    private void spnMidiChannelPos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMidiChannelPos1PropertyChange

    }//GEN-LAST:event_spnMidiChannelPos1PropertyChange

    private void spnMidiChannelPos1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMidiChannelPos1StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        int val = (int) spnMidiChannelPos1.getValue() - 1;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].MidiChannel = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CHANNEL, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            setChangeFlag();
        }

    }//GEN-LAST:event_spnMidiChannelPos1StateChanged

    private void latchingPos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_latchingPos1PropertyChange

    }//GEN-LAST:event_latchingPos1PropertyChange

    private void latchingPos1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_latchingPos1ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        int val = latchingPos1.isSelected() ? 1 : 0;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].latching = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_LATCHING, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_latchingPos1ActionPerformed

    private void radCurveNegPos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_radCurveNegPos1PropertyChange

    }//GEN-LAST:event_radCurveNegPos1PropertyChange

    private void radCurveNegPos1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radCurveNegPos1ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        int val = radCurveNegPos1.isSelected() ? 0 : 1; // reversed for pos dir
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvedirection = (int) val; // save val as pos dir updated
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVEDIRECTION, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_radCurveNegPos1ActionPerformed

    private void spnMaxRangePos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_spnMaxRangePos1PropertyChange

    }//GEN-LAST:event_spnMaxRangePos1PropertyChange

    private void spnMaxRangePos1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMaxRangePos1StateChanged
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        int val = Integer.parseInt(String.valueOf(spnMaxRangePos1.getValue()));
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].max = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_MAX, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_spnMaxRangePos1StateChanged

    private void radCurvePosPos1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_radCurvePosPos1PropertyChange

    }//GEN-LAST:event_radCurvePosPos1PropertyChange

    private void radCurvePosPos1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radCurvePosPos1ActionPerformed
        // standard code for each control
        // 1. set preset and mmode from nav controls
        // 2. set position of where you are
        // 3. set slot of what slot is selected in the position
        // 4. set which slot attribute this control represents
        // 5. calculate the UI controls' value into a byte value
        // 6. update the main bank
        // 7. if autosync is on, send the update to the pedal, otherwise, set the change flag

        int pre = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        int position = 0;
        int slot = getSelectedSlot(position);
        int val = radCurvePosPos1.isSelected() ? 1 : 0;
        if (!modelQuiet) {
            mainBank.preset[pre].mode[mode].pos[position].slot[slot].curvedirection = (int) val;
        }
        if (pedal.api.connected && autoSync && (!pedalQuiet)) {// convert val to 2 char
            try {
                pedal.api.setAttribute(pre, mode, position, slot, SLOT_CURVEDIRECTION, (byte) val);
                //void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
            } catch (Exception ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            setChangeFlag();
        }
    }//GEN-LAST:event_radCurvePosPos1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton butAddBank;
    private javax.swing.JButton butApiSend;
    private javax.swing.JButton butApiSend1;
    private javax.swing.JButton butChooseFile;
    private javax.swing.JButton butDeleteBank;
    private javax.swing.JButton butEditBank;
    private javax.swing.ButtonGroup butGrpPos1;
    private javax.swing.ButtonGroup butGrpPos2;
    private javax.swing.ButtonGroup butGrpPos3;
    private javax.swing.ButtonGroup butGrpPos4;
    private javax.swing.ButtonGroup butGrpSelectedSlot;
    private javax.swing.JButton butLoad;
    private javax.swing.JButton butRefresh1;
    private javax.swing.JButton butRefreshBankList;
    private javax.swing.JButton butSave;
    private javax.swing.ButtonGroup buttonGroupPosition;
    private javax.swing.ButtonGroup buttonGroupSlot1;
    private javax.swing.ButtonGroup buttonGroupSlot2;
    private javax.swing.ButtonGroup buttonGroupSlot3;
    private javax.swing.JCheckBox chkAutoSync;
    private javax.swing.JComboBox<String> comCurveTypePos1;
    private javax.swing.JComboBox<String> comCurveTypePos2;
    private javax.swing.JComboBox<String> comCurveTypePos3;
    private javax.swing.JComboBox<String> comCurveTypePos4;
    private javax.swing.JComboBox<String> comMidiCmdPos1;
    private javax.swing.JComboBox<String> comMidiCmdPos2;
    private javax.swing.JComboBox<String> comMidiCmdPos3;
    private javax.swing.JComboBox<String> comMidiCmdPos4;
    private javax.swing.JComboBox<String> comMidiSubCommandPos1;
    private javax.swing.JComboBox<String> comMidiSubCommandPos2;
    private javax.swing.JComboBox<String> comMidiSubCommandPos3;
    private javax.swing.JComboBox<String> comMidiSubCommandPos4;
    private javax.swing.JSpinner comMode;
    private javax.swing.JSpinner comPreset;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JCheckBox latchingPos1;
    private javax.swing.JCheckBox latchingPos2;
    private javax.swing.JCheckBox latchingPos3;
    private javax.swing.JCheckBox latchingPos4;
    private javax.swing.JLabel lblBankName;
    private javax.swing.JLabel lblBlueLED;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JRadioButton radCurveNegPos1;
    private javax.swing.JRadioButton radCurveNegPos2;
    private javax.swing.JRadioButton radCurveNegPos3;
    private javax.swing.JRadioButton radCurveNegPos4;
    private javax.swing.JRadioButton radCurvePosPos1;
    private javax.swing.JRadioButton radCurvePosPos2;
    private javax.swing.JRadioButton radCurvePosPos3;
    private javax.swing.JRadioButton radCurvePosPos4;
    private javax.swing.JRadioButton radPos1Slot1;
    private javax.swing.JRadioButton radPos1Slot2;
    private javax.swing.JRadioButton radPos1Slot3;
    private javax.swing.JRadioButton radPos2Slot1;
    private javax.swing.JRadioButton radPos2Slot2;
    private javax.swing.JRadioButton radPos2Slot3;
    private javax.swing.JRadioButton radPos3Slot1;
    private javax.swing.JRadioButton radPos3Slot2;
    private javax.swing.JRadioButton radPos3Slot3;
    private javax.swing.JRadioButton radPos4Slot1;
    private javax.swing.JRadioButton radPos4Slot2;
    private javax.swing.JRadioButton radPos4Slot3;
    private javax.swing.JLabel sl00;
    private javax.swing.JLabel sl01;
    private javax.swing.JLabel sl02;
    private javax.swing.JLabel sl10;
    private javax.swing.JLabel sl11;
    private javax.swing.JLabel sl12;
    private javax.swing.JLabel sl20;
    private javax.swing.JLabel sl21;
    private javax.swing.JLabel sl22;
    private javax.swing.JLabel sl30;
    private javax.swing.JLabel sl31;
    private javax.swing.JLabel sl32;
    private javax.swing.JSpinner spnMaxRangePos1;
    private javax.swing.JSpinner spnMaxRangePos2;
    private javax.swing.JSpinner spnMaxRangePos3;
    private javax.swing.JSpinner spnMaxRangePos4;
    private javax.swing.JSpinner spnMidiChannelPos1;
    private javax.swing.JSpinner spnMidiChannelPos2;
    private javax.swing.JSpinner spnMidiChannelPos3;
    private javax.swing.JSpinner spnMidiChannelPos4;
    private javax.swing.JSpinner spnMinRangePos1;
    private javax.swing.JSpinner spnMinRangePos2;
    private javax.swing.JSpinner spnMinRangePos3;
    private javax.swing.JSpinner spnMinRangePos4;
    private javax.swing.JLabel txtActionStatus;
    private javax.swing.JList<String> txtBanks;
    private javax.swing.JLabel txtChg;
    private javax.swing.JTextField txtFileName;
    private javax.swing.JLabel txtSerNum;
    private javax.swing.JLabel txtStatus;
    private javax.swing.JLabel txtSwVer;
    // End of variables declaration//GEN-END:variables

    boolean checkCanClose() {
        if (pedal.api.connected && (!canClose)) {
            // do what here to get choice/save
            Object[] options = {"Discard Changes and Proceed", "Sync Now then Proceed", "Do Not Proceed"};
            int n = JOptionPane.showOptionDialog(new JOptionPane(),
                    "Discard Changes to Pedal Data?",
                    "Pedal Data Changed Without Syncing",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, //do not use a custom Icon
                    options, //the titles of buttons
                    options[0]); //default button title

            switch (n) {
                case 0: // proceed, allow data to be discarded
                    canClose = true;
                    break;
                case 1: // Try to Sync now and proceed
                    try {
                        pedalSync();
                        canClose = true;
                    } catch(Exception e) {
                        System.err.println("pedalSync():"+e );
                        txtActionStatus.setText("did not sync pedal:"+e);
                    }
                    //txtActionStatus.setText("Action halted, data not overwritten.  Maybe save now.");
                    break;
                case 2: // canClose remains false
                    //txtActionStatus.setText("Action halted, data not overwritten.  Maybe save now.");
                    break;
                default:
            }
        }
        return canClose;
    }

    // perform the UI part of deciding if we can and should load
    void loadFile() {
        // load the file, handle the headers and data, and populate the controls based on the available content
        // open the selected file, load the contents, and populate the UI
        if (checkCanClose()) {
            doLoad(); // get contents of file

            popUiFromBank(); // populate controls
        }
    }

// perform the loading and processing part
// a bank is 5 presets, a preset is a collection of 3 modes, a mode is a collection of 4 positions, a position is a collection of 3 slots
    void doLoad() {
        String fileName = txtFileName.getText();
        String rtnVal = "";
        String wholeFile = "";
        String bankName = "";

        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                int lineNum = 0;
                String line;
                String[] tspl;
                // parse the file.
                // first line must be "bankname=name", no quotes, case-insensitive
                // # comments any line
                // data must consist of at least one preset.
                while ((line = reader.readLine()) != null) {
                    //tspl = line.split("#"); // ignore all to the right of the # sign on all lines
                    //line = tspl[0];
                    if (!line.startsWith("#")) { // not a comment
                        lineNum++;
                        String testline = line.toLowerCase();
                        // first line must be bank name
                        switch (lineNum) {
                            case 1:
                                if (testline.startsWith("bankname=")) {
                                    bankName = line.substring(9);
                                    lblBankName.setText(bankName);
                                    System.out.println("bank name=" + bankName);
                                } else {
                                    System.out.println("bank read error:  first line must be 'bankname=nameofyourbankhere'");
                                    //status="error";
                                    return;
                                    // handle error more later
                                }
                                break;
                            default: {
                                wholeFile += line; // comments are lines that start with the # sign.                                    
                            }

                        }
                    }
                }
                reader.close();
            }
            wholeFile = wholeFile.trim();  //remove extra whitespace at end if present   

        } catch (IOException e) {
            System.err.format("Exception occurred trying to read bank '%s'.", fileName);
            System.err.println("Error Loading bank file " + fileName + " " + e);
        }

        // file is loaded, do headers, populate numeric preset, and txt buffer
        int preset; // address a preset number
        // check for existence and strip sysex headers and/or api command+argument info.  If there is a preset, or a bank, or a slot in the file????

        //String 
        // convert the buffer into an array of actual numbers represented by the hex text in the file.
        numericPreset = convertTextToIntArr(wholeFile); // this has to have the comments removed first?
        //convertTextToIntArr
        boolean isSysexPresent = false;
        boolean isEffigyMMAHeaderPresent = false;
// check for sysex header stuff
        // requiring the sysex header isn't necessary here, but we need to check and insert it before sending.  Maybe store the header and footer separately.
        int header = 0; // number of bytes to skip in header
        int bufAddr = 4; // = header + vendor id; // where the api cmd starts
        //int apicmd = 0; // what is the command to do ***************** left off here??

        // check for an F0 in the first byte, representing a sysex message header (0xF0)
        if (isSysexHeaderPresent(wholeFile)) {
            System.out.println("found Sysex header");
            isSysexPresent = true;
        } else {
            System.out.println("File " + txtFileName.getText() + " is not a sysex message, F0 not present in byte 1.");
            txtActionStatus.setText("File " + txtFileName.getText() + " is not a sysex message, F0 not present in byte 1.");
            return;
        }

        // and just to make sure, check for the sysex message delimiter at the end (0xF7)
        if (isSysexFooterPresent(wholeFile)) {
            System.out.println("found Sysex footer");
            isSysexPresent = true;
        } else {
            System.out.println("File is not a sysex message, F7 not present in last byte");
            txtActionStatus.setText("File " + txtFileName.getText() + " is not a sysex message, F7 not present in last byte");
            return;
        }

        // gatekeeper - file is correct sysex header
        // header and mma vendor id is present.
        int apicmd = numericPreset[bufAddr++];
        //comApiCmd.setSelectedIndex(apicmd);
        switch (apicmd) {
            case 0: // set preset in pedal
            case 3: // retrieve preset from pedal

                comPreset.setValue(numericPreset[bufAddr++]);

                arg1 = "0" + comPreset.getValue(); // put the preset in the argument text box
                break;

            case 1:  // set single value in pedal??
                comPreset.setValue(numericPreset[bufAddr++]);
                comMode.setValue(numericPreset[bufAddr++]);
                //          comPos.setSelectedIndex(numericPreset[bufAddr++]);
                //          comSlot.setSelectedIndex(numericPreset[bufAddr++]);                
//?                //setSelectedSlot(numericPreset[bufAddr++]);
                //txtAttr.setSelectedIndex(bufAddr++);
                attributeVal = String.valueOf(numericPreset[bufAddr++]); // **** tag maybe wrong or incomplete, maybe set the mainbank value?

                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter(sb, Locale.US);
                //SlotAddress sa = new SlotAddress((int) comPreset.getValue(),comMode.getSelectedIndex(),comPos.getSelectedIndex(),getSelectedSlot());
                //cmdTxt+=sa.toString();  // put the slot address in the slot address text field
                //cmdTxt+=" ";
                //indAttr = formatter.format("%02x",Integer.parseInt(attributeVal)); // the slot data is what gets made, the other tools help it, so doesn't matter if there is nothing in the attr val
                byte attrv = (byte) Integer.parseInt(attributeVal);
                //        mainBank.preset[(int) comPreset.getValue()].mode[comMode.getSelectedIndex()].pos[comPos.getSelectedIndex()].slot[comSlot.getSelectedIndex()].setAttrVal(indAttr, attrv);
                break;
            case 2: // it's a preset
                preset = numericPreset[bufAddr++]; // next is the api cmd
                for (int m = 0; m < 3; m++) {
                    for (int p = 0; p < 4; p++) {
                        for (int s = 0; s < 3; s++) {
                            try {
                                mainBank.preset[preset].mode[m].pos[p].slot[s].MidiCommand = numericPreset[bufAddr++];
                                if (mainBank.preset[preset].mode[m].pos[p].slot[s].MidiCommand > 0) {
                                    mainBank.preset[preset].mode[m].pos[p].slot[s].MidiChannel = numericPreset[bufAddr++];
                                    mainBank.preset[preset].mode[m].pos[p].slot[s].MidiSubCommand = numericPreset[bufAddr++];
                                    mainBank.preset[preset].mode[m].pos[p].slot[s].curvetype = numericPreset[bufAddr++];
                                    mainBank.preset[preset].mode[m].pos[p].slot[s].curvedirection = numericPreset[bufAddr++];
                                    mainBank.preset[preset].mode[m].pos[p].slot[s].min = numericPreset[bufAddr++];
                                    mainBank.preset[preset].mode[m].pos[p].slot[s].max = numericPreset[bufAddr++];
                                    mainBank.preset[preset].mode[m].pos[p].slot[s].latching = numericPreset[bufAddr++];
                                }
                            } catch (Exception e) {
                                System.out.println("Error loading preset from file:" + e);
                                txtActionStatus.setText("Error loading preset from file:" + e);
                            }
                        }
                    }
                }
                System.out.println("loaded preset " + preset + ".");
                String xxx = mainBank.preset[preset].toString();
                txtBuffer = xxx;
                System.out.println("preset text='" + xxx + "'");
                break;

            case 4: // cmd to get slot from pedal so we are looking at a slotaddres (we don't have the data so just the address)

                comPreset.setValue(numericPreset[bufAddr++]);
                comMode.setValue(numericPreset[bufAddr++]);
                //comMode.setSelectedIndex(numericPreset[bufAddr++]);
                //           comPos.setSelectedIndex(numericPreset[bufAddr++]);
                //           comSlot.setSelectedIndex(numericPreset[bufAddr++]);
                //setSelectedSlot(numericPreset[bufAddr++]);
                break;

            case 5: // it's a slot
                // load slot
                SlotAddress sa = new SlotAddress(numericPreset[bufAddr++], numericPreset[bufAddr++], numericPreset[bufAddr++], numericPreset[bufAddr++]);
                // load the slot here ****************************************8
                mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].MidiCommand = numericPreset[bufAddr++];
                if (mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].MidiCommand > 0) {
                    mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].MidiChannel = numericPreset[bufAddr++];
                    mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].MidiSubCommand = numericPreset[bufAddr++];
                    mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].curvetype = numericPreset[bufAddr++];
                    mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].curvedirection = numericPreset[bufAddr++];
                    mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].min = numericPreset[bufAddr++];
                    mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].max = numericPreset[bufAddr++];
                    mainBank.preset[sa.preset].mode[sa.mode].pos[sa.position].slot[sa.slot].latching = numericPreset[bufAddr++];
                    System.out.println("loaded slot " + sa.toString());

                } else {
                    System.out.println("empty slot " + sa.toString());
                }

                txtSlotData = sa.toString();
                // txtBuffer.setText(sa.toString()); I don't think so
                break;

            case 18: // it's a bank
                mainBank.bankName = bankName;
                //preset = numericPreset[bufAddr++]; // next is the api cmd
                for (int pr = 0; pr < 5; pr++) {
                    for (int m = 0; m < 3; m++) {
                        for (int p = 0; p < 4; p++) {
                            for (int s = 0; s < 3; s++) {
                                try {
                                    mainBank.preset[pr].mode[m].pos[p].slot[s].MidiCommand = numericPreset[bufAddr++];
                                    if (mainBank.preset[pr].mode[m].pos[p].slot[s].MidiCommand > 0) {
                                        mainBank.preset[pr].mode[m].pos[p].slot[s].MidiChannel = numericPreset[bufAddr++];
                                        mainBank.preset[pr].mode[m].pos[p].slot[s].MidiSubCommand = numericPreset[bufAddr++];
                                        mainBank.preset[pr].mode[m].pos[p].slot[s].curvetype = numericPreset[bufAddr++];
                                        mainBank.preset[pr].mode[m].pos[p].slot[s].curvedirection = numericPreset[bufAddr++];
                                        mainBank.preset[pr].mode[m].pos[p].slot[s].min = numericPreset[bufAddr++];
                                        mainBank.preset[pr].mode[m].pos[p].slot[s].max = numericPreset[bufAddr++];
                                        mainBank.preset[pr].mode[m].pos[p].slot[s].latching = numericPreset[bufAddr++];
                                    }
                                } catch (Exception e) {
                                    System.out.println("Error loading preset from file:" + e);
                                    txtActionStatus.setText("Error loading preset from file:" + e);
                                }
                            }
                        }
                    }
                }
                System.out.println("loaded bank " + bankName + ".");
                String yyy = mainBank.toString();
                txtBuffer = yyy;
                System.out.println("bank text='" + yyy + "'");
                break;

            case 6:
                comMode.setValue(bufAddr++);
                arg1 = "0" + String.valueOf(comMode.getValue()); // put the preset in the argument text box

                break;

            case 12: // change all channels, for now, too...
            // but later maybe do something fancier like actually change all teh channels in the loaded bank/preset with this comand???
            // let's not get too fancy.  

            case 8:
            case 10:
            case 13:
            case 14:
            case 15:
                //comMode.setSelectedIndex(bufAddr++);
                arg1 = "0" + String.valueOf(numericPreset[bufAddr++]); // put the preset in the argument text box
                break;

            // no arguments = nothing to set since the combo box is already set    
            case 7: // hit mode swith
            case 9: // recalibrate w/dflt#samples
            case 11: // rebooot
            default:
                arg1 = ""; // put the preset in the argument text box

            // perhaps for the rest, move the apicmd combo to the command, set the preset, ettc.
            //System.out.println("File does not contain an Effigy Sysex message command using preset or slot data.  Currently only these are supported. ");
            // it's not a download preset or fault api cmd so we really shouldn't do anything further      }
        }
        txtActionStatus.setText("Loaded file " + txtFileName.getText()); // move to end of more loading ?? ******* tag
    }

    void saveFile() {

        // we're saving the "main Bank"
        // re-inject the sysex headers and api info, and sysex footerr 
        String fullFile = mainBank.toString(); // we're saving the main Bank, this is the contents
        if (!isSysexHeaderPresent(fullFile)) {
            fullFile = sysexHeaderTxt + "12 " + fullFile;
            //System.out.println("fullFile="+fullFile);
        }
        if (!isSysexFooterPresent(fullFile)) {
            //System.out.println("adding sysex footer");
            fullFile = fullFile + sysexDelimiterTxt;
        }

        String ofn = txtFileName.getText();
        if (extDir.isEmpty()) {
            ofn = dataDir + lblBankName.getText() + ".ecp";
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ofn), "utf-8"))) {
            if (lblBankName.getText().trim().equalsIgnoreCase("pedal bank")) {
                //private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

                writer.write("#Pedal Bank - Autogenerated " + sdf.format(date.getTime()) + "\n");
                writer.write("#Effigy Labs LLC\n");
                writer.write("#This file is overwritten - do not modify, all changes will be lost.\n");
                writer.write("#To properly save this file, save to another file with your OS in the control panel.\n");
            } else {
                // write soething else
                Date date = new Date();
                DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

                writer.write("#Bank " + lblBankName.getText() + " - saved " + sdf.format(date.getTime()) + "\n");
                writer.write("#Effigy Labs LLC\n");
            }
            writer.write("bankName=" + lblBankName.getText() + "\n");
            writer.write(fullFile);
            writer.close();
            //System.out.println("Saved file "+fileName);
            //status = "Saved bank file.";
        } catch (UnsupportedEncodingException ex) {
            System.err.println("Error saving bank file.  " + ex.toString());
            Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            //status = "Error saving file";
            Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private boolean isSysexHeaderPresent(int[] stray) {
        return isSysexHeaderPresent(convertNumericPresetToText(stray));
    }

    private boolean isSysexHeaderPresent(byte[] stray) {
        return isSysexHeaderPresent(convertbytePresetToText(stray));
    }

    private boolean isSysexFooterPresent(int[] stray) {
        return isSysexFooterPresent(convertNumericPresetToText(stray));
    }

    private boolean isSysexFooterPresent(byte[] stray) {
        return isSysexFooterPresent(convertbytePresetToText(stray));
    }

    private boolean isSysexHeaderPresent(String stray) {
        //System.out.println("isSysexHeaderPresent");
        //System.out.println("str len="+stray.length());
        boolean rtnval = true;
        if (!nonNull(stray)) {
            return false;
        }
        if (stray.isEmpty()) {
            return false;
        }
        if (stray.length() < 4) {
            return false;
        }

        try {
            int[] bufTest = new int[4];
            String p = "([0-9,A-F,a-f]+)"; // look for hex characters
            Pattern pattern = Pattern.compile(p);
            Matcher matcher = pattern.matcher(stray);
            int gct = 0;
            while (matcher.find() && gct < 4) {
                String gn = matcher.group();
                //System.out.println("group "+gct+": "+gn);
                int ng = Integer.parseInt(gn, 16);
                //System.out.println("["+gct+"]="+ng);
                bufTest[gct] = ng;
                //System.out.print("gct="+gct);
                gct++;
            }
            if (gct < 3) {
                //System.out.println("nope, len="+gct) ;
                return false;

            }

            if (bufTest[0] != 0xF0) {
                return false;
            }
            if (bufTest[1] != 0x00) {
                return false;
            }
            if (bufTest[2] != 0x02) {
                return false;
            }
            if (bufTest[3] != 0x21) {
                return false;
            }

        } // regex match the group. 
        catch (Exception e) {
            System.out.println("Problem finding the numbers in the text: " + e);
        }

        return rtnval;
    }

    private boolean isSysexFooterPresent(String stray) {
        //System.out.println("isSysexFooterPresent, stray="+stray);
        boolean rtnval = true;
        if (!nonNull(stray)) {
            return false;
        }
        if (stray.isEmpty()) {
            return false;
        }
        if (stray.length() < 2) {
            return false;
        }

        try {
            String p = "([0-9,A-F,a-f]+)\\Z"; // look for hex characters
            Pattern pattern = Pattern.compile(p);
            Matcher matcher = pattern.matcher(stray);
            int gct = 0;
            while (matcher.find() && gct < 1) {
                //System.out.println("matcher.end="+matcher.end());
                //int gn = matcher.end();
                //System.out.println("group "+gct+": "+gn);
                int ng = Integer.parseInt(matcher.group(gct), 16);
                //System.out.println("["+gct+"]="+ng);
                if (ng != 0xF7) {
                    return false;
                }
                gct++;
            }

        } // regex match the group. 
        catch (Exception e) {
            System.out.println("Problem finding the numbers in the text: " + e);
        }
        return rtnval;
    }

    public byte[] convertTextToByteArr(String cmdTxt) {
        //System.out.println("convertTextToByteArr, cmdTxt="+cmdTxt);
        // make a temporary array until we know the final length
        int bufSz = cmdTxt.length(); // calculate final size from txt size, and handle for incomplete input.  3 text bytes (2 value bytes and a space) for evry 1 output byte.
        byte[] buffTmp = new byte[bufSz];
        //System.out.println("buffTmp sz="+buffTmp.length);
        //  the final result, declared here for scope but not sized yet 
        byte[] buffRtn = null;
        boolean wsf = false;

        // regex stuff
        String p = "([0-9,A-F,a-f]{2})"; // look for hex characters
        Pattern pattern = Pattern.compile(p);
        //System.out.println("buff="+buff);
        try {
            Matcher matcher = pattern.matcher(cmdTxt);
            int gct = 0;
            //matcher..
            //int ign = matcher.groupCount();
            while (matcher.find()) {
                String gn = matcher.group();
                //System.out.println("group "+gct+": "+gn);
                int ng = Integer.parseInt(gn, 16);
                //System.out.println("["+gct+"]="+ng);
                buffTmp[gct] = (byte) ng;
                //System.out.print(gct+" ");
                gct++;
            }
            //System.out.println("gct="+gct);
            buffRtn = new byte[gct];
            for (int c = 0; c < gct; c++) {
                //System.out.println("c="+c);
                buffRtn[c] = (byte) buffTmp[c];
            }
            //System.arraycopy(buffTmp, 0, buffRtn, 0, gct); //return buffPreset; - do not use, arraycopy is broken for this type of copy
            //System.out.println("rtnSz="+gct);
            return buffRtn;

        } // regex match the group. 
        catch (Exception e) {
            System.out.println("a Problem finding the numbers in the text: " + e);
        }
        return buffRtn; // should never get here
    }

    // get data out of the slot and position controls to rebuild the slot, position, mode,  preset, and bank 
    // prototypical slot data rebuid.  return a byte array, and update the text slot address text, for now
    byte[] genPos1Data() {
        byte[] rtnval;
        byte[] emptyval = new byte[1];
        emptyval[0] = (byte) 0x00;
        if (comMidiCmdPos1.getSelectedIndex() == 0) { // empty slot 
            txtSlotData = "00";
            txtActionStatus.setText("Slot empty");
            txtBuffer = mainBank.toString();
            emptyval[0] = 0x00;
            return emptyval;
        }

        // slotvals is where the data from the slot goes from the controls
        int[] slotvals = new int[8];
        String rtnStr = "";

        // set the midi command combo box to the correct item - translate the value into the index. The first TWO characters can form the command, making it
        // possible to have 256 commands, not just 16.
        // midi command
        String mc = comMidiCmdPos1.getSelectedItem().toString().substring(0, 1); // allow 1st 2 chars to be the api cmd for cmds >> 0x0F 
        slotvals[0] = Integer.parseInt(mc, 16);

        // midi channel
        slotvals[1] = Integer.parseInt(spnMidiChannelPos1.getValue().toString()) - 1;

        // subcommand
        if (comMidiSubCommandPos1.getSelectedIndex() < 0) {
            slotvals[2] = 0; // set tot submd to empty
        } else {
            slotvals[2] = comMidiSubCommandPos1.getSelectedIndex();
        }

        // curve type
        if (comCurveTypePos1.getSelectedIndex() < 0) {
            slotvals[3] = 0;
        } else {
            slotvals[3] = comCurveTypePos1.getSelectedIndex();
        }

        // curve direction
        if (radCurvePosPos1.isSelected()) {
            slotvals[4] = 1;
        } else {
            slotvals[4] = 0;
        }

        // min
        slotvals[5] = Integer.parseInt(String.valueOf(spnMinRangePos1.getValue()));

        // max
        slotvals[6] = Integer.parseInt(String.valueOf(spnMaxRangePos1.getValue()));

        // latching
        if (latchingPos1.isSelected()) {
            slotvals[7] = 1;
        } else {
            slotvals[7] = 0;
        }

        // move all this stuff to the slot class and have it just tostring itselfr
        // format into nice double-hex characters for each byte separated by a space
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        for (int sc = 0; sc < slotvals.length; sc++) {
            formatter.format("%02x", slotvals[sc]);  // convert to hex
            formatter.format(" ");
        } // iterate through slot values
        System.out.println("built slot " + sb);

        String tsd = sb.toString().toUpperCase(); // put in uppercase coz I like it like that
        txtSlotData = tsd; // put in uppercase coz I like it like that            
        //txtBuffer.setText(mainBank.preset[(int) comPreset.getValue()].toString());

        // convert to byte array for output.
        int[] tna = convertTextToIntArr(tsd);
        rtnval = convertNumericPresetToByte(tna);
        return rtnval;
    } // genPos1Data

    byte[] genPos2Data() {
        byte[] rtnval;
        byte[] emptyval = new byte[1];
        emptyval[0] = (byte) 0x00;
        if (comMidiCmdPos2.getSelectedIndex() == 0) { // empty slot 
            txtSlotData = "00";
            txtActionStatus.setText("Slot empty");
            txtBuffer = mainBank.toString();
            emptyval[0] = 0x00;
            return emptyval;
        }

        // slotvals is where the data from the slot goes from the controls
        int[] slotvals = new int[8];
        String rtnStr = "";

        // set the midi command combo box to the correct item - translate the value into the index. The first TWO characters can form the command, making it
        // possible to have 256 commands, not just 16.
        // midi command
        String mc = comMidiCmdPos2.getSelectedItem().toString().substring(0, 1); // allow 1st 2 chars to be the api cmd for cmds >> 0x0F 
        slotvals[0] = Integer.parseInt(mc, 16);

        // midi channel
        slotvals[1] = Integer.parseInt(spnMidiChannelPos2.getValue().toString()) - 1;

        // subcommand
        if (comMidiSubCommandPos2.getSelectedIndex() < 0) {
            slotvals[2] = 0; // set tot submd to empty
        } else {
            slotvals[2] = comMidiSubCommandPos2.getSelectedIndex();
        }

        // curve type
        if (comCurveTypePos2.getSelectedIndex() < 0) {
            slotvals[3] = 0;
        } else {
            slotvals[3] = comCurveTypePos2.getSelectedIndex();
        }

        // curve direction
        if (radCurvePosPos2.isSelected()) {
            slotvals[4] = 1;
        } else {
            slotvals[4] = 0;
        }

        // min
        slotvals[5] = Integer.parseInt(String.valueOf(spnMinRangePos2.getValue()));

        // max
        slotvals[6] = Integer.parseInt(String.valueOf(spnMaxRangePos2.getValue()));

        // latching
        if (latchingPos2.isSelected()) {
            slotvals[7] = 1;
        } else {
            slotvals[7] = 0;
        }

        // move all this stuff to the slot class and have it just tostring itselfr
        // format into nice double-hex characters for each byte separated by a space
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        for (int sc = 0; sc < slotvals.length; sc++) {
            formatter.format("%02x", slotvals[sc]);  // convert to hex
            formatter.format(" ");
        } // iterate through slot values
        System.out.println("built slot " + sb);

        String tsd = sb.toString().toUpperCase(); // put in uppercase coz I like it like that
        txtSlotData = tsd; // put in uppercase coz I like it like that            
        //txtBuffer.setText(mainBank.preset[(int) comPreset.getValue()].toString());

        // convert to byte array for output.
        int[] tna = convertTextToIntArr(tsd);
        rtnval = convertNumericPresetToByte(tna);
        return rtnval;
    } // genPos2Data

    byte[] genPos3Data() {
        byte[] rtnval;
        byte[] emptyval = new byte[1];
        emptyval[0] = (byte) 0x00;
        if (comMidiCmdPos3.getSelectedIndex() == 0) { // empty slot 
            txtSlotData = "00";
            txtActionStatus.setText("Slot empty");
            txtBuffer = mainBank.toString();
            emptyval[0] = 0x00;
            return emptyval;
        }

        // slotvals is where the data from the slot goes from the controls
        int[] slotvals = new int[8];
        String rtnStr = "";

        // set the midi command combo box to the correct item - translate the value into the index. The first TWO characters can form the command, making it
        // possible to have 256 commands, not just 16.
        // midi command
        String mc = comMidiCmdPos3.getSelectedItem().toString().substring(0, 1); // allow 1st 2 chars to be the api cmd for cmds >> 0x0F 
        slotvals[0] = Integer.parseInt(mc, 16);

        // midi channel
        slotvals[1] = Integer.parseInt(spnMidiChannelPos3.getValue().toString()) - 1;

        // subcommand
        if (comMidiSubCommandPos3.getSelectedIndex() < 0) {
            slotvals[2] = 0; // set tot submd to empty
        } else {
            slotvals[2] = comMidiSubCommandPos3.getSelectedIndex();
        }

        // curve type
        if (comCurveTypePos3.getSelectedIndex() < 0) {
            slotvals[3] = 0;
        } else {
            slotvals[3] = comCurveTypePos3.getSelectedIndex();
        }

        // curve direction
        if (radCurvePosPos3.isSelected()) {
            slotvals[4] = 1;
        } else {
            slotvals[4] = 0;
        }

        // min
        slotvals[5] = Integer.parseInt(String.valueOf(spnMinRangePos3.getValue()));

        // max
        slotvals[6] = Integer.parseInt(String.valueOf(spnMaxRangePos3.getValue()));

        // latching
        if (latchingPos3.isSelected()) {
            slotvals[7] = 1;
        } else {
            slotvals[7] = 0;
        }

        // move all this stuff to the slot class and have it just tostring itselfr
        // format into nice double-hex characters for each byte separated by a space
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        for (int sc = 0; sc < slotvals.length; sc++) {
            formatter.format("%02x", slotvals[sc]);  // convert to hex
            formatter.format(" ");
        } // iterate through slot values
        System.out.println("built slot " + sb);

        String tsd = sb.toString().toUpperCase(); // put in uppercase coz I like it like that
        txtSlotData = tsd; // put in uppercase coz I like it like that            
        //txtBuffer.setText(mainBank.preset[comPreset.getSelectedIndex()].toString());

        // convert to byte array for output.
        int[] tna = convertTextToIntArr(tsd);
        rtnval = convertNumericPresetToByte(tna);
        return rtnval;
    } // genPos3Data

    byte[] genPos4Data() {
        byte[] rtnval;
        byte[] emptyval = new byte[1];
        emptyval[0] = (byte) 0x00;
        if (comMidiCmdPos4.getSelectedIndex() == 0) { // empty slot 
            txtSlotData = "00";
            txtActionStatus.setText("Slot empty");
            txtBuffer = mainBank.toString();
            emptyval[0] = 0x00;
            return emptyval;
        }

        // slotvals is where the data from the slot goes from the controls
        int[] slotvals = new int[8];
        String rtnStr = "";

        // set the midi command combo box to the correct item - translate the value into the index. The first TWO characters can form the command, making it
        // possible to have 256 commands, not just 16.
        // midi command
        String mc = comMidiCmdPos4.getSelectedItem().toString().substring(0, 1); // allow 1st 2 chars to be the api cmd for cmds >> 0x0F 
        slotvals[0] = Integer.parseInt(mc, 16);

        // midi channel
        slotvals[1] = Integer.parseInt(spnMidiChannelPos4.getValue().toString()) - 1;

        // subcommand
        if (comMidiSubCommandPos4.getSelectedIndex() < 0) {
            slotvals[2] = 0; // set tot submd to empty
        } else {
            slotvals[2] = comMidiSubCommandPos4.getSelectedIndex();
        }

        // curve type
        if (comCurveTypePos4.getSelectedIndex() < 0) {
            slotvals[3] = 0;
        } else {
            slotvals[3] = comCurveTypePos4.getSelectedIndex();
        }

        // curve direction
        if (radCurvePosPos4.isSelected()) {
            slotvals[4] = 1;
        } else {
            slotvals[4] = 0;
        }

        // min
        slotvals[5] = Integer.parseInt(String.valueOf(spnMinRangePos4.getValue()));

        // max
        slotvals[6] = Integer.parseInt(String.valueOf(spnMaxRangePos4.getValue()));

        // latching
        if (latchingPos4.isSelected()) {
            slotvals[7] = 1;
        } else {
            slotvals[7] = 0;
        }

        // move all this stuff to the slot class and have it just tostring itselfr
        // format into nice double-hex characters for each byte separated by a space
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        for (int sc = 0; sc < slotvals.length; sc++) {
            formatter.format("%02x", slotvals[sc]);  // convert to hex
            formatter.format(" ");
        } // iterate through slot values
        System.out.println("built slot " + sb);

        String tsd = sb.toString().toUpperCase(); // put in uppercase coz I like it like that
        txtSlotData = tsd; // put in uppercase coz I like it like that            
        //txtBuffer.setText(mainBank.preset[comPreset.getSelectedIndex()].toString());

        // convert to byte array for output.
        int[] tna = convertTextToIntArr(tsd);
        rtnval = convertNumericPresetToByte(tna);
        return rtnval;
    } // genPos4Data

    private byte[] genPosData(javax.swing.JComboBox comMidiCmd,
            javax.swing.JSpinner spnMidiChannel,
            javax.swing.JComboBox comMidiSubCommand,
            javax.swing.JComboBox comCurveType,
            javax.swing.JRadioButton radCurvePosDirection,
            javax.swing.JRadioButton radCurveNegDirection,
            javax.swing.JSpinner spnMin,
            javax.swing.JSpinner spnMax,
            javax.swing.JCheckBox chkLatching
    ) {
        byte[] rtnval;
        byte[] emptyval = new byte[1];
        emptyval[0] = (byte) 0x00;
        if (comMidiCmdPos4.getSelectedIndex() == 0) { // empty slot 
            txtSlotData = "00";
            txtActionStatus.setText("Slot empty");
            txtBuffer = mainBank.toString();
            emptyval[0] = 0x00;
            return emptyval;
        }

        // slotvals is where the data from the slot goes from the controls
        int[] slotvals = new int[8];
        String rtnStr = "";

        // set the midi command combo box to the correct item - translate the value into the index. The first TWO characters can form the command, making it
        // possible to have 256 commands, not just 16.
        // midi command
        String mc = comMidiCmdPos4.getSelectedItem().toString().substring(0, 1); // allow 1st 2 chars to be the api cmd for cmds >> 0x0F 
        slotvals[0] = Integer.parseInt(mc, 16);

        // midi channel
        slotvals[1] = Integer.parseInt(spnMidiChannelPos4.getValue().toString()) - 1;

        // subcommand
        if (comMidiSubCommandPos4.getSelectedIndex() < 0) {
            slotvals[2] = 0; // set tot submd to empty
        } else {
            slotvals[2] = comMidiSubCommandPos4.getSelectedIndex();
        }

        // curve type
        if (comCurveTypePos4.getSelectedIndex() < 0) {
            slotvals[3] = 0;
        } else {
            slotvals[3] = comCurveTypePos4.getSelectedIndex();
        }

        // curve direction
        if (radCurvePosPos4.isSelected()) {
            slotvals[4] = 1;
        } else {
            slotvals[4] = 0;
        }

        // min
        slotvals[5] = Integer.parseInt(String.valueOf(spnMinRangePos4.getValue()));

        // max
        slotvals[6] = Integer.parseInt(String.valueOf(spnMaxRangePos4.getValue()));

        // latching
        if (latchingPos4.isSelected()) {
            slotvals[7] = 1;
        } else {
            slotvals[7] = 0;
        }

        // move all this stuff to the slot class and have it just tostring itselfr
        // format into nice double-hex characters for each byte separated by a space
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        for (int sc = 0; sc < slotvals.length; sc++) {
            formatter.format("%02x", slotvals[sc]);  // convert to hex
            formatter.format(" ");
        } // iterate through slot values
        System.out.println("built slot " + sb);

        String tsd = sb.toString().toUpperCase(); // put in uppercase coz I like it like that
        txtSlotData = tsd; // put in uppercase coz I like it like that            
        //txtBuffer.setText(mainBank.preset[comPreset.getSelectedIndex()].toString());

        // convert to byte array for output.
        int[] tna = convertTextToIntArr(tsd);
        rtnval = convertNumericPresetToByte(tna);
        return rtnval;
    } // genPosData

    String convertbytePresetToText(byte[] arrayToConvert) {
        String returnStr = "";
        if (arrayToConvert.length == 0) {
            return returnStr;
        }

        for (int lc = 0; lc < arrayToConvert.length; lc++) {
            returnStr += (String.format("%02X", arrayToConvert[lc]));
            //returnStr.concat(arrayToConvert[lc].
            returnStr += " ";
        }
        return returnStr;
    }

    String convertNumericPresetToText(int[] arrayToConvert) {
        String returnStr = "";
        for (int lc = 0; lc < arrayToConvert.length; lc++) {
            returnStr += String.format("%02X ", arrayToConvert[lc]);
            //returnStr.concat(arrayToConvert[lc].
        }
        return returnStr.trim().toUpperCase();
    }

    byte[] convertNumericPresetToByte(int[] arrayToConvert) {
        // assume everything and nothing
        byte[] rtnVal = new byte[arrayToConvert.length];
        for (int c = 0; c < arrayToConvert.length; c++) {
            rtnVal[c] = (byte) arrayToConvert[c];
        }
        return rtnVal;
    }

    int[] convertTextToIntArr(String txtToConvert) {
        byte[] brtn = convertTextToByteArr(txtToConvert);
        //System.out.println("txt input length:"+txtToConvert.length());
        //System.out.println("byte arr len:"+brtn.length);
        int[] irtn = new int[brtn.length];
        //System.out.println("int arr len:"+irtn.length);

        for (int c = 0; c < brtn.length; c++) {
            irtn[c] = (int) brtn[c];
        }
        //System.arraycopy(brtrn, 0, irtn, 0, brtn.length); //return buffPreset;

        return irtn;

    }


    /*
  * popSubCmd populates the subcommand selector (combobox) based on the MIDI command selected:
  *
  *MIDI - CMD Desc                  - (Implementor) SubCommand
  *  8 - NoteOn                     - none
  *  9 - NoteOff                    - none
  *  A - Aftertouch (polyphonic)    - none
  *  B - Control Change             - (MIDI) CC0-127
  *  C - Program Change             - (MIDI) Pgm 0-127
  *  D - Aftertouch (channel)       - none
  *  E - Pitch Bend                 - (Effigy) Pitch Direction 0=down 1=up
  *
  * Note the subcommand is not the value.  All the "none"s still have the input
  * value from the sensors properly supplied in the MIDI output
  *
  * input: text of the selected item
  * the subcommnd object to populate
  * once the subcommand object is populated, the value to set it to
     */
//void popSubCmd(int selItem, javax.swing.JComboBox comSub, int setVal) {
    void popSubCmd(int selItem, javax.swing.JComboBox comSub) {
        System.out.print("popSubCmd(selItem=" + selItem + ",getName()=" + comSub.getName() + "),cmd=");
        //Ensure the contents of teh subcommand agree with the command:
        // if the command is zero, so musst be the subcommand regardless of anythig that is sent!
        comSub.removeAllItems();
        switch (selItem) {
            // do a described 0-127
            case 11: { // 0x0B = 12, the midi code for a control change
                System.out.println("Sub=CC");
                CCarr ccarr = new CCarr();
                for (int ccl = 0; ccl < 128; ccl++) {
                    comSub.addItem(ccarr.CCarr[ccl]);
                }
            } // case 11
            break;

            // do a simple 0-127 0x0C
            case 12: { // program change, just provide a channel #.  positional input only sends the program change one time so latching is recommended for this code.
                System.out.println("Sub=Program Change");
                for (int pgmchg = 0; pgmchg < 128; pgmchg++) {
                    comSub.addItem(Integer.toString(pgmchg));
                }

            } // case 12
            break;

            case 14: { // 0x0E = 14, the midi code for pitch bend
                System.out.println("Sub=Pitch");
                comSub.addItem("Pitch Down");
                //System.out.println("added down");
                comSub.addItem("Pitch Up");
                //System.out.println("added up");
                //         if (setVal > 1) setVal = 1; // this will be used to set to the second value "pitch up" in the combo

            } // case 14
            break;
            default: { // pro forma for clarity
                // nothing really since the sub is already clear
            }
            break;

        }// switch midicmd
    } // pop subcmd

// prototypical popUiFromBank passing the position's controls
    void popUiFromBank(int pos,
            javax.swing.JComboBox comMidiCmd,
            javax.swing.JSpinner spnMidiChannel,
            javax.swing.JComboBox comMidiSubCommand,
            javax.swing.JComboBox comCurveType,
            javax.swing.JRadioButton radCurvePosDirection,
            javax.swing.JRadioButton radCurveNegDirection,
            javax.swing.JSpinner spnMin,
            javax.swing.JSpinner spnMax,
            javax.swing.JCheckBox chkLatching) {
        // get the midi command for each position
        int posMidiCmd = mainBank.preset[(int) comPreset.getValue()].mode[(int) comMode.getValue()].pos[pos].slot[getSelectedSlot(pos)].MidiCommand; // slot attribute addressability
        String P = Integer.toHexString(posMidiCmd);
        // set the controls ************ maybe redo this a better way, if there is one?  don't make an endless loop       
        comMidiCmd.selectWithKeyChar((char) P.charAt(0)); // set the combo box item to be what the midi command in the preset is
        // set the midi sub command
        if (posMidiCmd > 0) { // position 1
            int poschannel = mainBank.preset[(int) comPreset.getValue()].mode[(int) comMode.getValue()].pos[pos].slot[getSelectedSlot(pos)].MidiChannel; // slot attribute addressability
            int possubcmd = mainBank.preset[(int) comPreset.getValue()].mode[(int) comMode.getValue()].pos[pos].slot[getSelectedSlot(pos)].MidiSubCommand; // slot attribute addressability
            int poscurvetype = mainBank.preset[(int) comPreset.getValue()].mode[(int) comMode.getValue()].pos[pos].slot[getSelectedSlot(pos)].curvetype; // slot attribute addressability
            int poscurvedir = mainBank.preset[(int) comPreset.getValue()].mode[(int) comMode.getValue()].pos[pos].slot[getSelectedSlot(pos)].curvedirection; // slot attribute addressability
            int posmin = mainBank.preset[(int) comPreset.getValue()].mode[(int) comMode.getValue()].pos[pos].slot[getSelectedSlot(pos)].min; // slot attribute addressability
            int posmax = mainBank.preset[(int) comPreset.getValue()].mode[(int) comMode.getValue()].pos[pos].slot[getSelectedSlot(pos)].max; // slot attribute addressability
            int poslatching = mainBank.preset[(int) comPreset.getValue()].mode[(int) comMode.getValue()].pos[pos].slot[getSelectedSlot(pos)].latching; // slot attribute addressability

            // put the items in the subcommand corresponding to the main midicommand andset the selected index to the value
            // put the items in the subcommand corresponding to the main midicommand and set the value.  the change in the command will cause the subcommand to be correctly populated, so as long as the command is changed before the subcommand is changed, the subcommand list will always be ready toa accept the correct value of the subcommand data value.
            int ct = comMidiSubCommand.getItemCount();
            if (ct > 0) { // bother only if there's something in it at the time, else gracefully walk away...just walk away.
                if (ct < possubcmd) {
                    possubcmd = ct - 1;
                }
                comMidiSubCommand.setSelectedIndex(possubcmd); // pitch up is position 1                
            }

            spnMidiChannel.setValue(poschannel + 1);
            comCurveType.setSelectedIndex(poscurvetype);

            switch (poscurvedir) {
                case 0: {
                    radCurveNegDirection.setSelected(true);
                }
                break;
                case 1: {
                    radCurvePosDirection.setSelected(true);
                }
                break;
                default: {
                    // no default action
                }
                break;

            }
            //radCurvePosSlot1.set   // figure out the radio buttons later
            spnMin.setValue(posmin);
            spnMax.setValue(posmax);

            //latchingSlot1.set // figure outt the checkbox later
            if (poslatching > 0) {
                chkLatching.setSelected(true);
            } else {
                chkLatching.setSelected(false);
            }
        } // position pos 
    }

// popslots puts data into the slot controls from the selected addresses in the preset/bank.  The genslot* methods get data from the controls
    void popUiFromBank() {
        popUiFromBank(99); // populate all normally
        return;
    } // pop UI from bank

    void popUiFromBank(int pos) {
        // pop just one position (like when a slot changer changed), or if pos = 99, do them all (called from popUiFromBank())
        // go through the four positions and populate the UI
        System.out.println("popUiFromBank(" + pos + ")");
        pedalQuiet = true;
        modelQuiet = true; // by this time we are initialized and can update from any change - ????? **** tag
        int preset = (int) comPreset.getValue();
        int mode = (int) comMode.getValue() - 1;
        // int q = comAttr.getselectedIndex(); // no
        CCarr ccarr = new CCarr(); // control code midi subcommand list 

        // the next section repeats the controls populations for each position.
        // int posx = pos;
        // if pos = 99 make it repeat
        if (pos == 0 | pos == 99) {
            // position 1
            spnMidiChannelPos1.setValue(0);
            comMidiSubCommandPos1.removeAllItems();
            comCurveTypePos1.setSelectedIndex(0);
            radCurveNegPos1.setSelected(false);
            radCurvePosPos1.setSelected(false);
            spnMinRangePos1.setValue(0);
            spnMaxRangePos1.setValue(0);
            latchingPos1.setSelected(false);

            // get the midi command for each position
            int pos1MidiCmd = mainBank.preset[preset].mode[mode].pos[0].slot[getSelectedSlot(0)].MidiCommand; // slot attribute addressability
            String P1 = Integer.toHexString(pos1MidiCmd);
            comMidiCmdPos1.selectWithKeyChar((char) P1.charAt(0)); // set the combo box item to be what the midi command in the preset is, this will cause it to set the subcommand as well

            if (pos1MidiCmd > 0) { // position 1
                int pos1channel = mainBank.preset[preset].mode[mode].pos[0].slot[getSelectedSlot(0)].MidiChannel; // slot attribute addressability
                int pos1subcmd = mainBank.preset[preset].mode[mode].pos[0].slot[getSelectedSlot(0)].MidiSubCommand; // slot attribute addressability
                int pos1curvetype = mainBank.preset[preset].mode[mode].pos[0].slot[getSelectedSlot(0)].curvetype; // slot attribute addressability
                int pos1curvedir = mainBank.preset[preset].mode[mode].pos[0].slot[getSelectedSlot(0)].curvedirection; // slot attribute addressability
                int pos1min = mainBank.preset[preset].mode[mode].pos[0].slot[getSelectedSlot(0)].min; // slot attribute addressability
                int pos1max = mainBank.preset[preset].mode[mode].pos[0].slot[getSelectedSlot(0)].max; // slot attribute addressability
                int pos1latching = mainBank.preset[preset].mode[mode].pos[0].slot[getSelectedSlot(0)].latching; // slot attribute addressability

                // channel
                spnMidiChannelPos1.setValue(pos1channel + 1);

                // subcommand
                // we must do a little work to translate the sub command to the combo box index value.
                //  popSubCmd preps the combobox for our value.
                switch (pos1MidiCmd) {
                    case 0: // do nothing, combo model will be empty
                    case 8: // noteOn
                    case 9: // noteOff
                    case 10: // noteOff
                    case 13: // aftertouch
                        break;
                    case 14: // pitch bend
                        int setVal = 0;
                        if (pos1subcmd > 0) {
                            setVal = 1; // limit values > 1 to index 1 (pitch up)
                        }
                        comMidiSubCommandPos1.setSelectedIndex(setVal);
                        break;
                    default:
                        System.out.println("pos1subcmd=" + pos1subcmd);
                        comMidiSubCommandPos1.setSelectedIndex(pos1subcmd);
                }

                // curve type
                comCurveTypePos1.setSelectedIndex(pos1curvetype);

                // curve direction
                switch (pos1curvedir) {
                    case 0: {
                        radCurveNegPos1.setSelected(true);
                    }
                    break;
                    case 1: {
                        radCurvePosPos1.setSelected(true);
                    }
                    break;
                    default: {
                        // no default action
                    }
                    break;
                }
                //radCurvePosSlot1.set   // figure out the radio buttons later
                spnMinRangePos1.setValue(pos1min);
                spnMaxRangePos1.setValue(pos1max);

                //latchingSlot1.set // figure outt the checkbox later
                if (pos1latching > 0) {
                    latchingPos1.setSelected(true);
                } else {
                    latchingPos1.setSelected(false);
                }
            } // position 1 
        }

        if (pos == 1 | pos == 99) {
            // position 2
            spnMidiChannelPos2.setValue(0);
            comMidiSubCommandPos2.removeAllItems();
            comCurveTypePos2.setSelectedIndex(0);
            radCurveNegPos2.setSelected(false);
            radCurvePosPos2.setSelected(false);
            spnMinRangePos2.setValue(0);
            spnMaxRangePos2.setValue(0);
            latchingPos2.setSelected(false);

            int pos2MidiCmd = mainBank.preset[preset].mode[mode].pos[1].slot[getSelectedSlot(1)].MidiCommand; // slot attribute addressability
            String P2 = Integer.toHexString(pos2MidiCmd);
            comMidiCmdPos2.selectWithKeyChar((char) P2.charAt(0)); // set the combo box item to be what the midi command in the preset is

            if (pos2MidiCmd > 0) { // position 2

                int pos2channel = mainBank.preset[preset].mode[mode].pos[1].slot[getSelectedSlot(1)].MidiChannel; // slot attribute addressability
                int pos2subcmd = mainBank.preset[preset].mode[mode].pos[1].slot[getSelectedSlot(1)].MidiSubCommand; // slot attribute addressability
                int pos2curvetype = mainBank.preset[preset].mode[mode].pos[1].slot[getSelectedSlot(1)].curvetype; // slot attribute addressability
                int pos2curvedir = mainBank.preset[preset].mode[mode].pos[1].slot[getSelectedSlot(1)].curvedirection; // slot attribute addressability
                int pos2min = mainBank.preset[preset].mode[mode].pos[1].slot[getSelectedSlot(1)].min; // slot attribute addressability
                int pos2max = mainBank.preset[preset].mode[mode].pos[1].slot[getSelectedSlot(1)].max; // slot attribute addressability
                int pos2latching = mainBank.preset[preset].mode[mode].pos[1].slot[getSelectedSlot(1)].latching; // slot attribute addressability

                // subcommand
                // we must do a little work to translate the sub command to the combo box index value.
                //  popSubCmd preps the combobox for our value.
                switch (pos2MidiCmd) {
                    case 0: // do nothing, combo model will be empty
                        break;
                    case 8: // noteOn
                    case 9: // noteOff
                    case 10: // noteOff
                    case 13: // aftertouch
                    case 14: // pitch bend
                        int setVal = 0;
                        if (pos2subcmd > 0) {
                            setVal = 1; // limit values > 1 to index 1 (pitch up)
                        }
                        comMidiSubCommandPos2.setSelectedIndex(setVal);
                        break;
                    default:
                        comMidiSubCommandPos2.setSelectedIndex(pos2subcmd);
                }

                spnMidiChannelPos2.setValue(pos2channel + 1);
                comCurveTypePos2.setSelectedIndex(pos2curvetype);

                switch (pos2curvedir) {
                    case 0: {
                        radCurveNegPos2.setSelected(true);
                    }
                    break;
                    case 1: {
                        radCurvePosPos2.setSelected(true);
                    }
                    break;
                    default: {
                        // no default action
                    }
                    break;

                }
                //radCurvePosSlot1.set   // figure out the radio buttons later
                spnMinRangePos2.setValue(pos2min);
                spnMaxRangePos2.setValue(pos2max);

                //latchingSlot1.set // figure outt the checkbox later
                if (pos2latching > 0) {
                    latchingPos2.setSelected(true);
                } else {
                    latchingPos2.setSelected(false);
                }
            } // position 2
        }

        if (pos == 2 | pos == 99) {

            // position 3
            spnMidiChannelPos3.setValue(0);
            comMidiSubCommandPos3.removeAllItems();
            comCurveTypePos3.setSelectedIndex(0);
            radCurveNegPos3.setSelected(false);
            radCurvePosPos3.setSelected(false);
            spnMinRangePos3.setValue(0);
            spnMaxRangePos3.setValue(0);
            latchingPos3.setSelected(false);

            int pos3MidiCmd = mainBank.preset[preset].mode[mode].pos[2].slot[getSelectedSlot(2)].MidiCommand; // slot attribute addressability
            String P3 = Integer.toHexString(pos3MidiCmd);

            comMidiCmdPos3.selectWithKeyChar((char) P3.charAt(0)); // set the combo box item to be what the midi command in the preset is
            //popSubCmd(pos3MidiCmd,comMidiSubCommandPos3); // populate the subcommand list with the corresponding values of the main command

            if (pos3MidiCmd > 0) { // position 1

                int pos3channel = mainBank.preset[preset].mode[mode].pos[2].slot[getSelectedSlot(2)].MidiChannel; // slot attribute addressability
                int pos3subcmd = mainBank.preset[preset].mode[mode].pos[2].slot[getSelectedSlot(2)].MidiSubCommand; // slot attribute addressability
                int pos3curvetype = mainBank.preset[preset].mode[mode].pos[2].slot[getSelectedSlot(2)].curvetype; // slot attribute addressability
                int pos3curvedir = mainBank.preset[preset].mode[mode].pos[2].slot[getSelectedSlot(2)].curvedirection; // slot attribute addressability
                int pos3min = mainBank.preset[preset].mode[mode].pos[2].slot[getSelectedSlot(2)].min; // slot attribute addressability
                int pos3max = mainBank.preset[preset].mode[mode].pos[2].slot[getSelectedSlot(2)].max; // slot attribute addressability
                int pos3latching = mainBank.preset[preset].mode[mode].pos[2].slot[getSelectedSlot(2)].latching; // slot attribute addressability

                // subcommand
                // we must do a little work to translate the sub command to the combo box index value.
                //  popSubCmd preps the combobox for our value.
                switch (pos3MidiCmd) {
                    case 0: // do nothing, combo model will be empty
                        break;
                    case 8: // noteOn
                    case 9: // noteOff
                    case 10: // noteOff
                    case 13: // aftertouch
                    case 14: // pitch bend
                        int setVal = 0;
                        if (pos3subcmd > 0) {
                            setVal = 1; // limit values > 1 to index 1 (pitch up)
                        }
                        comMidiSubCommandPos3.setSelectedIndex(setVal);
                        break;
                    default:
                        comMidiSubCommandPos3.setSelectedIndex(pos3subcmd);
                }

                spnMidiChannelPos3.setValue(pos3channel + 1);
                comCurveTypePos3.setSelectedIndex(pos3curvetype);

                switch (pos3curvedir) {
                    case 0: {
                        radCurveNegPos3.setSelected(true);
                    }
                    break;
                    case 1: {
                        radCurvePosPos3.setSelected(true);
                    }
                    break;
                    default: {
                        // no default action
                    }
                    break;

                }
                //radCurvePosSlot1.set   // figure out the radio buttons later
                spnMinRangePos3.setValue(pos3min);
                spnMaxRangePos3.setValue(pos3max);

                //latchingSlot3.set // figure outt the checkbox later
                if (pos3latching > 0) {
                    latchingPos3.setSelected(true);
                } else {
                    latchingPos3.setSelected(false);
                }
            } // position 3 
        }

        if (pos == 3 | pos == 99) {

            // position 4
            spnMidiChannelPos4.setValue(0);
            comMidiSubCommandPos4.removeAllItems();
            comCurveTypePos4.setSelectedIndex(0);
            radCurveNegPos4.setSelected(false);
            radCurvePosPos4.setSelected(false);
            spnMinRangePos4.setValue(0);
            spnMaxRangePos4.setValue(0);
            latchingPos4.setSelected(false);

            int pos4MidiCmd = mainBank.preset[preset].mode[mode].pos[3].slot[getSelectedSlot(3)].MidiCommand; // slot attribute addressability
            String P4 = Integer.toHexString(pos4MidiCmd);

            comMidiCmdPos4.selectWithKeyChar((char) P4.charAt(0)); // set the combo box item to be what the midi command in the preset is

            //popSubCmd(pos4MidiCmd,comMidiSubCommandPos4); // populate the subcommand list with the corresponding values of the main command
            if (pos4MidiCmd > 0) { // position 1

                int pos4channel = mainBank.preset[preset].mode[mode].pos[3].slot[getSelectedSlot(3)].MidiChannel; // slot attribute addressability
                int pos4subcmd = mainBank.preset[preset].mode[mode].pos[3].slot[getSelectedSlot(3)].MidiSubCommand; // slot attribute addressability
                int pos4curvetype = mainBank.preset[preset].mode[mode].pos[3].slot[getSelectedSlot(3)].curvetype; // slot attribute addressability
                int pos4curvedir = mainBank.preset[preset].mode[mode].pos[3].slot[getSelectedSlot(3)].curvedirection; // slot attribute addressability
                int pos4min = mainBank.preset[preset].mode[mode].pos[3].slot[getSelectedSlot(3)].min; // slot attribute addressability
                int pos4max = mainBank.preset[preset].mode[mode].pos[3].slot[getSelectedSlot(3)].max; // slot attribute addressability
                int pos4latching = mainBank.preset[preset].mode[mode].pos[3].slot[getSelectedSlot(3)].latching; // slot attribute addressability

                // subcommand
                // we must do a little work to translate the sub command to the combo box index value.
                //  popSubCmd preps the combobox for our value.
                switch (pos4MidiCmd) {
                    case 0: // do nothing, combo model will be empty
                        break;
                    case 8: // noteOn
                    case 9: // noteOff
                    case 10: // noteOff
                    case 13: // aftertouch
                    case 14: // pitch bend
                        int setVal = 0;
                        if (pos4subcmd > 0) {
                            setVal = 1; // limit values > 1 to index 1 (pitch up)
                        }
                        comMidiSubCommandPos4.setSelectedIndex(setVal);
                        break;
                    default:
                        comMidiSubCommandPos4.setSelectedIndex(pos4subcmd);
                }

                spnMidiChannelPos4.setValue(pos4channel + 1);
                comCurveTypePos4.setSelectedIndex(pos4curvetype);

                switch (pos4curvedir) {
                    case 0: {
                        radCurveNegPos4.setSelected(true);
                    }
                    break;
                    case 1: {
                        radCurvePosPos4.setSelected(true);
                    }
                    break;
                    default: {
                        // no default action
                    }
                    break;

                }
                //radCurvePosSlot1.set   // figure out the radio buttons later
                spnMinRangePos4.setValue(pos4min);
                spnMaxRangePos4.setValue(pos4max);

                //latchingSlot1.set // figure outt the checkbox later
                if (pos4latching > 0) {
                    latchingPos4.setSelected(true);
                } else {
                    latchingPos4.setSelected(false);
                }
            } // position 4
        }
        pedalQuiet = false;
        modelQuiet = false;

        txtChg.setText("");
        canClose = true;
        //mainBank.
        System.out.println("popUiFromBank():end");
    } // pop UI from bank

    /**
     * safely delete the pedal bank file
     */
    private void deletePedalBankFile() {
        try {
            File pedalBankFile = new File(dataDir + "Pedal Bank.ecp");
            if (!pedalBankFile.exists()) {
                return;
            }
            archivePedalBankFile();
            if (pedalBankFile.delete()) {
                //System.out.println("deleted pedal bank file"); // file deleted ok   
            }
        } catch (Exception e) {
            System.err.println("error deleting Pedal Bank temp file: " + e);
        }

    }

    /**
     * Check for existence of history file and create if it does not exist.
     * Then, create an archival file name for the current pedal bank file. Then,
     * copy the current pedal bank file to the history see the restore from
     * history method unArchivePedalBankFile(File archfile); // caller knows and
     * creates the file name see the restore from history method
     * unArchivePedalBankFile(Date datetime); // this method matches the
     * argument with the closest file in time see the restore from history
     * method unArchivePedalBankFile(); // restore most recent pedal bank file
     * if possible
     */
    private void archivePedalBankFile() {
        // check and make "history directory"
        File pedalBankFile = new File(dataDir + "Pedal Bank.ecp");
        
        if (!pedalBankFile.exists()) {
            //System.err.println("archivePedalBankFile(): pedal bank.ecp file not found");
            return;
        } // do nothing if no file

        File srcDirFile = new File(srcDir);
        String histDir = dataDir + "history" + fileSeparator;
        File histFolder = new File(histDir);
        if (!histFolder.exists()) {
            histFolder.mkdir();
        }
// create an archival file name
        String archivalFileName = Date.from(Instant.now()).toString();
        archivalFileName = archivalFileName.replaceAll(":", "");
        archivalFileName = archivalFileName.replaceAll(" ", "");

        archivalFileName = histDir + "pedalBankArchive" + archivalFileName + ".ecp";

// copy current pedal bank file to history
        File pbf = new File(archivalFileName);
        try {
            Files.copy(pedalBankFile.toPath(), pbf.toPath(), COPY_ATTRIBUTES);
        } catch (IOException ex) {
            Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setSelectedBankName(String bankName) {
        System.out.println("setSelectedBankName(" + bankName + ")");
        ListModel bl = txtBanks.getModel();
        int resint = 0;
        for (int fi = 0; fi < bl.getSize(); fi++) {
            if (bankName.equalsIgnoreCase((String) bl.getElementAt(fi))) {
                resint = fi;
                break;
            }
        }
        txtBanks.setSelectedIndex(resint);
    }

    // these methods implement the ability to drag a file name into the bank list.
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        System.out.println("DragEnter event");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        System.out.println("dragOver event");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        System.out.println("dropActionChanged event");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        System.out.println("dragExit event");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            // Ok, get the dropped object and try to figure out what it is
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            for (int i = 0; i < flavors.length; i++) {
                System.out.println("Possible flavor: "
                        + flavors[i].getMimeType());
                // Check for file lists specifically
                if (flavors[i].isFlavorJavaFileListType()) {
                    // Great! Accept copy drops...
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    //txtActionStatus.setText("Dropped file"+); ta.blah blah

                    // And add the list of file names to our text area
                    java.util.List list = (java.util.List) tr
                            .getTransferData(flavors[i]);
                    for (int j = 0; j < list.size(); j++) {
//            txtBanks.append(list.get(j) + "\n"); // get the model, add tot he model, put the model back
                    }

                    // If we made it this far, everything worked.
                    dtde.dropComplete(true);
                    return;
                } // Ok, is it another Java object?
                else if (flavors[i].isFlavorSerializedObjectType()) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    //      ta.setText("Successful text drop.\n\n");
                    Object o = tr.getTransferData(flavors[i]);
                    //        ta.append("Object: " + o);
                    dtde.dropComplete(true);
                    return;
                } // How about an input stream?
                else if (flavors[i].isRepresentationClassInputStream()) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
//          ta.setText("Successful text drop.\n\n");
//          ta.read(new InputStreamReader((InputStream) tr
//              .getTransferData(flavors[i])),
//              "from system clipboard");
                    dtde.dropComplete(true);
                    return;
                }
            }
            // Hmm, the user must not have dropped a file list
            System.out.println("Drop failed: " + dtde);
            dtde.rejectDrop();
        } catch (Exception e) {
            e.printStackTrace();
            dtde.rejectDrop();
        }
    }

} // end of class

