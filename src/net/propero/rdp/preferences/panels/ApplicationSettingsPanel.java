package net.propero.rdp.preferences.panels;

import net.propero.rdp.Options;
import net.propero.rdp.preferences.PreferencesFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/26/11
 * Time: 10:54 AM
 */
public class ApplicationSettingsPanel extends JPanel implements PreferencePanel {

    String originalApplicationPath = "";
    JTextField applicationPath;

    public ApplicationSettingsPanel() {
        originalApplicationPath = Options.getStartupCommand();

        setLayout(new GridLayout(3, 1));
        JCheckBox checkBox = new JCheckBox("Start only the following Windows-Based application when you log into the remote computer:");
        add(checkBox);

        JPanel helper = new JPanel(new GridLayout(3, 1));
        JLabel applicationDescription = new JLabel("Application path and file name:");
        applicationPath = new JTextField(originalApplicationPath);
        JLabel applicationExample = new JLabel("(Example: C:\\Program Files\\Program\\Program.exe)");
        helper.add(applicationDescription);
        helper.add(applicationPath);
        helper.add(applicationExample);
        add(helper);

        helper = new JPanel(new GridLayout(3, 1));
        JLabel directoryDescription = new JLabel("Working directory:");
        JTextField directoryPath = new JTextField("");
        JLabel directoryExample = new JLabel("(Example: C:\\Documents\\My Data)");
        helper.add(directoryDescription);
        helper.add(directoryPath);
        helper.add(directoryExample);
        add(helper);

        PreferencesFrame.setMaxW(480);
        PreferencesFrame.setMaxH(400);
    }

    @Override
    public boolean isDirty() {
        return !originalApplicationPath.equals(applicationPath.getText());
    }

    @Override
    public void doApplyAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doApplyAction:");
        Options.setStartupCommand(applicationPath.getText());
    }

    @Override
    public void doOKAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doOKAction:");
        Options.setStartupCommand(applicationPath.getText());
    }

    @Override
    public void doCancelAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doCancelAction:");
    }

    public void movedToFront () {
    }
}
