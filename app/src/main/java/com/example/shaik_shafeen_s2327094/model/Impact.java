package com.example.shaik_shafeen_s2327094.model;

// Stores the impact a situation has on traffic.
public class Impact {
    private String delaysType = "";      // delays / longDelays / veryLongDelays ...
    private int delaySeconds = -1;
    private int lanesRestricted = -1;
    private int operationalLanes = -1;

    // Getter methods to access the impact details.
    public String getDelaysType() { return delaysType; }
    public int getDelaySeconds() { return delaySeconds; }
    public int getLanesRestricted() { return lanesRestricted; }
    public int getOperationalLanes() { return operationalLanes; }

    // Setter methods to update the impact details.
    public void setDelaysType(String v) { delaysType = v; }
    public void setDelaySeconds(int v) { delaySeconds = v; }
    public void setLanesRestricted(int v) { lanesRestricted = v; }
    public void setOperationalLanes(int v) { operationalLanes = v; }

    // Checks if any impact information has been set.
    public boolean isEmpty() {
        return delaysType.isEmpty() && delaySeconds < 0
                && lanesRestricted < 0 && operationalLanes < 0;
    }
}
