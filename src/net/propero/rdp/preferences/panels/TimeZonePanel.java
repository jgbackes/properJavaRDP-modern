package net.propero.rdp.preferences.panels;

import net.propero.rdp.Options;
import net.propero.rdp.preferences.PreferencesFrame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 2/10/11
 * Time: 10:58 AM
 */
public class TimeZonePanel extends JPanel implements PreferencePanel {
    JList standardTimes;
    JList daylightTimes;

    public TimeZonePanel() {
        Date today = new Date();
        Map<String, String> standardTimeMap = new HashMap<String, String>();
        Map<String, String> daylightTimeMap = new HashMap<String, String>();

        // Get all time zone ids
        String[] zoneIds = TimeZone.getAvailableIDs();

        for (String zoneId : zoneIds) {
            // Get time zone by time zone id
            TimeZone tz = TimeZone.getTimeZone(zoneId);

            // Get the display name
            String shortName = tz.getDisplayName(tz.inDaylightTime(today), TimeZone.SHORT);
            String longName = tz.getDisplayName(tz.inDaylightTime(today), TimeZone.LONG);

            if (tz.useDaylightTime()) {
                daylightTimeMap.put(shortName, longName);
            } else {
                standardTimeMap.put(shortName, longName);
            }
        }

        JLabel timezoneDescription = new JLabel("Set both standard and daylight savings timezones");
        add(timezoneDescription);

        Vector <String> times;

        times = new Vector<String>(standardTimeMap.values());
        standardTimes = new JList(times);
        standardTimes.setSelectedValue(Options.getTimezoneName(), true);
        standardTimes.setVisibleRowCount(4);


        JPanel standardContainer = new JPanel();
        TitledBorder border = new TitledBorder(
                new LineBorder(Color.black),
                "Standard times",
                TitledBorder.LEFT,
                TitledBorder.TOP);
        standardContainer.setBorder(border);
        JScrollPane standardTimesScrollPane = new JScrollPane(standardTimes);
        standardTimesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        standardContainer.add(standardTimesScrollPane);
        add(standardContainer);

        times = new Vector<String>(daylightTimeMap.values());
        daylightTimes = new JList(times);
        daylightTimes.setSelectedValue(Options.getTimezoneDaylightSavingsName(), true);
        daylightTimes.setVisibleRowCount(4);

        JPanel daylightContainer = new JPanel();
        border = new TitledBorder(
                new LineBorder(Color.black),
                "Daylight times",
                TitledBorder.LEFT,
                TitledBorder.TOP);
        daylightContainer.setBorder(border);
        JScrollPane daylightTimesScrollPane = new JScrollPane(daylightTimes);
        daylightContainer.add(daylightTimesScrollPane);
        add(daylightContainer);

        PreferencesFrame.setMaxW(480);
        PreferencesFrame.setMaxH(600);
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void doApplyAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doApplyAction:");
        Options.setTimezoneName(standardTimes.getSelectedValue().toString());
        Options.setTimezoneDaylightSavingsName(daylightTimes.getSelectedValue().toString());
    }

    @Override
    public void doOKAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doOKAction:");
        Options.setTimezoneName(standardTimes.getSelectedValue().toString());
        Options.setTimezoneDaylightSavingsName(daylightTimes.getSelectedValue().toString());
    }

    @Override
    public void doCancelAction() {
        String className = this.getClass().getName();
        System.out.println(className + ":doCancelAction:");
    }

    public void movedToFront () {
        standardTimes.ensureIndexIsVisible(standardTimes.getSelectedIndex());
        daylightTimes.ensureIndexIsVisible(daylightTimes.getSelectedIndex());
    }
}
