/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EffigyControlPanel;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

/**
 *
 * @author jody
 */

   class MyReceiver implements Receiver  {
      Receiver rcvr;
      byte[] msg;
      String msgType = "";
      public boolean hasSysexMessage = false;
      public boolean hasMIDIMessage = false;
      
      public MyReceiver() {
         try {
            System.out.println("receiver:communicating with pedal...");
            this.rcvr = MidiSystem.getReceiver();
         } catch (MidiUnavailableException e) {
            e.printStackTrace();
         }
      }
       
      @Override
      public void send(MidiMessage message, long timeStamp) {
          
         //System.out.print("pedal msg: ");
         int mst = message.getStatus();
         int ml = message.getLength();
         msg = message.getMessage();
         System.out.println("receiver:incoming midi msg: type="+message.getClass()+", len="+ml);
         msgType = message.getClass().getSimpleName();
         if(msgType.contains("SysexMessage")) {
            hasSysexMessage = true;
            System.out.println(byteToTextHexPairs(msg));
         }
         if(msgType.contains("ShortMessage")) {
            hasMIDIMessage = true;
            System.out.println(byteToTextHexPairs(msg));
         }
         /*
         System.out.println("msg="+msg.toString());
         for(int mc = 0;mc<ml;mc++) {             
            System.out.format(", msg[%d]=%02x",+mc,(msg[mc] & 0xFF ));
            System.out.print(" ");
            //System.out.println("timeStamp="+timeStamp);
         
         }
         System.out.println();
         */
         //rcvr.send(message, timeStamp);  // thru?
      }
      
      public byte[] getMessage() {
          byte[] rtn = null;
          if (hasSysexMessage) {
              rtn = msg; 
              hasSysexMessage = false;
          }
          if (hasMIDIMessage) {
              rtn = msg; 
              hasMIDIMessage = false;
          }
          return rtn;
      }
      
      @Override
      public void close() {
         rcvr.close();
      }
   
   String byteToTextHexPairs(byte[] arrayToConvert) {
        String returnStr = "";
        if(arrayToConvert.length == 0) return returnStr;
        
        for(int lc=0;lc<arrayToConvert.length;lc++) {
            returnStr+=(String.format("%02X ",arrayToConvert[lc]));
            //returnStr.concat(arrayToConvert[lc].
            //returnStr+=" ";                    
        }
        return returnStr;
     }
   
   } 