package EffigyControlPanel;

import java.util.Formatter;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.SysexMessage;
import javax.swing.JOptionPane;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jody
 */
public class ConfigDialog extends javax.swing.JDialog {

    String fileSeparator = System.getProperty("file.separator");
    String srcDir = System.getProperty("user.dir");
    String dataDir = srcDir + fileSeparator + "data" + fileSeparator;
    Property config;
    String[] arg1List = new String[] {"Preset#",
                                      "Attr Addr",
                                      "Preset#",
                                      "Preset#",
                                      "Slot Addr",
                                      "Slot Addr",
                                      "Mode",
                                      "-",
                                      "-",
                                      "-",
                                      "#samples",
                                      "!",
                                      "Channel",
                                       "Porch Sz",
                                      "Boot Mode",
                                      "Ciel Sz",
                                      "LED Brt%",
                                      "Sys Blk",
                                      "PR#Dflt"};
    String[] arg2List = new String[] {"-",
                                      "-",
                                      "TBD",
                                      "Attr",
                                      "SlotAddr",
                                      "SlotData",
                                      "",
                                      "",
                                      "",
                                      "",
                                      "",
                                      "",
                                      "",
                                      "",
                                      "",
                                      "",
                                      "",
                                      "",
                                      ""};
    boolean[] argResponseList = new boolean[] {false,false,false,true,true,false,false,false,false,false,false,true,false,false,false,false,false, true,false };
    String[] arg1Template = new String[] {"00",
                                          "00 00 00 00 00",
                                          "00",
                                          "00",
                                          "00 00 00 00",
                                          "00 00 00 00",
                                          "00",
                                          "",
                                          "00",
                                          "",
                                          "0A",
                                          "",
                                          "00",
                                          "05",
                                          "00",
                                          "05",
                                          "05",
                                           "",
                                          "00"};
    String[] apiTxtArr = new String[] {
            "Activates the preset stored in the Pedal. (loads EEPROM preset into pedal live preset)",   // 0
            "Change one attribute of a pedal preset (preset/mode/position/slot#/attribute/value)", // 1
            "Send displayed preset to pedal and save in the pedal's EEPROM.  If preset 0, activate it", // 2
            "Upload displayed preset from pedal to the control panel and display in the slot editor", // 3
            "Upload displayed slot from pedal (preset/mode/position/slot#) and show in slot data",  // 4
            "Download displayed slot to pedal (preset/mode/position/slot#) and update in EEPROM", // 5
            "Switch to the displayed mode immediately in the current preset", // 6
            "Wait for user to select mode. Same as hitting the mode switch.",  // 7
            "Set sensitivity %.  Same as turning the knob when it controls sensitivity.", // 8
            "Recalibrate the sensors.  Use for example if tension strap is adjusted without restarting.",  // 9
            "Recalibrate the input system with the displayed # of samples x100",  // A
            "Restart the pedal immediately.  Checks first if data might be lost", // B
            "Change all slot channels to the displayed channel in the pedal live preset", // C
            "Set space between upper idle edge and lower limit (USE CAREFULLY)", // D
            "set Boot Mode: 0=Mode select, 1-3 = autoselect that mode", // E
            "set space between lower max edge and upper limit (USE CAREFULLY)", // F
            "max brightness % on comm LED.  set between 0-100, 0=off", // 10 / 16
            "Upload system block from pedal", // 11 / 17
            "set preset to load by default" // 12 / 18
    };
    
