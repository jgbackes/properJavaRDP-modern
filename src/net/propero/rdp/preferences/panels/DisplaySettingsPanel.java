package net.propero.rdp.preferences.panels;

import net.propero.rdp.Options;
import net.propero.rdp.preferences.PreferencesFrame;
import net.propero.rdp.tools.SpringUtilities;

import javax.swing.*;
import java.awt.*;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/26/11
 * Time: 10:54 AM
 */
public class DisplaySettingsPanel extends JPanel implements PreferencePanel {

    JComboBox screenSizeChooser;
    JComboBox colorChooser;
    JComboBox locationChooser;
    JCheckBox showBackground;
    JCheckBox showSmoothing;
    JCheckBox showLiveDragging;
    JCheckBox showAnimation;
    JCheckBox showThemes;
    JCheckBox allowCaching;
    JCheckBox showMenu;

    private int screenWidth;
    private int screenHeight;


    public DisplaySettingsPanel() {
        screenWidth = Options.getWidth();
        screenHeight = Options.getHeight();

        setLayout(new SpringLayout());

        JPanel allDropDowns = new JPanel(new SpringLayout());

        String[] screenSizes = {"640x480", "800x600", "1024x768", "1280x1024", "1600x1200"};
        screenSizeChooser = new JComboBox(screenSizes);
        screenSizeChooser.setSelectedItem("" + screenWidth + "x" + screenHeight);

        JLabel sizeLabel = new JLabel("Size: ");
        allDropDowns.add(sizeLabel);
        sizeLabel.setLabelFor(screenSizeChooser);
        sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        allDropDowns.add(screenSizeChooser);

        String[] colors = {"Thousands", "Millions"};
        colorChooser = new JComboBox(colors);

        JLabel colorLabel = new JLabel("Colors: ");
        allDropDowns.add(colorLabel);
        colorLabel.setLabelFor(colorChooser);
        colorLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        allDropDowns.add(colorChooser);

        String[] locations = {"Main display", "Secondary display"};
        JComboBox locationChooser = new JComboBox(locations);

        JLabel locationLabel = new JLabel("Open on: ");
        allDropDowns.add(locationLabel);
        locationLabel.setLabelFor(locationChooser);
        locationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        allDropDowns.add(locationChooser);

        //Lay out the drop downs panel.
        SpringUtilities.makeCompactGrid(allDropDowns,
                3, 2,       //rows, cols
                6, 6,       //initX, initY
                6, 6);      //xPad, yPad

        JPanel allCheckBoxes = new JPanel(new SpringLayout());
        showBackground = new JCheckBox("Show desktop background");
        showSmoothing = new JCheckBox("Show font smoothing");
        showLiveDragging = new JCheckBox("Show contents of window while dragging");
        showAnimation = new JCheckBox("Show menu and window animation");
        showThemes = new JCheckBox("Show themes");
        allowCaching = new JCheckBox("Allow bitmap caching");
        showMenu = new JCheckBox("Do not show the Mac menu bar and the Dock in full-screen mode");
        allCheckBoxes.add(showBackground);
        allCheckBoxes.add(showSmoothing);
        allCheckBoxes.add(showLiveDragging);
        allCheckBoxes.add(showAnimation);
        allCheckBoxes.add(showThemes);
        allCheckBoxes.add(allowCaching);
        allCheckBoxes.add(showMenu);
        //Lay out the drop downs panel.
        SpringUtilities.makeCompactGrid(allCheckBoxes,
                7, 1,       //rows, cols
                6, 6,       //initX, initY
                6, 6);      //xPad, yPad

        add(allDropDowns);
        add(allCheckBoxes);

        //Lay out the panel.
        SpringUtilities.makeCompactGrid(this,
                2, 1,       //rows, cols
                6, 6,       //initX, initY
                6, 6);      //xPad, yPad

        PreferencesFrame.setMaxW(480);
        PreferencesFrame.setMaxH(500);
    }

    public void setDimensionsFromString(String dimensions) {
        StringTokenizer tokenizer = new StringTokenizer(dimensions, "x");
        String width = (String) tokenizer.nextElement();
        String height = (String) tokenizer.nextElement();
        screenWidth = Integer.parseInt(width);
        screenHeight = Integer.parseInt(height);
    }

    public String getDimensions() {
        return screenWidth + "x" + screenHeight;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void doApplyAction() {
        System.out.println("DisplaySettingsPanel:doApplyAction: getSelectedItem() = " + screenSizeChooser.getSelectedItem());
        setDimensionsFromString((String) screenSizeChooser.getSelectedItem());
        Options.setWidth(screenWidth);
        Options.setHeight(screenHeight);
    }

    @Override
    public void doOKAction() {
        System.out.println("DisplaySettingsPanel:doOKAction: getSelectedItem() = " + screenSizeChooser.getSelectedItem());
        setDimensionsFromString((String) screenSizeChooser.getSelectedItem());
        Options.setWidth(screenWidth);
        Options.setHeight(screenHeight);
    }

    @Override
    public void doCancelAction() {
        System.out.println("DisplaySettingsPanel:doCancelAction: getSelectedItem() = " + screenSizeChooser.getSelectedItem());
    }

    public void movedToFront() {
    }
}
