package com.dist.system.info.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public abstract class Observer extends Observable implements PropertyChangeListener {
    public abstract void update(String eventType, Object oldValue, Object newValue);

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        update(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
    }
}