    // sysex message stuff
    String sysexHeaderTxt = "F0 00 02 21 ";
    String sysexDelimiterTxt = " F7"; 
    byte[] sysexHeader = { (byte) 0xF7, (byte) 0x00, (byte) 0x02, (byte) 0x21 };
    byte[] sysexDelimiter = { (byte) 0xF7 };
    boolean isPresetLoaded = false;
    Bank ConfigBank;
    Pedal ConfigPedal;
    
    
    
    
    /**
     * Creates new form ConfigDialog
     * @param parent
     * @param modal
     * @param currentBank
     */
    public ConfigDialog(java.awt.Frame parent, boolean modal, Bank mainBank, Pedal pedal) {
        
        super(parent, modal);
        initComponents();
 
        // pedal bank given to this
        ConfigBank = mainBank;
        ConfigPedal = pedal;
//        txtBuffer.setText("1234");
        txtBuffer.setText(ConfigBank.toString());

        // properties access
        config = new Property(dataDir+"config.txt");
        chkLoadLastBank.setSelected(config.getBool("loadLastBank"));
        chkLoadBgImage.setSelected(config.getBool("loadBgImage"));
        chkAutoPedalConnect.setSelected(config.getBool("autoConnect"));

        System.out.println(ConfigBank.status);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new TransparentPanel();
        arg1Label1 = new javax.swing.JLabel();
        butRefresh = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        comApiCmd = new javax.swing.JComboBox<>();
        txtArg1 = new javax.swing.JTextField();
        txtSlotData = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        arg2Label = new javax.swing.JLabel();
        arg1Label = new javax.swing.JLabel();
        butRefresh2 = new javax.swing.JButton();
        butRefresh3 = new javax.swing.JButton();
        butApiSend = new javax.swing.JButton();
        comAttr = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        txtAttributeVal = new javax.swing.JLabel();
        txtApiCmd = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        txtActionStatus = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        butApiSend1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtBuffer = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        comSlot = new javax.swing.JComboBox<>();
        comPos = new javax.swing.JComboBox<>();
        comMode = new javax.swing.JComboBox<>();
        comPreset = new javax.swing.JComboBox<>();
        chkLoadLastBank = new javax.swing.JCheckBox();
        chkAutoPedalConnect = new javax.swing.JCheckBox();
        chkLoadBgImage = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel2.setBackground(new java.awt.Color(0,0,0,0.15f));
        jPanel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jPanel2.setOpaque(false);

        arg1Label1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        arg1Label1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        arg1Label1.setText("Arg 2");

        butRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/EffigyControlPanel/refresh-tiny.gif"))); // NOI18N
        butRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRefreshActionPerformed(evt);
            }
        });

        jLabel23.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel23.setText("Slot Data");

        comApiCmd.setMaximumRowCount(20);
        comApiCmd.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0 - Select Pedal Preset", "1 - Set single value in a preset", "2 - Send preset to pedal", "3 - Get preset from pedal", "4 - Get slot from pedal", "5 - Send slot to pedal", "6 - Set mode in current preset", "7 - User Mode Switch ", "8 - Set sensitivity % (knob)", "9 - Recalibrate sensors", "A - Recalibrate w/#samples", "B - Reboot immediately", "C - Change all live channels", "D - set Porch size", "E - set Boot mode", "F - set Ceiling size ", "10 - set LED max brightness %", "11 - get system Block", "12 - ", "13 - ", "14 - get pedal software version" }));
        comApiCmd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comApiCmdActionPerformed(evt);
            }
        });

        jLabel26.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel26.setText("Pedal Settings");

        arg2Label.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        arg2Label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        arg2Label.setText("arg 2");

        arg1Label.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        arg1Label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        arg1Label.setText("Arg 1");

        butRefresh2.setText("x");
        butRefresh2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRefresh2ActionPerformed(evt);
            }
        });

        butRefresh3.setText("x");
        butRefresh3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRefresh3ActionPerformed(evt);
            }
        });

        butApiSend.setBackground(new java.awt.Color(204, 0, 0));
        butApiSend.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        butApiSend.setForeground(new java.awt.Color(255, 51, 51));
        butApiSend.setText("Send");
        butApiSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butApiSendActionPerformed(evt);
            }
        });

        comAttr.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "MIDI  Command", "MIDI Channel", "Subcommand", "Curve Type", "Curve Direction", "Min", "Max", "Latching" }));
        comAttr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comAttrActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel6.setText("Attr");

        txtAttributeVal.setText("N/A");

        txtApiCmd.setBackground(new java.awt.Color(0, 0, 0));
        txtApiCmd.setText("None");

        jLabel25.setBackground(new java.awt.Color(0, 0, 0));
        jLabel25.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel25.setText("API Cmd:");

        txtActionStatus.setBackground(new java.awt.Color(0, 0, 0));
        txtActionStatus.setText("no action performed");

        jLabel30.setBackground(new java.awt.Color(0, 0, 0));
        jLabel30.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel30.setText("Action:");

        butApiSend1.setBackground(new java.awt.Color(255, 255, 255));
        butApiSend1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        butApiSend1.setForeground(new java.awt.Color(0, 0, 204));
        butApiSend1.setText("Close");
        butApiSend1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butApiSend1ActionPerformed(evt);
            }
        });

        txtBuffer.setColumns(20);
        txtBuffer.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        txtBuffer.setLineWrap(true);
        txtBuffer.setRows(5);
        txtBuffer.setToolTipText("contents of current bank");
        txtBuffer.setWrapStyleWord(true);
        txtBuffer.setOpaque(false);
        txtBuffer.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                txtBufferPropertyChange(evt);
            }
        });
        jScrollPane1.setViewportView(txtBuffer);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel1.setText("Bank Data");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setText("Preset");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel3.setText("Mode");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel4.setText("Position");

        jLabel27.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel27.setText("Slot");

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel8.setText("S  L  O  T      A  D  D  R  E  S  S");

        comSlot.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        comSlot.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3" }));
        comSlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comSlotActionPerformed(evt);
            }
        });

        comPos.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        comPos.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4" }));
        comPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comPosActionPerformed(evt);
            }
        });

        comMode.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3" }));
        comMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comModeActionPerformed(evt);
            }
        });

        comPreset.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        comPreset.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "1", "2", "3", "4" }));
        comPreset.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comPresetItemStateChanged(evt);
            }
        });
        comPreset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comPresetActionPerformed(evt);
            }
        });

        chkLoadLastBank.setText("Automatically load last Bank");
        chkLoadLastBank.setActionCommand("llb");
        chkLoadLastBank.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkLoadLastBankActionPerformed(evt);
            }
        });

        chkAutoPedalConnect.setText("automatically connect to pedal");
        chkAutoPedalConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkAutoPedalConnectActionPerformed(evt);
            }
        });

        chkLoadBgImage.setText("Show Pedal Image background");
        chkLoadBgImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkLoadBgImageActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(comApiCmd, 0, 425, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(butRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(butApiSend, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(butApiSend1, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel25)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtApiCmd, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel30)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtActionStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkLoadLastBank)
                            .addComponent(chkLoadBgImage)
                            .addComponent(chkAutoPedalConnect))
                        .addContainerGap())
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(102, 102, 102)
                                .addComponent(jLabel26))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                            .addComponent(comAttr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtAttributeVal))
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel2)
                                                .addComponent(comPreset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGap(18, 18, 18)
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel3)
                                                .addComponent(comMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGap(13, 13, 13)
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel4)
                                                .addComponent(comPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel27)
                                                .addComponent(comSlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(arg1Label1, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(arg1Label))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(arg2Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(txtArg1, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                                                .addComponent(butRefresh3))
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(butRefresh2)
                                                .addGap(0, 0, Short.MAX_VALUE))))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel23)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtSlotData, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel26)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(comApiCmd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(butRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(butApiSend)
                        .addComponent(butApiSend1)))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel2)
                                .addComponent(jLabel3)
                                .addComponent(jLabel4)
                                .addComponent(jLabel27))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(comSlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(comPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(comMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(comPreset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comAttr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6)
                            .addComponent(txtAttributeVal)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtArg1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(butRefresh2, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(arg1Label))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(arg2Label)
                            .addComponent(arg1Label1)
                            .addComponent(butRefresh3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel23)
                            .addComponent(txtSlotData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtApiCmd)
                            .addComponent(jLabel25))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtActionStatus)
                            .addComponent(jLabel30)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(chkLoadLastBank)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkAutoPedalConnect)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkLoadBgImage)))
                .addGap(75, 75, 75)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 347, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void butRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRefreshActionPerformed
        comApiCmdActionPerformed(evt);
    }//GEN-LAST:event_butRefreshActionPerformed

    private void comApiCmdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comApiCmdActionPerformed

        // set up data and arguments for each commmand
        // populate api helper controls when api command changes

        // defaults unless there is a better way
        arg1Label.setText(arg1List[comApiCmd.getSelectedIndex()]);
        arg2Label.setText(arg2List[comApiCmd.getSelectedIndex()]);
        txtApiCmd.setText(apiTxtArr[comApiCmd.getSelectedIndex()]);
        txtArg1.setText(arg1Template[comApiCmd.getSelectedIndex()]);

        // populate the address and data fields based on the appropriate selected command
        String tmp = "";
        switch (comApiCmd.getSelectedIndex()) {
            case 0: // load live preset from pedal eeprom, use preset#
            case 2: // send preset to pedal
            case 3: // get preset from pedal

            txtArg1.setText("0" + String.valueOf(comPreset.getSelectedIndex()));

            break;
            case 1: // set single attribute
            //                txtArg1.setText(String.valueOf(comPreset.getSelectedIndex())+" 0");
            SlotAddress sa = new SlotAddress(comPreset.getSelectedIndex(),comMode.getSelectedIndex(),comPos.getSelectedIndex(),comSlot.getSelectedIndex());

            txtArg1.setText(sa.toString());
            txtSlotData.setText(ConfigBank.preset[comPreset.getSelectedIndex()].mode[comMode.getSelectedIndex()].pos[comPos.getSelectedIndex()].slot[comSlot.getSelectedIndex()].getAttrVal(comAttr.getSelectedIndex()));

            case 4: //  get slot from pedal
            SlotAddress sa2 = new SlotAddress(comPreset.getSelectedIndex(),comMode.getSelectedIndex(),comPos.getSelectedIndex(),comSlot.getSelectedIndex());
            txtArg1.setText(sa2.toString());

            break;
            case 5: // send slot to pedal
            SlotAddress sa3 = new SlotAddress(comPreset.getSelectedIndex(),comMode.getSelectedIndex(),comPos.getSelectedIndex(),comSlot.getSelectedIndex());

            txtArg1.setText(sa3.toString());
            //txt+txtSlotData.getText();
            txtSlotData.setText(ConfigBank.preset[comPreset.getSelectedIndex()].mode[comMode.getSelectedIndex()].pos[comPos.getSelectedIndex()].slot[comSlot.getSelectedIndex()].toString()+" ");
            break;
            case 16:
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb,Locale.US);
            formatter.format("%02x",Integer.parseInt(txtArg1.getText()));
            txtArg1.setText(sb.toString());
            break;
            default:
            break;
        }
    }//GEN-LAST:event_comApiCmdActionPerformed

    private void butRefresh2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRefresh2ActionPerformed
        txtArg1.setText("");
    }//GEN-LAST:event_butRefresh2ActionPerformed

    private void butRefresh3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRefresh3ActionPerformed
        txtSlotData.setText("");
    }//GEN-LAST:event_butRefresh3ActionPerformed

    private void butApiSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butApiSendActionPerformed

        if(!ConfigPedal.connected) {
           JOptionPane.showMessageDialog(null, "Pedal not connected.");
           return;            
        }
        
        if(!ConfigPedal.api.connected) {
           JOptionPane.showMessageDialog(null, "Pedal not connected.");
           return;
        }
        // action to send command to pedal has been invoked -
        // 1. prepare the command
        // 2. prepare the  arguments
        // 3. send the command
        // 4. wait for a response if necessary
        // 5. process response if necessary

        ///////////////////////////////////
        // 1. prepare the command itself //
        ///////////////////////////////////
        int apicmd;
        // act on the command
        apicmd = comApiCmd.getSelectedIndex();
        String cmdTxt = "";
        String apiTxtChar = comApiCmd.getSelectedItem().toString().substring(0,2).trim();
        if(apiTxtChar.length() == 1) {
            apiTxtChar = "0"+apiTxtChar+" ";
        }
        cmdTxt+=sysexHeaderTxt+apiTxtChar;
        /////////////////////////////
        //2. prepare the arguemnts //
        /////////////////////////////
        // the header and api cmd are now loaded.  for the commands that do nothing else, nothing else is needed to be done.
        // the rest of the commands add or connect arguments and control the send/receive.
        switch (apicmd) {
            
            // sorting groups of responses by response and argument complexity
            // no-response, no argument
            // no-response, 1 argument simple
            // no-response, 1 argument non-simple
            // response, 1 argument simple
            // response, 1 argument non-simple
            // response, 2 arguments?
            // and beyond
            
            // no arguments no response
            //7 - User Selects mode
            //B - Reboot immediately
            //9 - Recalibrate sensors
            case 9: // no-response, no arguments
            case 7: // no-response, no arguments
            case 11: // no-response, no arguments
                break;

                // 1 argument no response simple argument
                //0 - Load preset from EEPROM
                //6 - Set mode in current preset
                //8 - Set sensitivity %
                //A - Recalibrate w/#samples
                //C - Change all live channels
                //D - set Porch size
                //E - set Boot mode
                //F - set Ceiling size
                // 1 argument non-simple
            case 0: // no-response, 1 simple argument: preset
            case 6: // no-response, 1 argument, simple: mode
            case 8: // no-response, 1 argument, simple
            case 10: // no-response, 1 argument, simple
            case 12: // no-response, 1 argument, simple
            case 13: // no-response, 1 argument,simple
            case 14: // no-response, 1 argument,simple
            case 15: // no-response, 1 argument,simple
            case 16: // no-response, 1 argument,simple

                // response required, 1 argument
                //3 - Get preset from pedal
                //4 - Get slot from pedal
            case 3: // response, 1 argument, simple
            case 4: // response, 1 argument, non-simple
                //String xxxx = txtArg1.getText();
                //System.out.println("arg1='"+xxxx+"'");
                cmdTxt+=" "+txtArg1.getText(); // slot address
                // handle the response waiting etc. later
                break;

                //5 - Send slot to pedal
            case 5: // no-response, 2 arguuments, non-simple
                //cmdTxt+=   slot addr + slot data //
                cmdTxt+=txtArg1.getText()+" "+txtSlotData.getText(); // slot data
                break;

                //1 - Set single value in a preset
            case 1: // no-response, 2 arguments, 1 non simple, 1 simple
                // form slot address from controls
                //
                //               StringBuilder sb = new StringBuilder();
                //               Formatter formatter = new Formatter(sb,Locale.US);
                //               SlotAddress sa = new SlotAddress(comPreset.getSelectedIndex(),comMode.getSelectedIndex(),comPos.getSelectedIndex(),getSelectedSlot());
                //               cmdTxt+=sa.toString();  // put the slot address in the slot address text field
                //               cmdTxt+=" ";
                //               formatter.format("%02x ",comAttr.getSelectedIndex()); // even  if not changed will give the default attr (0)
                //               formatter.format("%02x",Integer.parseInt(txtSlotData.getText())); // the slot data is what gets made, the other tools help it, so doesn't matter if there is nothing in the attr val
                cmdTxt+=txtArg1.getText().trim()+" 0"+comAttr.getSelectedIndex()+" "+txtSlotData.getText()+" ";  // put the slot address in the slot address text field
                break;

                //2 - Send preset to pedal
            case 2: // no-response, 2 arguments, 1 simple, 1 non-simple
                // in this case take the whole preset txt and send it as-is!!

                StringBuilder sb2 = new StringBuilder();
                Formatter formatter2 = new Formatter(sb2,Locale.US);

                // use the argument displayed, not the direct combo index, in case they chhanged it.  The combo updates the argument so best is indirect here.
                formatter2.format("%02x ",Integer.parseInt(txtArg1.getText()));
                // rather than...
                //formatter.format("%02x",comPreset.getSelectedIndex());

                cmdTxt+=sb2.toString()+ConfigBank.preset[comPreset.getSelectedIndex()].toString()+" "; // check this

                //cmdTxt = txtBuffer.getText();
                break;
                
            default:
                cmdTxt+=txtArg1.getText(); // slot address
                cmdTxt+=txtSlotData.getText(); // slot data
                break;
                // not implemented if not caught by now
        }
        ////////////////////////////////
        //3. send and receive     //
        ////////////////////////////////
        
        // The command should be all built except the sysex footer.  The space should be on the end for all cases. then send the command, wait if necessary for the response.
        cmdTxt = cmdTxt.trim()+sysexDelimiterTxt; // check this ***************
        txtApiCmd.setText(cmdTxt);
        System.out.println("cmdTxt="+cmdTxt);
        // determine the output command to form.
        // if the command is a download, download the displayed preset (3)
        // or slot (4) of the displayed slot.  The arg1 is (either the slot# or the whole slot address)
        // the preset is the preset displayed in the controls, i.e. the address is taken from the displayed controls.
        // sort out all the sysex headers if present or absent and if they should be changed or added etc.
        // output is xfrba - transfer byte array
        byte[] xfrba = ConfigBank.convertTextToByteArr(cmdTxt);
        //ShortMessage myMsg = new ShortMessage();

        // attempt to connect *************** if autoconnect tag ******************
        if(!ConfigPedal.connected) {
            ConfigPedal.api.discoverPedal();
        }
        
    }//GEN-LAST:event_butApiSendActionPerformed

    private void comAttrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comAttrActionPerformed
        comApiCmdActionPerformed(evt);
        if(comAttr.getItemCount() < 1) {
            //txtActionStatus.setText("nothing selected");
            return;
        }
        // attribute selected.  populate the attribute value using the slot address - as proto proof and marginally useful, for now, maybe
    //    txtAttributeVal.setText(String.valueOf(ConfigBank.preset[comPreset.getSelectedIndex()].mode[comMode.getSelectedIndex()].pos[comPos.getSelectedIndex()].slot[getSelectedSlot()].getAttrVal(comAttr.getSelectedIndex())));
    }//GEN-LAST:event_comAttrActionPerformed

    private void butApiSend1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butApiSend1ActionPerformed
        setVisible(false);
        dispose();

    }//GEN-LAST:event_butApiSend1ActionPerformed

    private void txtBufferPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_txtBufferPropertyChange
        System.out.println("txtBufferProperrtyChange");
        // if somethign changes the text box, should it update...

        // should we take the text and use it to rebuild?  I thiknk we should ******** tag

        //canClose = false; txtChg.setText("*"); //?
        //txtChg.setText("*"); //?
        //        PopPositionSlotData(); // for now ****************** tag
    }//GEN-LAST:event_txtBufferPropertyChange

    private void comSlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comSlotActionPerformed
        // TODO ensure the set slots work with no further code needed here
    }//GEN-LAST:event_comSlotActionPerformed

    private void comPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comPosActionPerformed
        //comApiCmdActionPerformed(evt);
        //System.out.println("event="+evt.getActionCommand());
        try
        {
            //PopPositionSlotData();
        }

        catch (Exception e)
        {
            System.out.println("Exception occurred populating controls:"+e.getMessage());
            txtActionStatus.setText("Exception occurred populating controls");
        }
    }//GEN-LAST:event_comPosActionPerformed

    private void comModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comModeActionPerformed
        //comApiCmdActionPerformed(evt);
        //System.out.println("event="+evt.getActionCommand());
        try
        {
            //PopPositionSlotData();
        }

        catch (Exception e)
        {
            System.out.println("Exception occurred populating controls:"+e.getMessage());
            txtActionStatus.setText("Exception occurred populating controls");
        }
    }//GEN-LAST:event_comModeActionPerformed

    private void comPresetItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comPresetItemStateChanged
        //
    }//GEN-LAST:event_comPresetItemStateChanged

    private void comPresetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comPresetActionPerformed
        //comApiCmdActionPerformed(evt);
        //System.out.println("event="+evt.getActionCommand());
        // ****** tag *********
        // when the preset # is changed by the user, switch between which preset is displayed in the bank.
        // to be implemented later, right now the preset just serves as a read for the api commander
        try
        {
            //PopPositionSlotData();
        }

        catch (Exception e)
        {
            System.out.println("Exception occurred populating controls:"+e.getMessage());
            txtActionStatus.setText("Exception occurred populating controls");
        }
    }//GEN-LAST:event_comPresetActionPerformed

    private void chkLoadBgImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkLoadBgImageActionPerformed
        config = new Property(dataDir+"config.txt");
        if(chkLoadBgImage.isSelected()) {
            config.setBool("loadBgImage",true);
            System.out.println("setting load bg image on");
        } else {
            config.setBool("loadBgImage",false);
            System.out.println("setting load bg image on");
        }

    }//GEN-LAST:event_chkLoadBgImageActionPerformed

    private void chkAutoPedalConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkAutoPedalConnectActionPerformed
        if(chkLoadBgImage.isSelected()) {
            config.setBool("loadBgImage",true);
            System.out.println("setting auto peal connect on");
        } else {
            config.setBool("loadBgImage",false);
            System.out.println("setting auto pedal connect off");
        }

    }//GEN-LAST:event_chkAutoPedalConnectActionPerformed

    private void chkLoadLastBankActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkLoadLastBankActionPerformed
        if(chkLoadLastBank.isSelected()) {
            config.setBool("loadLastBank",true);
            config.setString("lastBank", ConfigBank.bankName);
            System.out.println("setting loadLastBank on, last bank name="+ConfigBank.bankName);
        } else {
            config.setBool("loadLastBank",false);
            System.out.println("setting loadLastBank off");
        }
    }//GEN-LAST:event_chkLoadLastBankActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel arg1Label;
    private javax.swing.JLabel arg1Label1;
    private javax.swing.JLabel arg2Label;
    private javax.swing.JButton butApiSend;
    private javax.swing.JButton butApiSend1;
    private javax.swing.JButton butRefresh;
    private javax.swing.JButton butRefresh2;
    private javax.swing.JButton butRefresh3;
    private javax.swing.JCheckBox chkAutoPedalConnect;
    private javax.swing.JCheckBox chkLoadBgImage;
    private javax.swing.JCheckBox chkLoadLastBank;
    private javax.swing.JComboBox<String> comApiCmd;
    private javax.swing.JComboBox<String> comAttr;
    private javax.swing.JComboBox<String> comMode;
    private javax.swing.JComboBox<String> comPos;
    private javax.swing.JComboBox<String> comPreset;
    private javax.swing.JComboBox<String> comSlot;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel txtActionStatus;
    private javax.swing.JLabel txtApiCmd;
    private javax.swing.JTextField txtArg1;
    private javax.swing.JLabel txtAttributeVal;
    private javax.swing.JTextArea txtBuffer;
    private javax.swing.JTextField txtSlotData;
    // End of variables declaration//GEN-END:variables
}
