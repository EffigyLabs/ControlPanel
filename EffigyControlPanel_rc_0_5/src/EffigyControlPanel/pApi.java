/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EffigyControlPanel;

import java.util.Formatter;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import uk.co.xfactorylibrarians.coremidi4j.*;

/**
 *
 * @author jody
 * 
 */
class pApi implements pedalApi {
    public String status = "uninitialized";
    public MidiDevice[] pedalDevices = new MidiDevice[2];
    public SystemBlock sysblk = new SystemBlock();
    public String swVersion = "";
    public Bank pedalBank = null; // main storage for preset slot data
    public boolean connected = false; // master flag to test for a connected device
    public boolean inMidSysex = false; // synchronization flag for multiple MIDI messages arriving before the last MIDI message is completed. not sure if we'll use this like this.
    public MidiDevice device;
    public MidiDevice pedalIn; // what we get messages from the pedal from
    public MidiDevice pedalOut; // what we send messages to the pedal to
    public int waitTime = 10000; // default wait time to receive response from pedal in send/receive operations.
        
    String sysexHeaderTxt = "F0 00 02 21 ";
    String sysexFooterTxt = " F7"; 
    byte[] sysexHeader = { (byte) 0xF7, (byte) 0x00, (byte) 0x02, (byte) 0x21 };
    byte[] sysexDelimiter = { (byte) 0xF7 };
    // the GET and SET verbs are from the perspective of the app, not the pedal.  So GET_something means, get FROM the pedal TO the app here.  SET means, set send to/set in the pedal
    int API_LOADPRESET = 0; // in the pedal, switch to a preset in the EEPROM
    int API_SET_ATTR = 1; // set a single attribute in the pedal
    int API_SET_PRESET = 2; // send a preset to the pedal
    int API_GET_PRESET = 3; // get a preset from the pedal
    int API_GET_SLOT = 4;
    int API_SET_SLOT = 5;
    int API_SET_MODE = 6;
    int API_MODE_SELECT = 7;
    int API_SET_SENSITIVITY = 8;
    int API_RECALIBRATE = 9;
    int API_RECALIBRATE_WITH_SAMPLES = 10;
    int API_RESET = 11;
    int API_CHG_ALL_CHANNELS = 12;
    int API_SET_PORCH_SIZE = 13;
    int API_SET_BOOT_MODE = 14;
    int API_SET_CEILING_SIZE = 15;
    int API_SET_LED_FADE_BRIGHTNESS = 16;
    int API_GET_SYSTEM_BLOCK = 17;
    int API_GET_BANK = 18;
    int API_SET_PRESET_TO_LOAD = 19;
    int API_GET_SOFTWARE_VERSION = 20;
    //int API_FUTURE = 21;
    
    
    public pApi() {
            // open it up, set receiver, etc?  just like in the pedal?
            
    }

