/*
 * A Bank is a group of 5 presets
 */
package EffigyControlPanel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import static java.util.Objects.nonNull;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;

/**
 *
 * @author jody
 * 
 * Class Bank
 *  pass zero or one argument, string bankName
 *  this class encapsulates all the file loading and saving.
 *  
 */
public class Bank {
    public String status = "uninitialized"; // for super to get any time
     public String fileSeparator;
     public String dataDir = ""; // get it from properrties later
     public Preset preset[]; // the bank data is just 5 preset objects
     public String bankName; // name we are given as a bank reference identifier
     public String fileName; // access to physical persistence device
     public int bankNumber; // special to the app if bank zero, the pedal's bank itself, not merely a created and saved bank, but the uploaded operational bank
     int numericPreset[] = new int[1440]; // 1 fully loaded bank, maximum size of anything
     public boolean hasChanged = false;
     public int slotCount = 0;
     private String gblVerRes = ""; // internal storage of verifyBank results
     
    String sysexHeaderTxt = "F0 00 02 21 ";
    String sysexFooterTxt = " F7"; 
    byte[] sysexHeader = { (byte) 0xF0, (byte) 0x00, (byte) 0x02, (byte) 0x21 };
    byte[] sysexDelimiter = { (byte) 0xF7 };

    /**
     * DEPRECATED
     * @param bankNum 
     */
    public Bank(int bankNum) {
        bankName = "";
        bankNumber = bankNum;
        if(bankNum == 0) {
            bankName = "Pedal Bank";
        }
        Initialize();
        status = "initialized";
    } 
    
    /**
     * 
     * @param aBankName
     * @param bankNum 
     */
    public Bank(String aBankName, int bankNum) {
        bankName = aBankName;
        bankNumber = bankNum;
        if(bankNum == 0) {
            bankName = "Pedal Bank";
        }
        Initialize();
        status = "initialized";
    } 
    
    /**
     * 
     */
    public Bank() {
        bankName = "";
        Initialize();
        status = "initialized";
    }
    
    /**
     * 
     * @param aName 
     */
    public Bank(String aName) {
       bankName = aName;
       fileName = dataDir + aName + ".ecp";
       Initialize(); 
       status = "initialized";
    
    }

