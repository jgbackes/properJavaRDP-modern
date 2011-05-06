package net.propero.rdp.preferences.panels;

import net.propero.rdp.Options;
import net.propero.rdp.tools.SpringUtilities;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/26/11
 * Time: 10:55 AM
 */
public class LoginSettingsPanel extends JPanel implements PreferencePanel {

    private final static int textHeight = 12;
    private final static int textWidth = 120;

    JTextField userNameTextField;
    JTextField passwordTextField;
    JTextField domainTextField;
    JCheckBox addToKeychain;
    JCheckBox autoReconnect;
    JCheckBox packetLogging;

    public LoginSettingsPanel() {
        setLayout(new SpringLayout());

        JPanel allTextFields = new JPanel(new SpringLayout());
        userNameTextField = new JTextField(Options.getUserName());
        userNameTextField.setPreferredSize(new Dimension(textWidth, textHeight));
        JLabel userNameDescription = new JLabel("User name: ");
        allTextFields.add(userNameDescription);
        userNameDescription.setLabelFor(userNameTextField);
        userNameDescription.setHorizontalAlignment(SwingConstants.RIGHT);
        allTextFields.add(userNameTextField);

        passwordTextField = new JPasswordField(Options.getPassword());
        passwordTextField.setPreferredSize(new Dimension(textWidth, textHeight));
        JLabel passwordDescription = new JLabel("Password: ");
        allTextFields.add(passwordDescription);
        passwordDescription.setLabelFor(passwordTextField);
        passwordDescription.setHorizontalAlignment(SwingConstants.RIGHT);
        allTextFields.add(passwordTextField);

        domainTextField = new JTextField(Options.getDomain());
        domainTextField.setPreferredSize(new Dimension(textWidth, textHeight));
        JLabel domainDescription = new JLabel("Domain: ");
        allTextFields.add(domainDescription);
        domainDescription.setLabelFor(domainTextField);
        domainDescription.setHorizontalAlignment(SwingConstants.RIGHT);
        allTextFields.add(domainTextField);


        //Lay out the text boxes panel.
        SpringUtilities.makeCompactGrid(allTextFields,
                3, 2,       //rows, cols
                6, 6,       //initX, initY
                6, 6);      //xPad, yPad


        JPanel allCheckboxes = new JPanel(new SpringLayout());
        addToKeychain = new JCheckBox("Add user information to your keychain");
        autoReconnect = new JCheckBox("Reconnect automatically if disconnected");
        packetLogging = new JCheckBox("Enable packet logging");
        packetLogging.setSelected(Options.isDebugHexDump());
        allCheckboxes.add(addToKeychain);
        allCheckboxes.add(autoReconnect);
        allCheckboxes.add(packetLogging);

        //Lay out the check panel.
        SpringUtilities.makeCompactGrid(allCheckboxes,
                3, 1,       //rows, cols
                6, 6,       //initX, initY
                6, 6);      //xPad, yPad

        JLabel panelDescription = new JLabel("To log in automatically to the Windows-based computer, type your user information:");

        this.add(panelDescription); // Description of the panel
        this.add(allTextFields);    // All of the text fields and labels
        this.add(allCheckboxes);    // Checkboxes
        this.add(new JPanel());     // Padding

        //Lay out the text boxes panel.
        SpringUtilities.makeCompactGrid(this,
                4, 1,       //rows, cols
                6, 6,       //initX, initY
                6, 6);      //xPad, yPad
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void doApplyAction() {
        Options.setUserName(userNameTextField.getText());
        Options.setPassword(passwordTextField.getText());
        Options.setDomain(domainTextField.getText());
        Options.setDebugHexDump(packetLogging.isSelected());
    }

    @Override
    public void doOKAction() {
        doApplyAction();
    }

    @Override
    public void doCancelAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doCancelAction:");
    }

    @Override
    public void movedToFront() {
    }
}
