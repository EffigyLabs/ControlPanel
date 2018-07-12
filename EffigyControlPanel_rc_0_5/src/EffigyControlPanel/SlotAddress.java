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
 * A slot address is a collection of four integers which mirror the preset structure, at the four levels of preset, mode, position, and slot numbers.
 * A slot address for example is 0 0 0 0 meaning preset zero, mode 1, position 1, slot 1.  
 * Presets are numbered 0-4 and modes, slots, and positions are numbered 1-3 (1-4 for positions), no zero, to accentuate preset zero being the special, live, preset.
 */
public class SlotAddress {
    //int bank;
    int preset;
    int mode;
    int position;
    int slot;
    
    public SlotAddress() {
        preset = 0;
        mode = 0;
        position = 0;
        slot = 0;
    }
    public SlotAddress(int pn, int md, int pos, int sl) {
        preset = pn;
        mode = md;
        position = pos;
        slot = sl;
}
    @Override
    public String toString() {
        String rtnval = "";
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb,Locale.US);
        //formatter.format("02X",bank);
        //formatter.format(" ");
        formatter.format("%02X",preset);
        formatter.format(" ");
        formatter.format("%02X",mode);
        formatter.format(" ");
        formatter.format("%02X",position);
        formatter.format(" ");
        formatter.format("%02X",slot);
        rtnval = sb.toString().toUpperCase(); // put in uppercase coz I like it like that
        return rtnval;
    }

    public int[] toIntArr() {
        int[] rtnval = new int[] {0, 0, 0, 0};
        rtnval[0] = preset;
        rtnval[1] = mode;
        rtnval[2] = position;
        rtnval[3] = slot;
        return rtnval;
    }

    public byte[] toByteArr() {
        byte[] rtnval = new byte[] {0, 0, 0, 0};
        rtnval[0] = (byte) preset;
        rtnval[1] = (byte) mode;
        rtnval[2] = (byte) position;
        rtnval[3] = (byte) slot;
        return rtnval;
    }

}
