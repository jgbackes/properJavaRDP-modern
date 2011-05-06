package net.propero.rdp.preferences.panels;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/27/11
 * Time: 11:26 PM
 */
public interface PreferencePanel {

    public boolean isDirty();

    public void doApplyAction();

    public void doOKAction();

    public void doCancelAction();

    public void movedToFront();
}
