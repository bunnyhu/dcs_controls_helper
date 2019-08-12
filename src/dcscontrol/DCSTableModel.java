package dcscontrol;

import javafx.beans.property.SimpleStringProperty;

/**
 * POJO model for storing the DCS control informations
 */
public class DCSTableModel implements Comparable {    
    /** Joystick/keyboard command like JOY_BTN1 */
    private SimpleStringProperty bindkey;
    /** DCS action like "Landing gear Up" */
    private SimpleStringProperty action;
    /** DCS action group like "Weapons" */
    private SimpleStringProperty group;
    /** device name from filename */
    private SimpleStringProperty device;
    /** PDF form field key, must be same like bindkey or empty */
    private SimpleStringProperty formkey;

    /**
     * Create and fill a POJO object
     * @param bindkey Joystick/keyboard command like JOY_BTN1
     * @param action DCS action like "Landing gear Up"
     * @param group DCS action group like "Flight control"
     * @param device device name like "Saitek Cyborg 3D" for grouping bind keys
     * @param formkey PDF form field key, must be same like bindkey or empty
     */
    public DCSTableModel(String bindkey, String action, String group, String device, String formkey) {
        setBindkey(bindkey);
        setAction(action);
        setGroup(group);
        setDevice(device);
        setFormkey(formkey);
    }

    /** Joystick/keyboard command like JOY_BTN1 */
    public String getBindkey() {
        return bindkey.get();
    }

    /** Joystick/keyboard command like JOY_BTN1 */
    public void setBindkey(String bindkey) {
        this.bindkey = new SimpleStringProperty(bindkey);
    }

    /** DCS action like "Landing gear Up" */
    public String getAction() {
        return action.get();
    }

    /** DCS action like "Landing gear Up" */
    public void setAction(String action) {
        this.action = new SimpleStringProperty(action);
    }

    /** DCS action group like "Weapons" */
    public String getGroup() {
        return group.get();
    }

    /** DCS action group like "Weapons" */
    public void setGroup(String group) {
        this.group = new SimpleStringProperty(group);
    }

    /** device name from filename */
    public String getDevice() {
        return device.get();
    }

    /** device name from filename */
    public void setDevice(String device) {
        this.device = new SimpleStringProperty(device);
    }

    /** PDF form field key, must be same like bindkey or empty */
    public String getFormkey() {
        return formkey.get();
    }

    /** PDF form field key, must be same like bindkey or empty */
    public void setFormkey(String formkey) {
        this.formkey = new SimpleStringProperty(formkey);
    }
    
    @Override
    public String toString() {
        return getBindkey() + " | " + getAction() + " | " + getDevice();
    }

    /**
     * Overrided compare method.
     * - first compate the device
     * <p>
     * - if same, and JOY_BTN compare the button number as number
     * <p>
     * - in any other variation do the normal string compare
     * @param o Other object
     * @return int 
     */
    @Override
    public int compareTo(Object o) {
        DCSTableModel other = (DCSTableModel) o;
        
        // Different device?
        if ( this.getDevice().compareTo(other.getDevice()) != 0 ) {
            return this.getDevice().compareTo(other.getDevice());
        }

        // JOY_BTN number as number (and not string)
        Integer n1;
        Integer n2;
        if (other.getBindkey().contains("JOY_BTN") && this.getBindkey().contains("JOY_BTN")) {
            try {
                n1 = Integer.parseInt(
                    this.getBindkey().substring(this.getBindkey().indexOf("JOY_BTN")+7, this.getBindkey().length()));
                n2 = Integer.parseInt(
                    other.getBindkey().substring(other.getBindkey().indexOf("JOY_BTN")+7, other.getBindkey().length()));
                return n1.compareTo(n2);
            } catch (NumberFormatException ex) {
                // Like JOY_BTN_POW_1 ... skip this error
            } catch (Exception ex) {
                System.err.println("Error in compateTo: " + ex.getMessage());
            }
        }
        
        // Default String compate
        return this.getBindkey().compareTo(other.getBindkey());
    }

    /**
     * Unpack the device name from file name ( before the { char )
     * <p>
     * Arduino Leonardo {0084B6C0-5546-11e9-8001-444553540000}.html = Arduino Leonardo
     * @param filename html file name
     * @return String
     */
    public static String getDeviceFromFilename(String filename) {
        if (!filename.isEmpty() && (filename.indexOf("{") != -1)) {
            filename = filename.substring(0, filename.indexOf("{"));
        }
        return filename;
    }
    
    
}
