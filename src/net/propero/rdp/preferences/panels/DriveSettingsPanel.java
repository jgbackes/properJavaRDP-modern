package net.propero.rdp.preferences.panels;

import net.propero.rdp.Utilities;
import net.propero.rdp.preferences.PreferencesFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/26/11
 * Time: 10:54 AM
 */
public class DriveSettingsPanel extends JPanel implements PreferencePanel {
    public DriveSettingsPanel() {
        setLayout(new GridLayout(3, 1));
        JLabel drivesDescription = new JLabel("Make the following Mac dick drives or folders available on the Windows-based computer::");
        add(drivesDescription);


        String[] folders = {"~\\", "~\\Music", "~\\Documents"};
        JComboBox foldersComboBox = new JComboBox(folders);
        add(foldersComboBox);

        JLabel warning = new JLabel("Someone with access to your Windows-based computer can open your Mac disk drives and folders. This is potentially unsafe.",
                Utilities.getNewImageIcon("resources/images/settings/warning.png"), JLabel.CENTER);
        add(warning);

        PreferencesFrame.setMaxW(480);
        PreferencesFrame.setMaxH(300);
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void doApplyAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doApplyAction:");
    }

    @Override
    public void doOKAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doOKAction:");
    }

    @Override
    public void doCancelAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doCancelAction:");
    }

    public void movedToFront () {
    }
}
