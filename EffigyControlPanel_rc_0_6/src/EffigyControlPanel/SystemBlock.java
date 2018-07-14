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
public class SystemBlock {
            
  int serialNumber = 0; // 2 bytes d of R4Cddddd
  int knobMaxValue = 0; // max knob value calibrated at factory - 0-1024 for a 1M potentiometer as are the B1M pots in the R4Cs.
  int knobCurrentValue = 0; // saved knob value as saved by last flip
  int bootmode = 0; // 0=mode select, 1-3 = autoselect mode 1-3
  int presetToLoad = 0; // 0=factory preset, 1-5 = selects user-defined preset
  int knobControl = 0;  // toggle between knob as sensitivity control and as a position, 0/false - sensitivity, 1/true = pos 4  
  byte fademax = 16; // led fade max (brightness) (%)
  byte porchsize = 5; // stored/altered porch size
  byte ceilsize = 5; // stored/altered porch size
  // adding new attributes should be bitsize-specified and remove the field size from the pool of remaining reserved bits
  int byteborder;
  int reserved; // for now reserved the remaining 95 bytes just to see...
  byte[] getSystemBlockSysexMsg = new byte[] {(byte) 0xF0, (byte) 0x00, (byte) 0x02, (byte) 0x21, (byte) 0x11, (byte) 0xF7};
  byte[] factoryResetBlock;
  
        public SystemBlock() {
            refreshData();
            //serialNumber = 0;
        }

        public void sync() {
            //System.out.println(pedalBank.hasChanged);
                // send changed data to pedal
                
            //}
        }
        public  void refreshData() {
            // sysex msg to get systemblock
            //byte[] sysexHeader = { (byte) 0xF7, (byte) 0x00, (byte) 0x02, (byte) 0x21 };
            // check if pedal open?
            // sendreceive 
            byte[] resp = null;
     //       resp = sendReceive(getSystemBlockSysexMsg);

        }

        
        

    
}