    //public MidiDevice[] discoverPedal() {
    @Override
    public void discoverPedal() {
        boolean inputconnected = false;
        boolean outputconnected = false;
        // discover and connect the pedal's MIDI input and output ports
        MidiDevice.Info[] infos = CoreMidiDeviceProvider.getMidiDeviceInfo();
        status = "connecting";
        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);
                String devtype = device.getClass().getSimpleName();
                String devname = device.getDeviceInfo().getName();
                //System.out.println("device "+devname+" type='"+devtype+"'");
                if(    (devname.contains("Effigy Labs Control Pedal")) &&
                        ( devtype.contains("CoreMidiSource") )
                        )   {
                    status = "found input";                        
                    // set this device to be the receiver
                    pedalIn = device;
                    pedalDevices[0] = device;
                    pedalDevices[0].open();
                    inputconnected = true;
                }
                if(    (devname.contains("Effigy Labs Control Pedal")) &&
                        ( devtype.contains("CoreMidiDestination") )
                        )   {
                    status = "found output";                        
                    // set this device to be the receiver
                    pedalOut = device;
                    pedalDevices[1] = device;
                    pedalDevices[1].open();
                    outputconnected = true;
                }
                if(inputconnected && outputconnected) {
                    System.out.println("discovered and connected to pedal");
                    status = "connected";
                    connected = true;
                    break; // stop looking
                }
            }catch (MidiUnavailableException e) {
                status = "error connecting to pedal: "+e.toString(); // Handle or throw exception...
                System.err.println("error connecting to pedal: "+e.toString());
            } catch (Exception e) {
                status = "error connecting to pedal: "+e.toString(); // Handle or throw exception...
                System.err.println("error connecting to pedal: "+e.toString());
            }
        }    
        
        if(!(inputconnected && outputconnected)) {
        status = "not connected";
        connected = false;
        }
                
    //return pedalDevices;
    }


    @Override
    public int apicmd(int cmd, byte[] data) {
        //if(!this.connected) return 0; // once implemented
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void loadPreset(int presetToLoad) {
        //if(!this.connected) return; // once implemented
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAttribute(int preset, int mode, int position, int slot, int attr, byte data) {
        if(!this.connected) return;
        StringBuilder sb = new StringBuilder();

        Formatter formatter = new Formatter(sb,Locale.US);
        formatter.format("%02x ",(int) API_SET_ATTR); 
        formatter.format("%02x ",(int) preset);
        formatter.format("%02x ",(int) mode);
        formatter.format("%02x ",(int) position);
        formatter.format("%02x ",(int) slot);
        formatter.format("%02x ",(int) attr);
        formatter.format("%02x ",(int) data);
        String cmd = sb.toString();

        try {
            sendMsg(convertTextToByteArr(sysexHeaderTxt + cmd + sysexFooterTxt));
        } catch (MidiUnavailableException ex) {
            Logger.getLogger(pApi.class.getName()).log(Level.SEVERE, null, ex);
        }
                
    }

    @Override
    public void setAttribute(SlotAddress sa, int attr, byte data) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPreset(int presetNum, String presetdata) {
        if(!this.connected) return;
        try {
            // send preset to pedal            
            //sendMsg(convertTextToByteArr(sysexHeaderTxt + "02 " + presetNum + " "+ pedalBank.preset[presetNum].toString()+ sysexFooterTxt)); // tell pedal to send preset pr
            
            sendMsg(convertTextToByteArr(sysexHeaderTxt + "02 0" + presetNum + " " + presetdata + sysexFooterTxt));
        } catch (MidiUnavailableException ex) {
            Logger.getLogger(pApi.class.getName()).log(Level.SEVERE, null, ex);
            status = "Error setting preset " + presetNum + ":" +ex;
        }

    }

    @Override

    public void setBank(byte[] data) {
            //download  pedal bank from the pedal itself (pedal uploads to us)
/*
            byte[] sndBuf = null;
            
            for(int pr=0;pr<5;pr++) {
                try {
                    sndBuf = api.sendReceive(convertTextToByteArr("F0 00 02 21 02 0" + pr + " "+ pedalBank.preset[pr].toString()+" F7")); // tell pedal to send preset pr
                    System.out.println("sent preset "+pr);
                    //System.out.println("preset "+pr+"="+byteToTextHexPairs(rcvBuf));
                } catch (InterruptedException ex) {
                    status = "Error sending pedal bank, preset "+pr+":"+ex;
                    System.err.println(status);
                    Logger.getLogger(Pedal.class.getName()).log(Level.SEVERE, null, ex);
                    
                }  
              pedalBank.preset[pr].importArr(sndBuf, 6);
            }
*/
    }

    @Override
    public byte[] getPreset(int presetNumber) {
        if(!this.connected) {
            byte nothin[] = new byte[] { 0x00 };
                return nothin;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] getSlot(int preset, int mode, int position, int slot) {
        if(!this.connected) {
            byte nothin[] = new byte[] { 0x00 };
                return nothin;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] getSlot(SlotAddress sa) {
        if(!this.connected) {
            byte nothin[] = new byte[] { 0x00 };
                return nothin;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSlot(int preset, int mode, int position, int slot, byte[] data) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSlot(SlotAddress sa, byte[] data) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMode(int mode) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void enterModeSwitch() {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSensitivity(int sensitivityPct) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void recalibrate() {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void recalibrate(int samples) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reboot() {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void changeAllChannels(int channelNum) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPorchSize(int porchSize) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setBootMode(int bootMode) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setCeilingSize(int ceilingSize) {
        if(!this.connected) return;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setBrightness(int brightnessPercent) {
        if(!this.connected) return;
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SystemBlock getSystemBlock() {
    //public SystemBlock getSystemBlock() {
            if(!connected) {
            SystemBlock nothin= new SystemBlock();
                return nothin;
        }
        byte[] trimmedrawresults = null;
        SystemBlock systemblock = new SystemBlock();
        try {
            //byte[] getSystemBlockSysex = {0xF7 0x00 0x02 0x21 0x11 0xF7}; // complete command to tell pedal to send it's system  block to us
            System.out.println("retrieving system block...");
            //status = "retrieving system block";
            byte[] rawresults = null;
            rawresults = sendReceive(systemblock.getSystemBlockSysexMsg,waitTime);
            try {
                if(!(rawresults.length > 0)) {
                    System.err.println("Error retrieving system block, pedal may be still disconnected.");
                    SystemBlock nothin = new SystemBlock();
                    return nothin; 
                }
                
            } catch (Exception e) {
                    System.err.println("Error retrieving system block, pedal may be still disconnected:"+e);
                    SystemBlock nothin = new SystemBlock();
                    return nothin; 
            }

            
            int offset = 5;
            int systemBlockSize = 12;
            trimmedrawresults = new byte[systemBlockSize*2];
            
            System.arraycopy(rawresults, offset, trimmedrawresults, 0, systemBlockSize*2);

            // recombine simply
            systemblock.serialNumber = reCombineInt( trimmedrawresults[2],
                                                  trimmedrawresults[3],
                                                  trimmedrawresults[0],
                                                  trimmedrawresults[1]);
            System.out.println("systemblock.serialNumber="+systemblock.serialNumber);
            
            systemblock.knobMaxValue = reCombineInt( trimmedrawresults[6],
                                                  trimmedrawresults[7],
                                                  trimmedrawresults[4],
                                                  trimmedrawresults[5]);
       //     System.out.println("systemblock.knobMaxValue="+systemblock.knobMaxValue);

            systemblock.knobCurrentValue = reCombineInt( trimmedrawresults[10],
                                                  trimmedrawresults[11],
                                                  trimmedrawresults[8],
                                                  trimmedrawresults[9]);
     //       System.out.println("systemblock.knobCurrentValue="+systemblock.knobCurrentValue);
            
            //int numbytes = 3;
            //int bytespernum = 4;
            //int bitadr = numbytes * bytespernum * 8;
            //bitadr--;
            // the rest are bytes
            systemblock.bootmode = reCombineInt(trimmedrawresults[12], trimmedrawresults[13]);
    //        System.out.println("systemblock.bootmode="+systemblock.bootmode);

            systemblock.presetToLoad = reCombineInt(trimmedrawresults[14], trimmedrawresults[15]);
      //      System.out.println("systemblock.presetToLoad="+systemblock.presetToLoad);

            systemblock.knobControl = reCombineInt(trimmedrawresults[16], trimmedrawresults[17]);
        //    System.out.println("systemblock.knobControl="+systemblock.knobControl);

            systemblock.fademax = (byte) reCombineInt(trimmedrawresults[18], trimmedrawresults[19]);
          //  System.out.println("systemblock.fademax="+systemblock.fademax);

            systemblock.porchsize= (byte) reCombineInt(trimmedrawresults[20], trimmedrawresults[21]);
  //          System.out.println("systemblock.porchsize="+systemblock.porchsize);

            systemblock.ceilsize = (byte) reCombineInt(trimmedrawresults[22], trimmedrawresults[23]);
    //        System.out.println("systemblock.ceilsize="+systemblock.ceilsize);
            
            return systemblock;
        } catch (Exception ex) {
            Logger.getLogger(Pedal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return systemblock;
    }
    
    @Override
    public byte[] sendBank() {
        if(!this.connected) {
            byte nothin[] = new byte[] { 0x00 };
                return nothin;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPresetToLoad(int presetNum) {
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] getSwVersion() {
        if(!this.connected) {
            byte nothin[] = new byte[] { 0x00 };
                return nothin;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    //pedalApi pap = new pedalApi() {};

 String convertByteToTextArray(byte[] arrayToConvert) {
        String returnStr = "";
        if(arrayToConvert.length == 0) return returnStr;
        
        for(int lc=0;lc<arrayToConvert.length;lc++) {
            returnStr+=(String.format("%02X",arrayToConvert[lc]));
            //returnStr.concat(arrayToConvert[lc].
            returnStr+=" ";                    
        }
        return returnStr;
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
                int ng = Integer.parseInt(gn,16);
                //System.out.println("["+gct+"]="+ng);
                buffTmp[gct] = (byte) ng;
                //System.out.print(gct+" ");
                gct++;
            }
            //System.out.println("gct="+gct);
            buffRtn = new byte[gct];
            for(int c=0;c<gct;c++) {
                //System.out.println("c="+c);
                buffRtn[c] = (byte) buffTmp[c];
            }
            //System.arraycopy(buffTmp, 0, buffRtn, 0, gct); //return buffPreset; - do not use, arraycopy is broken for this type of copy
            //System.out.println("rtnSz="+gct);
            return buffRtn;
            
        } // regex match the group. 
        catch (Exception e) {
               System.out.println("a Problem finding the numbers in the text: "+e);
        }
       return buffRtn; // should never get here
}

    public byte[] sendReceive(byte[] sysexData) throws InterruptedException {
        if(!this.connected) {
            byte nothin[] = new byte[] { 0x00 };
                System.err.println("sendReceive:not connected");
                return nothin;
        }
        return sendReceive(sysexData, waitTime);
    }
           
    // send a command and/or data to the pedal and get a response
    public byte[] sendReceive(byte[] sysexData,int timeoutMs) throws InterruptedException {
        if(!this.connected) {
            byte nothin[] = new byte[] { 0x00 };
                System.err.println("sendReceivewt:not connected");
                return nothin;
        }
            if(inMidSysex) {
                System.out.println("api: new sendReceive request received prior to completion of previous request.");                
            } else {
                inMidSysex = true;
            }
            
            try {
                // send and wait for a response here
                //ShortMessage myMsg = new ShortMessage();
                SysexMessage mySys = new SysexMessage();
                try {
                    mySys.setMessage(sysexData, sysexData.length);
                } catch (InvalidMidiDataException ex) {
                    Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
                long timeStamp = -1;
                //send buffer contents to pedal, pealOut is the outbound connection to the pedal
                
                /////////// set up the outgoing path
                
                try {
                    if(!(pedalOut.isOpen())) pedalOut.open();
                } catch (Exception e) {
                    System.err.println("Error opening pedal: "+e.toString());
                    //txtActionStatus.setText("Error opening pedal: "+e.getMessage());
                }
                Receiver rcvr = pedalOut.getReceiver();
                
                /////////////// set up the incoming path - may not be needed
                
                try {
                    if(!(pedalIn.isOpen())) pedalIn.open();
                } catch (Exception e) {
                    System.err.println("Error opening pedal: "+e.toString());
                    //txtActionStatus.setText("Error opening pedal: "+e.getMessage());
                }
                // connect the pedal to code we control, the MyReceiver class.
                MyReceiver incomingRcvr = new MyReceiver(); // instantiate our special message-intercepting receiver
                Transmitter xmtr = pedalIn.getTransmitter(); // get the transmitter sending the midi messages from the pedal
                xmtr.setReceiver(incomingRcvr); // connect the pedal's transmitter to our receiver, and now we're waiting
                System.out.println("created transmitter and connected incoming receiver");
                
                rcvr.send(mySys, timeStamp); // send request
                System.out.println("sent api command to pedal.");
                
                ////////////////////
                // get a response //
                ////////////////////
                // if the command has a response, set up the receiver and receive and process the response
                //if(argResponseList[comApiCmd.getSelectedIndex()] == true) {
                    // wait a bit for a response
                    long ctm = System.currentTimeMillis();
                    while(incomingRcvr.hasSysexMessage == false) { // incoming response?
                        if((System.currentTimeMillis() - ctm) > timeoutMs) { // 2 second wait currently
                            //ctm = System.currentTimeMillis(); // ???
                            //txtActionStatus.setText("Timed out waiting for pedal");
                            System.err.println("Timed out waiting for pedal for "+timeoutMs+" ms");
                            inMidSysex = false;
                            break; // bail after 2 seconds of waiting
                        }
                        Thread.sleep(1); // timing for more  reliable communication
                        // System.out.print(".");// do nothing
                    };
                    
                    ///////////////////////////////////
                    // process response if present   //
                    ///////////////////////////////////
                    
                    if(incomingRcvr.hasSysexMessage) { // it won't if we timed out
                        byte[] testinc = incomingRcvr.getMessage();
                        if(pedalOut.isOpen()) pedalOut.close(); // **************** uncomment out the closes to keep the receiver open
                        if(pedalIn.isOpen()) pedalIn.close(); //  ^^
                        System.out.println("received message from pedal, len="+testinc.length);
                        inMidSysex = false;
                        return testinc;
                        }
                        
                        // do something with the incoming message depending on what we're waiting on.
                                       
                // command is sent, any response has been received.
                //System.out.println("closing pedal");
            } catch (MidiUnavailableException ex) {
                Logger.getLogger(Pedal.class.getName()).log(Level.SEVERE, null, ex);
                inMidSysex = false;
            }
        byte[] resp = null;
        inMidSysex = false;
        return resp;
        //throw Exception("not implemented yet");
        
    }

    private void sendMsg(byte[] xfrba) throws MidiUnavailableException {                                           

            inMidSysex = true;
            SysexMessage mySys = new SysexMessage();
            try {
                mySys.setMessage(xfrba, xfrba.length);
            } catch (InvalidMidiDataException ex) {
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            long timeStamp = -1;
            //send buffer contents to pedal, pealOut is the outbound connection to the pedal

            /////////// set up the outgoing path

            try {
                if(!(pedalOut.isOpen())) pedalOut.open();
            } catch (Exception e) {
                System.err.println("Error opening pedal: "+e.toString());
                //txtActionStatus.setText("Error opening pedal: "+e.getMessage());
            }
            Receiver rcvr = pedalOut.getReceiver();

            /////////////// set up the incoming path - may not be needed

            rcvr.send(mySys, timeStamp); // send request
            System.out.println("sent api command to pedal.");
            inMidSysex = false;

            // do something with the incoming message depending on what we're waiting on. ****** tag **** if should close constantly or close/open on communication
            if(pedalOut.isOpen()) pedalOut.close();
            if(pedalIn.isOpen()) pedalIn.close(); //??
            
            // command is sent, any response has been received.
            //System.out.println("closing pedal");
    }                                          

    
    public void sync() {
       //System.out.println(pedalBank.hasChanged);
     // send changed data to pedal
            
      //}
     }

    
    public MyReceiver getListener() {
        // set up receiver code here
        MyReceiver rcvr = new MyReceiver();
        //System.out.println("api: receiver");
        return rcvr;
    }
    
    public String getSwVer() {

        byte[] rawresults;
        int offset = 5;
        int verSize = 1;

        try {
            rawresults = sendReceive(convertTextToByteArr(sysexHeaderTxt + "14" + sysexFooterTxt));
            byte[] trimmedrawresults = new byte[(rawresults.length-6)];
            System.arraycopy(rawresults, offset, trimmedrawresults, 0, (rawresults.length-6));
            //  recombine simply
            byte x1 = (byte) (trimmedrawresults[0] * 16);
          //  System.out.println("x1="+x1);

            byte x2 = (byte) ((byte) trimmedrawresults[1] + x1);
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb,Locale.US);
            formatter.format("%02d",x2);
            
            swVersion = "R4C_" + formatter.toString();
            System.out.println("software version reported:"+swVersion);
        
        } catch (InterruptedException ex) {
            Logger.getLogger(Pedal.class.getName()).log(Level.SEVERE, null, ex);
        }
            
       return swVersion;
    }

    @Override
    public void setPreset(int presetNum, byte[] data) {
            System.out.println("setPreset "+presetNum);
//send  pedal bank to the pedal itself (pedal downloads from us)
            // do it a preset at a time rather than individual slots or attributes.
//            byte[] sndBuf = null;
            
  //          for(int pr=0;pr<5;pr++) {
                try {
                    //api.setPreset(pr, pedalBank.preset[pr].toByteArr());
                    
                    sendMsg(convertTextToByteArr(sysexHeaderTxt + "02 0" + presetNum + " "+ convertByteToTextArray(data) + sysexFooterTxt));
                    System.out.println("sent preset "+presetNum);
                    
                } catch (Exception ex) {
                    status = "Error sending pedal bank, preset "+presetNum+":"+ex;
                    System.err.println(status);
                    Logger.getLogger(Pedal.class.getName()).log(Level.SEVERE, null, ex);
                    
                }  
              //pedalBank.preset[presetNum].importArr(data, 6); //?? ******** tag
            

        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    private short reCombineInt(byte trimmedrawresult, byte trimmedrawresult0, byte trimmedrawresult1, byte trimmedrawresult2) {
            short pt1;
            short pt2;
            short pt3;
            short pt4;
            // serial number is in the first four bytes, first two are the little endian  and second two are big endian
            // the math is done here explicitly to show what is happening and may be less obscure than bitwise shifting
            // and converting back and forth...
            pt1 = (short) (trimmedrawresult1 * 16);
            pt2 = (short) trimmedrawresult2;
            pt3 = (short) (trimmedrawresult * 4096);
            pt4 = (short) (trimmedrawresult0 * 256);
            short result = (short) (pt1 + pt2 + pt3 + pt4); 
            //System.out.println("reCombine result="+result);
            return result;
    }

    private short reCombineInt(byte trimmedrawresult, byte trimmedrawresult0) {
            short pt1;
            short pt2;            // serial number is in the first four bytes, first two are the little endian  and second two are big endian
            // the math is done here explicitly to show what is happening and may be less obscure than bitwise shifting
            // and converting back and forth...
            pt1 = (short) (trimmedrawresult * 16);
            pt2 = (short) trimmedrawresult0;
            short result = (short) (pt1 + pt2); 
            //System.out.println("reCombine result="+result);
            return result;
    }

    // recombine from high+low pairs, big-endian I suppose
    private byte[] reCombine(byte[] data) {
            byte[] results = new byte[(int) data.length / 2];
            for(int c=0;c<data.length;c+=2) {
                results[c] =  (byte) ((data[c] * 16)+ (data[c+1]));
            }
            
      return results;           
    }

}
