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
 * the Preset class establishes data structures and methods for import/export to and from the structure.
 * The structural unit of the preset is superceded by an array of 5 presets called a bank.
 * The bank is currently implemented as the pedal's ability to store 1 bank/5 presets in its eeprom,
 *    including preset zero which is the LIVE preset.
 * The rest of the current model is based on a preset and its components.  
 * Each preset consists of 3 modes.  Each mode consists of 4 positions.  Each position consists of 3 slots.  
 * A slot is the information required to produce output from input from the pedal sensors and knob.
 * 
 * Presets may be exported, imported, queried, or output to a string.
 */
public class Preset {
    public String status = "uninitialized";
    public Mode mode[];

    public Preset() {
        mode = new Mode[3];
        for(int c=0;c<3;c++) {
            mode[c] = new Mode(); // instantiate the three modes comprosing a preset
        }
        status = "initialized";
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb,Locale.US);
        for(int moder=0;moder<3;moder++) {
            for(int pos=0;pos<4;pos++) {
                for(int slot=0;slot<3;slot++) {
                    formatter.format("%02x ",mode[moder].pos[pos].slot[slot].MidiCommand);
                    if (mode[moder].pos[pos].slot[slot].MidiCommand > 0) {
                        formatter.format("%02x ",mode[moder].pos[pos].slot[slot].MidiChannel);
                        formatter.format("%02x ",mode[moder].pos[pos].slot[slot].MidiSubCommand);
                        formatter.format("%02x ",mode[moder].pos[pos].slot[slot].curvetype);
                        formatter.format("%02x ",mode[moder].pos[pos].slot[slot].curvedirection);
                        formatter.format("%02x ",mode[moder].pos[pos].slot[slot].min);
                        formatter.format("%02x ",mode[moder].pos[pos].slot[slot].max);
                        formatter.format("%02x ",mode[moder].pos[pos].slot[slot].latching);
                    }
                }
            }
        }
    return sb.toString().trim().toUpperCase();
    }

    /**
     * Produces a valid, readable Preset in THP (Text Hex Pair) format, with comments and lines for each slot
     * @return 
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb,Locale.US);
        for(int moder=0;moder<3;moder++) {
            for(int pos=0;pos<4;pos++) {
                for(int slot=0;slot<3;slot++) {
                    formatter.format("#  mode "+moder+" pos "+pos+" slot "+slot+"");
                    if (mode[moder].pos[pos].slot[slot].MidiCommand > 0) {
                        formatter.format("\n#   MidiCommand\n"+mode[moder].pos[pos].slot[slot].MidiCommand);
                        formatter.format("\n#   MidiChannel\n"+mode[moder].pos[pos].slot[slot].MidiChannel);
                        formatter.format("\n#   MidiSubCmd\n"+mode[moder].pos[pos].slot[slot].MidiSubCommand);
                        formatter.format("\n#   curvetype\n"+mode[moder].pos[pos].slot[slot].curvetype);
                        formatter.format("\n#   curvedirection\n"+mode[moder].pos[pos].slot[slot].curvedirection);
                        formatter.format("\n#   min\n"+mode[moder].pos[pos].slot[slot].min);
                        formatter.format("\n#   max\n"+mode[moder].pos[pos].slot[slot].max);
                        formatter.format("\n#   latching\n"+mode[moder].pos[pos].slot[slot].latching);
                        formatter.format("\n");
                    } else {
                        formatter.format("\n#empty\n");
                    }
                }
            }
        }
    formatter.format("#preset end\n");
    return sb.toString().trim().toUpperCase();
    }

    public int[] toIntArr() {
        System.out.println("preset:toIntArr()");
        int[] rtnval = new int[288];
        int bufPtr = 0;
        for(int moder=0;moder<3;moder++) {
            for(int pos=0;pos<4;pos++) {
                for(int slot=0;slot<3;slot++) {
                   rtnval[bufPtr] = mode[moder].pos[pos].slot[slot].MidiCommand;
                   //System.out.println("m="+moder+",p="+pos+",s="+slot+",attr=0,bufPtr="+bufPtr+",v="+mode[moder].pos[pos].slot[slot].getAttrVal(0));
                    bufPtr++;
                    if (mode[moder].pos[pos].slot[slot].MidiCommand > 0) {
                        for(int sp=0;sp<7;sp++) {
                            rtnval[bufPtr+sp] = mode[moder].pos[pos].slot[slot].getAttrValInt(sp+1); // update the slot data in our structure
                            //System.out.println("m="+moder+",p="+pos+",s="+slot+",attr="+sp+",bufPtr="+bufPtr+",v="+mode[moder].pos[pos].slot[slot].getAttrVal(sp));
                        }
                        bufPtr+=7;
                    }
                }
            }
        }
        int[] result = new int[bufPtr];
        for(int c=0;c<bufPtr;c++) {
            result[c] = rtnval[c];
        }
        //System.arraycopy(rtnval, 0, result, 0, bufPtr); // this is broken for this, do not use
    return result;
    }

    public byte[] toByteArr() {
        //byte[] rtnval = toIntArr().
        int[] intval = toIntArr();
        byte[] rtnval = new byte[intval.length];
        for(int c=0;c<intval.length;c++) {
            rtnval[c] = (byte) intval[c];
        }
    return rtnval;
    }

    void importArr(byte[] testinc, int offset) {
       int bufPtr = offset;
        for(int moder=0;moder<3;moder++) {
            for(int pos=0;pos<4;pos++) {
                for(int slot=0;slot<3;slot++) {
                    mode[moder].pos[pos].slot[slot].MidiCommand = testinc[bufPtr]; // update the midicommand in our structure
                    bufPtr++;
                    if (mode[moder].pos[pos].slot[slot].MidiCommand > 0) {
                        for(int sp=0;sp<7;sp++) {
                            mode[moder].pos[pos].slot[slot].setAttrVal(sp+1, testinc[bufPtr+sp]); // update the slot data in our structure
                        }
                        status="pre slot="+mode[moder].pos[pos].slot[slot].toString();
                        bufPtr+=7;                        
                    }
                }
            }
        }
    }
}
