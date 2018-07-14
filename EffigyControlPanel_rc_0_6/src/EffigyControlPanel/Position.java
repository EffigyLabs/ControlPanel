/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EffigyControlPanel;

/**
 *
 * @author jody
 * A position is an array of 3 slots.  A slot is the information required to produce output from the input sensor associated with this position.
 * For example, the  input sensor #1 contains three slots which operate independently of each other except for the input from the sensor in that position.
 * Each slot operates simultaneously as the others for that position.  By configuring the slots differently, you can create almost any effect.  For example,
 * slot 1 could produce a MOD effect up to 90%, and an aftertouch effect on slot 2 could operate from 90-100% to build almost any of your own aftertouch-like effects.
 */
public class Position {
    Slot slot[];

    public Position() {
        slot = new Slot[3];
        for(int c=0;c<3;c++) {
            slot[c] = new Slot();
        }
        //System.out.println("making slots");
    }
}
