package net.propero.rdp.preferences;

import net.propero.rdp.Rdp;
import net.propero.rdp.Utilities;
import net.propero.rdp.preferences.panels.ApplicationSettingsPanel;
import net.propero.rdp.preferences.panels.DisplaySettingsPanel;
import net.propero.rdp.preferences.panels.DriveSettingsPanel;
import net.propero.rdp.preferences.panels.LoginSettingsPanel;
import net.propero.rdp.preferences.panels.PreferencePanel;
import net.propero.rdp.preferences.panels.PrinterSettingsPanel;
import net.propero.rdp.preferences.panels.SecuritySettingsPanel;
import net.propero.rdp.preferences.panels.SoundSettingsPanel;
import net.propero.rdp.preferences.panels.TimeZonePanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/26/11
 * Time: 10:48 AM
 */
public class PreferencesFrame extends JFrame {

    protected static Logger logger = Logger.getLogger(Rdp.class);

    final Toolbar tabs = new Toolbar();

    private static int maxW = 600;
    private static int maxH = 480;
    private static final Dimension panelSize = new Dimension(600, 480); // width, height

    public PreferencesFrame() {

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(panelSize);

        final Dimension originalTabsDim = tabs.getPreferredSize();

        tabs.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {

                Component p = ((JTabbedPane) e.getSource()).getSelectedComponent();
                Dimension panelDim = p.getPreferredSize();

                Dimension nd = new Dimension(
                        originalTabsDim.width - (maxW - panelDim.width),
                        originalTabsDim.height - (maxH - panelDim.height));

                tabs.setPreferredSize(nd);

                pack();

                ((PreferencePanel) p).movedToFront();
            }

        });

        add(tabs, BorderLayout.NORTH);
        add(new ButtonBar(this), BorderLayout.SOUTH);

        pack();
    }

    public static int getMaxW() {
        return maxW;
    }

    public static void setMaxW(int maxW) {
        PreferencesFrame.maxW = maxW;
    }

    public static int getMaxH() {
        return maxH;
    }

    public static void setMaxH(int maxH) {
        PreferencesFrame.maxH = maxH;
    }

    public Toolbar getToolbar() {
        return this.tabs;
    }

    class Toolbar extends JTabbedPane {

        public Toolbar() {
            Utilities.addTab(this, new LoginSettingsPanel(),
                    "Login", "resources/images/settings/loginSettings.png", '2');
            Utilities.addTab(this, new DisplaySettingsPanel(),
                    "Display", "resources/images/settings/displaySettings.png", '1');
            Utilities.addTab(this, new TimeZonePanel(),
                    "Timezone", "resources/images/settings/timezoneSettings.png", '9');
            Utilities.addTab(this, null,
                    "Keyboard", "resources/images/settings/keyboardSettings.png", '3');
            Utilities.addTab(this, new SoundSettingsPanel(),
                    "Sound", "resources/images/settings/soundSettings.png", '4');
            Utilities.addTab(this, new DriveSettingsPanel(),
                    "Drives", "resources/images/settings/driveSettings.png", '5');
            Utilities.addTab(this, new PrinterSettingsPanel(),
                    "Printers", "resources/images/settings/printerSettings.png", '6');
            Utilities.addTab(this, new ApplicationSettingsPanel(),
                    "Applications", "resources/images/settings/applicationSettings.png", '7');
            Utilities.addTab(this, new SecuritySettingsPanel(),
                    "Security", "resources/images/settings/securitySettings.png", '8');
            setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

            ((PreferencePanel) getCurrentPanel()).movedToFront();
        }

        public PreferencePanel getCurrentPanel() {
            return (PreferencePanel) this.getSelectedComponent();

        }
    }

    class ButtonBar extends JPanel {

        private PreferencesFrame preferencesFrame;

        public ButtonBar(PreferencesFrame preferencesFrame) {
            this.preferencesFrame = preferencesFrame;

            this.setLayout(new BorderLayout());
            setBorder(new EtchedBorder());
            setBackground(Color.WHITE);
            JPanel helper = new JPanel(new BorderLayout());
            helper.add(Utilities.getButton("Cancel", 'c',
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            doCancelButton();
                        }
                    }), BorderLayout.CENTER);

            add(helper, BorderLayout.WEST);

            helper = new JPanel(new BorderLayout());
            helper.add(Utilities.getButton("OK", 'o',
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            doOKButton();
                        }
                    }), BorderLayout.WEST);

            helper.add(Utilities.getButton("Apply", 'a',
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            doApplyButton();
                        }
                    }), BorderLayout.EAST);
            add(helper, BorderLayout.EAST);
        }

        public void doCancelButton() {
            logger.debug("doCancelButton");
            PreferencePanel preferencePanel = this.preferencesFrame.getToolbar().getCurrentPanel();
            if (null != preferencePanel) {
                if (preferencePanel.isDirty()) {
                    Object[] options = {"OK", "Cancel"};
                    int which = JOptionPane.showOptionDialog(null, "There are unsaved changes, are you sure you want to continue?", "Warning",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                            null, options, options[0]);

                    if (which == 0) {
                        return;
                    }
                }
                preferencePanel.doCancelAction();
            }
            preferencesFrame.setVisible(false);
            preferencesFrame.dispose();
        }

        public void doOKButton() {
            logger.debug("doOKButton");
            PreferencePanel preferencePanel = this.preferencesFrame.getToolbar().getCurrentPanel();
            if (null != preferencePanel) {
                preferencePanel.doOKAction();
            }
            preferencesFrame.setVisible(false);
            preferencesFrame.dispose();
        }

        public void doApplyButton() {

            logger.debug("doApplyButton");
            PreferencePanel preferencePanel = this.preferencesFrame.getToolbar().getCurrentPanel();
            if (null != preferencePanel) {
                preferencePanel.doApplyAction();
            }
        }
    }

    public static void main(String[] args) {
        PreferencesFrame preferencesFrame = new PreferencesFrame();
        preferencesFrame.setVisible(true);
    }
}
