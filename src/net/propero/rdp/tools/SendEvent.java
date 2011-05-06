/* SendEvent.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.4 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:40 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 * 
 * (See gpl.txt for details of the GNU General Public License.)
 * 
 */
package net.propero.rdp.tools;

import net.propero.rdp.Input;
import net.propero.rdp.Rdp;

import javax.swing.*;
import java.awt.*;

import static java.awt.GridBagConstraints.*;
import static java.lang.Integer.*;

public class SendEvent extends JFrame {

    private javax.swing.JPanel jContentPane = null;

    private JTextField inputTypeField = null;
    private JTextField flagsField = null;
    private JTextField param1Field = null;
    private JTextField param2Field = null;
    private JButton sendEventButton = null;
    private JTextField flagMaskField = null;
    private JButton applyMaskButton = null;

    Rdp rdp;

    /**
     * This is the default constructor
     * @param rdp the session object that will receive the events created by this UI
     */
    public SendEvent(Rdp rdp) {
        super();
        this.setSize(300, 200);
        this.setContentPane(getJContentPane());
        this.setTitle("Send Event");
        this.rdp = rdp;
    }

    /**
     * This method initializes jContentPane
     *
     * @return JPanel of input items
     */
    private javax.swing.JPanel getJContentPane() {
        if (jContentPane == null) {
            JLabel jLabel3 = new JLabel();
            JLabel jLabel2 = new JLabel();
            JLabel jLabel1 = new JLabel();
            JLabel jLabel = new JLabel();
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout(new GridBagLayout());
            gridBagConstraints1.gridx = 1;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.weightx = 1.0;
            gridBagConstraints1.fill = HORIZONTAL;
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.gridy = 0;
            jLabel.setText("Input Type");
            gridBagConstraints3.gridx = 1;
            gridBagConstraints3.gridy = 1;
            gridBagConstraints3.weightx = 1.0;
            gridBagConstraints3.fill = HORIZONTAL;
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.gridy = 1;
            jLabel1.setText("Flags");
            gridBagConstraints5.gridx = 1;
            gridBagConstraints5.gridy = 3;
            gridBagConstraints5.weightx = 1.0;
            gridBagConstraints5.fill = HORIZONTAL;
            gridBagConstraints6.gridx = 0;
            gridBagConstraints6.gridy = 3;
            jLabel2.setText("Param 1");
            gridBagConstraints7.gridx = 1;
            gridBagConstraints7.gridy = 5;
            gridBagConstraints7.weightx = 1.0;
            gridBagConstraints7.fill = HORIZONTAL;
            gridBagConstraints8.gridx = 0;
            gridBagConstraints8.gridy = 5;
            jLabel3.setText("Param 2");
            gridBagConstraints9.gridx = 1;
            gridBagConstraints9.gridy = 6;
            gridBagConstraints10.gridx = 1;
            gridBagConstraints10.gridy = 2;
            gridBagConstraints10.weightx = 1.0;
            gridBagConstraints10.fill = HORIZONTAL;
            gridBagConstraints11.gridx = 2;
            gridBagConstraints11.gridy = 2;
            jContentPane.add(getInputTypeField(), gridBagConstraints1);
            jContentPane.add(getParam1Field(), gridBagConstraints5);
            jContentPane.add(jLabel, gridBagConstraints2);
            jContentPane.add(getFlagsField(), gridBagConstraints3);
            jContentPane.add(jLabel1, gridBagConstraints4);
            jContentPane.add(jLabel2, gridBagConstraints6);
            jContentPane.add(getParam2Field(), gridBagConstraints7);
            jContentPane.add(jLabel3, gridBagConstraints8);
            jContentPane.add(getNewSendEventButton(), gridBagConstraints9);
            jContentPane.add(getFlagMaskField(), gridBagConstraints10);
            jContentPane.add(getNewApplyMaskButton(), gridBagConstraints11);
        }
        return jContentPane;
    }

    /**
     * This method initializes inputTypeField
     *
     * @return javax.swing.JTextField
     */
    private JTextField getInputTypeField() {
        if (inputTypeField == null) {
            inputTypeField = new JTextField();
        }
        return inputTypeField;
    }

    /**
     * This method initializes flagsField
     *
     * @return javax.swing.JTextField
     */
    private JTextField getFlagsField() {
        if (flagsField == null) {
            flagsField = new JTextField();
        }
        return flagsField;
    }

    /**
     * This method initializes param1Field
     *
     * @return javax.swing.JTextField
     */
    private JTextField getParam1Field() {
        if (param1Field == null) {
            param1Field = new JTextField();
        }
        return param1Field;
    }

    /**
     * This method initializes param2Field
     *
     * @return javax.swing.JTextField
     */
    private JTextField getParam2Field() {
        if (param2Field == null) {
            param2Field = new JTextField();
        }
        return param2Field;
    }

    /**
     * Create a button for sending the event
     *
     * @return javax.swing.JButton
     */
    private JButton getNewSendEventButton() {
        if (sendEventButton == null) {
            sendEventButton = new JButton();
            sendEventButton.setText("Send Event");
            sendEventButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (rdp != null) {
                        rdp.sendInput(Input.getTime(),
                                decode(inputTypeField.getText()),
                                decode(flagsField.getText()),
                                decode(param1Field.getText()),
                                decode(param2Field.getText()));
                    }
                }
            });
        }
        return sendEventButton;
    }

    /**
     * This method initializes flagMaskField
     *
     * @return javax.swing.JTextField
     */
    private JTextField getFlagMaskField() {
        if (flagMaskField == null) {
            flagMaskField = new JTextField();
        }
        return flagMaskField;
    }

    /**
     * Create a button that performs the Apply Button task
     *
     * @return javax.swing.JButton
     */
    private JButton getNewApplyMaskButton() {
        if (applyMaskButton == null) {
            applyMaskButton = new JButton();
            applyMaskButton.setText("Apply Mask");
            applyMaskButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // apply the mask to the flags field
                    flagsField.setText("0x" + toHexString(decode(flagsField.getText()) | decode(flagMaskField.getText())));
                    flagMaskField.setText("");
                }
            });
        }
        return applyMaskButton;
    }
}
