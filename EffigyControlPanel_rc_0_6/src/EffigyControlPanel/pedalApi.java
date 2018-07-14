/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EffigyControlPanel;

/**
 *
 * @author jody
 */
public interface pedalApi {

    int apicmd(int cmd, byte data[]);    

// apicmd = 0
// load a preset from EEPROM, essentially copies a non-zero preset to the live preset, preset# 1-4
void loadPreset(int presetToLoad); // pedal api 0

// discover pedal??
void discoverPedal(); // no pedal command but a connector

// send an attribute to the pedal and store in the specified address
void setAttribute(int preset, int mode, int position, int slot, int attr, byte data); // pedal api 1
void setAttribute(SlotAddress sa, int attr, byte data); // pedal api 1

// send a preset to the pedal and store in EEPROM.  if preset number is 0, it will be active immediately
void setPreset(int presetNum, String presetString); // pedal api 2
void setPreset(int presetNum, byte[] data); // pedal api 2, but convert the data yourself

// send 5 presets at a time to the pedal and store.
void setBank(byte[] data); // pedal api 2 - the data will be the same as 5 presets' data separated by a space, so just another outer loop around the regular preset parser will work

// get the specified preset number from the pedal.
byte[] getPreset(int presetNumber); // pedal api 3
//Preset getPreset(int presetNumber); // pedal api 3

// get slot data from the pecdal
byte[] getSlot(int preset, int mode, int position, int slot); // pedal api 4
byte[] getSlot(SlotAddress sa); // pedal api 4

// send a slot to the pedal
void setSlot(int preset, int mode, int position, int slot, byte data[]); // pedal api 5
void setSlot(SlotAddress sa, byte data[]); // pedal api 5
    
// switch modes in the pedal immediately - like hitting the mode switch and pressing a position automatically
void setMode(int mode); // pedal api 6

// like hitting the mode switch momentarily, enter into blinking position select and allow user to press on a position 1-3
void enterModeSwitch(); // pedal api 7

// set sensitivity knob virtual position, 0 = all the way to the left = largest range = lowest sensitivity, 100 = all the way to the right = smallest range = highest sensitivity
void setSensitivity(int sensitivityPct); // pedal api 8

// recalibrate with default numbber of samples or a given number of samples
void recalibrate(); // pedal api 9
void recalibrate(int samples); // pedal api 10

// restart the program (but do not reboot the hardware chip itself)
void reboot(); // pedal api 11

// change all channels - this may be wrong *********
void changeAllChannels(int channelNum); // pedal api 12

// change porch size
void setPorchSize(int porchSize); // pedal api 13: { 

// change boot mode
void setBootMode(int bootMode); // pedal api 14: { 

// change porch size
void setCeilingSize(int ceilingSize); // pedal api 15: { 

// set LED fade brightness
void setBrightness(int brightnessPercent); // pedal api 16

// send system block
//byte[] getSystemBlock(); // pedal api 17
SystemBlock getSystemBlock(); // pedal api 17

// send all five presets at once
byte[] sendBank(); // pedal api 18
// serializedBank sendBank(); // pedal api 18
 
// set which preset to load on startup, 0-4.  presetes 1-4 are copied to preset 0.  preset 0 is the default which is the live preset.
void setPresetToLoad(int presetNum); // pedal api 19

// report software version
byte[] getSwVersion(); // pedal api 20

// set which mode the knob is in, either sensitivity mode, or position 4.  this is also controlled by the 5-second mode switch press on the pedal itself
void setKnobMode(int knobMode); // pedal api 19


}
