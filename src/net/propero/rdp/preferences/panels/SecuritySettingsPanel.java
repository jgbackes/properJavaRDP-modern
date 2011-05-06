package net.propero.rdp.preferences.panels;

import net.propero.rdp.preferences.PreferencesFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/26/11
 * Time: 10:56 AM
 */
public class SecuritySettingsPanel extends JPanel implements PreferencePanel {
    public SecuritySettingsPanel() {
        setLayout(new GridLayout(3, 1));

        JLabel message1 = new JLabel(
                "Authentication verifies that you are connecting to the correct Windows-based computer."
        );
        JLabel message2 = new JLabel(
                "Authentication might require you to type a full computer name to connect. Example: computer.example.com"
        );

        JPanel cluster = new JPanel(new GridLayout(3, 1));
        JRadioButton alwaysConnect = new JRadioButton("Always connect, even if authentication fails");
        JRadioButton warnMe = new JRadioButton("Warn me if authentication fails");
        JRadioButton DoNotConnect = new JRadioButton("Do not connect if authentication fails");
        ButtonGroup group = new ButtonGroup();
        cluster.add(alwaysConnect);
        cluster.add(warnMe);
        cluster.add(DoNotConnect);
        group.add(alwaysConnect);
        group.add(warnMe);
        group.add(DoNotConnect);


        add(message1);
        add(message2);
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
