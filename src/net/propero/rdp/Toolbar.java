package net.propero.rdp;

import net.propero.rdp.preferences.PreferencesFrame;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/20/11
 * Time: 8:39 PM
 */
public class Toolbar extends JPanel {

    private Logger logger = Logger.getLogger(Rdp.class);

    private RdesktopFrame parentFrame;

    public Toolbar(RdesktopFrame parentFrame) {

        this.parentFrame = parentFrame;

        this.setLayout(new GridLayout(1, 6));

        add(Utilities.getPictureButton("Full Screen",
                "Resize the window to fill the entire screen",
                "resources/images/fullScreen.png", '1',
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        doFullScreen();
                    }
                }));

        add(Utilities.getPictureButton("Fit Window",
                "Insure that the window fits on the current screen",
                "resources/images/fitWindow.png", '2',
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        doFitWindow();
                    }
                }));

        add(Utilities.getPictureButton("Get Clipboard",
                "Set your clipboard with the server's clipboard contents",
                "resources/images/getClipboard.png", '3',
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        doGetClipboard();
                    }
                }));

        add(Utilities.getPictureButton("Send Clipboard",
                "Send contents of your local clipboard to remote server",
                "resources/images/sendClipboard.png", '4',
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        doSetClipboard();
                    }
                }));

        add(Utilities.getPictureButton("Print Screen",
                "Print current screen, hold alt for entire screen",
                "resources/images/print.png", '5',
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        doPrintScreen();
                    }
                }));

        add(Utilities.getPictureButton("Screen Capture",
                "Capture fun window to file, hold alt for entire screen",
                "resources/images/screenCapture.png", '5',
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        int modifiers = actionEvent.getModifiers();
                        logger.debug("Toolbar:Screen Capture Button Pressed.");
                        logger.debug("\tALT : "
                                + checkMod(modifiers, ActionEvent.ALT_MASK));
                        logger.debug("\tCTRL : "
                                + checkMod(modifiers, ActionEvent.CTRL_MASK));
                        logger.debug("\tMETA : "
                                + checkMod(modifiers, ActionEvent.META_MASK));
                        logger.debug("\tSHIFT: "
                                + checkMod(modifiers, ActionEvent.SHIFT_MASK));

                        doCaptureScreen(checkMod(modifiers, ActionEvent.ALT_MASK));
                    }

                    private boolean checkMod(int modifiers, int mask) {
                        return ((modifiers & mask) == mask);
                    }
                }));

        add(Utilities.getPictureButton("Settings",
                "Display the settings dialog",
                "resources/images/settings.png", '6',
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        doSettings();
                    }
                }));
    }

    private void doFullScreen() {
        System.out.println("doFullScreen");
        //parentFrame.goFullScreen();
        JOptionPane.showMessageDialog(null, "Sorry not implemented yet.");

    }

    private void doFitWindow() {
        System.out.println("doFitWindow");
        //parentFrame.leaveFullScreen();
        JOptionPane.showMessageDialog(null, "Sorry not implemented yet.");
    }

    private void doGetClipboard() {
        JOptionPane.showMessageDialog(null, "Sorry not implemented yet.");
    }

    private void doSetClipboard() {
        JOptionPane.showMessageDialog(null, "Sorry not implemented yet.");
    }

    /**
     * Capture contents of either the screen or the RDP window to disc
     *
     * @param fullScreen true is the entire screen is to be copied
     *                   false if just the front RDP window
     */
    private void doCaptureScreen(boolean fullScreen) {

        try {
            final String defaultFileName = "properJavaRDP-capture.jpg";
            final JFileChooser chooser = new JFileChooser();

            chooser.addPropertyChangeListener(JFileChooser.DIRECTORY_CHANGED_PROPERTY,
                    new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent evt) {

                            chooser.setSelectedFile(
                                    new File(chooser.getCurrentDirectory().getAbsolutePath() + File.separator + defaultFileName));

                            chooser.updateUI();
                        }
                    });

            chooser.setSelectedFile(new File(chooser.getCurrentDirectory().getAbsolutePath() + File.separator + defaultFileName));
            int result = chooser.showSaveDialog(parentFrame);

            if (result == JFileChooser.APPROVE_OPTION) {
                BufferedImage screenCapture;

                if (fullScreen) {
                    screenCapture = ScreenImage.createDesktopImage();
                } else {
                    screenCapture = ScreenImage.createImage(parentFrame);
                }

                File selectedFile = chooser.getSelectedFile();

                ScreenImage.writeImage(screenCapture, selectedFile.getAbsolutePath());
            }
        } catch (AWTException e) {
            logger.warn(e);
        } catch (IOException e) {
            logger.warn(e);
        }
    }

    private void doSettings() {
        PreferencesFrame settings = new PreferencesFrame();
        settings.setVisible(true);
    }

    private void doPrintScreen() {
        //--- Create a printerJob object
        PrinterJob printJob = PrinterJob.getPrinterJob();

        //--- Set the printable class to this one since we
        //--- are implementing the Printable interface
        printJob.setPrintable(parentFrame);

        //--- Show a print dialog to the user. If the user
        //--- clicks the print button, then print otherwise
        //--- cancel the print job
        if (printJob.printDialog()) {
            try {
                printJob.print();
            } catch (Exception PrintException) {
                PrintException.printStackTrace();
            }
        }
    }
}
