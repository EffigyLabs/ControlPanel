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
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class CPMain extends JFrame {
      private TransparentPanel contentPane;

     /**
     * Launch the application.
     * @param args
     */
     public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    try {
                        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                    } catch (UnsupportedLookAndFeelException ex) {
                        ex.printStackTrace();
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    } catch (InstantiationException ex) {
                        ex.printStackTrace();
                    } catch (ClassNotFoundException ex) {
                        ex.printStackTrace();
                    }
                    // Turn off metal's use of bold fonts 
                    UIManager.put("swing.boldMetal", Boolean.FALSE);
                    
        
        
                    CPMain frame = new CPMain();
                    frame.setVisible(false);
                    String fileSeparator = System.getProperty("file.separator");
                    String dataDir = System.getProperty("user.dir") + fileSeparator + "data" + fileSeparator; 
                    Property config = new Property(dataDir+"config.txt");
                    if(config.getBool("loadBgImage")) {
                        ImagePanel imp = new ImagePanel();
                        frame.getContentPane().add(imp);                                
                    }
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setTitle("Effigy Labs Control Panel");
                  //  frame.setIconImage(new Im(dataDir + "effigy E logo 300x300.png"));
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public CPMain() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        contentPane = new TransparentPanel();
        contentPane.setBackground(new Color(0, 0, 0,0));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(new CPContentFrame().getContentPane());
        //contentPane.setOpaque(false);
        //contentPane.setLayout(null);        
        
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();

//        int winWidth = contentPane.getWidth(); // fix ******************* tag
//        int winHeight = contentPane.getHeight();  // fix ******************* tag
        int winWidth = 750;
        int winHeight = 900;

        setBounds(0, 0, winWidth, winHeight);  //  main window size, W/H - use ultimately the fframe's w/h properties ********** tag 
        //this.setLocation(width, width);
        setLocation((width / 2) - (winWidth / 2),(height / 2) - (winHeight / 2));
    }


    @SuppressWarnings("serial")
    public static class ImagePanel extends JPanel {

        BufferedImage img;

        public ImagePanel() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            setLocation(new java.awt.Point(20, 0));  // upper left part inside the window where the image starts being painted
            setSize(new java.awt.Dimension(700, 820)); // size of image

            try {
                String fileSeparator = System.getProperty("file.separator");
                String srcDir = System.getProperty("user.dir") + fileSeparator;
                File imgfile = new File(srcDir + "pedalbg.png");
                img = ImageIO.read(imgfile);
            } catch (IOException ex) {
                Logger.getLogger(CPMain.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
        }

        //@Override
        //public Dimension getPreferredSize() {
        //    return new Dimension(800, 800);
        //}
    }

}
