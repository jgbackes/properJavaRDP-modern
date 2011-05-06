package net.propero.rdp.preferences.panels;

import net.propero.rdp.preferences.PreferencesFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/26/11
 * Time: 10:57 AM
 */
public class PrinterSettingsPanel extends JPanel implements PreferencePanel {
    public PrinterSettingsPanel() {
        setLayout(new GridLayout(2, 1));
        JCheckBox checkBox = new JCheckBox("Use a printer that is connect to the mac");
        add(checkBox);

        String[] printers = {"Hall HP Printer", "Dell in closet", "Lexmark Bld 5"};
        JComboBox printersComboBox = new JComboBox(printers);
        add(printersComboBox);

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
