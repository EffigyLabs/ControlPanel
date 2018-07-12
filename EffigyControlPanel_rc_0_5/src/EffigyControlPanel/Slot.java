/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EffigyControlPanel;

import java.util.Formatter;
import java.util.Locale;

/**
 *
 * @author jody
 * A slot is a collection of attributes and methods used to create an interface between the sensor input and the desired MIDI output.
 * A slot consists of both MIDI elements, such as the MIDI command word and channel, and Effigy-specific elements such as the latching, curve, and min/max functions.
 * A slot on the pedal consists of 8 public attributes, and (at least) 2 private attributes (confirmed and latched).
 * The constructor can accept a slot address object for the slot addressability.
 * Slots can get and set their attributes cardinally or by name, and can generate a string version of themselves via toString override method.
 */
public class Slot {
               SlotAddress sa; // for when it needs to be aware of it's own address.  Like house numbers.
                
               int preset = 0;
               int mode = 0;
               int position = 0;
               int slotnumber = 0;
               
               int MidiCommand = 0;
               int MidiChannel = 0;
               int MidiSubCommand = 0;
               int curvetype = 0;
               int curvedirection = 0;
               int min = 0;
               int max = 100;
               int latching = 0;
               int latched = 0; // really only implemented inside the pedal but mentioned here for documentation
               int changed = 0;
               int confirmed = 0;
               
               //other constructors to be included
               // Slot(SlotAddres sa, attr, value)
               void Slot(int cmd, int chn, int sub, int curvedir, int curvetype, int minner, int maxer, int latch) {
                   MidiCommand = cmd;
                   this.MidiChannel = chn;
                   this.MidiSubCommand = sub;
                   this.curvetype = curvetype;
                   this.curvedirection = curvedir;
                   this.min = minner;
                   this.max = maxer;
                   this.latching = latch;
               }
               
               void Slot(int presetin, int modein, int positionin, int slotin) {
                   System.out.println("slot constructor w slot addr");
                   Integer x = new Integer(presetin);
                   preset = presetin;
                   mode = modein;
                   position = positionin;
                   slotnumber = slotin;
                   sa.preset = new Integer(presetin);
                   sa.mode = modein;
                   sa.position = positionin;
                   sa.slot = slotin;
               }
               
               void Slot() {
                 System.out.println("in slot constructor w no args");
                 MidiCommand  = new Integer(0);
                 preset = 0;
                 preset = 0;
                 mode = 0;
                 position = 0;
                 slotnumber = 0;
                 sa.preset = preset;
                 sa.mode = mode;
                 sa.position = position;
                 sa.slot = slotnumber;
               }

   public String toString() {
        String rtnval = "";
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb,Locale.US);

        formatter.format("%02X ",MidiCommand);
        formatter.format("%02X ",MidiChannel);
        formatter.format("%02X ",MidiSubCommand);
        formatter.format("%02X ",curvetype);
        formatter.format("%02X ",curvedirection);
        formatter.format("%02X ",min);
        formatter.format("%02X ",max);
        formatter.format("%02X",latching);
        rtnval = sb.toString().toUpperCase(); // put in uppercase coz I like it like that
        return rtnval;
    }

    public int[] toIntArr() {
        int[] rtnval = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
        rtnval[0] = MidiCommand;
        rtnval[1] = MidiChannel;
        rtnval[2] = MidiSubCommand;
        rtnval[3] = curvetype;
        rtnval[4] = curvedirection;
        rtnval[5] = min;
        rtnval[6] = max;
        rtnval[7] = latching;
        return rtnval;
    }

    public byte[] toByteArr() {
        byte[] rtnval = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
        rtnval[0] = (byte) MidiCommand;
        rtnval[1] = (byte) MidiChannel;
        rtnval[2] = (byte) MidiSubCommand;
        rtnval[3] = (byte) curvetype;
        rtnval[4] = (byte) curvedirection;
        rtnval[5] = (byte) min;
        rtnval[6] = (byte) max;
        rtnval[7] = (byte) latching;
        return rtnval;
    }

    void emptySlot() {
//               preset = 0;
//               mode = 0;
//               position = 0;
//               slotnumber = 0;
               
               MidiCommand = 0;  // this is the main thing here that "empties" the slot
               MidiChannel = 0;
               MidiSubCommand = 0;
               curvetype = 0;
               curvedirection = 0;
               min = 0;
               max = 100;
               latching = 0;
               latched = 0; // host object only
               changed = 0; // host object only
               confirmed = 0; // host obj only
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    public int getAttrValInt(int selectedIndex) {
        return (Integer.parseInt(getAttrVal(selectedIndex)));
    }
    
    public String getAttrVal(int selectedIndex) {
        switch (selectedIndex) {
        case 0: return   String.valueOf(MidiCommand);
        case 1: return String.valueOf(MidiChannel);
        case 2: return String.valueOf(MidiSubCommand);
        case 3: return String.valueOf(curvetype);
        case 4: return String.valueOf(curvedirection);
        case 5: return String.valueOf(min);
        case 6: return String.valueOf(max);
        case 7: return String.valueOf(latching);
        default: return String.valueOf(MidiCommand);
        }
    }
    
    public void setAttrVal(int selectedIndex, byte val) {
        switch (selectedIndex) {
        case 0: 
            MidiCommand = val;
            break;
        case 1: 
            MidiChannel = val;
            break;
        case 2: 
            MidiSubCommand = val;
            break;
        case 3: 
            curvetype = val;
            break;
        case 4: 
            curvedirection = val;
            break;
        case 5: 
            min = val;
            break;
        case 6: 
            max = val;
            break;
        case 7: 
            latching = val;
            break;
        default: MidiCommand = val;
        }
    }

}
