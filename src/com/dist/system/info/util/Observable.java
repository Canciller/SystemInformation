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
     * Add listener.
     * @param observer
     */
    public void addObserver(PropertyChangeListener observer) {
        support.addPropertyChangeListener(observer);
    }

    /**
     * Remove listener.
     * @param observer
     */
    public void removeObserver(PropertyChangeListener observer) {
        support.removePropertyChangeListener(observer);
    }

    /**
     * Notify listeners.
     * @param eventType
     * @param oldValue
     * @param newValue
     */
    public void notifyObservers(String eventType, Object oldValue, Object newValue)
    {
        support.firePropertyChange(eventType, oldValue, newValue);
    }
}
