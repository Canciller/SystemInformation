package com.dist.system.info.client;

import com.dist.system.info.util.Observable;
import org.json.JSONObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class SystemInfo extends Observable implements PropertyChangeListener {
    /**
     * Get system information.
     */
    public JSONObject get() {
        // TODO: Get real information.
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("cpu", "Intel Core i5");
        jsonObject.put("ram", "4GB");
        jsonObject.put("disk", "1TB");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String event = evt.getPropertyName();

        switch (event) {
            case "client:connected": {
                notify(
                        "system:info:ready",
                        evt.getOldValue(),
                        get());
                break;
            }
            default: break;
        }
    }
}