    /**
     * 
     * @param bankName
     * @param bcs 
     */
    Bank(String bankName, String bcs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * create a new preset object in the preset array.  Each preset instantiates 3 modes and so on down to the slots.
     */
    public void Initialize() { 
        
        // 
        if(dataDir.isEmpty()) {
            fileSeparator = System.getProperty("file.separator");
            dataDir = System.getProperty("user.dir") + fileSeparator + "data" + fileSeparator;
        }

        // create an empty array of five Preset objects
        preset = new Preset[5]; // this wont actually instantiate the presets, you have to new them each, this new just makes the new array, not new presets....
        for(int np=0;np<preset.length;np++) {
            preset[np] = new Preset(); // instantiate a new preset
        }
        
    }

    /**
     * 
     * @param dirName 
     * set the directory where bank files will bbe loaded and saved.  this location is not persisted.
     */
    public void setDir(String dirName) {
        dataDir = dirName;
    }

    /**
     * delete a bank physical file
     * but do not destroy ourselves.
     */
    void deleteBank() {
        // build file name from bank name and delete that file.  Assume verification/authentication
        if(bankName.equalsIgnoreCase("")  ) {
            System.err.println("deleteBank: Bank has no name, cannot delete");
            return;
        }

        
        File ftd = new File(dataDir+bankName+".ecp");
        
            try {
                //ftd.delete();
                Files.delete(ftd.toPath());
                //System.out.println("deleteBank: deleted bank file "+ftd.getName());
                
            } catch (IOException e) {
                System.err.println("IOException deleting file "+ftd+":"+e);
            }                
    }
    
    /**
     * delete the specified bank's file
     * @param bn 
     */
    void deleteBank(String bn) {
        bankName = bn;
        deleteBank();
    }
    
    /**
     * 
     * @param bankNum 
     * sync the bank's contents to the indicated bank number 
     * @deprecated
     * Do not use this, use bank names instead
     */
    
    void saveBank() { // we only call with no arguments when we just want to save ourselves
        //System.out.println("saveBank() no args, self-save");
        //if(bankName.isEmpty()) bankName = "unNamed"; // shouldn't happen
        //if(fileName.isEmpty()) fileName = dataDir + bankName + ".ecp";
//        if(fileName.isEmpty()) fileName = dataDir + bankName + ".ecp"; // but not bank name
        //System.out.println("saveBank:bankName="+bankName);
        //System.out.println("saveBank:fileName="+fileName);
        saveBank(bankName);
    }
    
    public String getBankNameFromFileName(String fn) {
        //System.out.println("getBankNamefromFileName("+fn+")");
        String res = fn;
        File tf = new File(fn);
        String path = tf.getPath();
        String fnm = tf.getName();
        String sfn = ""; // short file name

        if((!fn.contains(fileSeparator)) && (!fn.contains("."))) return fn; // it's just a bank name to begin with
        

        // if there is no separator but an extension, remove it
        if(fn.contains(".")) {
             String tspl[] = fn.split("\\.");
            
             if(fn.contains(fileSeparator)) {
                  String tspL0 = tspl[0].substring(fn.lastIndexOf(fileSeparator)+1);
                  //System.out.println("made bank name '"+tspL0+"'.");
                  return tspL0; // to the end which should be our bank name
             }
             else {
                 String tspL0 = tspl[0];
                 //System.out.println("made bank name '"+tspL0+"'.");
                 return tspL0;
             }      
        } else { 
        // if there is no dot but a separator, it must be either just a path (malformed) or, a path + name and no extension, which might come from the file chooser
            if(fn.contains(fileSeparator)) {
              String tspL0 = fn.substring(fn.lastIndexOf(fileSeparator));  
              //System.out.println("made bank name '"+tspL0+"'.");
              return tspL0; // to the end which should be our bank name 
            }
        }
        // should never get here but required for dumb syntax checker
        System.out.println("returning '"+res+"'?");
        return res;
    }
    
    /**
     * always return the correct file name:
     *  with or without an extension
     *  with or without a whole path
     *  with or without a malformed name e.g. dataDir + file name + extension other than .ecp.
     *  banks are always saved as dataDir + bank Name + .ecp
     *  external files (those with a path name, that is different from the data dir name) are saved as-is except if they are missing an extension, if so, .ecp is appended to the file name.
     * @param fn
     * @return 
     */
    public String makeFileName(String fn) {
        System.out.println("makeFileName("+fn+")");
        String res = fn;
        File tf = new File(fn);
        String path = tf.getPath();
        String fnm = tf.getName();
        String sfn = ""; // short file name
        
        // when can we bail
        // if file contains file separator, add an extension if it doesn't have one and that's it.
        if(fn.contains(fileSeparator)) // file separator?
             if(fn.contains(".")) { // and a space?
                 //System.out.println("made '"+fn+"'.");
                 return fn; // we're all good?  -  we should check if the xtension = .ecp and if the path is the data directory, replace th extension, otherwise, allow it to be anything
             } else { // no extension
                 //System.out.println("made '"+fn+".ecp'.");
                 return fn + ".ecp";
             }
        else { // no file separator so either a bank name or a file name with no path
            // if a bank name
            if(!fn.contains(".")) {
                String rv = dataDir + fn + ".ecp";
                //System.out.println("made '"+rv+"'.");
                return rv;
            } else {
                String tspl[] = fn.split("\\.");
                String rv = dataDir + tspl[0] + ".ecp";
                //System.out.println("made '"+rv+"'.");
                return rv;
            }            
        }
    }
    /**
     * saveBank receives a file name and saves the main Bank to that file name.
     * if the fn is a string with no path or file extension, it is considered a bank name, and the save will occur normally with bank name as file name in the dataDir.
     * if fn is a full path, extract the file name and use that as the bank name, so the internal bankName always matches the file name.
     * The file name can bbe used to supply a bank-only name.
     * 
     * @param fn 
     */
    void saveBank(String fn) { // implements a save-as function as well
        System.out.println("saveBank( String "+fn+")");
        String tmpFileName = makeFileName(fn);
        String tmpBankName = getBankNameFromFileName(fn);
        
        // re-inject the sysex headers and api info, and sysex footerr 
        String fullFile = toString(); // this literally prints all five presets in their text forms
        // eventually make the toString able to embed comments saying what the parts are
        if(!isSysexHeaderPresent(fullFile)) {
            fullFile = sysexHeaderTxt + "12 " + fullFile; 
            //System.out.println("saveBank: adding sysex header");
        }
        if(!isSysexFooterPresent(fullFile)) {
            //System.out.println("saveBank: adding sysex footer");
            fullFile = fullFile + sysexFooterTxt; 
        }
        
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(tmpFileName), "utf-8"))) {
              Date date = new Date();              
              DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

              writer.write("#Bank '" + tmpBankName + "' - Autogenerated " + sdf.format(date.getTime()) + "by Effigy Labs Control Panel\n");
              writer.write("#Effigy Labs LLC\n");
              writer.write("#If this file is in the control panel data directory, do not modify, all changes will be lost.\n");              
              writer.write("#To properly save this file, save to another folder with your OS, or with the control panel 'Copy->'.\n");              
              
              writer.write("bankName="+tmpBankName+"\n");
              writer.write(fullFile);
              writer.close();
              status = "Saved bank file.";
              System.out.println("saveBank: saved bank file "+tmpFileName);
            } catch (UnsupportedEncodingException ex) {
                System.err.println("saveBank: Error saving bank file "+ex);
                Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
                status = "Error saving file";
        } catch (IOException ex) {
            System.err.println("saveBank: IO Error saving bank file "+ex);
            status = "IO Error saving file";
            Logger.getLogger(CPContentFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Take a string or a file object
     * @param filename
     * @param commit
     */ 
    public void loadBank(String filename, boolean commit) {
        //System.out.println("loadBank(String "+filename+","+commit+")");
        String loadFileName = makeFileName(filename);
        loadBank(new File(loadFileName), commit);    
    }
    
    // update this code with the java platform independent user.dir etc. **** tag ****
    public void loadBank(File file, boolean commit) {
       //System.out.println("loadBank(File "+file.getAbsolutePath()+","+commit+")");
       String rtnval = "";
       fileName = file.getAbsolutePath(); // this does include the file name
       
       int ver = verifyBank(fileName);
       if(ver > 1) {
           System.err.println("loadBank: verify bank failed, verify code="+ver);
           return;
       }
       
       String wholefile = "";
       try
         {
           BufferedReader reader = null;
           try {
              reader = new BufferedReader(new FileReader(fileName));
           } catch (FileNotFoundException ex) {
               Logger.getLogger(Bank.class.getName()).log(Level.SEVERE, null, ex);
           }
                int lineNum = 0;
                String line;
                String[] tspl;
                // parse the file.
                // first line must be "bankname=name", no quotes, case-insensitive
                // # comments any line
                // data must consist of at least one preset.
                while ((line = reader.readLine()) != null) {
                    //tspl = line.split("#"); // ignore all to the right of the # sign on all lines
                    //line = tspl[0];
                    //System.out.println("line='"+line+"'");
                    if( (!line.startsWith("#"))
//                     || (!line.isEmpty()) 
                            ) { // not a comment
                        lineNum++;

                        String testline=line.toLowerCase();
                        // first line must be bank name
                        switch (lineNum) {
                            case 1:
                              if(testline.startsWith("bankname=")) {
                                  bankName=line.substring(9); // set the name of the bank itself - this is where it ought to set it
                                  //System.out.println("loadBank: bank name="+bankName);
                              } else {
                                  System.err.println("loadBank: bank read error:  first line must be 'bankname=nameofyourbankhere'");
                                  //status="error";
                                  return;
                                  // handle error more later
                                }
                              break;
                            default: {
                                wholefile+=line; // comments are lines that start with the # sign.                                    
                            }

                        }
                    } // non-commented lines
                    //else {
                    //    System.out.println("skipped line '"+line+"'");
                    //}
                }
                reader.close();
            }
              catch (Exception e)
        {
         //System.err.format("Exception occurred trying to read bank '%s'.", fileName);
         System.err.println("loadBank: Error Loading bank file "+fileName+" "+e);
        }

        wholefile = wholefile.trim();  //remove extra whitespace at end if present   
        
         System.out.println("loadBank: Loaded bank file "+fileName);
         status = "loaded bank file "+fileName;
         
        // file is loaded, do headers, populate numeric preset, and txt buffer
        String bN = "";

        int offset = 0;
        if(isSysexHeaderPresent(wholefile)) {
            offset = 5;
        }

        // check for existence and strip sysex headers and/or api command+argument info.  If there is a preset, or a bank, or a slot in the file????

        // convert the buffer into an array of actual numbers represented by the hex text in the file.
        numericPreset = convertTextToIntArr(wholefile);
        
        // the file will have the sysex headers ***** try and catch for this later ****** tag ********
        if(commit) {
            int bufAddr = offset; // start of data - this is the offset to change
              
            for(int pr=0;pr<5;pr++) { // each preset 
                for(int m=0;m<3;m++) { // each mode
                    for(int p=0;p<4;p++) { // each position
                        for(int s=0;s<3;s++) { // each slot
                            try {
                                preset[pr].mode[m].pos[p].slot[s].MidiCommand = numericPreset[bufAddr++];
                                if(preset[pr].mode[m].pos[p].slot[s].MidiCommand > 0) {
                                    slotCount++;
                                    preset[pr].mode[m].pos[p].slot[s].MidiChannel = numericPreset[bufAddr++];
                                    preset[pr].mode[m].pos[p].slot[s].MidiSubCommand = numericPreset[bufAddr++];
                                    preset[pr].mode[m].pos[p].slot[s].curvetype = numericPreset[bufAddr++];
                                    preset[pr].mode[m].pos[p].slot[s].curvedirection = numericPreset[bufAddr++];
                                    preset[pr].mode[m].pos[p].slot[s].min = numericPreset[bufAddr++];
                                    preset[pr].mode[m].pos[p].slot[s].max = numericPreset[bufAddr++];
                                    preset[pr].mode[m].pos[p].slot[s].latching = numericPreset[bufAddr++];
                                }
                            } catch (Exception e) {
                                System.err.println("loadBank: Error loading bank from file:"+e);
                                status = "Error loading bank from file:"+e;
                            }
                        }
                    }
                }
                //System.out.println("loaded preset "+pr+".");
              } // load all presets
        } // if commit is on  
    }

    
    public Preset exportPreset(int pN) {
        //pR = preset[pN].toByteArr();
        return preset[pN];
    }
    
     @Override
     /**
      * toString invokes the toString of it's 5 presets, and concatenates them, with a space for a separator
      */
    public String toString() {
        String rtnval = "";
         for (int pr = 0;pr< preset.length;pr++) {
             rtnval += preset[pr].toString()+" "; // the bank tostring is literally five preset tostrings with a space in between each             
         }
         //for (Preset preset1 : preset) {
         //    rtnval += preset1.toString()+" "; // the bank tostring is literally five preset tostrings with a space in between each
         //}
        return rtnval.toUpperCase().trim(); // cleanly
    }


    public String toFormattedString() {
        String rtnval = "";
        rtnval += "#bank "+bankName+"\n";
        for (int pr = 0;pr< preset.length;pr++) {             
            rtnval += "# preset "+pr+"\n";
            rtnval += preset[pr].toFormattedString()+" "; // the bank tostring is literally five preset tostrings with a space in between each             
        }
         //for (Preset preset1 : preset) {
         //    rtnval += preset1.toString()+" "; // the bank tostring is literally five preset tostrings with a space in between each
         //}
        return rtnval.toUpperCase().trim(); // cleanly
    }



    // "empty" all the slots to "clear" the presets.  "Empty" mainly means the MidiCommand attribute = 0, but the other attributes are cleared for cleanliness, except the max which defaults to "100".
    void clear() {
        slotCount = 0;
        for(int pr=0;pr<5;pr++) {
            for(int m=0;m<3;m++) {
                for(int pos=0;pos<4;pos++) {
                    for(int s=0;s<3;s++) {
                        preset[pr].mode[m].pos[pos].slot[s].emptySlot(); // the slot has a method to clear itself so just call that for all the slots in the bank.
                    }
                }
            }
        }
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


 /**
  * each byte in the array is expanded to be a text 2-character hex number and a space separator.  That's all we do here, no header/footer business, just textifying an array.
  * @param arrayToConvert
  * @return 
  */   
 String convertByteToTextArray(byte[] arrayToConvert) {
        String returnStr = "";
        if(arrayToConvert.length == 0) return returnStr;        
        for(int lc=0;lc<arrayToConvert.length;lc++) {
            returnStr+=(String.format("%02X",arrayToConvert[lc]));
            returnStr+=" ";                    
        }
        return returnStr;
     }
    
     String convertNumericPresetToText(int[] arrayToConvert) {
        String returnStr = "";
        for(int lc=0;lc<arrayToConvert.length;lc++) {
            returnStr+=String.format("%02X ",arrayToConvert[lc]);
             //returnStr.concat(arrayToConvert[lc].
        }
        return returnStr.trim().toUpperCase();
     }
     byte[] convertNumericPresetToByte(int[] arrayToConvert) {
         // assume everything and nothing
         byte[] rtnVal = new byte[arrayToConvert.length];
         for(int c=0;c<arrayToConvert.length;c++) {
             rtnVal[c] = (byte) arrayToConvert[c];
         }
         return rtnVal;
     }
     

     int[] convertTextToIntArr(String txtToConvert) {
        byte[] brtn = convertTextToByteArr(txtToConvert);
        //System.out.println("txt input length:"+txtToConvert.length());
        //System.out.println("byte arr len:"+brtn.length);
        int[] irtn = new int[brtn.length];
        //System.out.println("int arr len:"+irtn.length);

        for(int c=0;c<brtn.length;c++) {
            irtn[c] = (int) brtn[c];
        }
        //System.arraycopy(brtrn, 0, irtn, 0, brtn.length); //return buffPreset;

        return irtn;
         
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
        String p = "([0-9,A-F,a-f]{2})"; // look for hex character pairs (exactly 2 matching)
        Pattern pattern = Pattern.compile(p);
        //System.out.println("buff="+buff);
        try {
            Matcher matcher = pattern.matcher(cmdTxt);
            int gct = 0;
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
               System.err.println("bank:convertTextToByteArr() Problem finding the numbers in the text: "+e);
        }
       return buffRtn; // should never get here
}

    private boolean isSysexHeaderPresent(int[] stray) {
        return isSysexHeaderPresent(convertNumericPresetToText(stray));
    }

    private boolean isSysexHeaderPresent(byte[] stray) {
        return isSysexHeaderPresent(convertByteToTextArray(stray));
    }

    private boolean isSysexFooterPresent(int[] stray) {
        return isSysexFooterPresent(convertNumericPresetToText(stray));
    }

    private boolean isSysexFooterPresent(byte[] stray) {
        return isSysexFooterPresent(convertByteToTextArray(stray)); 
    }

    private boolean isSysexFooterPresent(String stray) {
        //System.out.println("isSysexFooterPresent, stray="+stray);
        boolean rtnval = true;
        if(!nonNull(stray)) return false;
        if(stray.isEmpty()) return false;
        if(stray.length() < 2) return false;

        // get the name here


        try {
            String p = "([0-9,A-F,a-f]+)\\Z"; // look for hex characters
            Pattern pattern = Pattern.compile(p);
            Matcher matcher = pattern.matcher(stray);
            int gct = 0;
            while (matcher.find() && gct < 1) {
                //System.out.println("matcher.end="+matcher.end());
                //int gn = matcher.end();
                //System.out.println("group "+gct+": "+gn);
                int ng = Integer.parseInt(matcher.group(gct),16);
                //System.out.println("["+gct+"]="+ng);
                if(ng != 0xF7) return false;
                gct++;
            }

            } // regex match the group. 
        catch (Exception e) {
                   System.err.println("bank:isSysexFooterPresent: Problem finding the numbers in the text: "+e);
            }
        return rtnval;
}

    
    private boolean isSysexHeaderPresent(String stray) {
    //System.out.println("isSysexHeaderPresent");
    //System.out.println("str len="+stray.length());
    boolean rtnval = true;
    if(!nonNull(stray)) return false;
    if(stray.isEmpty()) return false;
    if(stray.length() < 4) return false;
    
    try {
        int[] bufTest = new int[4];
        String p = "([0-9,A-F,a-f]+)"; // look for hex characters
        Pattern pattern = Pattern.compile(p);
        Matcher matcher = pattern.matcher(stray);
        int gct = 0;
        while (matcher.find() && gct < 4) {
            String gn = matcher.group();
            //System.out.println("group "+gct+": "+gn);
            int ng = Integer.parseInt(gn,16);
            //System.out.println("["+gct+"]="+ng);
            bufTest[gct] = ng;
            //System.out.print("gct="+gct);
            gct++;
        }
        if(gct < 3) {
            //System.out.println("nope, len="+gct) ;
            return false;
            
        } 
  
        if(bufTest[0] != 0xF0) return false;
        if(bufTest[1] != 0x00) return false;
        if(bufTest[2] != 0x02) return false;
        if(bufTest[3] != 0x21) return false;
            
        } // regex match the group. 
     catch (Exception e) {
        System.out.println("bank: isSysexHeaderPresent: Problem finding the numbers in the text: "+e);
     }
    
    return rtnval;
}
    
    FilenameFilter bankFilesMatcher = new FilenameFilter() {
        @Override
        public boolean accept(File file, String name) {
            return name.toLowerCase().endsWith(".ecp");
    }
};

    
   public File[] getBankList(String srcDir) {

        File dir = new File(srcDir);       
        File[] files = dir.listFiles(bankFilesMatcher);
        
        if (files.length == 0) {
            System.err.println("bank: getBankList: No Banks found.");
        } 
        
        return files;
 }

   
    void importArr(byte[] testinc, int offset) {
       int bufPtr = offset;
        for(int pr=0;pr<4;pr++) {
            for(int moder=0;moder<3;moder++) {
                for(int pos=0;pos<4;pos++) {
                    for(int slot=0;slot<3;slot++) {
                        preset[pr].mode[moder].pos[pos].slot[slot].MidiCommand = testinc[bufPtr]; // update the midicommand in our structure
                        bufPtr++;
                        if (preset[pr].mode[moder].pos[pos].slot[slot].MidiCommand > 0) {
                            for(int sp=0;sp<7;sp++) {
                                preset[pr].mode[moder].pos[pos].slot[slot].setAttrVal(sp+1, testinc[bufPtr+sp]); // update the slot data in our structure
                            }
                            status="pre slot="+preset[pr].mode[moder].pos[pos].slot[slot].toString();
                            bufPtr+=7;                        
                        }
                    }
                }
            }
        }
    }


   
   
   
    /** 
     * Supposed to return the model
     * @param srcDir
     * @return 
     */
/*
   public File[] genBankListModel(String srcDir) {
        // populate bank list
        DefaultListModel model;
        model = new DefaultListModel();
        File[] file = getBankList(srcDir);
        for(int f=0;f<file.length;f++) {
            loadBank(file[f],false); //the false is COMMIT.  a commit of false does not commit the loaded bank to the pedal.
            model.addElement(bankName);            
        };

        return file;
    }
*/
   
    /**
     * Verify bank - read a bank file and return codes based on condition of file
     * 0 - file is a valid bank
     * 1 - file is a valid, empty, bank
     * 2 - file problem: not found, string supplied is not a file name, or security problem
     * 3 - file is empty
     * 4 - file contains no data, e.g., all comments or no numeric pairs found 
     * 5 - file bankName=bankname not first non-commented line
     * 6 - bankName= is empty
     * 7 - bankName contains invalid characters or mismatches file name
     * 8 - incomplete bank data - the data began as a valid bank but did not finish
     * 9 - invalid bank data - bad midi command
     * 10 - invalid bank data - bad midi channel
     * 11 - invalid bank data - bad midi subcommand
     * 12 - invalid bank data - bad curve type
     * 13 - invalid bank data - bad curve direction
     * 14 - invalid bank data - bad min value
     * 15 - invalid bank data - bad max value
     * 16 - invalid bank data - bad latching value
     * 17 - info - sysex header found at beginning of data - maybe it should
     * 18 - info - sysex footer found at end of data
     *
     * @param file
     * @param commit 
     */
    public String verifyBank(String filename, boolean rtnStrInd) {
        boolean nop = rtnStrInd;
        String rtnStr = "";
        gblVerRes = "verifyBank Results:\n";
        int intRes = verifyBank(filename);
        rtnStr = gblVerRes; // intermediate value to clear global before returning
        gblVerRes = ""; // reset it to make sure verifyBank() below doesn't inadvertently fill up the string
        return rtnStr; // retturn the verification results
                
    }
    
    public int verifyBank(String filename) {
        int returnVal = 0; // start with ok
        
        System.out.println("verifyBank(String "+filename+")");
        if(gblVerRes.length()>0) {
            gblVerRes+="verifyBank(String "+filename+")";
        }
        File aFile = null;
        try {
            aFile = new File(filename);
        } catch (Exception e) {
            System.err.println("verifyBank: file error:"+filename+":"+e);
            if(gblVerRes.length()>0) {
                gblVerRes+="verifyBank: file error:"+filename+":"+e;
            }
            return 2;
        }
        if(!aFile.isFile()) {
            System.err.println("verifyBank: file error, not a file:"+aFile.getAbsolutePath());
            if(gblVerRes.length()>0) {
                gblVerRes+="verifyBank: file error, not a file:"+aFile.getAbsolutePath();
            }
            return 2;
        }
        if(!aFile.exists()) {
            System.err.println("verifyBank: file error, file does not exist:"+aFile.getAbsolutePath());
            if(gblVerRes.length()>0) {
                gblVerRes+="verifyBank: file error, file does not exist:"+aFile.getAbsolutePath();
            }
            return 2;
        }
        if(aFile.isDirectory()) {
            System.err.println("verifyBank: file error, file is a folder name:"+aFile.getAbsolutePath());
            if(gblVerRes.length()>0) {
                gblVerRes+="verifyBank: file error, file is a folder name:"+aFile.getAbsolutePath();
            }
            return 2;
        }
        if(!aFile.canRead()) {
            System.err.println("verifyBank: file error, cannot read file:"+aFile.getAbsolutePath());
            if(gblVerRes.length()>0) {
                gblVerRes+="verifyBank: file error, cannot read file:"+aFile.getAbsolutePath();
            }
            return 2;
        }
        //if(!aFile) {
        //    System.err.println("verifyBank: cannot read file:"+aFile.getAbsolutePath());
        //    return 2;
        //}
   
        // etc. etc. for the rest of the c hecks below, get code from loadBank and elsewhere to implement the checks
      String rtnval = "";
      String tFileName = aFile.getAbsolutePath(); // this does include the file name
      String fBankName = getBankNameFromFileName(tFileName);
      String wholefile = "";
      String tBankName = "";
      try
        {
          BufferedReader reader = null;
          try {
              reader = new BufferedReader(new FileReader(tFileName));
          } catch (Exception ex) {
              System.err.println("verifyBank: file error, cannot open:"+tFileName);
                if(gblVerRes.length()>0) {
                    gblVerRes+="verifyBank: file error, cannot open:"+tFileName;
                }
              return 2;
          }
                int lineNum = 0;
                String line;
                String[] tspl;
                // parse the file.
                // first line must be "bankname=name", no quotes, case-insensitive
                // # comments any line
                // data must consist of at least one preset.
                while ((line = reader.readLine()) != null) {
                    //tspl = line.split("#"); // ignore all to the right of the # sign on all lines
                    //line = tspl[0];
                    //System.out.println("line='"+line+"'");
                    if( (!line.startsWith("#"))
//                     || (!line.isEmpty()) 
                            ) { // not a commen
                        lineNum++;

                        String testline=line.toLowerCase();
                        // first line must be bank name
                        switch (lineNum) {
                            case 1:
                              if(testline.startsWith("bankname=")) {
                                  tBankName=line.substring(9); // set the name of the bank itself
                                  if(tBankName.isEmpty()) {
                                      System.err.println("verifyBank: bank Name error: no bank Name supplied after bankName= :"+tFileName);
                                        if(gblVerRes.length()>0) {
                                            gblVerRes+="verifyBank: bank Name error: no bank Name supplied after bankName= :"+tFileName;
                                        }
                                      return 6;
                                  }
                                  if(!tBankName.equalsIgnoreCase(fBankName)) {
                                      System.err.println("verifyBank: bankName= to file name mismatch: bankName="+tBankName+", file name="+tFileName);
                                        if(gblVerRes.length()>0) {
                                            gblVerRes+="verifyBank: bankName= to file name mismatch: bankName="+tBankName+", file name="+tFileName;
                                        }
                                      return 7;
                                  }
                                  //System.out.println("verifyBank: found bank name:"+tBankName);
                              } else {
                                  System.err.println("verifyBank: bank Name error: first non-comment line must be 'bankname=nameofyourbankhere'");
                                    if(gblVerRes.length()>0) {
                                        gblVerRes+="verifyBank: bank Name error: first non-comment line must be 'bankname=nameofyourbankhere'";
                                    }
                                  return 5;
                              }
                              try {
                                File testBankNameFile = new File(tFileName);  // if you can form a file name with it, it'll do for us.  For now.
                              } catch (Exception e) {
                                  System.err.println("verifyBank: bank Name error:"+tBankName+":"+e);
                                    if(gblVerRes.length()>0) {
                                        gblVerRes+="verifyBank: bank Name error:"+tBankName+":"+e;
                                    }
                                  return 7;
                              }
                              break;
                            default: {
                                wholefile+=line; // comments are lines that start with the # sign.                                    
                            }
                        }
                    } // non-commented lines
                    else {
                        //System.out.println("skipped line '"+line+"'");
                    }
                }
                if(lineNum == 0) {
                    System.err.println("verifyBank: empty file error on:"+tFileName);
                    if(gblVerRes.length()>0) {
                        gblVerRes+="verifyBank: empty file error on:"+tFileName;
                    }
                    return 3;
                }
            reader.close();
        }
          catch (Exception e) {
            System.err.println("verifyBank: file error on" + tFileName+":"+e);
            if(gblVerRes.length()>0) {
                gblVerRes+="verifyBank: file error on:"+tFileName+":"+e;
            }
            return 2;
        }

        wholefile = wholefile.trim();  //remove extra whitespace at end if present           
        //System.out.println("verifyBank: Loaded bank file "+tFileName);
         
        // file is loaded, do headers, populate numeric preset, and txt buffer
        String bN = "";

        // check for existence and strip sysex headers and/or api command+argument info.  If there is a preset, or a bank, or a slot in the file????

        // convert the buffer into an array of actual numbers represented by the hex text in the file.
        numericPreset = convertTextToIntArr(wholefile);
        
        int offset = 0;
        if(isSysexHeaderPresent(wholefile)) {
            offset = 5;
            //System.out.println("verifyBank: MIDI sysex header present, proper offset is "+offset);
        }
        else {
            //System.err.println("verifyBank: MIDI sysex header not present, proper offset is zero.");
        }
        // the file will have the sysex headers ***** try and catch for this later ****** tag ********
        int bufAddr = offset; // start of data
        for(int pr=0;pr<5;pr++) { // each preset 
            for(int m=0;m<3;m++) { // each mode
                for(int p=0;p<4;p++) { // each position
                    for(int s=0;s<3;s++) { // each slot
                        try {
                            // main midi command.  valid values are zero or 8-15
                            int MidiCmd = numericPreset[bufAddr++];
                            if( MidiCmd > 15 
                             || MidiCmd < 0 
                             || (MidiCmd > 1 && MidiCmd < 8)
                            ) { // 
                                System.err.println("verifyBank: value error:outside 0,8-15:MIDI command:"+MidiCmd+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                if(gblVerRes.length()>0) {
                                    gblVerRes+="verifyBank: value error:outside 0,8-15:MIDI command:"+MidiCmd+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                }
                                returnVal = 9; // attempt to continue by not returning but merely setting a return code value
                            } 

                            if(MidiCmd > 0) {

                                // midi channel.  valid values are 0-15 but shown as 1-16
                                int MidiChannel = numericPreset[bufAddr++];
                                if( MidiChannel > 15 || MidiCmd < 0) {
                                    System.err.println("verifyBank: value error:outside 1-16:MIDI channel:"+(MidiChannel+1)+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                    if(gblVerRes.length()>0) {
                                        gblVerRes+="verifyBank: value error:outside 1-16:MIDI channel:"+(MidiChannel+1)+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                    }
                                    returnVal = 10; // attempt to continue by not returning but merely setting a return code value
                                } 

                                // midi subcommand.  valid range depends on the command.  aftertouches and zero should be empty, cc, noteon/off and pgm chg are 0-127, and pitch bend is 0 or 1 but 0-127 is acceptable with 2-127 same as 1
                                int MidiSubCommand = numericPreset[bufAddr++];                                    

                                // check for outside 0-127 for all
                                if(MidiSubCommand < 0 || MidiSubCommand > 127)  {
                                     System.err.println("verifyBank: subcommand value error: value not in range 0-127: "+MidiSubCommand +" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                        if(gblVerRes.length()>0) {
                                            gblVerRes+="verifyBank: subcommand value error: value not in range 0-127: "+MidiSubCommand +" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                        }
                                     returnVal = 11; // attempt to continue by not returning but merely setting a return code value                                                                                   
                                 } else {

                                    switch (MidiCmd) {
                                       case 0:  // nothing will happen since we are only here if the command isn't zero in the first place
                                           break;
                                       case 10: // Aftertouch (polyphonic), empty
                                       case 13: // Aftertouch (channel), empty
                                              // warn only if anything is in here
                                              if(!(MidiSubCommand == 0)) {
                                                   System.err.println("verifyBank: subcommand value warning: empty Midi Command with non-empty sub command value: "+MidiSubCommand +" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                                    if(gblVerRes.length()>0) {
                                                        gblVerRes+="verifyBank: subcommand value warning: empty Midi Command with non-empty sub command value: "+MidiSubCommand +" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                                    }
                                                   //returnVal = 11; // attempt to continue by not returning but merely setting a return code value                                                                                   
                                              }
                                           break;
                                       case 14: // Pitch Bend, 0 (pitch down), 1 (pitch up) preferred, 1-127 (pitch up)
                                              if(MidiSubCommand > 1)  {
                                                   System.err.println("verifyBank: subcommand value warning: pitch bend uses only 0 for pitch down and 1 or higher for pitch up.  value > 1 accepted: "+MidiSubCommand +" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                                    if(gblVerRes.length()>0) {
                                                        gblVerRes+="verifyBank: subcommand value warning: pitch bend uses only 0 for pitch down and 1 or higher for pitch up.  value > 1 accepted: "+MidiSubCommand +" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                                    }
                                                   //returnVal = 11; // attempt to continue by not returning but merely setting a return code value                                                                                   
                                              }
                                           break;
                                       default: 

                                   }
                                 }

                                // curve type
                                int curveType = numericPreset[bufAddr++];
                                if( curveType > 4 || curveType < 0) {
                                    System.err.println("verifyBank: value error:outside 0-4:curve type:"+curveType+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                    if(gblVerRes.length()>0) {
                                        gblVerRes+="verifyBank: value error:outside 0-4:curve type:"+curveType+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                    }
                                    returnVal = 12; // attempt to continue by not returning but merely setting a return code value
                                } 

                                // curve direction
                                int curveDirection = numericPreset[bufAddr++];
                                if( curveDirection > 3 || curveDirection < 0) {
                                    System.err.println("verifyBank: value error:outside 0-3:curve direction:"+curveDirection+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                    if(gblVerRes.length()>0) {
                                        gblVerRes+="verifyBank: value error:outside 0-3:curve direction:"+curveDirection+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                    }
                                    returnVal = 13; // attempt to continue by not returning but merely setting a return code value
                                }                                     

                                // min
                                int min = numericPreset[bufAddr++];
                                if( min > 100 || min < 0) {
                                    System.err.println("verifyBank: value error:outside 0-100:min:"+min+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                    if(gblVerRes.length()>0) {
                                        gblVerRes+="verifyBank: value error:outside 0-100:min:"+min+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                    }
                                    returnVal = 14; // attempt to continue by not returning but merely setting a return code value
                                }                                     

                                // max
                                int max = numericPreset[bufAddr++];
                                if( max > 100 || max < 0) {
                                    System.err.println("verifyBank: value error:outside 0-100:max:"+max+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                    if(gblVerRes.length()>0) {
                                        gblVerRes+="verifyBank: value error:outside 0-100:max:"+max+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                    }
                                    returnVal = 15; // attempt to continue by not returning but merely setting a return code value
                                }                                     

                                // latching
                                int latching = numericPreset[bufAddr++];
                                if( latching > 1 || latching < 0) {
                                    System.err.println("verifyBank: value error:outside 0-1:latching:"+latching+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"");
                                    if(gblVerRes.length()>0) {
                                        gblVerRes+="verifyBank: value error:outside 0-1:latching:"+latching+" in preset "+pr+" mode "+m+" position "+p+" slot "+s+"";
                                    }
                                    returnVal = 16; // attempt to continue by not returning but merely setting a return code value
                                }                                     
                            }
                        } catch (Exception e) {
                            System.err.println("verifyBank: Error loading bank from file:"+e);
                            if(gblVerRes.length()>0) {
                                gblVerRes+="verifyBank: Error loading bank from file:"+e;
                            }
                            status = "Error loading bank from file:"+e;
                        }
                    }
                }
            }
            //System.out.println("verifyBank: verified preset "+pr+".");
          } // iterate all presets
            if(returnVal > 0) {
                System.err.println("verifyBank: Bank is not valid.");
                if(gblVerRes.length()>0) {
                    gblVerRes+="verifyBank: Bank is not valid.";
                }
            } else {
                System.out.println("verifyBank: Bank is valid.");
                if(gblVerRes.length()>0) {
                    gblVerRes+="verifyBank: Bank is valid.";
                }                
            }
        return returnVal;
    }
}
