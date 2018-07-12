/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EffigyControlPanel;

/**
 *
 * @author jody
 * 
 * A mode is a set of four positions.  A single mode is active at one time during pedal operation.
 * A mode's four positions may be activated simultaneously, according to the physical model and how the sensors and knob may be physically activated.
 * For example, in the current hardware, the center position may be activated at the same time as one of the side positions, but the side positions may not be
 * activated simultaneously because of the bump (and by intent).
 * The mode switch allows modes to be rapidly switched for real-time effects.
 */
public class Mode {
    Position pos[] = new Position[4];
    
    public Mode() {
        pos = new Position[4];
        for(int c=0;c<4;c++) {
            pos[c] = new Position();
        }
        //System.out.println("making positions!");
    }
        
}
