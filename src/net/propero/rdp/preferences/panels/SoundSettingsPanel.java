package net.propero.rdp.preferences.panels;

import net.propero.rdp.preferences.PreferencesFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/26/11
 * Time: 3:41 PM
 */
public class SoundSettingsPanel extends JPanel implements PreferencePanel {
    public SoundSettingsPanel() {
        setLayout(new GridLayout(2, 1));
        JPanel cluster = new JPanel(new GridLayout(3, 1));
        JRadioButton onMacintosh = new JRadioButton("On the Macintosh computer only");
        JRadioButton onWindows = new JRadioButton("On the Windows-based computer only");
        JRadioButton noSound = new JRadioButton("Do not play sound");
        ButtonGroup group = new ButtonGroup();
        cluster.add(onMacintosh);
        cluster.add(onWindows);
        cluster.add(noSound);
        group.add(onMacintosh);
        group.add(onWindows);
        group.add(noSound);

        add(new JLabel("Play sound from the Windows-based computer:"));
        add(cluster);

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
