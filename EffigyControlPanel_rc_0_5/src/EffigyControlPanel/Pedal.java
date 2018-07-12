package EffigyControlPanel;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.midi.MidiDevice;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jody
 * Defines a pedal and its structure and communication interfaces
 * developed originally for the Effigy Control Pedal
 * (C) 2018, Effigy Labs LLC
 */
public class Pedal {
    public String status = "uninitialized";
    //public MidiDevice[] pedalDevices = new MidiDevice[2];
    public SystemBlock sysblk = new SystemBlock();
    public String swVersion = "";
    public Bank pedalBank = null; // main storage for preset slot data
    public boolean connected = false; // master flag to test for a connected device
    public boolean inMidSysex = false; // synchronization flag for multiple MIDI messages arriving before the last MIDI message is completed. not sure if we'll use this like this.
    public MidiDevice device;
    public MidiDevice pedalIn; // what we get messages from the pedal from
    public MidiDevice pedalOut; // what we send messages to the pedal to
    public int waitTime = 5000; // default wait time to receive response from pedal in send/receive operations.
    
    String sysexHeaderTxt = "F0 00 02 21 ";
    String sysexFooterTxt = " F7"; 
    byte[] sysexHeader = { (byte) 0xF7, (byte) 0x00, (byte) 0x02, (byte) 0x21 };
    byte[] sysexDelimiter = { (byte) 0xF7 };

    pApi api;
    
    
    Pedal() {
        try {
            pedalBank = new Bank();
            api = new pApi();
            api.discoverPedal();
            if(api.connected)  {
                sysblk = api.getSystemBlock();
                getBankFromPedal();
                // get last known bank synced? ****** tag 
                pedalBank.saveBank("Pedal Bank"); // save to file always get the pedal bank and save it to bank zero before doing more
                // api.
             }
            
        } catch (Exception e) {
            System.err.println("error discovering pedal:"+e);
        }
    }
    
    public void getBankFromPedal() {
            byte[] rcvBuf = null;
            
            for(int pr=0;pr<5;pr++) {
                try {
                    // rcvBuf = sendReceive(convertTextToByteArr("F0 00 02 21 12 " + pr + " F7")); // prototypical command to get the whole system block at once (take out of the pr loop)
                    rcvBuf = api.sendReceive(convertTextToByteArr("F0 00 02 21 03 0" + pr + " F7")); // tell pedal to send preset pr
                    System.out.println("received preset "+pr);
                    //System.out.println("preset "+pr+"="+byteToTextHexPairs(rcvBuf));
                } catch (InterruptedException ex) {
                    status = "Error getting pedal bank "+ex;
                    System.err.println(status);
                    Logger.getLogger(Pedal.class.getName()).log(Level.SEVERE, null, ex);
                    
                }  
              pedalBank.preset[pr].importArr(rcvBuf, 6);
            }
        
    }
    public void sendBankToPedal() {
            //send  pedal bank to the pedal itself (pedal downloads from us)
            // do it a preset at a time rather than individual slots or attributes.
            byte[] sndBuf = null;
            
            for(int pr=0;pr<5;pr++) {
                try {
                    api.setPreset(pr, pedalBank.preset[pr].toByteArr());
                    //ndReceive(convertTextToByteArr("F0 00 02 21 02 0" + pr + " "+ pedalBank.preset[pr].toString()+" F7")); // tell pedal to send preset pr
                    System.out.println("sent preset "+pr);
                    //System.out.println("preset "+pr+"="+byteToTextHexPairs(rcvBuf));
                } catch (Exception ex) {
                    status = "Error sending pedal bank, preset "+pr+":"+ex;
                    System.err.println(status);
                    Logger.getLogger(Pedal.class.getName()).log(Level.SEVERE, null, ex);
                    
                }  
              pedalBank.preset[pr].importArr(sndBuf, 6);
            }
    
}
    
    public String convertbytePresetToText(byte[] arrayToConvert) {
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

    
    public boolean isPedalConnected() {
          return connected;
    }




}