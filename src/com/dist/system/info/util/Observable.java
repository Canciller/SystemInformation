package com.dist.system.info.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class Observable {
    protected PropertyChangeSupport support;

    /**
     * Observable constructor.
     */
    public Observable() {
        support = new PropertyChangeSupport(this);
    }

    /**
     * Add change listener.
     * @param pcl
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    /**
     * Remove change listener.
     * @param pcl
     */
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }

    /**
     * Notify listeners.
     * @param type
     * @param oldValue
     * @param newValue
     */
    public void notify(String type, Object oldValue, Object newValue)
    {
        support.firePropertyChange(type, oldValue, newValue);
    }
}
